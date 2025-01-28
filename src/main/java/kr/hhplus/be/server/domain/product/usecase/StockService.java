package kr.hhplus.be.server.domain.product.usecase;

import org.springframework.transaction.annotation.Transactional;
import kr.hhplus.be.server.common.aop.annotation.DistributedLock;
import kr.hhplus.be.server.common.aop.annotation.Monitored;
import kr.hhplus.be.server.common.aop.annotation.Monitoring;
import kr.hhplus.be.server.domain.coupon.usecase.CouponControlService;
import kr.hhplus.be.server.domain.order.dto.OrderItemCommand;
import kr.hhplus.be.server.domain.order.entity.Order;
import kr.hhplus.be.server.domain.order.entity.OrderItem;
import kr.hhplus.be.server.domain.order.repository.OrderRepository;
import kr.hhplus.be.server.domain.order.type.OrderStatusType;
import kr.hhplus.be.server.domain.order.usecase.OrderControlService;
import kr.hhplus.be.server.domain.product.entity.Stock;
import kr.hhplus.be.server.domain.product.repository.StockRepository;
import kr.hhplus.be.server.interfaces.common.type.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.Comparator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;


import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {

    private final ProductService productService;
    private final StockRepository stockRepository;
    private final OrderControlService orderControlService;
    private final CouponControlService couponControlService;
    private final OrderRepository orderRepository;
    private final StockRestoreService stockRestoreService;

    /**
     * 재고 복구하기
     */
    @Monitored
    @Monitoring
    @DistributedLock(lockName = "stock_lock", waitTime = 5000, leaseTime = 10000)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void restoreStock(List<OrderItem> orderItems, Long orderId, Long userId) {
        stockRestoreService.executeRestore(orderItems);
//        Order order = orderRepository.getOrder(orderId)
//                .orElseThrow(() -> new NoSuchElementException(ErrorCode.ORDER_NOT_FOUND.getMessage()));
//
//        if (order.getOrderStatus().equals(OrderStatusType.PENDING)) {
//            stockRestoreService.executeRestore(orderItems);
//        }
    }

    /**
     * 재고 차감하기
     */
    @Monitored
    @Monitoring
    @DistributedLock(lockName = "stock_lock",  waitTime = 5000, leaseTime = 10000)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void validateAndDeductStock(List<OrderItemCommand> orderItems) {
        productService.validateProducts(orderItems);  // 상품 존재 확인

        log.info("[재고 차감 시작] orderItems={}", orderItems);
        orderItems.stream()
                .sorted(Comparator.comparing(OrderItemCommand::getProductId))
                .forEach(item -> {
                    Stock stock = stockRepository.getStock(item.getProductId())
                            .orElseThrow(() -> new NoSuchElementException(ErrorCode.PRODUCT_STOCK_NOT_FOUND.getMessage()));

                    log.info("[재고 차감 진행중] productId={}, requestQuantity={}, currentStock={}",
                            item.getProductId(),
                            item.getQuantity(),
                            stock.getRemainingStock());

                    if (stock.getRemainingStock() < item.getQuantity()) {
                        log.info("[재고 부족으로 차감 실패]");

                        throw new IllegalStateException(
                                String.format(ErrorCode.INSUFFICIENT_STOCK.getMessage() + " 상품ID: %d, 요청수량: %d, 재고수량: %d",
                                        item.getProductId(),
                                        item.getQuantity(),
                                        stock.getRemainingStock())
                        );
                    }
                    stock.deductStock(item.getQuantity());
                    stockRepository.save(stock);

                    log.info("[재고 차감 완료] productId={}, deductedQuantity={}, remainingStock={}",
                            item.getProductId(),
                            item.getQuantity(),
                            stock.getRemainingStock());
                });
    }


}
