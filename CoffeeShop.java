import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.*;
import java.util.List;

// Product 类
class Product {
    private String id;
    private String name;
    private String description;
    private String category;
    private double price;
    private int orderCount; // 记录商品被点单的次数

    public Product(String id, String name, String description, String category, double price) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.price = price;
        this.orderCount = 0;
    }

    public String getDetails() {
        return name + ": " + description + " (" + category + ") - $" + price;
    }

    public double getPrice() { return price; }
    public String getCategory() { return category; }
    public String getId() { return id; }
    public int getOrderCount() { return orderCount; }
    public void incrementOrderCount(Integer value) { this.orderCount++; }

    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setCategory(String category) { this.category = category; }
    public void setPrice(double price) { this.price = price; }
}

// Order 类
class Order {
	private String orderId;
	private String timeStamp;
	private String customerId;
	private List<Product> items;
	private boolean isCompleted;
	private double totalPrice; // 新增的 totalPrice 字段

	public Order(String orderId, String timestamp, String customerId) {
		this.orderId = orderId;
	    this.timeStamp = timestamp;
	    this.customerId = customerId;
	    this.items = new ArrayList<>();
	    this.isCompleted = false;
	    this.totalPrice = 0; // 初始化为0
	}
	
	 public void addItem(Product product) { items.add(product); }
	 public void addItem(Product product, int amount) {
		 for (int i = 0; i < amount; i++){
	         items.add(product);
	     }
	 }
	 public void deleteItem(Product product) { items.remove(product); }
	 
	 // 获取订单总价
	 public double getTotalPrice() { 
	     return totalPrice > 0 ? totalPrice : items.stream().mapToDouble(Product::getPrice).sum();
	 }
	
	 // 设置总价
	 public void setTotalPrice(double totalPrice) { 
	     this.totalPrice = totalPrice; 
	 }
	
	 public String getOrderDetails() { return "Order ID: " + orderId + " Total: $" + getTotalPrice(); }
	 public boolean isCompleted() { return isCompleted; }
	 public void completeOrder() { this.isCompleted = true; }
	 public void resetOrder() { this.isCompleted = false; }
	 public List<Product> getItems() { return items; }
}

// Customer 类
class Customer {
    private String customerId;
    private String name;
    private List<Order> orders;

    public Customer(String customerId, String name) {
        this.customerId = customerId;
        this.name = name;
        this.orders = new ArrayList<>();
    }

    public void placeOrder(Order order) { orders.add(order); }
    public List<Order> getOrderHistory() { return orders; }
}

// Menu 类
class Menu {
    private List<Product> products;

    public Menu() { this.products = new ArrayList<>(); }
    public List<Product> getAllProducts() {
        return new ArrayList<>(products); // 返回产品的副本
    }
    public void loadFromFile(String filePath) {}
    public void displayMenu() { products.forEach(p -> System.out.println(p.getDetails())); }
    public Product getProductById(String productId) { return products.stream().filter(p -> p.getId().equals(productId)).findFirst().orElse(null); }
    public void addProduct(Product product) { products.add(product); }
    public boolean removeProduct(String productId) { return products.removeIf(p -> p.getId().equals(productId)); }
}

// OrderManager 类
class OrderManager {
    private List<Order> orders;

    public OrderManager() { this.orders = new ArrayList<>(); }
    public void loadOrdersFromFile(String filePath) {}
    public void generateReport(Menu menu) {
        double totalRevenue = 0;
        System.out.println("=== Sales Report ===");
        for (Product product : menu.getAllProducts()) {
            System.out.println(product.getDetails() + " - Ordered " + product.getOrderCount() + " times");
        }
        for (Order order : orders) {
            totalRevenue += order.getTotalPrice();
        }
        System.out.println("Total Revenue: $" + totalRevenue);
    }
    public void addOrder(Order order) { orders.add(order); }
    public boolean removeOrder(String orderId) { return orders.removeIf(o -> o.getOrderDetails().contains(orderId)); }
}


//折扣抽象类
abstract class Discount {
	private String discountName;
	public Discount(String discountName) {
		this.discountName = discountName;
	}
	public String getDiscountName() {
		return discountName;
	}
	
	// 抽象方法，子类实现具体折扣规则
    public abstract double applyDiscount(Order order);
}

//默认折扣
class DefaultDiscount extends Discount {
    public DefaultDiscount() {
        super("default"); // 设置折扣名称为 "default"
    }

    @Override
    public double applyDiscount(Order order) {
        double total = order.getTotalPrice();
        
        // 满50打8折
        if (total >= 50) {
            return total*0.8;
        }
        // 满30减5
        else if (total >= 30) {
            return total - 5;
        }
        // 满20减2
        else if (total >= 20) {
            return total - 2;
        }
        
        return total; // 不满足时，返回原价
    }
}

//DiscountManager类
class DiscountManager {
    private Map<String, Discount> discountRules;

    public DiscountManager() {
        this.discountRules = new HashMap<>();
        // 默认添加 "default" 折扣规则
        addDiscount(new DefaultDiscount());
    }

    // 添加折扣规则
    public void addDiscount(Discount discount) {
        discountRules.put(discount.getDiscountName(), discount);
    }

    // 删除折扣规则
    public boolean removeDiscount(Discount discount) {
        return discountRules.remove(discount.getDiscountName()) != null;
    }

