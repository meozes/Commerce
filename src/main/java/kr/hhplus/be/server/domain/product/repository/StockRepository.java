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

public interface StockRepository {
    Stock getStock(Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "javax.persistence.lock.timeout", value = "3000")})
    @Query("select s from Stock s where s.product.id = :productId")
    Stock getStockWithLock(@Param("productId") Long productId);

    List<Stock> getStocks(Collection<Long> productIds);

    Stock save(Stock stock);

}
