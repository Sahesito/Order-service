package sahe.com.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import sahe.com.orderservice.model.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private Long id;
    private Long userId;
    private Order.OrderStatus status;
    private BigDecimal totalAmount;
    private String shippingAddress;
    private String shippingCity;
    private String shippingCountry;
    private String paymentMethod;
    private String notes;
    private List<OrderItemResponse> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public OrderResponse(Order order) {
        this.id = order.getId();
        this.userId = order.getUserId();
        this.status = order.getStatus();
        this.totalAmount = order.getTotalAmount();
        this.shippingAddress = order.getShippingAddress();
        this.shippingCity = order.getShippingCity();
        this.shippingCountry = order.getShippingCountry();
        this.paymentMethod = order.getPaymentMethod();
        this.notes = order.getNotes();
        this.items = order.getItems().stream()
                .map(OrderItemResponse::new)
                .collect(Collectors.toList());
        this.createdAt = order.getCreatedAt();
        this.updatedAt = order.getUpdatedAt();
    }
}
