package model;

import java.util.List;

// Observer interface
public interface QueueObserver {
    void updateQueue(List<Order> orders);

    void updateServers(List<ServerThread> servers);
}
