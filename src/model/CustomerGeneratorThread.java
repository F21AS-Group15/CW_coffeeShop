package model;

import util.CoffeeShopLogger;
import util.OrderQueue;

import java.util.ArrayList;
import java.util.List;

// Customer generation thread
public class CustomerGeneratorThread extends Thread {
    private OrderQueue orderQueue;
    private List<Order> preOrders;
    private boolean running;

    public CustomerGeneratorThread(OrderQueue orderQueue, List<Order> preOrders) {
        this.orderQueue = orderQueue;
        this.preOrders = new ArrayList<>(preOrders);
        this.running = true;
    }

    @Override
    public void run() {
        CoffeeShopLogger.getInstance().logEvent("Start processing pre-orders");

        // Process all online pre-orders
        for (Order order : preOrders) {
            if (!running) break;

            try {
                orderQueue.addOrder(order);
            } catch (InterruptedException e) {
                break;
            }
        }
        CoffeeShopLogger.getInstance().logEvent("All pre-orders have been added to the queue");
    }

    public void stopGenerating() {
        this.running = false;
        this.interrupt();
    }
}
