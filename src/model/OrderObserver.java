package model;

public interface OrderObserver {
    void onOrderUpdated(Order order);
}
