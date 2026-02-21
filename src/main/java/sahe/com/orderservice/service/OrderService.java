package sahe.com.orderservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sahe.com.orderservice.client.*;
import sahe.com.orderservice.dto.OrderItemRequest;
import sahe.com.orderservice.dto.OrderRequest;
import sahe.com.orderservice.dto.OrderResponse;
import sahe.com.orderservice.dto.OrderStatusUpdateRequest;
import sahe.com.orderservice.model.Order;
import sahe.com.orderservice.model.OrderItem;
import sahe.com.orderservice.repository.OrderRepository;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final InventoryClient inventoryClient;

    // Obtener todas las ordenes
    public List<OrderResponse> getAllOrders() {
        log.info("Obteniendo todos los pedidos");
        return orderRepository.findAll()
                .stream()
                .map(OrderResponse::new)
                .collect(Collectors.toList());
    }

    // Obtener orden x id
    public OrderResponse getOrderById(Long id) {
        log.info("Obtener orden por id: {}", id);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado con id: " + id));
        return new OrderResponse(order);
    }

    // Obtener ordenes x usuario
    public List<OrderResponse> getOrdersByUserId(Long userId) {
        log.info("Obtener pedidos por id de usuario: {}", userId);
        return orderRepository.findByUserId(userId)
                .stream()
                .map(OrderResponse::new)
                .collect(Collectors.toList());
    }

    // Obtener ordenes x estado
    public List<OrderResponse> getOrdersByStatus(Order.OrderStatus status) {
        log.info("Obtener pedidos por estado: {}", status);
        return orderRepository.findByStatus(status)
                .stream()
                .map(OrderResponse::new)
                .collect(Collectors.toList());
    }

    // Crear pedido
    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        log.info(": {}", request.getUserId());
        // 1. Validar y obtener información de productos
        for (OrderItemRequest itemRequest : request.getItems()) {
            // Verificar que el producto existe y está activo
            ProductResponse product = productClient.getProductById(itemRequest.getProductId());
            if (!product.getActive()) {
                throw new RuntimeException("Producto " + product.getName() + " no esta activo");
            }
            // Verificar disponibilidad en inventario
            AvailabilityResponse availability = inventoryClient.checkAvailability(
                    itemRequest.getProductId(),
                    itemRequest.getQuantity()
            );

            if (!availability.getAvailable()) {
                throw new RuntimeException("Stock insuficiente de producto: " + product.getName());
            }
        }

        // 2. Crear el pedido
        Order order = new Order();
        order.setUserId(request.getUserId());
        order.setShippingAddress(request.getShippingAddress());
        order.setShippingCity(request.getShippingCity());
        order.setShippingCountry(request.getShippingCountry());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setNotes(request.getNotes());
        order.setStatus(Order.OrderStatus.PENDING);

        // 3. Crear los items del pedido
        for (OrderItemRequest itemRequest : request.getItems()) {
            ProductResponse product = productClient.getProductById(itemRequest.getProductId());

            OrderItem orderItem = new OrderItem();
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setUnitPrice(product.getPrice());
            orderItem.calculateSubtotal();
            order.addItem(orderItem);
        }

        // 4. Calcular total
        order.calculateTotal();

        // 5. Guardar pedido
        Order savedOrder = orderRepository.save(order);
        log.info("Pedido creado con id: {}", savedOrder.getId());

        // 6. Reducir stock en inventario
        for (OrderItem item : savedOrder.getItems()) {
            try {
                log.info("Attempting to reduce stock for product {} by {} units",
                        item.getProductId(), item.getQuantity());

                InventoryResponse reducedInventory = inventoryClient.reduceStockByProductId(
                        item.getProductId(),
                        new StockUpdateRequest(item.getQuantity(), "Order #" + savedOrder.getId())
                );

                log.info("Stock reduced successfully for product: {}. New quantity: {}",
                        item.getProductId(), reducedInventory.getQuantity());
            } catch (Exception e) {
                log.error("ERROR reducing stock for product {}: {}", item.getProductId(), e.getMessage(), e);
                // Lanzar excepción para que falle la orden
                throw new RuntimeException("Failed to reduce stock for product " + item.getProductId() + ": " + e.getMessage());
            }
        }

        // 7. Actualizar estado a CONFIRMED
        savedOrder.setStatus(Order.OrderStatus.CONFIRMED);
        Order confirmedOrder = orderRepository.save(savedOrder);

        log.info("Pedido confirmado con éxito");
        return new OrderResponse(confirmedOrder);
    }

    // Actualizar estado de pedido
    @Transactional
    public OrderResponse updateOrderStatus(Long id, OrderStatusUpdateRequest request) {
        log.info("Actualizando pedido {} estado a: {}", id, request.getStatus());

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado con id: " + id));

        // Validar transiciones de estado
        validateStatusTransition(order.getStatus(), request.getStatus());

        order.setStatus(request.getStatus());
        Order updatedOrder = orderRepository.save(order);

        log.info("Estado del pedido actualizado correctamente");
        return new OrderResponse(updatedOrder);
    }

    // Cancelar pedido
    @Transactional
    public OrderResponse cancelOrder(Long id) {
        log.info("Cancelando pedido: {}", id);

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado con id: " + id));

        // Solo se puede cancelar si está en PENDING o CONFIRMED
        if (order.getStatus() != Order.OrderStatus.PENDING &&
                order.getStatus() != Order.OrderStatus.CONFIRMED) {
            throw new RuntimeException("No se puede cancelar el pedido en estado: " + order.getStatus());
        }

        order.setStatus(Order.OrderStatus.CANCELLED);
        Order cancelledOrder = orderRepository.save(order);

        // Devolver stock al inventario
        for (OrderItem item : cancelledOrder.getItems()) {
            try {


                log.info("Se debe devolver el stock del producto.: {}", item.getProductId());
                // inventoryClient.addStock(item.getProductId(), new StockUpdateRequest(item.getQuantity(), "Order cancelled"));
            } catch (Exception e) {
                log.error("Error al devolver el stock del producto: {}", item.getProductId(), e);
            }
        }

        log.info("Pedido cancelado con éxito");
        return new OrderResponse(cancelledOrder);
    }

    // Eliminar orden
    @Transactional
    public void deleteOrder(Long id) {
        log.info("Eliminando orden: {}", id);

        if (!orderRepository.existsById(id)) {
            throw new RuntimeException("Pedido no encontrado con id: " + id);
        }

        orderRepository.deleteById(id);
        log.info("Orden eliminada exitosamente");
    }

    // Validar transicion de estado
    private void validateStatusTransition(Order.OrderStatus current, Order.OrderStatus newStatus) {
        // PENDING → CONFIRMED, CANCELLED
        if (current == Order.OrderStatus.PENDING) {
            if (newStatus != Order.OrderStatus.CONFIRMED && newStatus != Order.OrderStatus.CANCELLED) {
                throw new RuntimeException("Transición de estado no válida de PENDIENTE a " + newStatus);
            }
        }

        // CONFIRMED → SHIPPED, CANCELLED
        if (current == Order.OrderStatus.CONFIRMED) {
            if (newStatus != Order.OrderStatus.SHIPPED && newStatus != Order.OrderStatus.CANCELLED) {
                throw new RuntimeException("Transición de estado no válida de CONFIRMADO a " + newStatus);
            }
        }

        // SHIPPED → DELIVERED
        if (current == Order.OrderStatus.SHIPPED) {
            if (newStatus != Order.OrderStatus.DELIVERED) {
                throw new RuntimeException("Transición de estado no válida de ENVIADO a " + newStatus);
            }
        }

        // DELIVERED → no se puede cambiar
        if (current == Order.OrderStatus.DELIVERED) {
            throw new RuntimeException("No se puede cambiar el estado del pedido entregado");
        }

        // CANCELLED → no se puede cambiar
        if (current == Order.OrderStatus.CANCELLED) {
            throw new RuntimeException("No se puede cambiar el estado del pedido cancelado");
        }
    }
}
