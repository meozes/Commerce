package kr.hhplus.be.server.domain.product.repository;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import kr.hhplus.be.server.domain.product.entity.Stock;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StockRepository {
    Stock getStock(Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({
            @QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000"),
            @QueryHint(name = "jakarta.persistence.lock.scope", value = "EXTENDED")
    })
    @Query("select s from Stock s where s.product.id = :productId")
    Optional<Stock> getStockWithLock(@Param("productId") Long productId);

    List<Stock> getStocks(Collection<Long> productIds);

    Stock save(Stock stock);

    Optional<Stock> findById(Long id);

}
