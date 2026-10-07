package com.akshat.SpringEcom.service;

import com.akshat.SpringEcom.model.Order;
import com.akshat.SpringEcom.model.OrderItem;
import com.akshat.SpringEcom.model.Product;
import com.akshat.SpringEcom.model.dto.OrderItemRequest;
import com.akshat.SpringEcom.model.dto.OrderItemResponse;
import com.akshat.SpringEcom.model.dto.OrderRequest;
import com.akshat.SpringEcom.model.dto.OrderResponse;
import com.akshat.SpringEcom.repo.OrderRepo;
import com.akshat.SpringEcom.repo.ProductRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OrderService {

    @Autowired
    private ProductRepo productRepo;
    @Autowired
    private OrderRepo orderRepo;

    @Transactional
    public OrderResponse placeOrder(OrderRequest request) {

        if (request.items() == null || request.items().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order must contain at least one item");
        }

        // Row-lock every product up front, in ascending id order, so concurrent orders
        // can't both pass the stock check or deadlock on each other's locks
        Map<Integer, Product> lockedProducts = new HashMap<>();
        request.items().stream()
                .map(OrderItemRequest::productId)
                .distinct()
                .sorted()
                .forEach(productId -> lockedProducts.put(productId,
                        productRepo.findByIdForUpdate(productId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                        "Product not found: " + productId))));

        Order order = new Order();
        String orderId = "ORD" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        order.setOrderId(orderId);
        order.setCustomerName(request.customerName());
        order.setEmail(request.email());
        order.setStatus("PLACED");
        order.setOrderDate(LocalDate.now());

        List<OrderItem> orderItems = new ArrayList<>();
        for (OrderItemRequest itemReq : request.items()) {

            if (itemReq.quantity() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Quantity must be positive for product " + itemReq.productId());
            }

            Product product = lockedProducts.get(itemReq.productId());

            if (product.getStockQuantity() < itemReq.quantity()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Insufficient stock for " + product.getName() + ": requested "
                                + itemReq.quantity() + ", available " + product.getStockQuantity());
            }

            product.setStockQuantity(product.getStockQuantity() - itemReq.quantity());
            if (product.getStockQuantity() == 0) {
                product.setProductAvailable(false);
            }
            productRepo.save(product);

            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .quantity(itemReq.quantity())
                    .totalPrice(product.getPrice().multiply(BigDecimal.valueOf(itemReq.quantity())))
                    .order(order)
                    .build();
            orderItems.add(orderItem);

        }

        order.setOrderItems(orderItems);
        Order savedOrder = orderRepo.save(order);

        List<OrderItemResponse> itemResponses = new ArrayList<>();
        for (OrderItem item : order.getOrderItems()) {
            OrderItemResponse orderItemResponse = new OrderItemResponse(
                    item.getProduct().getName(),
                    item.getQuantity(),
                    item.getTotalPrice()
            );
            itemResponses.add(orderItemResponse);
        }

        OrderResponse orderResponse = new OrderResponse(
                savedOrder.getOrderId(),
                savedOrder.getCustomerName(),
                savedOrder.getEmail(),
                savedOrder.getStatus(),
                savedOrder.getOrderDate(),
                itemResponses
        );

        return orderResponse;
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrderResponses() {

        List<Order> orders = orderRepo.findAllWithItems();
        List<OrderResponse> orderResponses = new ArrayList<>();

        for (Order order : orders) {


            List<OrderItemResponse> itemResponses = new ArrayList<>();

            for (OrderItem item : order.getOrderItems()) {
                OrderItemResponse orderItemResponse = new OrderItemResponse(
                        item.getProduct().getName(),
                        item.getQuantity(),
                        item.getTotalPrice()
                );
                itemResponses.add(orderItemResponse);

            }
            OrderResponse orderResponse = new OrderResponse(
                    order.getOrderId(),
                    order.getCustomerName(),
                    order.getEmail(),
                    order.getStatus(),
                    order.getOrderDate(),
                    itemResponses
            );
            orderResponses.add(orderResponse);
        }

        return orderResponses;
    }
}
