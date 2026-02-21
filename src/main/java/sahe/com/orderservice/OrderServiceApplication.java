package sahe.com.orderservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import sahe.com.orderservice.config.FeignConfig;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(defaultConfiguration = FeignConfig.class)
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
        System.out.println("""
                ORDER SERVICE - RUNNING
                Port: 8085
                GET /orders - Get all orders
                GET /orders/{id} - Get order by id
                GET /orders/user/{userId} - Get orders by user
                POST /orders - Create order
                PATCH /orders/{id}/status - Update status
                PATCH /orders/{id}/cancel - Cancel order
                """);
    }

}
