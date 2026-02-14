## 🧾 Order Service – SmartCommerce
This module orchestrates the complete order lifecycle. It validates products, checks stock availability, reserves inventory, and enforces strict state transitions — all while keeping business rules centralized and secure.

## 🎯 Why This Service Exists
- Single Source of Truth for Orders
  All order creation, updates, cancellations, and status transitions are handled here — nowhere else.

- Cross-Service Orchestration
  Coordinates with:
    Product Service → validates product existence and active status
    Inventory Service → checks availability and reduces stock

- Transactional Consistency
  Order creation and stock reduction are wrapped in a transaction. If stock reduction fails, the order fails. No half-broken state.

- Strict Lifecycle Control
  Enforces valid status transitions to prevent business logic chaos.

- Security-First Design
  JWT-based stateless authentication with role-based endpoint restrictions.

## 🔑 Core Capabilities
# Order Management
- Create order (validates products + stock)
- Get order by ID
- Get orders by user
- Get orders by status
- List all orders (ADMIN only)
- Delete order (ADMIN only)
- Order Status Lifecycle
- Supported states:
- 
PENDING → CONFIRMED → SHIPPED → DELIVERED
          ↘
           CANCELLED

Invalid transitions are blocked at service level.

## ⚙️ Order Creation Flow (Critical Path)
- Validate each product via Product Service.
- Verify stock availability via Inventory Service.
- Create order and items.
- Calculate total amount.
- Persist order.
- Reduce stock for each item.
- Update status to CONFIRMED.
- If stock reduction fails → the entire transaction rolls back.
- Clean. Controlled. Predictable.

## 🗄️ Persistence
Database: PostgreSQL (smartcommerce_orders)
Tables:
orders
order_items
One-to-many relationship (Order → OrderItem)
Automatic timestamping
Cascade persistence with orphan removal

## 🔗 Service Integration
- Uses OpenFeign for internal communication:
Product validation
Stock availability check
Stock reduction after order confirmation
Designed to operate inside a Eureka-discovered microservices environment.

## 📌 Technical Highlights
- Clean domain separation
- Transactional integrity
- Controlled state transitions
- Role-based access control

Microservice orchestration without leaking responsibilities

Clear logging for stock reduction traceability
