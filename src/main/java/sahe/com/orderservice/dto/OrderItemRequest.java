package sahe.com.orderservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderItemRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "The quantity is required")
    @Min(value = 1, message = "the quantity must be at least 1")
    private Integer quantity;
}
