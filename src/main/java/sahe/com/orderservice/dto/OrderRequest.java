package sahe.com.orderservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class OrderRequest {

    @NotNull(message = "Se requiere ID de usuario")
    private Long userId;

    @NotBlank(message = "Se requiere dirección de envío")
    private String shippingAddress;

    @NotBlank(message = "Se requiere ciudad de envío")
    private String shippingCity;

    @NotBlank(message = "Se requiere país de envío")
    private String shippingCountry;

    @NotBlank(message = "Se requiere método de pago")
    private String paymentMethod;

    private String notes;

    @NotEmpty(message = "El pedido debe tener al menos un artículo")
    @Valid
    private List<OrderItemRequest> items;
}
