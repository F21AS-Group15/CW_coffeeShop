package util;

import model.Order;
import model.QueueObserver;

import java.util.*;

public class OrderQueue {
    public PriorityQueue<Order> preOrderQueue;  // 预定订单优先队列
    public PriorityQueue<Order> walkInQueue;    // 现场订单优先队列
    public int maxSize;
    public Object lock = new Object();

    public OrderQueue(int maxSize) {
        this.maxSize = maxSize;
        // 按入队时间排序（最早的在队首）
        Comparator<Order> timeComparator = Comparator.comparingLong(Order::getEnqueueTime);
        this.preOrderQueue = new PriorityQueue<>(timeComparator);
        this.walkInQueue = new PriorityQueue<>(timeComparator);
    }
    public synchronized int getQueueSize() {
        return preOrderQueue.size() + walkInQueue.size();
    }
    // 添加订单时记录时间戳
    public void addOrder(Order order) throws InterruptedException {
        synchronized (lock) {
            while (getTotalSize() >= maxSize) {
                lock.wait();
            }

            order.setEnqueueTime(System.currentTimeMillis()); // 关键：记录入队时间

            if ("PRE_ORDER".equals(order.getOrderType())) {
                preOrderQueue.add(order);
            } else {
                walkInQueue.add(order);
            }

            lock.notifyAll();
        }
    }

    // 严格按FIFO获取订单
    public Order getNextOrder() throws InterruptedException {
        synchronized (lock) {
            while (getTotalSize() == 0) {
                lock.wait();
            }

            // 优先处理预定订单中最早的
            if (!preOrderQueue.isEmpty()) {
                return preOrderQueue.poll();
            }
            // 然后处理现场订单中最早的

            return walkInQueue.poll();
        }
    }

    private int getTotalSize() {
        return preOrderQueue.size() + walkInQueue.size();
    }

    // 获取队列快照（按入队时间排序）
    public List<Order> getQueueSnapshot() {
        synchronized (lock) {
            List<Order> allOrders = new ArrayList<>();
            allOrders.addAll(preOrderQueue);
            allOrders.addAll(walkInQueue);
            allOrders.sort(Comparator.comparingLong(Order::getEnqueueTime));
            return Collections.unmodifiableList(allOrders);
        }
    }
}
