package model;

import util.CoffeeShopLogger;
import util.OrderQueue;

import java.util.Map;
import java.util.stream.Collectors;

// 服务员线程
public class ServerThread extends Thread {
    private OrderQueue orderQueue;
    private Order currentOrder;
    private boolean running;
    private int serveTime; // 服务时间(毫秒)

    public ServerThread(String name, OrderQueue orderQueue, int serveTime) {
        super(name);
        this.orderQueue = orderQueue;
        this.serveTime = serveTime;
        this.running = true;
    }

    // 新增运行状态控制方法
    public synchronized void setRunning(boolean running) {
        this.running = running;
        if (running) {
            this.notify(); // 如果从停止状态恢复运行，则唤醒线程
        }
    }

    public String getCurrentOrderDetails() {
        if (currentOrder == null) return "空闲中";

        StringBuilder sb = new StringBuilder();
        sb.append("订单号: ").append(currentOrder.getOrderId()).append("\n");
        sb.append("客户: ").append(currentOrder.getCustomerId()).append("\n");
        sb.append("类型: ").append(currentOrder.getOrderType()).append("\n\n");

        // 商品明细
        currentOrder.getItems().stream()
                .collect(Collectors.groupingBy(p -> p, Collectors.counting()))
                .forEach((product, count) -> {
                    sb.append(String.format("▸ %-15s ×%-2d @ ¥%-6.2f\n",
                            product.getName(), count, product.getPrice()));
                });

        // 价格计算
        double originalPrice = currentOrder.calculateOriginalPrice();
        sb.append("\n──────────────\n");
        sb.append(String.format("商品总价: ¥%.2f\n", originalPrice));
        sb.append(String.format("折扣金额: -¥%.2f\n", currentOrder.getDiscountAmount()));
        sb.append(String.format("实付金额: ¥%.2f\n\n", currentOrder.getTotalPrice()));

        return sb.toString();
    }

    public synchronized boolean isRunning() {
        return running;
    }

    @Override
    public void run() {
        CoffeeShopLogger.getInstance().logEvent(getName() + " 开始工作");
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
                if (order == null) continue; // 安全检查

                currentOrder = order;
                try {
                    CoffeeShopLogger.getInstance().logEvent(getName() + " 开始处理订单: " + getCurrentOrderDetails());

                    // 处理时间与商品数量成正比
                    int processingTime = serveTime * order.getItems().size();
                    Thread.sleep(processingTime);

                    order.completeOrder();
                    CoffeeShopLogger.getInstance().logEvent(getName() + " 完成订单: " + order.getOrderId());
                } finally {
                    currentOrder = null;
                }
            } catch (InterruptedException e) {
                if (!running) {
                    break; // 正常停止
                }
                CoffeeShopLogger.getInstance().logEvent(getName() + " 处理被中断");
            }
        }
        CoffeeShopLogger.getInstance().logEvent(getName() + " 结束工作");
    }

    private void processFullOrder(Order order) throws InterruptedException {
        // 计算基于商品数量的处理时间
        int processingTime = serveTime * order.getItems().size();
        Thread.sleep(processingTime);
    }

    public void stopWorking() {
        this.running = false;
        this.interrupt();
    }

    public Order getCurrentOrder() {
        return currentOrder;
    }
}
