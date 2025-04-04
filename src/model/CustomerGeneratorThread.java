package model;

import util.CoffeeShopLogger;
import util.OrderQueue;

import java.util.ArrayList;
import java.util.List;

//顾客生成线程
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
        CoffeeShopLogger.getInstance().logEvent("开始处理预定订单");

        // 先处理所有线上预定订单
        for (Order order : preOrders) {
            if (!running) break;

            try {
                orderQueue.addOrder(order);
                Thread.sleep(1000); // 控制订单生成速度
            } catch (InterruptedException e) {
                break;
            }
        }

        CoffeeShopLogger.getInstance().logEvent("所有预定订单已加入队列");
    }


    public void stopGenerating() {
        this.running = false;
        this.interrupt();
    }
}
