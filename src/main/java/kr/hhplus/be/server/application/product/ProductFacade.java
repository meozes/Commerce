package kr.hhplus.be.server.application.product;

import kr.hhplus.be.server.domain.order.usecase.OrderService;
import kr.hhplus.be.server.domain.product.dto.ProductRankInfo;
import kr.hhplus.be.server.domain.product.entity.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class ProductFacade {

    private OrderService orderService;

    public List<ProductRankInfo> getTopProducts() {
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.minusDays(1);
        LocalDate startDate = endDate.minusDays(3);

        List<ProductRankInfo> products = orderService.getTopProductsByOrderDate(startDate, endDate);

        return IntStream.range(0, products.size())
                .mapToObj(i -> products.get(i).withRank(i + 1))
                .collect(Collectors.toList());
    }

}
