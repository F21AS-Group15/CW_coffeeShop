package controller;

import model.*;
import util.CoffeeShopLogger;
import util.OrderQueue;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.concurrent.*;
// 咖啡店模拟控制器
public class CoffeeShopSimulator {
    // region 常量配置
    public static int DEFAULT_SERVE_TIME_MS = 5000;
    public static int MAX_QUEUE_SIZE = 100;
    // endregion

    // region 成员变量
    public OrderQueue orderQueue;
    public int serverCount;
    public int serveTime;
    public List<ServerThread> servers;
    public OrderManager orderManager;
    public List<QueueObserver> observers = new CopyOnWriteArrayList<>();
    public CustomerGeneratorThread customerGenerator;
    public volatile boolean isRunning = false;
    public ScheduledExecutorService scheduler;
    // endregion

    // region 构造方法
    public CoffeeShopSimulator(OrderQueue queue,
                               int serverCount,
                               int serveTime,
                               OrderManager orderManager) {
        // 参数校验
        Objects.requireNonNull(queue, "订单队列不能为null");
        Objects.requireNonNull(orderManager, "订单管理器不能为null");
        if (serverCount <= 0) throw new IllegalArgumentException("服务员数量必须大于0");

        // 初始化成员变量
        this.orderQueue = queue;
        this.serverCount = serverCount;
        this.serveTime = serveTime;
        this.orderManager = orderManager;

        // 初始化服务员列表
        this.servers = new ArrayList<>(serverCount);
        for (int i = 1; i <= serverCount; i++) {
            servers.add(new ServerThread("服务员-" + i, orderQueue, serveTime));
        }
    }


    public CoffeeShopSimulator(OrderQueue orderQueue, int i, int i1) {
    }
    // endregion

    // region 公开控制方法
    public synchronized void startSimulation() {
        if (isRunning) return;

        isRunning = true;
        CoffeeShopLogger.getInstance().logEvent("模拟启动");

        // 初始化线程池
        scheduler = Executors.newScheduledThreadPool(servers.size() + 1);

        // 启动服务员线程
        servers.forEach(server -> {
            server.setRunning(true);
            scheduler.execute(server);
        });

        // 启动顾客生成线程
        List<Order> pendingOrders = getPendingPreOrders();
        customerGenerator = new CustomerGeneratorThread(orderQueue, pendingOrders);
        scheduler.execute(customerGenerator);

        // 启动状态观察定时器
        scheduler.scheduleAtFixedRate(this::notifyObservers, 0, 1, TimeUnit.SECONDS);
    }

    public synchronized void stopSimulation() {
        if (!isRunning) return;

        isRunning = false;
        CoffeeShopLogger.getInstance().logEvent("模拟停止中...");

        // 停止顾客生成
        if (customerGenerator != null) {
            customerGenerator.stopGenerating();
        }

        // 停止服务员
        servers.forEach(ServerThread::stopWorking);

        // 关闭线程池
        if (scheduler != null) {
            scheduler.shutdownNow();
        }

        // 最终状态通知
        notifyObservers();
        CoffeeShopLogger.getInstance().logEvent("模拟已停止");
    }
    // endregion

    // region 观察者管理
    public void addObserver(QueueObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
        }
    }

    public void removeObserver(QueueObserver observer) {
        observers.remove(observer);
    }

    private void notifyObservers() {
        List<Order> queueSnapshot = orderQueue.getQueueSnapshot();
        List<ServerThread> serversSnapshot = new ArrayList<>(servers);

        observers.forEach(observer -> {
            observer.updateQueue(queueSnapshot);
            observer.updateServers(serversSnapshot);
        });
    }
    // endregion

    // region 辅助方法
    private List<Order> getPendingPreOrders() {
        return orderManager.getOrders().stream()
                .filter(order -> !order.isCompleted() && "PRE_ORDER".equals(order.getOrderType()))
                .collect(Collectors.toList());
    }

    public boolean isRunning() {
        return isRunning;
    }

    public OrderQueue getOrderQueue() {
        return orderQueue;
    }
    // endregion

    // region 内部线程类

    /**
     * 顾客生成线程 - 负责将预定订单加入队列
     */
    private static class CustomerGeneratorThread implements Runnable {
        private final OrderQueue queue;
        private final List<Order> orders;
        private volatile boolean running = true;

        public CustomerGeneratorThread(OrderQueue queue, List<Order> orders) {
            this.queue = queue;
            this.orders = new ArrayList<>(orders); // 防御性复制
        }

        @Override
        public void run() {
            CoffeeShopLogger.getInstance().logEvent("开始处理预定订单");

            for (Order order : orders) {
                if (!running) break;

                try {
                    queue.addOrder(order);
//                    Thread.sleep(500); // 控制订单生成速度
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            CoffeeShopLogger.getInstance().logEvent("预定订单添加完成");
        }

        public void stopGenerating() {
            running = false;
        }
    }
    // endregion
}
