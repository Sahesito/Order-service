package sahe.com.orderservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderItemRequest {

    @NotNull(message = "Id del producto es requerido")
    private Long productId;

    @NotNull(message = "La cantidad es requerida")
    @Min(value = 1, message = "la cantidad debe ser por lo menos 1")
    private Integer quantity;
}
