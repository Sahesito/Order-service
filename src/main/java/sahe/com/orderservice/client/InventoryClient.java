package sahe.com.orderservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "inventory-service")
public interface InventoryClient {

    @GetMapping("/inventory/check-availability")
    AvailabilityResponse checkAvailability(
            @RequestParam Long productId,
            @RequestParam Integer quantity);

    @GetMapping("/inventory/product/{productId}")
    InventoryResponse getInventoryByProductId(@PathVariable Long productId);

    @PostMapping("/inventory/product/{productId}/reduce-stock")
    InventoryResponse reduceStockByProductId(
            @PathVariable Long productId,
            @RequestBody StockUpdateRequest request);


}
