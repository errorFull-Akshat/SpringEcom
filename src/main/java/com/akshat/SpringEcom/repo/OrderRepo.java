package com.akshat.SpringEcom.repo;

import com.akshat.SpringEcom.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepo extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderId(String orderById);

    // Loads orders, their items and the items' products in a single query
    @Query("SELECT o FROM orders o " +
            "LEFT JOIN FETCH o.orderItems oi " +
            "LEFT JOIN FETCH oi.product " +
            "ORDER BY o.id, oi.id")
    List<Order> findAllWithItems();
}
