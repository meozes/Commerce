package kr.hhplus.be.server.infra.product;

import kr.hhplus.be.server.domain.product.entity.Stock;
import kr.hhplus.be.server.domain.product.repository.StockRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface StockRepositoryImpl extends JpaRepository<Stock, Long>, StockRepository {
    @Override
    default Stock getStock(Long productId) {
        return findByProductId(productId);
    }

    @Override
    default List<Stock> getStocks(Collection<Long> productIds) {
        return findByProductIdIn(productIds);
    }

    Stock findByProductId(Long productId);
    List<Stock> findByProductIdIn(Collection<Long> productIds);

    @Override
    default Stock save(Stock stock) {
        return saveAndFlush(stock);
    }

}