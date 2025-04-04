package model;

public interface OrderObserver {
    void onOrderUpdated(Order order); // 定义订单更新回调方法
}
