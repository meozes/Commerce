package kr.hhplus.be.server.interfaces.product.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.hhplus.be.server.application.product.ProductFacade;
import kr.hhplus.be.server.domain.product.dto.ProductInfo;
import kr.hhplus.be.server.domain.product.dto.ProductRankInfo;
import kr.hhplus.be.server.domain.product.dto.ProductSearch;
import kr.hhplus.be.server.domain.product.dto.ProductSearchQuery;
import kr.hhplus.be.server.domain.product.usecase.ProductService;
import kr.hhplus.be.server.interfaces.common.ApiResponse;
import kr.hhplus.be.server.interfaces.product.response.ProductResponse;

import kr.hhplus.be.server.interfaces.product.response.TopProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@Tag(name = "상품 API", description = "상품 조회 API")
@RestController
@RequestMapping("api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductFacade productFacade;

    @Operation(summary = "상품 단건 조회", description = "해당 상품을 조회합니다.")
    @GetMapping("/{productId}")
    public ApiResponse<ProductResponse> getProduct(
            @Parameter(name = "productId", description = "상품 ID", required = true)
            @PathVariable("productId") Long productId
    ) {
        ProductSearch productSearch = ProductSearch.of(productId);
        ProductInfo info = productService.getProduct(productSearch);
        return ApiResponse.ok(ProductResponse.from(info));
    }


    @Operation(summary = "상품 다건 조회", description = "전체 상품을 페이징하여 조회합니다.")
    @GetMapping()
    public ApiResponse<Page<ProductResponse>> getProducts(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        ProductSearchQuery query = ProductSearchQuery.of(page, size);
        Page<ProductInfo> productInfos = productService.getProducts(query);
        Page<ProductResponse> responses = productInfos.map(ProductResponse::from);
        return ApiResponse.ok(responses);
    }


    @Operation(summary = "3일간 인기 5상품 조회", description = "3일간의 인기있는 상품 다섯개를 조회합니다.")
    @GetMapping("/top")
    public ApiResponse<TopProductResponse> getTop5Products()
    {
        List<ProductRankInfo> info = productFacade.getTopProducts();
        return ApiResponse.ok(TopProductResponse.of(info));
    }
}