    // 应用折扣规则，默认使用"default"折扣规则
    public double applyDiscount(String discountName, Order order) {
        // 如果折扣名称为空或null，使用默认折扣规则
        if (discountName == null || discountName.isEmpty()) {
            discountName = "default";
        }

        // 获取指定折扣规则并应用，如果没有该规则，则返回原价
        Discount discount = discountRules.getOrDefault(discountName, new DefaultDiscount());
        return discount.applyDiscount(order);
    }
}

public class CoffeeShop {
    private Menu menu;
    private OrderManager orderManager;
    private DiscountManager discountManager;
    private JFrame frame;
    private JPanel productPanel;
    private JTextArea orderDetails;
    private JButton placeOrderButton;
    private Map<Product, Integer> selectedProducts; // 商品和对应的购买数量

    public CoffeeShop() {
        menu = new Menu();
        orderManager = new OrderManager();
        discountManager = new DiscountManager();
        selectedProducts = new HashMap<>();
        loadMenuFromFile("src/menu.txt");
        loadOrdersFromFile("src/orders.txt");
        setupGUI();
    }

    private void loadMenuFromFile(String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 5) {
                    menu.addProduct(new Product(parts[0], parts[1], parts[2], parts[3], Double.parseDouble(parts[4])));
                }
            }
        } catch (IOException e) {
            System.out.println("Error loading menu: " + e.getMessage());
        }
    }

    private void loadOrdersFromFile(String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length > 3) {  // 确保至少有 orderId, timestamp 和 customerId
                    String orderId = parts[0];
                    String timestamp = parts[1];
                    String customerId = parts[2];

                    // 创建一个新的 Order 对象
                    Order order = new Order(orderId, timestamp, customerId);

                    // 将所有产品添加到订单中
                    for (int i = 3; i < parts.length; i++) {
                        String productId = parts[i];

                        // 根据产品ID获取对应的产品（假设你有一个 menu 对象）
                        Product product = menu.getProductById(productId);
                        if (product != null) {
                            order.addItem(product, 1);  // 将产品添加到订单
                        }
                    }

                    // 假设你有一个 orders 列表来存储所有的订单
                    orderManager.addOrder(order);
                }
            }
        } catch (IOException e) {
            System.out.println("Error loading orders: " + e.getMessage());
        }
    }

    private void setupGUI() {
        frame = new JFrame("Order Management System");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);
        frame.setLayout(new BorderLayout());

        // 商品面板
        productPanel = new JPanel();
        productPanel.setLayout(new BoxLayout(productPanel, BoxLayout.Y_AXIS));
        for (Product product : menu.getAllProducts()) {
            JPanel itemPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JLabel nameLabel = new JLabel(product.getDetails());
            JButton minusButton = new JButton("-");
            JLabel quantityLabel = new JLabel("0");
            JButton plusButton = new JButton("+");

            minusButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int quantity = selectedProducts.getOrDefault(product, 0);
                    if (quantity > 0) {
                        quantity--;
                        selectedProducts.put(product, quantity);
                        quantityLabel.setText(String.valueOf(quantity));
                    }
                }
            });

            plusButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int quantity = selectedProducts.getOrDefault(product, 0);
                    quantity++;
                    selectedProducts.put(product, quantity);
                    quantityLabel.setText(String.valueOf(quantity));
                }
            });

            itemPanel.add(nameLabel);
            itemPanel.add(minusButton);
            itemPanel.add(quantityLabel);
            itemPanel.add(plusButton);
            productPanel.add(itemPanel);
        }

        JScrollPane productScroll = new JScrollPane(productPanel);
        frame.add(productScroll, BorderLayout.CENTER);

        // 下单按钮
        placeOrderButton = new JButton("Place Order");
        placeOrderButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                placeOrder();
            }
        });
        frame.add(placeOrderButton, BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    private void placeOrder() {
        Order order = new Order(UUID.randomUUID().toString(), new Date().toString(), "customer1");
        for (Map.Entry<Product, Integer> entry : selectedProducts.entrySet()) {
            if (entry.getValue() > 0) {
                order.addItem(entry.getKey(), entry.getValue());
                entry.getKey().incrementOrderCount(entry.getValue()); // 增加商品的点单次数
            }
        }

        if (order.getItems().isEmpty()) {
            JOptionPane.showMessageDialog(frame, "No products selected!");
            return;
        }

        // 计算原始总价
        double totalPrice = order.getTotalPrice();
        // 应用折扣
        totalPrice = discountManager.applyDiscount(null, order);

        // 更新订单的总价
        order.setTotalPrice(totalPrice);

        // 完成订单
        order.completeOrder();
        orderManager.addOrder(order);

        // 显示购买成功弹窗
        JOptionPane.showMessageDialog(frame, "Order Placed Successfully!\n" + order.getOrderDetails());

        // 清空已选商品
        selectedProducts.clear();
        for (Component comp : productPanel.getComponents()) {
            if (comp instanceof JPanel) {
                for (Component subComp : ((JPanel) comp).getComponents()) {
                    if (subComp instanceof JLabel && ((JLabel) subComp).getText().matches("\\d+")) {
                        ((JLabel) subComp).setText("0");
                    }
                }
            }
        }
    }



    public static void main(String[] args) {
        CoffeeShop coffeeShop = new CoffeeShop();
        coffeeShop.frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                coffeeShop.orderManager.generateReport(coffeeShop.menu);
            }
        });
    }
}