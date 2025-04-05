package util;

import model.Order;

import java.util.*;

// Order queue class
public class OrderQueue {
    public PriorityQueue<Order> preOrderQueue;  // Pre-order priority queue
    public PriorityQueue<Order> walkInQueue;    // Walk-in order priority queue
    public int maxSize;
    public Object lock = new Object();

    public OrderQueue(int maxSize) {
        this.maxSize = maxSize;
        // Sort by enqueue time (earliest at the front)
        Comparator<Order> timeComparator = Comparator.comparingLong(Order::getEnqueueTime);
        this.preOrderQueue = new PriorityQueue<>(timeComparator);
        this.walkInQueue = new PriorityQueue<>(timeComparator);
    }

    public synchronized int getQueueSize() {
        return preOrderQueue.size() + walkInQueue.size();
    }

    // Add an order and record the timestamp
    public void addOrder(Order order) throws InterruptedException {
        synchronized (lock) {
            while (getQueueSize() >= maxSize) {
                lock.wait();
            }

            order.setEnqueueTime(System.currentTimeMillis()); // Record the enqueue time

            if ("PRE_ORDER".equals(order.getOrderType())) {
                preOrderQueue.add(order);
            } else {
                walkInQueue.add(order);
            }

            lock.notifyAll();
        }
    }

    // Get the next order by FIFO
    public Order getNextOrder() throws InterruptedException {
        synchronized (lock) {
            while (getQueueSize() == 0) {
                lock.wait();
            }

            // Prioritize the earliest pre-order
            if (!preOrderQueue.isEmpty()) {
                return preOrderQueue.poll();
            }
            // Then process the earliest walk-in order
            return walkInQueue.poll();
        }
    }

    // Get a snapshot of the queue (sorted by enqueue time)
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
