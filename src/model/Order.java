package model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

// Order class
public class Order {
    private String orderId;
    private String timeStamp;
    private String customerName;
    private List<Product> items;
    private boolean isCompleted;
    private double totalPrice;
    private String orderType;
    private double discountAmount;
    private List<OrderObserver> observers = new ArrayList<>();
    private long enqueueTime;

    public long getEnqueueTime() {
        return enqueueTime;
    }

    public void setEnqueueTime(long enqueueTime) {
        this.enqueueTime = enqueueTime;
    }

    private void notifyOrderChanged() {
        for (OrderObserver o : observers) {
            o.onOrderUpdated(this);
        }
    }
    public Order(String orderId, String timestamp, String customerName, String orderType) {
        this.orderId = orderId;
        this.timeStamp = timestamp;
        this.customerName = customerName;
        this.items = new ArrayList<>();
        this.isCompleted = false;
        this.totalPrice = 0;
        this.orderType = orderType;
    }

    // Add product to the order
    public void addItem(Product product) {
        items.add(product);
        notifyOrderChanged();
    }

    public void addItem(Product product, int amount) {
        for (int i = 0; i < amount; i++) {
            items.add(product);
        }
        notifyOrderChanged();
    }

    // Calculate the total price of the order
    public double getTotalPrice() {
        return totalPrice > 0 ? totalPrice : Math.round(items.stream().mapToDouble(Product::getPrice).sum() * 100.0) / 100.0;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = Math.round(totalPrice * 100.0) / 100.0;
        // Automatically calculate discount amount (original price - discounted price)
        this.discountAmount = calculateOriginalPrice() - this.totalPrice;
    }

    public double calculateOriginalPrice() {
        return Math.round(items.stream()
                .mapToDouble(Product::getPrice)
                .sum() * 100.0) / 100.0;
    }

    public void setDiscountAmount(double amount) {
        this.discountAmount = Math.round(amount * 100.0) / 100.0;
    }

    public double getDiscountAmount() {
        return discountAmount;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public synchronized void completeOrder() {
        isCompleted = true;
    }

    public List<Product> getItems() {
        return items;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public String getOrderType() {
        return orderType;
    }

    public String getOrderDetails() {

        StringBuilder sb = new StringBuilder();
        sb.append("Order ID: ").append(this.getOrderId()).append("\n");
        sb.append("Customer: ").append(this.getCustomerName()).append("\n");
        sb.append("Type: ").append(this.getOrderType()).append("\n\n");

        // Product details
        this.getItems().stream()
                .collect(Collectors.groupingBy(p -> p, Collectors.counting()))
                .forEach((product, count) -> {
                    sb.append(String.format("▸ %-15s ×%-2d @ $%-6.2f\n",
                            product.getName(), count, product.getPrice()));
                });

        // Price calculation
        double originalPrice = this.calculateOriginalPrice();
        sb.append("\n──────────────\n");
        sb.append(String.format("Total product price: $%.2f\n", originalPrice));
        sb.append(String.format("Discount amount: -$%.2f\n", this.getDiscountAmount()));
        sb.append(String.format("Amount payable: $%.2f\n\n", this.getTotalPrice()));

        return sb.toString();
    }

}
