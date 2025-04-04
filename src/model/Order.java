package model;

import java.util.ArrayList;
import java.util.List;

// 订单类
public class Order {
    private String orderId;
    private String timeStamp;
    private String customerId;
    private List<Product> items;
    private boolean isCompleted;
    private double totalPrice;
    private String orderType; // 新增字段
    private double discountAmount; // 新增字段
    private boolean isBeingProcessed; // 新增字段
    private List<OrderObserver> observers = new ArrayList<>();
    private long enqueueTime;  // 新增入队时间戳

    public long getEnqueueTime() {
        return enqueueTime;
    }

    public void setEnqueueTime(long enqueueTime) {
        this.enqueueTime = enqueueTime;
    }
    public void addObserver(OrderObserver o) {
        observers.add(o);
    }

    private void notifyOrderChanged() {
        for (OrderObserver o : observers) {
            o.onOrderUpdated(this);
        }
    }
    public Order(String orderId, String timestamp, String customerId, String orderType) {
        this.orderId = orderId;
        this.timeStamp = timestamp;
        this.customerId = customerId;
        this.items = new ArrayList<>();
        this.isCompleted = false;
        this.totalPrice = 0;
        this.orderType = orderType;
        this.isBeingProcessed = false;
    }

    // 添加商品到订单
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

    public synchronized boolean markAsProcessing() {
        if (isBeingProcessed || isCompleted) {
            return false;
        }
        isBeingProcessed = true;
        return true;
    }


    public synchronized boolean isAvailableForProcessing() {
        return !isBeingProcessed && !isCompleted;
    }

    // 从订单移除商品
    public void deleteItem(Product product) {
        items.remove(product);
        notifyOrderChanged();
    }

    // 计算订单总价
    public double getTotalPrice() {
        return totalPrice > 0 ? totalPrice : Math.round(items.stream().mapToDouble(Product::getPrice).sum() * 100.0) / 100.0;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = Math.round(totalPrice * 100.0) / 100.0;
        // 自动计算折扣金额（原价 - 折后价）
        this.discountAmount = calculateOriginalPrice() - this.totalPrice;
    }

    public double calculateOriginalPrice() {
        return Math.round(items.stream()
                .mapToDouble(Product::getPrice)
                .sum() * 100.0) / 100.0;
    }

    // 获取订单详情
    public String getOrderDetails() {
        return "Order ID: " + orderId + "\nTime: " + timeStamp + "\nTotal: $" + getTotalPrice();
    }

    public void setDiscountAmount(double amount) {
        this.discountAmount = Math.round(amount * 100.0) / 100.0;
    }

    public double getDiscountAmount() {
        return discountAmount;
    }

    // Getter和Setter方法
    public boolean isCompleted() {
        return isCompleted;
    }

    public synchronized void completeOrder() {
        isBeingProcessed = false;
        isCompleted = true;
    }

    public void resetOrder() {
        this.isCompleted = false;
    }

    public List<Product> getItems() {
        return items;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public String getOrderType() {
        return orderType;
    }

}
