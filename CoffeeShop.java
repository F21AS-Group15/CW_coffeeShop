import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.concurrent.CountDownLatch;

// 菜单项类
class MenuItem {
    private String id;
    private String name;
    private double price;
    private String category;

    public MenuItem(String id, String name, double price, String category) {
        if (id == null || name == null || category == null || price < 0) {
            throw new IllegalArgumentException("Invalid menu item data.");
        }
        this.id = id;
        this.name = name;
        this.price = price;
        this.category = category;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public String getCategory() { return category; }

    @Override
    public String toString() {
        return name + " (" + category + ") - $" + price;
    }
}

// 订单类
class Order {
    private String customerId;
    private List<MenuItem> items;
    private Date timestamp;

    public Order(String customerId, Date timestamp) {
        this.customerId = customerId;
        this.timestamp = timestamp;
        this.items = new ArrayList<>();
    }

    public void addItem(MenuItem item) {
        items.add(item);
    }

    public double calculateTotal() {
        double total = 0;
        for (MenuItem item : items) {
            total += item.getPrice();
        }
        return applyDiscount(total);
    }

    private double applyDiscount(double total) {
        long foodCount = items.stream().filter(i -> i.getCategory().equals("Food")).count();
        long beverageCount = items.stream().filter(i -> i.getCategory().equals("Beverage")).count();
        if (foodCount >= 2 && beverageCount >= 1) {
            return total * 0.8; // 20% 折扣
        }
        return total;
    }

    public String getSummary() {
        return "Customer " + customerId + " - Items: " + items.size() + " - Total: $" + calculateTotal();
    }
}

// 订单处理器
class OrderProcessor {
    private List<Order> orders;

    public OrderProcessor() {
        this.orders = new ArrayList<>();
    }

    public void processOrder(Order order) {
        orders.add(order);
    }

    public List<Order> getOrders() {
        return orders;
    }
}

// 文件管理类
class FileManager {
    public static Map<String, MenuItem> loadMenu(String filePath) throws IOException {
        Map<String, MenuItem> menu = new HashMap<>();
        List<String> lines = Files.readAllLines(Paths.get(filePath));
        for (String line : lines) {
            String[] parts = line.split(",");
            if (parts.length != 4) throw new InvalidDataException("Invalid menu format: " + line);
            menu.put(parts[0], new MenuItem(parts[0], parts[1], Double.parseDouble(parts[2]), parts[3]));
        }
        return menu;
    }

    public static void saveReport(List<Order> orders, String filePath) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));
        for (Order order : orders) {
            writer.write(order.getSummary() + "\n");
        }
        writer.close();
    }
}

// 自定义异常类
class InvalidDataException extends RuntimeException {
    public InvalidDataException(String message) {
        super(message);
    }
}

// GUI 界面
class CoffeeShopGUI {
    private JFrame frame;
    private JComboBox<MenuItem> menuComboBox;
    private JTextArea orderSummary;
    private OrderProcessor processor;
    private Order currentOrder;

    public CoffeeShopGUI(Map<String, MenuItem> menu) {
        processor = new OrderProcessor();
        frame = new JFrame("Coffee Shop");
        frame.setSize(400, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        menuComboBox = new JComboBox<>(menu.values().toArray(new MenuItem[0]));
        orderSummary = new JTextArea(10, 30);

        JButton addItemButton = new JButton("Add Item");
        addItemButton.addActionListener(e -> {
            if (currentOrder == null) {
                currentOrder = new Order(UUID.randomUUID().toString(), new Date());
            }
            currentOrder.addItem((MenuItem) menuComboBox.getSelectedItem());
            orderSummary.setText(currentOrder.getSummary());
        });

        JButton finalizeButton = new JButton("Finalize Order");
        finalizeButton.addActionListener(e -> {
            if (currentOrder != null) {
                processor.processOrder(currentOrder);
                currentOrder = null;
                orderSummary.setText("Order finalized!");
            }
        });

        JPanel panel = new JPanel();
        panel.add(menuComboBox);
        panel.add(addItemButton);
        panel.add(finalizeButton);
        panel.add(new JScrollPane(orderSummary));
        frame.add(panel);
        frame.setVisible(true);
    }
}

// 主程序
public class CoffeeShop {
    public static void main(String[] args) {
        try {
            Map<String, MenuItem> menu = FileManager.loadMenu("menu.txt");
            new CoffeeShopGUI(menu);
        } catch (IOException e) {
            System.err.println("Error loading menu: " + e.getMessage());
        }
    }
}
