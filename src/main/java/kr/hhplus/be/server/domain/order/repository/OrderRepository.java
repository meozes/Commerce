package kr.hhplus.be.server.domain.order.repository;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import kr.hhplus.be.server.domain.order.dto.UserOrderInfo;
import kr.hhplus.be.server.domain.order.entity.Order;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    Order save(Order order);

    Optional<Order> getOrder(Long orderId);

    List<Order> findAll();

    Optional<Order> findById(Long id);

    @Query("select o from Order o where o.userId = :userId order by o.createdAt desc limit 1")
    Order getUserLatestOrder(@Param("userId") Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({
            @QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"),
            @QueryHint(name = "jakarta.persistence.query.timeout", value = "3000")
    })
    @Query("select o from Order o where o.id = :orderId")
    Optional<Order> getOrderWithLock(Long orderId);
}
