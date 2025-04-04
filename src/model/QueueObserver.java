package model;

import java.util.List;

// 观察者接口
public interface QueueObserver {
    void updateQueue(List<Order> orders);

    void updateServers(List<ServerThread> servers);
}
