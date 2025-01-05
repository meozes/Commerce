package kr.hhplus.be.server.domain.product.repository;

import kr.hhplus.be.server.domain.product.entity.Stock;

import java.util.Collection;
import java.util.List;

public interface StockRepository {
    Stock findByProductId(Long productId);
    List<Stock> findByProductId(Collection<Long> productIds);
}
