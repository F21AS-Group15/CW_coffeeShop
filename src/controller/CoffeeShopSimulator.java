package controller;

import model.*;
import util.CoffeeShopLogger;
import util.OrderQueue;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.concurrent.*;

// Coffee shop simulation controller
public class CoffeeShopSimulator {
    public OrderQueue orderQueue;
    public int serverCount;
    public int serveTime;
    public List<ServerThread> servers;
    public OrderManager orderManager;
    public List<QueueObserver> observers = new CopyOnWriteArrayList<>();
    public CustomerGeneratorThread customerGenerator;
    public volatile boolean isRunning = false;
    public ScheduledExecutorService scheduler;

    public CoffeeShopSimulator(OrderQueue queue,
                               int serverCount,
                               int serveTime,
                               OrderManager orderManager) {
        // Parameter validation
        Objects.requireNonNull(queue, "Order queue cannot be null");
        Objects.requireNonNull(orderManager, "Order manager cannot be null");
        if (serverCount <= 0) throw new IllegalArgumentException("Number of servers must be greater than 0");

        // Initialize member variables
        this.orderQueue = queue;
        this.serverCount = serverCount;
        this.serveTime = serveTime;
        this.orderManager = orderManager;

        // Initialize server list
        this.servers = new ArrayList<>(serverCount);
        for (int i = 1; i <= serverCount; i++) {
            servers.add(new ServerThread("Server-" + i, orderQueue, serveTime));
        }
    }

    public synchronized void startSimulation() {
        if (isRunning) return;

        isRunning = true;
        CoffeeShopLogger.getInstance().logEvent("Simulation started");

        // Initialize thread pool
        scheduler = Executors.newScheduledThreadPool(servers.size() + 1);

        // Start server threads
        servers.forEach(server -> {
            server.setRunning(true);
            scheduler.execute(server);
        });

        // Start customer generation thread
        List<Order> pendingOrders = getPendingPreOrders();
        customerGenerator = new CustomerGeneratorThread(orderQueue, pendingOrders);
        scheduler.execute(customerGenerator);

        // Start status observer timer
        scheduler.scheduleAtFixedRate(this::notifyObservers, 0, 1, TimeUnit.SECONDS);
    }

    public synchronized void stopSimulation() {
        if (!isRunning) return;

        isRunning = false;
        CoffeeShopLogger.getInstance().logEvent("Simulation stopping...");

        // Stop customer generation
        if (customerGenerator != null) {
            customerGenerator.stopGenerating();
        }

        // Stop servers
        servers.forEach(ServerThread::stopWorking);

        // Shutdown thread pool
        if (scheduler != null) {
            scheduler.shutdownNow();
        }

        // Final state notification
        notifyObservers();
        CoffeeShopLogger.getInstance().logEvent("Simulation stopped");
    }

    public void addObserver(QueueObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
        }
    }

    private void notifyObservers() {
        List<Order> queueSnapshot = orderQueue.getQueueSnapshot();
        List<ServerThread> serversSnapshot = new ArrayList<>(servers);

        observers.forEach(observer -> {
            observer.updateQueue(queueSnapshot);
            observer.updateServers(serversSnapshot);
        });
    }

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

    public boolean areAllOrdersCompleted() {
        if (orderQueue == null || servers == null || orderManager == null) {
            return false; // If key components are not initialized, assume not completed
        }
        return orderManager.getOrders().stream()
                .allMatch(Order::isCompleted);
    }
}
