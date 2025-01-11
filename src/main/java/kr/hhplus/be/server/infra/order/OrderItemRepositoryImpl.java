package kr.hhplus.be.server.infra.order;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.hhplus.be.server.domain.order.entity.OrderItem;
import kr.hhplus.be.server.domain.order.repository.OrderItemRepository;
import kr.hhplus.be.server.domain.product.dto.ProductRankInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import kr.hhplus.be.server.domain.order.entity.QOrderItem;

@Repository
@RequiredArgsConstructor
public class OrderItemRepositoryImpl implements OrderItemRepository {
    private final OrderItemJpaRepository orderItemJpaRepository;
    private final JPAQueryFactory queryFactory;

    @Override
    public List<OrderItem> saveAll(List<OrderItem> orderItems) {
        return orderItemJpaRepository.saveAllAndFlush(orderItems);
    }

    @Override
    public List<OrderItem> getOrderItems(Long orderId) {
        return orderItemJpaRepository.findByOrderId(orderId);
    }

    @Override
    public List<ProductRankInfo> findTopProductsByOrderDate(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        return queryFactory
                .select(Projections.constructor(ProductRankInfo.class,
                        QOrderItem.orderItem.productId,
                        QOrderItem.orderItem.productName,
                        QOrderItem.orderItem.quantity.sum(),
                        QOrderItem.orderItem.productPrice))
                .from(QOrderItem.orderItem)
                .where(QOrderItem.orderItem.createdAt.between(startDateTime, endDateTime))
                .groupBy(QOrderItem.orderItem.productId,
                        QOrderItem.orderItem.productName,
                        QOrderItem.orderItem.productPrice)
                .orderBy(QOrderItem.orderItem.quantity.sum().desc())
                .limit(5)
                .fetch();
    }

    @Override
    public List<OrderItem> findAll() {
        return orderItemJpaRepository.findAll();
    }
}