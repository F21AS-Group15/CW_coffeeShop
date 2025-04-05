package model;

import util.CoffeeShopLogger;
import util.OrderQueue;

import java.util.stream.Collectors;

// Waiter thread
public class ServerThread extends Thread {
    private OrderQueue orderQueue;
    private Order currentOrder;
    private boolean running;
    private int serveTime; // Service time (milliseconds)

    public ServerThread(String name, OrderQueue orderQueue, int serveTime) {
        super(name);
        this.orderQueue = orderQueue;
        this.serveTime = serveTime;
        this.running = true;
    }

    // New method to control running state
    public synchronized void setRunning(boolean running) {
        this.running = running;
        if (running) {
            this.notify(); // If resuming from a stopped state, notify the thread
        }
    }

    public String getCurrentOrderDetails() {
        if (currentOrder == null) return "Idle";

        StringBuilder sb = new StringBuilder();
        sb.append("Order ID: ").append(currentOrder.getOrderId()).append("\n");
        sb.append("Customer: ").append(currentOrder.getCustomerName()).append("\n");
        sb.append("Type: ").append(currentOrder.getOrderType()).append("\n\n");

        // Product details
        currentOrder.getItems().stream()
                .collect(Collectors.groupingBy(p -> p, Collectors.counting()))
                .forEach((product, count) -> {
                    sb.append(String.format("▸ %-15s ×%-2d @ $%-6.2f\n",
                            product.getName(), count, product.getPrice()));
                });

        // Price calculation
        double originalPrice = currentOrder.calculateOriginalPrice();
        sb.append("\n──────────────\n");
        sb.append(String.format("Total Price: $%.2f\n", originalPrice));
        sb.append(String.format("Discount Amount: -$%.2f\n", currentOrder.getDiscountAmount()));
        sb.append(String.format("Amount Paid: $%.2f\n\n", currentOrder.getTotalPrice()));

        return sb.toString();
    }

    @Override
    public void run() {
        CoffeeShopLogger.getInstance().logEvent(getName() + " has started working");
        while (running || orderQueue.getQueueSize() > 0) {
            try {
                synchronized (this) {
                    while (!running && orderQueue.getQueueSize() > 0) {
                        wait();
                    }
                    if (!running) {
                        break;
                    }
                }

                Order order = orderQueue.getNextOrder();
                if (order == null) continue; // Safety check

                currentOrder = order;
                try {
                    CoffeeShopLogger.getInstance().logEvent(getName() + " starts processing order: " + getCurrentOrderDetails());

                    // Processing time is proportional to the number of items
                    int processingTime = serveTime * order.getItems().size();
                    Thread.sleep(processingTime);

                    order.completeOrder();
                    CoffeeShopLogger.getInstance().logEvent(getName() + " completed order: " + order.getOrderId());
                } finally {
                    currentOrder = null;
                }
            } catch (InterruptedException e) {
                if (!running) {
                    break; // Normal stop
                }
                CoffeeShopLogger.getInstance().logEvent(getName() + " was interrupted while processing");
            }
        }
        CoffeeShopLogger.getInstance().logEvent(getName() + " has finished working");
    }

    public void stopWorking() {
        this.running = false;
        this.interrupt();
    }

    public Order getCurrentOrder() {
        return currentOrder;
    }
}
