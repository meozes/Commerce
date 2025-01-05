package kr.hhplus.be.server.interfaces.product.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.hhplus.be.server.domain.product.entity.Product;
import kr.hhplus.be.server.domain.product.entity.Stock;
import kr.hhplus.be.server.interfaces.common.ApiResponse;
import kr.hhplus.be.server.interfaces.product.response.ProductResponse;
import kr.hhplus.be.server.interfaces.product.response.Top5ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Tag(name = "상품 API", description = "상품 조회 API")
@RestController
@RequestMapping("api/products")
public class ProductController {

    @Operation(summary = "상품 단건 조회", description = "해당 상품을 조회합니다.")
    @GetMapping("/{productId}")
    public ApiResponse<ProductResponse> getProduct(
            @Parameter(name = "productId", description = "상품 ID", required = true)
            @PathVariable("productId") Long productId
    ) {
        Product product = new Product(productId, "촉촉한 쿠키", 5000);
        Stock stock = new Stock(1L, product, 100, 30);
        return ApiResponse.ok(ProductResponse.of(product, stock));
    }


    @Operation(summary = "상품 다건 조회", description = "전체 상품을 페이징하여 조회합니다.")
    @GetMapping()
    public ApiResponse<Page<ProductResponse>> getProducts(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sort", defaultValue = "id") String sort
    ) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(sort));

        Product product1 = new Product(1L, "촉촉한 쿠키", 5000);
        Product product2 = new Product(2L, "촉촉한 우유", 10000);
        Stock stock1 = new Stock(1L, product1, 100, 30);
        Stock stock2 = new Stock(2L, product2, 200, 30);
        ProductResponse response1 = ProductResponse.of(product1, stock1);
        ProductResponse response2 = ProductResponse.of(product2, stock2);
        List<ProductResponse> responseList = Arrays.asList(response1, response2);

        int start = (int) pageRequest.getOffset();
        int end = Math.min((start + pageRequest.getPageSize()), responseList.size());

        Page<ProductResponse> pageResult = new PageImpl<>(
                responseList.subList(start, end),
                pageRequest,
                responseList.size()
        );
        return ApiResponse.ok(pageResult);
    }


    @Operation(summary = "3일간 인기 5상품 조회", description = "3일간의 인기있는 상품 다섯개를 조회합니다.")
    @GetMapping("/top")
    public ApiResponse<Top5ProductResponse> getTop5Products()
    {
        LocalDateTime today = LocalDateTime.now();
        LocalDateTime endDate = today.minusDays(1);
        LocalDateTime startDate = endDate.minusDays(3);

        Top5ProductResponse.ProductRankInfo product1 = new Top5ProductResponse.ProductRankInfo(1, 1L, "쿠키", 100, 30000);
        Top5ProductResponse.ProductRankInfo product2 = new Top5ProductResponse.ProductRankInfo(2, 2L, "우유", 100, 30000);
        Top5ProductResponse.ProductRankInfo product3 = new Top5ProductResponse.ProductRankInfo(3, 3L, "휴지", 100, 30000);
        List<Top5ProductResponse.ProductRankInfo> products = Arrays.asList(product1, product2, product3);

        return ApiResponse.ok(Top5ProductResponse.of(products, startDate, endDate));
    }
}
