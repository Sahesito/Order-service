package sahe.com.orderservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import sahe.com.orderservice.model.Order;

@Data
public class OrderStatusUpdateRequest {

    @NotNull(message = "Se requiere estado")
    private Order.OrderStatus status;
}