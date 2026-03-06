package sahe.com.orderservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sahe.com.orderservice.client.*;
import sahe.com.orderservice.dto.OrderItemRequest;
import sahe.com.orderservice.dto.OrderRequest;
import sahe.com.orderservice.dto.OrderResponse;
import sahe.com.orderservice.dto.OrderStatusUpdateRequest;
import sahe.com.orderservice.model.Order;
import sahe.com.orderservice.model.OrderItem;
import sahe.com.orderservice.repository.OrderRepository;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final InventoryClient inventoryClient;

    public List<OrderResponse> getAllOrders() {
        log.info("Receiving all orders");
        return orderRepository.findAll()
                .stream()
                .map(OrderResponse::new)
                .collect(Collectors.toList());
    }

    public OrderResponse getOrderById(Long id) {
        log.info("Get order by id: {}", id);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
        return new OrderResponse(order);
    }

    public List<OrderResponse> getOrdersByUserId(Long userId) {
        log.info("Get orders by user ID: {}", userId);
        return orderRepository.findByUserId(userId)
                .stream()
                .map(OrderResponse::new)
                .collect(Collectors.toList());
    }

    public List<OrderResponse> getOrdersByStatus(Order.OrderStatus status) {
        log.info("Get orders by status: {}", status);
        return orderRepository.findByStatus(status)
                .stream()
                .map(OrderResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        log.info(": {}", request.getUserId());
        for (OrderItemRequest itemRequest : request.getItems()) {
            ProductResponse product = productClient.getProductById(itemRequest.getProductId());
            if (!product.getActive()) {
                throw new RuntimeException("Product " + product.getName() + " is not active");
            }
            AvailabilityResponse availability = inventoryClient.checkAvailability(
                    itemRequest.getProductId(),
                    itemRequest.getQuantity()
            );

            if (!availability.getAvailable()) {
                throw new RuntimeException("Insufficient stock of product: " + product.getName());
            }
        }

        Order order = new Order();
        order.setUserId(request.getUserId());
        order.setShippingAddress(request.getShippingAddress());
        order.setShippingCity(request.getShippingCity());
        order.setShippingCountry(request.getShippingCountry());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setNotes(request.getNotes());
        order.setStatus(Order.OrderStatus.PENDING);

        for (OrderItemRequest itemRequest : request.getItems()) {
            ProductResponse product = productClient.getProductById(itemRequest.getProductId());

            OrderItem orderItem = new OrderItem();
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setUnitPrice(product.getPrice());
            orderItem.calculateSubtotal();
            order.addItem(orderItem);
        }

        order.calculateTotal();

        Order savedOrder = orderRepository.save(order);
        log.info("Order created with id: {}", savedOrder.getId());

        for (OrderItem item : savedOrder.getItems()) {
            try {
                log.info("Attempting to reduce stock for product {} by {} units",
                        item.getProductId(), item.getQuantity());

                InventoryResponse reducedInventory = inventoryClient.reduceStockByProductId(
                        item.getProductId(),
                        new StockUpdateRequest(item.getQuantity(), "Order #" + savedOrder.getId())
                );

                log.info("Stock reduced successfully for product: {}. New quantity: {}",
                        item.getProductId(), reducedInventory.getQuantity());
            } catch (Exception e) {
                log.error("ERROR reducing stock for product {}: {}", item.getProductId(), e.getMessage(), e);
                throw new RuntimeException("Failed to reduce stock for product " + item.getProductId() + ": " + e.getMessage());
            }
        }

        savedOrder.setStatus(Order.OrderStatus.CONFIRMED);
        Order confirmedOrder = orderRepository.save(savedOrder);

        log.info("Order successfully confirmed");
        return new OrderResponse(confirmedOrder);
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long id, OrderStatusUpdateRequest request) {
        log.info("Updating order status {} to: {}", id, request.getStatus());
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));

        validateStatusTransition(order.getStatus(), request.getStatus());
        order.setStatus(request.getStatus());
        Order updatedOrder = orderRepository.save(order);

        log.info("Order status successfully updated");
        return new OrderResponse(updatedOrder);
    }

    @Transactional
    public OrderResponse cancelOrder(Long id) {
        log.info("Canceling order: {}", id);

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));

        if (order.getStatus() != Order.OrderStatus.PENDING &&
                order.getStatus() != Order.OrderStatus.CONFIRMED) {
            throw new RuntimeException("The order cannot be cancelled in this state: " + order.getStatus());
        }
        order.setStatus(Order.OrderStatus.CANCELLED);
        Order cancelledOrder = orderRepository.save(order);
        for (OrderItem item : cancelledOrder.getItems()) {
            try {
                log.info("The product stock must be returned: {}", item.getProductId());
                // inventoryClient.addStock(item.getProductId(), new StockUpdateRequest(item.getQuantity(), "Order cancelled"));
            } catch (Exception e) {
                log.error("Error returning product stock: {}", item.getProductId(), e);
            }
        }

        log.info("Order successfully cancelled");
        return new OrderResponse(cancelledOrder);
    }

    @Transactional
    public void deleteOrder(Long id) {
        log.info("Deleting order: {}", id);
        if (!orderRepository.existsById(id)) {
            throw new RuntimeException("Order not found with id: " + id);
        }
        orderRepository.deleteById(id);
        log.info("Order successfully deleted");
    }

    private void validateStatusTransition(Order.OrderStatus current, Order.OrderStatus newStatus) {
        // PENDING → CONFIRMED, CANCELLED
        if (current == Order.OrderStatus.PENDING) {
            if (newStatus != Order.OrderStatus.CONFIRMED && newStatus != Order.OrderStatus.CANCELLED) {
                throw new RuntimeException("Invalid state transition from PENDING to" + newStatus);
            }
        }

        // CONFIRMED → SHIPPED, CANCELLED
        if (current == Order.OrderStatus.CONFIRMED) {
            if (newStatus != Order.OrderStatus.SHIPPED && newStatus != Order.OrderStatus.CANCELLED) {
                throw new RuntimeException("Invalid state transition from CONFIRMED to " + newStatus);
            }
        }

        // SHIPPED → DELIVERED
        if (current == Order.OrderStatus.SHIPPED) {
            if (newStatus != Order.OrderStatus.DELIVERED) {
                throw new RuntimeException("Invalid state transition from SHIPPED to " + newStatus);
            }
        }

        // DELIVERED
        if (current == Order.OrderStatus.DELIVERED) {
            throw new RuntimeException("The status of a delivered order cannot be changed.");
        }

        // CANCELLED
        if (current == Order.OrderStatus.CANCELLED) {
            throw new RuntimeException("The status of a cancelled order cannot be changed.");
        }
    }
}
