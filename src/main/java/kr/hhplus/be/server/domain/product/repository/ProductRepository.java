package kr.hhplus.be.server.domain.product.repository;

import kr.hhplus.be.server.domain.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    Optional<Product> getProduct(Long productId);

    Page<Product> getProducts(PageRequest pageRequest);

    Product save(Product product);

    List<Product> findAll();

}
