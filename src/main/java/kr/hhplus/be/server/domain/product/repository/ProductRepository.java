package kr.hhplus.be.server.domain.product.repository;

import kr.hhplus.be.server.domain.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ProductRepository {
    Product findById(Long productId);

    List<Product> findTop5Products(LocalDateTime startDate, LocalDateTime endDate);

    Page<Product> findAll(PageRequest pageRequest);
}
