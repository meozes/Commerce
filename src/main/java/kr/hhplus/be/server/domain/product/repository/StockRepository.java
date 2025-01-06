package kr.hhplus.be.server.domain.product.repository;

import kr.hhplus.be.server.domain.product.entity.Stock;

import java.util.Collection;
import java.util.List;

public interface StockRepository {
    Stock getStock(Long productId);
    List<Stock> getStocks(Collection<Long> productIds);
}
