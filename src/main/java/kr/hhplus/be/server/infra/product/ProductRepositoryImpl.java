package kr.hhplus.be.server.infra.product;

import kr.hhplus.be.server.domain.product.entity.Product;
import kr.hhplus.be.server.domain.product.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepositoryImpl extends JpaRepository<Product, Long>, ProductRepository {
    @Override
    default Optional<Product> getProduct(Long productId) {
        return findById(productId);
    }

    @Override
    default Page<Product> getProducts(PageRequest pageRequest) {
        return findAll((Pageable) pageRequest);
    }
}
