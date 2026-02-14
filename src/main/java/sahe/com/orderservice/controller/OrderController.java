package sahe.com.orderservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sahe.com.orderservice.dto.OrderRequest;
import sahe.com.orderservice.dto.OrderResponse;
import sahe.com.orderservice.dto.OrderStatusUpdateRequest;
import sahe.com.orderservice.model.Order;
import sahe.com.orderservice.service.OrderService;

import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    // GET http://localhost:8085/orders
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        log.info("GET /orders - Get all orders");
        List<OrderResponse> orders = orderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }

    // GET http://localhost:8085/orders/1
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER', 'CLIENT')")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long id) {
        log.info("GET /orders/{} - Get order by id", id);
        OrderResponse order = orderService.getOrderById(id);
        return ResponseEntity.ok(order);
    }

    // GET http://localhost:8085/orders/user/1
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT')")
    public ResponseEntity<List<OrderResponse>> getOrdersByUserId(@PathVariable Long userId) {
        log.info("GET /orders/user/{} - Get orders by user id", userId);
        List<OrderResponse> orders = orderService.getOrdersByUserId(userId);
        return ResponseEntity.ok(orders);
    }

    // GET http://localhost:8085/orders/status/PENDING
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    public ResponseEntity<List<OrderResponse>> getOrdersByStatus(@PathVariable Order.OrderStatus status) {
        log.info("GET /orders/status/{} - Get orders by status", status);
        List<OrderResponse> orders = orderService.getOrdersByStatus(status);
        return ResponseEntity.ok(orders);
    }

    // POST http://localhost:8085/orders
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT')")
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderRequest request) {
        log.info("POST /orders - Create order for user: {}", request.getUserId());
        OrderResponse createdOrder = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdOrder);
    }

    // PATCH http://localhost:8085/orders/1/status
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody OrderStatusUpdateRequest request) {
        log.info("PATCH /orders/{}/status - Update status to: {}", id, request.getStatus());
        OrderResponse updatedOrder = orderService.updateOrderStatus(id, request);
        return ResponseEntity.ok(updatedOrder);
    }

    // PATCH http://localhost:8085/orders/1/cancel
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT')")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable Long id) {
        log.info("PATCH /orders/{}/cancel - Cancel order", id);
        OrderResponse cancelledOrder = orderService.cancelOrder(id);
        return ResponseEntity.ok(cancelledOrder);
    }

    // DELETE http://localhost:8085/orders/1
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        log.info("DELETE /orders/{} - Delete order", id);
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }
}
