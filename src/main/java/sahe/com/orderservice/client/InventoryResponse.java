package sahe.com.orderservice.client;

import lombok.Data;

@Data
public class InventoryResponse {
    private Long id;
    private Long productId;
    private Integer quantity;
    private Integer reservedQuantity;
    private Integer availableQuantity;
    private String location;
}
