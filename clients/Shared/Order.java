package Shared;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public class Order implements Serializable {
    private String orderId;
    private String customerId;
    private List<OrderItem> items;
    private OrderStatus status;

    public Order() {
        this.orderId = UUID.randomUUID().toString();
        this.status = OrderStatus.PENDING;
    }

    public Order(String customerId, List<OrderItem> items) {
        this();
        this.customerId = customerId;
        this.items = items;
    }

    public String getOrderId() { return orderId; }
    public String getCustomerId() { return customerId; }
    public List<OrderItem> getItems() { return items; }
    public OrderStatus getStatus() { return status; }

    public void setOrderId(String orderId) { this.orderId = orderId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public void setItems(List<OrderItem> items) { this.items = items; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public double getTotalAmount() {
        return items.stream()
                .mapToDouble(item -> item.getProduct().getPrice() * item.getQuantity())
                .sum();
    }
}
