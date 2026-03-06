package sahe.com.orderservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class OrderRequest {

    @NotNull(message = "User ID required")
    private Long userId;

    @NotBlank(message = "Shipping address required")
    private String shippingAddress;

    @NotBlank(message = "Shipping city required")
    private String shippingCity;

    @NotBlank(message = "Shipping country required")
    private String shippingCountry;

    @NotBlank(message = "Payment method required")
    private String paymentMethod;

    private String notes;

    @NotEmpty(message = "The order must contain at least one item")
    @Valid
    private List<OrderItemRequest> items;
}
