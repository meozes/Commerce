package kr.hhplus.be.server.domain.order.validation;

import jakarta.persistence.EntityNotFoundException;
import kr.hhplus.be.server.domain.order.dto.OrderCommand;
import kr.hhplus.be.server.domain.order.dto.OrderItemCommand;
import kr.hhplus.be.server.domain.product.dto.ProductInfo;
import kr.hhplus.be.server.domain.product.dto.ProductSearch;
import kr.hhplus.be.server.domain.product.exception.InsufficientStockException;
import kr.hhplus.be.server.domain.product.usecase.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderProductValidation {

    private final ProductService productService;

    public void handleProduct(OrderCommand command) {
        //상품 존재여부 확인
        //주문만큼의 재고 있는지 확인
        for (OrderItemCommand item : command.getOrderItems()) {
            ProductSearch productSearch = ProductSearch.of(item.getProductId());
            ProductInfo productInfo = productService.getProduct(productSearch);

            if (productInfo == null) {
                throw new EntityNotFoundException("상품을 찾을 수 없습니다. productId: " + item.getProductId());
            }

            Integer remainingStock = productInfo.getRemainingStock();
            if (remainingStock < item.getQuantity()) {
                throw new InsufficientStockException(
                        String.format("상품의 재고가 부족합니다. 상품ID: %d, 요청수량: %d, 재고수량: %d",
                                item.getProductId(),
                                item.getQuantity(),
                                remainingStock
                        )
                );
            }
        }
    }
}
