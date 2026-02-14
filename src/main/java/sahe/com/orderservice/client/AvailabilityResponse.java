package sahe.com.orderservice.client;

import lombok.Data;

@Data
public class AvailabilityResponse {
    private Long productId;
    private Integer requestedQuantity;
    private Boolean available;
}
