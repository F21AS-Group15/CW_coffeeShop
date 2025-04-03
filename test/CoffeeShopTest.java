import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.Timer;
import java.util.stream.Collectors;

// 自定义异常：库存不足异常
class OutOfStockException extends Exception {
    public OutOfStockException(String message) {
        super(message);
    }
}

// 商品类
class Product {
    private String id;
    private String name;
    private String description;
    private String category;
    private double price;
    private int stock;
    private int orderCount; // 记录商品被订购的次数

    public Product(String id, String name, String description, String category, double price, int stock) {
        if (id == null || id.isEmpty()) throw new IllegalArgumentException("产品ID不能为空");
        if (name == null || name.isEmpty()) throw new IllegalArgumentException("产品名称不能为空");
        if (price <= 0) throw new IllegalArgumentException("价格必须大于0");
        if (stock < 0) throw new IllegalArgumentException("库存不能为负数");

        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.price = price;
        this.stock = stock;
        this.orderCount = 0;
    }

    // 获取商品详情
    public String getDetails() {
        return name + ": " + description + " (" + category + ") - $" + price + " (Stock: " + stock + ")";
    }

    // Getter方法
    public double getPrice() { return price; }
    public String getCategory() { return category; }
    public String getId() { return id; }
    public int getOrderCount() { return orderCount; }
    public void incrementOrderCount(int quantity) { this.orderCount += quantity; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    public String getName() { return name; }
    public String getDescription() { return description; }
}

// 订单类
class Order {
    private String orderId;
    private String timeStamp;
    private String customerId;
    private List<Product> items;
    private boolean isCompleted;
    private double totalPrice;
    private String orderType; // 新增字段
    private boolean isBeingProcessed; // 新增字段

    public Order(String orderId, String timestamp, String customerId, String orderType) {
        this.orderId = orderId;
        this.timeStamp = timestamp;
        this.customerId = customerId;
        this.items = new ArrayList<>();
        this.isCompleted = false;
        this.totalPrice = 0;
        this.orderType = orderType;
        this.isBeingProcessed = false;
    }

    // 添加商品到订单
    public void addItem(Product product) { items.add(product); }
    public void addItem(Product product, int amount) {
        for (int i = 0; i < amount; i++) {
            items.add(product);
        }
    }

    public synchronized boolean markAsProcessing() {
        if (isBeingProcessed || isCompleted) {
            return false;
        }
        isBeingProcessed = true;
        return true;
    }


    public synchronized boolean isAvailableForProcessing() {
        return !isBeingProcessed && !isCompleted;
    }

    // 从订单移除商品
    public void deleteItem(Product product) { items.remove(product); }

    // 计算订单总价
    public double getTotalPrice() {
        return totalPrice > 0 ? totalPrice : Math.round(items.stream().mapToDouble(Product::getPrice).sum() * 100.0) / 100.0;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = Math.round(totalPrice * 100.0) / 100.0;
    }

    // 获取订单详情
    public String getOrderDetails() {
        return "Order ID: " + orderId + "\nTime: " + timeStamp + "\nTotal: $" + getTotalPrice();
    }

    // Getter和Setter方法
    public boolean isCompleted() { return isCompleted; }
    public synchronized void completeOrder() {
        isBeingProcessed = false;
        isCompleted = true;
    }
    public void resetOrder() { this.isCompleted = false; }
    public List<Product> getItems() { return items; }
    public String getOrderId() { return orderId; }
    public String getCustomerId() { return customerId; }
    public String getTimeStamp() { return timeStamp; }
    public String getOrderType() { return orderType; }
}

// 菜单类
class Menu {
    private Map<String, Product> products;

    public Menu() {
        this.products = new HashMap<>();
    }

    // 获取所有商品
    public List<Product> getAllProducts() {
        //TODO
        return new ArrayList<>(products.values());
    }

    // 显示菜单
    public void displayMenu() {
        products.values().forEach(p -> System.out.println(p.getDetails()));
    }

    // 根据ID获取商品
    public Product getProductById(String productId) {
        return products.get(productId);
    }

    // 添加商品
    public void addProduct(Product product) {
        if (products.containsKey(product.getId())) {
            throw new IllegalArgumentException("产品ID已存在: " + product.getId());
        }
        products.put(product.getId(), product);
    }

    // 移除商品
    public boolean removeProduct(String productId) {
        return products.remove(productId) != null;
    }

    // 从文件加载菜单
    public void loadFromFile(String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            int lineNum = 0;
            while ((line = br.readLine()) != null) {
                lineNum++;
                try {
                    String[] parts = line.split(",");
                    if (parts.length != 6) {
                        System.err.println("第 " + lineNum + " 行格式错误: 需要6个字段，实际得到 " + parts.length);
                        continue;
                    }

                    // 验证每个字段
                    String id = parts[0].trim();
                    String name = parts[1].trim();
                    String description = parts[2].trim();
                    String category = parts[3].trim();
                    double price = Double.parseDouble(parts[4].trim());
                    int stock = Integer.parseInt(parts[5].trim());

                    addProduct(new Product(id, name, description, category, price, stock));
                } catch (Exception e) {
                    System.err.println("第 " + lineNum + " 行处理错误: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("加载菜单文件错误: " + e.getMessage());
        }
    }
}

// 订单管理类
class OrderManager {
    private List<Order> orders;
    private double totalRevenue;

    // 新增分类统计字段
    private double preOrderRevenue;
    private double walkInRevenue;
    private Map<Product, Integer> preOrderProductCounts;
    private Map<Product, Integer> walkInProductCounts;

    public OrderManager() {
        this.orders = new ArrayList<>();
        this.totalRevenue = 0;
        this.preOrderRevenue = 0;
        this.walkInRevenue = 0;
        this.preOrderProductCounts = new HashMap<>();
        this.walkInProductCounts = new HashMap<>();
    }

    public void addOrder(Order order) {
        orders.add(order);

        // 更新营收统计
        double orderTotal = order.getTotalPrice();
        totalRevenue += orderTotal;

        if ("PRE_ORDER".equals(order.getOrderType())) {
            preOrderRevenue += orderTotal;
            updateProductCounts(order, preOrderProductCounts);
        } else {
            walkInRevenue += orderTotal;
            updateProductCounts(order, walkInProductCounts);
        }
    }

    // 生成销售报告
    public void generateReport(Menu menu) {
        System.out.println("=== 每日销售报告 ===");
        System.out.println("\n=== 预定订单 ===");
        printCategoryReport(menu, getPreOrderProductCounts(), getPreOrderRevenue());

        System.out.println("\n=== 现场订单 ===");
        printCategoryReport(menu, getWalkInProductCounts(), getWalkInRevenue());

        System.out.println("\n=== 全体订单汇总 ===");
        System.out.println("总销售额: ?" + String.format("%.2f", totalRevenue));
        System.out.println("总订单数: " + orders.size());
        System.out.println("其中:");
        System.out.println("- 预定订单: " + preOrderProductCounts.values().stream().mapToInt(i->i).sum() + " 件商品");
        System.out.println("- 现场订单: " + walkInProductCounts.values().stream().mapToInt(i->i).sum() + " 件商品");
    }

    private void printCategoryReport(Menu menu, Map<Product, Integer> productCounts, double revenue) {
        System.out.println("商品销售明细:");
        productCounts.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .forEach(entry -> {
                    Product p = entry.getKey();
                    System.out.printf("%-20s ×%-4d ?%-8.2f (小计: ?%.2f)\n",
                            p.getName(),
                            entry.getValue(),
                            p.getPrice(),
                            p.getPrice() * entry.getValue());
                });
        System.out.println("----------------------------");
        System.out.println("该类订单总销售额: ?" + String.format("%.2f", revenue));
        System.out.println("商品种类数: " + productCounts.size());
    }

    private void updateProductCounts(Order order, Map<Product, Integer> countsMap) {
        for (Product product : order.getItems()) {
            countsMap.put(product, countsMap.getOrDefault(product, 0) + 1);
            product.incrementOrderCount(1); // 保留原有的总计数
        }
    }

    // 新增分类统计方法
    public Map<Product, Integer> getPreOrderProductCounts() {
        return new HashMap<>(preOrderProductCounts);
    }

    public Map<Product, Integer> getWalkInProductCounts() {
        return new HashMap<>(walkInProductCounts);
    }

    public double getPreOrderRevenue() {
        return preOrderRevenue;
    }

    public double getWalkInRevenue() {
        return walkInRevenue;
    }

    // 获取所有订单
    public List<Order> getOrders() {
        return orders;
    }

    // 移除订单
    public boolean removeOrder(String orderId) {
        return orders.removeIf(o -> o.getOrderId().equals(orderId));
    }

    // 增加总营收
    public void addToTotalRevenue(double amount) {
        this.totalRevenue += amount;
    }

    // 从文件加载订单
    public void loadFromFile(String filePath, Menu menu) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            int lineNum = 0;
            Map<String, Order> orderMap = new HashMap<>(); // 用于合并相同订单

            while ((line = br.readLine()) != null) {
                lineNum++;
                try {
                    String[] parts = line.split(",");
                    if (parts.length != 6) {
                        System.err.println("第 " + lineNum + " 行格式错误: 需要6个字段，实际得到 " + parts.length);
                        continue;
                    }

                    String orderId = parts[0].trim();
                    String timestamp = parts[1].trim();
                    String customerId = parts[2].trim();
                    String productId = parts[3].trim();
                    int quantity = Integer.parseInt(parts[4].trim());
                    String orderType = parts[5].trim();

                    Product product = menu.getProductById(productId);
                    if (product == null) {
                        System.err.println("第 " + lineNum + " 行错误: 产品ID不存在 - " + productId);
                        continue;
                    }

                    // 合并相同订单
                    Order order = orderMap.get(orderId);
                    if (order == null) {
                        order = new Order(orderId, timestamp, customerId, orderType);
                        orderMap.put(orderId, order);
                    }

                    // 添加商品到订单
                    if (quantity > 0) {
                        if (quantity > product.getStock()) {
                            System.err.println("第 " + lineNum + " 行警告: 订单数量超过当前库存 - 产品: " + product.getName() +
                                    ", 订单量: " + quantity + ", 库存: " + product.getStock());
                            quantity = Math.min(quantity, product.getStock());
                            if (quantity == 0) continue;
                        }
                        order.addItem(product, quantity);
                    }

                } catch (Exception e) {
                    System.err.println("第 " + lineNum + " 行处理错误: " + e.getMessage());
                }
            }

            // 将所有合并后的订单添加到订单管理器
            orderMap.values().forEach(this::addOrder);

        } catch (IOException e) {
            System.err.println("加载订单文件错误: " + e.getMessage());
        }
    }
}

// 折扣计算器类（重构后的核心折扣逻辑）
class DiscountCalculator {
    private static final double CAKE_PRICE = 4.0; // 蛋糕单价

    // 折扣计算结果
    static class DiscountResult {
        static final DiscountResult NO_DISCOUNT = new DiscountResult(false, "无可用折扣", 0.0);

        final boolean applied;      // 是否应用了折扣
        final String description;   // 折扣描述
        final double discountAmount; // 折扣金额

        DiscountResult(boolean applied, String description, double discountAmount) {
            this.applied = applied;
            this.description = description;
            this.discountAmount = discountAmount;
        }
    }

    /**
     * 计算最佳折扣（按照优先级顺序）
     * 优先级规则：蛋糕折扣 → 套餐折扣 → 默认折扣
     */
    public DiscountResult calculateBestDiscount(Order originalOrder) {
        // 创建不包含蛋糕的订单副本用于后续折扣计算
        Order orderWithoutCakes = createOrderWithoutCakes(originalOrder);

        // 按优先级顺序计算各种折扣
        DiscountResult cakeDiscount = applyCakeDiscount(originalOrder);
        DiscountResult mealDealDiscount = applyMealDealDiscount(orderWithoutCakes);
        DiscountResult defaultDiscount = applyDefaultDiscount(orderWithoutCakes);

        // 根据优先级选择最优折扣
        if (cakeDiscount.applied) {
            return cakeDiscount;
        }
        if (mealDealDiscount.applied) {
            return mealDealDiscount;
        }
        return defaultDiscount;
    }

    // 创建不包含蛋糕的订单副本
    private Order createOrderWithoutCakes(Order originalOrder) {
        Order orderWithoutCakes = new Order(
                originalOrder.getOrderId() + "-nocakes",
                originalOrder.getTimeStamp(),
                originalOrder.getCustomerId(),
                originalOrder.getOrderType()
        );

        for (Product product : originalOrder.getItems()) {
            if (!"Cake".equals(product.getName())) {
                orderWithoutCakes.addItem(product);
            }
        }
        return orderWithoutCakes;
    }

    // 应用蛋糕折扣规则：买3送1
    private DiscountResult applyCakeDiscount(Order order) {
        int cakeCount = countCakes(order);
        if (cakeCount >= 3) {
            int freeCakes = cakeCount / 3;
            double discountAmount = freeCakes * CAKE_PRICE;
            return new DiscountResult(
                    true,
                    "蛋糕买三送一 (减?" + discountAmount + ")",
                    discountAmount
            );
        }
        return DiscountResult.NO_DISCOUNT;
    }

    // 应用套餐折扣规则：2份食品+1份饮料打8折
    private DiscountResult applyMealDealDiscount(Order order) {
        int foodCount = countItemsByCategory(order, "Food");
        int beverageCount = countItemsByCategory(order, "Beverage");

        if (foodCount >= 2 && beverageCount >= 1) {
            double originalPrice = order.getTotalPrice();
            double discountedPrice = originalPrice * 0.8;
            return new DiscountResult(
                    true,
                    "套餐优惠 (8折)",
                    originalPrice - discountedPrice
            );
        }
        return DiscountResult.NO_DISCOUNT;
    }

    // 应用默认折扣规则：
    // - 满50元打8折
    // - 满30元减5元
    // - 满20元减2元
    private DiscountResult applyDefaultDiscount(Order order) {
        double originalPrice = order.getTotalPrice();

        if (originalPrice >= 50) {
            double discountedPrice = originalPrice * 0.8;
            return new DiscountResult(
                    true,
                    "满50元8折优惠",
                    originalPrice - discountedPrice
            );
        } else if (originalPrice >= 30) {
            return new DiscountResult(
                    true,
                    "满30元减5元",
                    5.0
            );
        } else if (originalPrice >= 20) {
            return new DiscountResult(
                    true,
                    "满20元减2元",
                    2.0
            );
        }
        return DiscountResult.NO_DISCOUNT;
    }

    // 统计蛋糕数量
    private int countCakes(Order order) {
        return (int) order.getItems().stream()
                .filter(p -> "Cake".equals(p.getName()))
                .count();
    }

    // 按分类统计商品数量
    private int countItemsByCategory(Order order, String category) {
        return (int) order.getItems().stream()
                .filter(p -> category.equals(p.getCategory()))
                .count();
    }
}
// 新增的日志类（单例模式）
class CoffeeShopLogger {
    private static CoffeeShopLogger instance;
    private StringBuilder log;

    private CoffeeShopLogger() {
        log = new StringBuilder();
        log.append("=== 咖啡店模拟日志 ===\n");
    }

    public static synchronized CoffeeShopLogger getInstance() {
        if (instance == null) {
            instance = new CoffeeShopLogger();
        }
        return instance;
    }

    public synchronized void logEvent(String message) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String logEntry = "[" + timestamp + "] " + message + "\n";
        log.append(logEntry);
        System.out.print(logEntry);
    }

    public synchronized void saveToFile(String filePath) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write(log.toString());
        }
    }
}

// 订单队列（线程安全）
class OrderQueue {
    private Queue<Order> preOrderQueue = new LinkedList<>(); // 高优先级队列（线上订单）
    private Queue<Order> walkInQueue = new LinkedList<>();   // 低优先级队列（现场订单）
    private int maxSize;
    private List<QueueObserver> observers = new ArrayList<>();

    public OrderQueue(int maxSize) {
        this.maxSize = maxSize;
    }
    private void notifyObservers() {
        List<Order> snapshot = getQueueSnapshot();
        for (QueueObserver observer : observers) {
            observer.updateQueue(snapshot);
        }
    }
    public synchronized int getQueueSize() {
        return preOrderQueue.size() + walkInQueue.size();
    }
    public void addObserver(QueueObserver observer) {
        observers.add(observer);
    }
    public synchronized void addOrder(Order order) throws InterruptedException {
        while (getTotalSize() >= maxSize) {
            wait();
        }

        // 根据订单类型添加到不同队列
        if ("PRE_ORDER".equals(order.getOrderType())) {
            preOrderQueue.add(order);
        } else {
            walkInQueue.add(order);
        }

        notifyAll();
        notifyObservers();
    }

    public synchronized Order getNextOrder() throws InterruptedException {
        while (true) {
            // 1. 检查队列是否为空
            while (getTotalSize() == 0) {
                wait(); // 等待新订单到来
            }

            // 2. 尝试获取订单
            Order order = !preOrderQueue.isEmpty() ? preOrderQueue.peek() : walkInQueue.peek();

            // 3. 成功获取可处理的订单
            if (order != null && order.markAsProcessing()) {
                if (!preOrderQueue.isEmpty()) {
                    preOrderQueue.poll();
                } else {
                    walkInQueue.poll();
                }
                notifyAll(); // 通知其他等待的线程
                return order;
            }

            // 4. 订单已被其他服务员获取，短暂等待后重试
            wait(100); // 添加短暂延迟防止忙等待
        }
    }

    private int getTotalSize() {
        return preOrderQueue.size() + walkInQueue.size();
    }

    public synchronized List<Order> getQueueSnapshot() {
        List<Order> snapshot = new ArrayList<>();
        snapshot.addAll(preOrderQueue);  // 线上订单在前
        snapshot.addAll(walkInQueue);   // 现场订单在后
        return snapshot;
    }
}

// 服务员线程
class ServerThread extends Thread {
    private OrderQueue orderQueue;
    private Order currentOrder;
    private boolean running;
    private int serveTime; // 服务时间(毫秒)

    public ServerThread(String name, OrderQueue orderQueue, int serveTime) {
        super(name);
        this.orderQueue = orderQueue;
        this.serveTime = serveTime;
        this.running = true;
    }

    // 新增运行状态控制方法
    public synchronized void setRunning(boolean running) {
        this.running = running;
        if (running) {
            this.notify(); // 如果从停止状态恢复运行，则唤醒线程
        }
    }

    public synchronized boolean isRunning() {
        return running;
    }

    @Override
    public void run() {
        CoffeeShopLogger.getInstance().logEvent(getName() + " 开始工作");
        while (running || orderQueue.getQueueSize() > 0) {
            try {
                synchronized (this) {
                    while (!running && orderQueue.getQueueSize() > 0) {
                        wait();
                    }
                    if (!running) {
                        break;
                    }
                }

                Order order = orderQueue.getNextOrder();
                if (order == null) continue; // 安全检查

                currentOrder = order;
                try {
                    CoffeeShopLogger.getInstance().logEvent(getName() + " 开始处理订单: " + order.getOrderId());

                    // 处理时间与商品数量成正比
                    int processingTime = serveTime * order.getItems().size();
                    Thread.sleep(processingTime);

                    order.completeOrder();
                    CoffeeShopLogger.getInstance().logEvent(getName() + " 完成订单: " + order.getOrderId());
                } finally {
                    currentOrder = null;
                }
            } catch (InterruptedException e) {
                if (!running) {
                    break; // 正常停止
                }
                CoffeeShopLogger.getInstance().logEvent(getName() + " 处理被中断");
            }
        }
        CoffeeShopLogger.getInstance().logEvent(getName() + " 结束工作");
    }

    private void processFullOrder(Order order) throws InterruptedException {
        // 计算基于商品数量的处理时间
        int processingTime = serveTime * order.getItems().size();
        Thread.sleep(processingTime);
    }

    public void stopWorking() {
        this.running = false;
        this.interrupt();
    }

    public Order getCurrentOrder() {
        return currentOrder;
    }
}

//顾客生成线程
class CustomerGeneratorThread extends Thread {
    private OrderQueue orderQueue;
    private List<Order> preOrders;
    private boolean running;

    public CustomerGeneratorThread(OrderQueue orderQueue, List<Order> preOrders) {
        this.orderQueue = orderQueue;
        this.preOrders = new ArrayList<>(preOrders);
        this.running = true;
    }

    @Override
    public void run() {
        CoffeeShopLogger.getInstance().logEvent("开始处理预定订单");

        // 先处理所有线上预定订单
        for (Order order : preOrders) {
            if (!running) break;

            try {
                orderQueue.addOrder(order);
                Thread.sleep(1000); // 控制订单生成速度
            } catch (InterruptedException e) {
                break;
            }
        }

        CoffeeShopLogger.getInstance().logEvent("所有预定订单已加入队列");
    }


    public void stopGenerating() {
        this.running = false;
        this.interrupt();
    }
}

// 观察者接口
interface QueueObserver {
    void updateQueue(List<Order> orders);
    void updateServers(List<ServerThread> servers);
}

// 咖啡店模拟控制器
class CoffeeShopSimulator {
    private OrderQueue orderQueue;
    private List<ServerThread> servers;
    private CustomerGeneratorThread customerGenerator;
    private List<QueueObserver> observers;
    private Timer observationTimer; //TODO:UTIL/SWING
    private  OrderManager orderManager; // 新增OrderManager引用
    private volatile boolean isRunning = false;


    // 新增模拟控制相关变量
    private CoffeeShopSimulator simulator;
    private JButton startSimulationButton;
    private JButton stopSimulationButton;
    private JSlider speedSlider;
    private JTextArea queueTextArea;
    private JTextArea serversTextArea;

    public CoffeeShopSimulator(int queueSize,
                               int serverCount,
                               int serveTime,
                               OrderManager orderManager) { // 新增参数
        this.orderQueue = new OrderQueue(queueSize);
        this.servers = new ArrayList<>();
        this.observers = new ArrayList<>();
        this.orderManager = orderManager; // 初始化orderManager

        // 初始化服务员线程
        for (int i = 1; i <= serverCount; i++) {
            servers.add(new ServerThread("服务员 " + i, orderQueue, serveTime));
        }

        // 初始化顾客生成线程（使用orderManager中的订单）
        this.customerGenerator = new CustomerGeneratorThread(
                orderQueue,
                new ArrayList<>(orderManager.getOrders()) // 使用订单副本
        );

        startObservation(1000); // 启动观察定时器
    }

    public OrderQueue getOrderQueue() {
        return this.orderQueue;
    }


    public synchronized void startSimulation() {
        if (isRunning) {
            return;
        }
        isRunning = true;

        // 1. 初始化观察定时器
        startObservation(1000);

        // 2. 启动所有服务员线程
        CoffeeShopLogger.getInstance().logEvent("启动 " + servers.size() + " 个服务员线程");
        servers.forEach(server -> {
            server.setRunning(true);
            server.start();
        });

        // 3. 启动顾客生成线程（只处理未完成的预定订单）
        List<Order> pendingPreOrders = orderManager.getOrders().stream()
                .filter(order -> !order.isCompleted() && "PRE_ORDER".equals(order.getOrderType()))
                .collect(Collectors.toList());

        customerGenerator = new CustomerGeneratorThread(orderQueue, pendingPreOrders);
        customerGenerator.start();

        // 4. 记录模拟开始状态
        CoffeeShopLogger.getInstance().logEvent(
                "模拟开始 - 待处理预定订单: " + pendingPreOrders.size() +
                        ", 服务员数量: " + servers.size()
        );

        // 5. 通知观察者初始状态
        notifyObservers();
    }

    public synchronized void stopSimulation() {
        if (!isRunning) {
            return;
        }
        isRunning = false;

        // 1. 停止顾客生成
        if (customerGenerator != null) {
            customerGenerator.stopGenerating();
        }

        // 2. 停止所有服务员
        servers.forEach(ServerThread::stopWorking);

        // 3. 停止观察定时器
        if (observationTimer != null) {
            observationTimer.cancel();
        }

        // 4. 等待队列处理完成
        waitForQueueToEmpty();

        // 5. 记录模拟结束
        CoffeeShopLogger.getInstance().logEvent("模拟结束 - 已处理订单: " +
                orderManager.getOrders().stream().filter(Order::isCompleted).count());
    }

    private void waitForQueueToEmpty() {
        int waitCount = 0;
        while (orderQueue.getQueueSize() > 0 && waitCount++ < 10) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void startObservation(int interval) {
        observationTimer = new Timer();
        observationTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                notifyObservers();
            }
        }, 0, interval);
    }

    public void addObserver(QueueObserver observer) {
        observers.add(observer);
    }


    private void stopObservation() {
        if (observationTimer != null) {
            observationTimer.cancel();
        }
    }

    private synchronized void notifyObservers() {
        List<Order> queueSnapshot = orderQueue.getQueueSnapshot();
        List<ServerThread> serversSnapshot = new ArrayList<>(servers);

        for (QueueObserver observer : observers) {
            observer.updateQueue(queueSnapshot);
            observer.updateServers(serversSnapshot);
        }
    }
}

// 咖啡店主类
public class CoffeeShop implements QueueObserver {
    private List<JLabel> quantityLabels = new ArrayList<>();
    private Menu menu;
    private OrderManager orderManager;
    private DiscountCalculator discountCalculator;  // 使用新的折扣计算器
    private JFrame frame;
    private JPanel productPanel;
    private JTextArea orderDetails;
    private JButton placeOrderButton;
    private Map<Product, Integer> selectedProducts;
    private Map<String, JLabel> stockLabels = new HashMap<>();
    private JLabel totalPriceLabel;
    private JLabel discountedPriceLabel;
    private JTextArea orderSummaryArea;
    private StringBuilder allOrderSummaries = new StringBuilder();
    private CoffeeShopSimulator simulator;
    private JPanel simulationPanel;
    private JTextArea queueTextArea;
    private JTextArea serversTextArea;
    private JButton startSimulationButton;
    private JButton stopSimulationButton;
    private JSlider speedSlider;

    public boolean isSimulationRunning() {
        return simulator != null;
    }

    public CoffeeShop() {
        menu = new Menu();
        orderManager = new OrderManager();  // 初始化orderManager
        discountCalculator = new DiscountCalculator();
        selectedProducts = new HashMap<>();
        allOrderSummaries = new StringBuilder();


        menu.loadFromFile("menu.txt");
        orderManager.loadFromFile("pre_orders.txt", menu);  // 加载预定订单
        setupGUI();

        // 初始化显示
        SwingUtilities.invokeLater(() -> {
            queueTextArea.setText("=== 当前队列状态 ===\n\n" +
                    "=== 预定订单 ===\n" +
                    "待处理: " + orderManager.getOrders().size() + "\n\n" +
                    "=== 现场订单 ===\n" +
                    "待处理: 0\n\n" +
                    "请点击【开始模拟】启动系统");
        });
    }

    // GUI初始化方法（保持不变）
    private void setupGUI() {
        frame = new JFrame("咖啡店订单管理系统");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 700);
        frame.setLayout(new BorderLayout());
        placeOrderButton = new JButton("确认下单");
        placeOrderButton.setEnabled(false); // 默认禁用，直到模拟开始
        // 渐变背景面板
        JPanel backgroundPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                Color color1 = new Color(255, 248, 240);  // 浅米色
                Color color2 = new Color(220, 240, 255);  // 浅蓝色
                GradientPaint gp = new GradientPaint(0, 0, color1, getWidth(), getHeight(), color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        backgroundPanel.setLayout(new BorderLayout());
        frame.setContentPane(backgroundPanel);

        // 顶部价格信息面板
        JPanel priceInfoPanel = new JPanel(new GridLayout(1, 4, 10, 0));
        priceInfoPanel.setOpaque(false);
        priceInfoPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JLabel totalLabel = new JLabel("商品总价:");
        totalLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 16));
        priceInfoPanel.add(totalLabel);

        totalPriceLabel = new JLabel("?0.00");
        totalPriceLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 16));
        totalPriceLabel.setForeground(new Color(0, 100, 0));  // 深绿色
        priceInfoPanel.add(totalPriceLabel);

        JLabel discountLabel = new JLabel("折后价格:");
        discountLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 16));
        priceInfoPanel.add(discountLabel);

        discountedPriceLabel = new JLabel("?0.00");
        discountedPriceLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 16));
        discountedPriceLabel.setForeground(new Color(200, 0, 0));  // 深红色
        priceInfoPanel.add(discountedPriceLabel);

        backgroundPanel.add(priceInfoPanel, BorderLayout.NORTH);

        // 主选项卡面板
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setOpaque(false);

        // ========== 商品面板 ==========
        productPanel = new JPanel();
        productPanel.setLayout(new GridLayout(0, 2, 15, 15));
        productPanel.setOpaque(false);
        productPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // 清空库存标签映射
        stockLabels.clear();

        for (Product product : menu.getAllProducts()) {
            // 创建商品卡片面板
            JPanel cardPanel = new JPanel(new BorderLayout()) {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setColor(new Color(255, 255, 255, 220));
                    g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                    g2d.setColor(new Color(200, 200, 200, 100));
                    g2d.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 15, 15);
                }
            };
            cardPanel.setOpaque(false);
            cardPanel.setPreferredSize(new Dimension(380, 180));

            // 商品信息面板
            JPanel infoPanel = new JPanel();
            infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
            infoPanel.setOpaque(false);
            infoPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

            // 商品名称
            JLabel nameLabel = new JLabel("<html><b><font size=+1>" + product.getName() + "</font></b></html>");
            nameLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
            infoPanel.add(nameLabel);
            infoPanel.add(Box.createVerticalStrut(5));

            // 商品描述
            JTextArea descriptionArea = new JTextArea(product.getDescription());
            descriptionArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
            descriptionArea.setLineWrap(true);
            descriptionArea.setWrapStyleWord(true);
            descriptionArea.setEditable(false);
            descriptionArea.setOpaque(false);
            descriptionArea.setMaximumSize(new Dimension(350, 40));
            infoPanel.add(descriptionArea);
            infoPanel.add(Box.createVerticalStrut(5));

            // 商品分类和价格
            JPanel detailPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
            detailPanel.setOpaque(false);

            JLabel categoryLabel = new JLabel("分类: " + product.getCategory());
            categoryLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
            detailPanel.add(categoryLabel);

            JLabel priceLabel = new JLabel("价格: ?" + product.getPrice());
            priceLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 13));
            priceLabel.setForeground(new Color(0, 100, 0));
            detailPanel.add(priceLabel);

            infoPanel.add(detailPanel);

            // 库存信息
            JLabel stockLabel = new JLabel("库存: " + product.getStock());
            stockLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
            stockLabel.setForeground(new Color(100, 100, 100));
            infoPanel.add(stockLabel);

            // 将库存标签存入映射
            stockLabels.put(product.getId(), stockLabel);

            // 数量选择面板
            JPanel quantityPanel = new JPanel();
            quantityPanel.setOpaque(false);
            quantityPanel.setLayout(new BoxLayout(quantityPanel, BoxLayout.X_AXIS));
            quantityPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));

            JButton minusButton = new JButton("-");
            minusButton.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
            minusButton.setPreferredSize(new Dimension(30, 30));
            minusButton.setBackground(new Color(255, 150, 150));
            minusButton.setForeground(Color.WHITE);
            minusButton.setFocusPainted(false);

            JLabel quantityLabel = new JLabel("0");
            quantityLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
            quantityLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            quantityLabels.add(quantityLabel);

            JButton plusButton = new JButton("+");
            plusButton.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
            plusButton.setPreferredSize(new Dimension(30, 30));
            plusButton.setBackground(new Color(150, 200, 150));
            plusButton.setForeground(Color.WHITE);
            plusButton.setFocusPainted(false);

            // 按钮事件处理
            minusButton.addActionListener(e -> {
                int quantity = selectedProducts.getOrDefault(product, 0);
                if (quantity > 0) {
                    quantity--;
                    selectedProducts.put(product, quantity);
                    quantityLabel.setText(String.valueOf(quantity));
                    updatePriceDisplay();
                }
            });

            plusButton.addActionListener(e -> {
                try {
                    int quantity = selectedProducts.getOrDefault(product, 0);
                    if (quantity >= product.getStock()) {
                        throw new OutOfStockException(
                                "库存不足！\n" + product.getName() +
                                        " 剩余: " + product.getStock() +
                                        "\n最大可添加数量: " + product.getStock()
                        );
                    }
                    quantity++;
                    selectedProducts.put(product, quantity);
                    quantityLabel.setText(String.valueOf(quantity));
                    updatePriceDisplay();
                } catch (OutOfStockException ex) {
                    JOptionPane.showMessageDialog(
                            frame,
                            ex.getMessage(),
                            "库存警告",
                            JOptionPane.WARNING_MESSAGE
                    );
                }
            });

            quantityPanel.add(minusButton);
            quantityPanel.add(quantityLabel);
            quantityPanel.add(plusButton);
            infoPanel.add(quantityPanel);

            cardPanel.add(infoPanel, BorderLayout.CENTER);
            productPanel.add(cardPanel);
        }

        JScrollPane productScroll = new JScrollPane(productPanel);
        productScroll.setOpaque(false);
        productScroll.getViewport().setOpaque(false);
        tabbedPane.addTab("商品菜单", productScroll);

        // ========== 订单详情面板 ==========
        JPanel orderPanel = new JPanel(new BorderLayout());
        orderPanel.setOpaque(false);
        orderPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        orderSummaryArea = new JTextArea();
        orderSummaryArea.setEditable(false);
        orderSummaryArea.setOpaque(false);
        orderSummaryArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        orderSummaryArea.setLineWrap(true);
        orderSummaryArea.setWrapStyleWord(true);

        // 加载所有历史订单
//        loadAllOrdersToDisplay();

        JScrollPane orderScroll = new JScrollPane(orderSummaryArea);
        orderScroll.setOpaque(false);
        orderScroll.getViewport().setOpaque(false);
        orderPanel.add(orderScroll, BorderLayout.CENTER);

        tabbedPane.addTab("订单详情", orderPanel);

        // ========== 折扣信息面板 ==========
        JPanel discountPanel = new JPanel(new BorderLayout());
        discountPanel.setOpaque(false);
        discountPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        JTextArea discountTextArea = new JTextArea();
        discountTextArea.setEditable(false);
        discountTextArea.setOpaque(false);
        discountTextArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        discountTextArea.setLineWrap(true);
        discountTextArea.setWrapStyleWord(true);

        String discountInfo = "当前优惠活动:\n\n" +
                "1. 蛋糕特惠\n" +
                "   - 购买3个或以上蛋糕，可免费获得1个\n\n" +
                "2. 套餐优惠\n" +
                "   - 2份食品 + 1份饮料可享8折优惠\n\n" +
                "3. 常规优惠\n" +
                "   - 满?20减?2\n" +
                "   - 满?30减?5\n" +
                "   - 满?50享8折优惠\n\n" +
                "注：优惠不可叠加使用，系统将自动应用最优折扣";

        discountTextArea.setText(discountInfo);

        JScrollPane discountScroll = new JScrollPane(discountTextArea);
        discountScroll.setOpaque(false);
        discountScroll.getViewport().setOpaque(false);
        discountPanel.add(discountScroll, BorderLayout.CENTER);

        tabbedPane.addTab("优惠信息", discountPanel);

        backgroundPanel.add(tabbedPane, BorderLayout.CENTER);

        // ========== 下单按钮 ==========
        placeOrderButton = new JButton("确认下单");
        placeOrderButton.setFont(new Font("Microsoft YaHei", Font.BOLD, 16));
        placeOrderButton.setBackground(new Color(70, 130, 180));
        placeOrderButton.setForeground(Color.WHITE);
        placeOrderButton.setFocusPainted(false);
        placeOrderButton.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        placeOrderButton.addActionListener(e -> placeOrder());

        backgroundPanel.add(placeOrderButton, BorderLayout.SOUTH);

        frame.setVisible(true);
        addSimulationTab();
    }

    private void addSimulationTab() {
        JPanel simulationPanel = new JPanel(new BorderLayout());
        simulationPanel.setOpaque(false);

        // 控制面板
        JPanel controlPanel = new JPanel(new FlowLayout());
        controlPanel.setOpaque(false);

        // 初始化按钮
        startSimulationButton = new JButton("开始模拟");
        startSimulationButton.addActionListener(e -> startSimulation());

        stopSimulationButton = new JButton("停止模拟");
        stopSimulationButton.setEnabled(false);
        stopSimulationButton.addActionListener(e -> stopSimulation());

        // 速度调节滑块
        speedSlider = new JSlider(500, 5000, 2000);
        speedSlider.setMajorTickSpacing(1000);
        speedSlider.setPaintTicks(true);
        speedSlider.setPaintLabels(true);

        controlPanel.add(startSimulationButton);
        controlPanel.add(stopSimulationButton);
        controlPanel.add(new JLabel("模拟速度:"));
        controlPanel.add(speedSlider);

        // 状态显示区域
        queueTextArea = new JTextArea(10, 30);
        queueTextArea.setEditable(false);
        serversTextArea = new JTextArea(10, 30);
        serversTextArea.setEditable(false);

        JPanel displayPanel = new JPanel(new GridLayout(1, 2));
        displayPanel.add(new JScrollPane(queueTextArea));
        displayPanel.add(new JScrollPane(serversTextArea));

        simulationPanel.add(controlPanel, BorderLayout.NORTH);
        simulationPanel.add(displayPanel, BorderLayout.CENTER);

        // 添加到主界面
        ((JTabbedPane)frame.getContentPane().getComponent(1)).addTab("模拟控制", simulationPanel);
    }

    private void startSimulation() {
        startSimulationButton.setEnabled(false);
        stopSimulationButton.setEnabled(true);

        // 确保 orderManager 已初始化
        if (orderManager == null) {
            orderManager = new OrderManager();
            try {
                orderManager.loadFromFile("pre_orders.txt", menu);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(frame, "无法加载订单: " + e.getMessage());
                return;
            }
        }

        // 初始化模拟器
        int serveTime = speedSlider.getValue();
        simulator = new CoffeeShopSimulator(
                100,
                2,
                serveTime,
                orderManager
        );

        // 添加观察者
        simulator.addObserver(this);
        simulator.getOrderQueue().addObserver(this);

        // 预加载订单到队列
        orderManager.getOrders().forEach(order -> {
            try {
                simulator.getOrderQueue().addOrder(order);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        simulator.startSimulation();
        CoffeeShopLogger.getInstance().logEvent("模拟启动，服务间隔: " + serveTime + "ms");

        // 初始显示（通过队列快照触发updateQueue）
        updateQueue(simulator.getOrderQueue().getQueueSnapshot());
    }

    private void stopSimulation() {
        startSimulationButton.setEnabled(true);
        stopSimulationButton.setEnabled(false);

        if (simulator != null) {
            simulator.stopSimulation();
            CoffeeShopLogger.getInstance().logEvent("模拟已手动停止");
        }

        // 保存当前状态
        try {
            CoffeeShopLogger.getInstance().saveToFile("coffee_shop_log.txt");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "日志保存失败: " + e.getMessage());
        }
    }

    private void updateQueueDisplay() {
        if (simulator != null) {
            List<Order> queueSnapshot = simulator.getOrderQueue().getQueueSnapshot();
            SwingUtilities.invokeLater(() -> {
                StringBuilder queueText = new StringBuilder();

                // 统计各类订单数量
                long preOrderCount = queueSnapshot.stream()
                        .filter(o -> "PRE_ORDER".equals(o.getOrderType()))
                        .count();
                long walkInCount = queueSnapshot.stream()
                        .filter(o -> "WALK_IN".equals(o.getOrderType()))
                        .count();

                queueText.append("当前队列状态:\n\n");
                queueText.append("=== 预定订单 ===\n");
                queueText.append("待处理: ").append(preOrderCount).append("\n\n");

                queueText.append("=== 现场订单 ===\n");
                queueText.append("待处理: ").append(walkInCount).append("\n");

                queueTextArea.setText(queueText.toString());
            });
        }
    }

    @Override
    public void updateQueue(List<Order> orders) {
        SwingUtilities.invokeLater(() -> {
            // 按订单ID分组，合并相同订单的显示
            Map<String, List<Order>> groupedOrders = orders.stream()
                    .collect(Collectors.groupingBy(Order::getOrderId));

            StringBuilder queueText = new StringBuilder();
            queueText.append("=== 当前队列状态 ===\n\n");

            // 预定订单
            queueText.append("=== 预定订单 ===\n");
            groupedOrders.values().stream()
                    .filter(list -> "PRE_ORDER".equals(list.get(0).getOrderType()))
                    .forEach(orderList -> appendMergedOrder(queueText, orderList));

            // 现场订单
            queueText.append("\n=== 现场订单 ===\n");
            groupedOrders.values().stream()
                    .filter(list -> "WALK_IN".equals(list.get(0).getOrderType()))
                    .forEach(orderList -> appendMergedOrder(queueText, orderList));

            queueTextArea.setText(queueText.toString());
        });
    }

    private void appendMergedOrder(StringBuilder sb, List<Order> orders) {
        if (orders.isEmpty()) return;

        Order firstOrder = orders.get(0);
        sb.append("订单ID: ").append(firstOrder.getOrderId()).append("\n");
        sb.append("客户: ").append(firstOrder.getCustomerId()).append("\n");
        sb.append("类型: ").append(firstOrder.getOrderType()).append("\n");

        // 合并所有商品
        Map<Product, Long> allProducts = orders.stream()
                .flatMap(order -> order.getItems().stream())
                .collect(Collectors.groupingBy(p -> p, Collectors.counting()));

        allProducts.forEach((product, count) -> {
            sb.append(String.format("  %-15s ×%-2d @ ?%-5.2f\n",
                    product.getName(), count, product.getPrice()));
        });

        double totalPrice = orders.stream()
                .mapToDouble(Order::getTotalPrice)
                .sum();

        sb.append(String.format("总价: ?%.2f\n\n", totalPrice));
    }

    private void appendOrderDetails(StringBuilder sb, Order order) {
        sb.append("订单ID: ").append(order.getOrderId()).append("\n");
        sb.append("客户: ").append(order.getCustomerId()).append("\n");
        sb.append("类型: ").append(order.getOrderType()).append("\n");

        // 按产品分组统计
        Map<Product, Long> productCounts = order.getItems().stream()
                .collect(Collectors.groupingBy(p -> p, Collectors.counting()));

        productCounts.forEach((product, count) -> {
            sb.append(String.format("  %-15s ×%-2d @ ?%-5.2f\n",
                    product.getName(), count, product.getPrice()));
        });

        sb.append(String.format("总价: ?%.2f\n\n", order.getTotalPrice()));
    }

    private void appendOrderInfo(StringBuilder sb, Order order) {
        sb.append("[").append(order.getOrderType()).append("] ");
        sb.append(order.getOrderId()).append(" - ");
        sb.append(order.getItems().size()).append(" 件商品\n");

        // 使用传统循环替代lambda表达式
        Map<String, Long> productCounts = order.getItems().stream()
                .collect(Collectors.groupingBy(Product::getName, Collectors.counting()));

        for (Map.Entry<String, Long> entry : productCounts.entrySet()) {
            sb.append("  ").append(entry.getKey()).append(" ×").append(entry.getValue()).append("\n");
        }
    }

    @Override
    public void updateServers(List<ServerThread> servers) {
        SwingUtilities.invokeLater(() -> {
            StringBuilder serversText = new StringBuilder();
            serversText.append("服务员状态:\n\n");

            for (ServerThread server : servers) {
                serversText.append(server.getName()).append(": ");
                Order currentOrder = server.getCurrentOrder();
                if (currentOrder != null) {
                    serversText.append("正在处理订单 ")
                            .append(currentOrder.getOrderId())
                            .append(" (")
                            .append(currentOrder.getItems().size())
                            .append(" 个商品)\n");
                } else {
                    serversText.append("空闲中\n");
                }
            }

            serversTextArea.setText(serversText.toString());
        });
    }


    // 更新价格显示
    private void updatePriceDisplay() {
        // 计算商品总价
        double totalPrice = selectedProducts.entrySet().stream()
                .mapToDouble(entry -> entry.getKey().getPrice() * entry.getValue())
                .sum();

        // 创建临时订单用于折扣计算
        Order tempOrder = new Order("temp", "temp", "temp","temp");
        selectedProducts.forEach((product, quantity) -> {
            if (quantity > 0) {
                tempOrder.addItem(product, quantity);
            }
        });

        // 计算最佳折扣
        DiscountCalculator.DiscountResult discount = discountCalculator.calculateBestDiscount(tempOrder);
        double discountedPrice = totalPrice - discount.discountAmount;

        // 更新UI显示
        totalPriceLabel.setText(String.format("?%.2f", totalPrice));
        discountedPriceLabel.setText(String.format("?%.2f", discountedPrice));
    }

    /**
     * 处理用户下单逻辑，将订单加入处理队列
     */
    private void placeOrder() {
        // 1. 检查是否选择了商品
        if (selectedProducts.values().stream().allMatch(q -> q == 0)) {
            JOptionPane.showMessageDialog(
                    frame,
                    "您还没有选择任何商品！\n请先添加商品到订单",
                    "订单错误",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        // 2. 检查模拟器是否已初始化
        if (simulator == null) {
            JOptionPane.showMessageDialog(
                    frame,
                    "模拟系统未启动，请先点击【开始模拟】",
                    "系统未就绪",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        // 3. 创建新订单
        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        Order order = new Order(orderId, timestamp, "现场顾客", "WALK_IN");

        // 4. 添加选中商品到订单并构建订单详情
        double totalPrice = 0;
        StringBuilder orderDetails = new StringBuilder();
        orderDetails.append("════════════ 订单详情 ════════════\n\n");
        orderDetails.append("订单编号: ").append(orderId).append("\n");
        orderDetails.append("下单时间: ").append(timestamp).append("\n");
        orderDetails.append("订单类型: 现场订单\n");
        orderDetails.append("────────────────────────────────\n\n");
        orderDetails.append("已购商品:\n");

        for (Map.Entry<Product, Integer> entry : selectedProducts.entrySet()) {
            if (entry.getValue() > 0) {
                Product product = entry.getKey();
                int quantity = entry.getValue();

                // 添加商品到订单
                order.addItem(product, quantity);

                // 计算小计
                double subtotal = product.getPrice() * quantity;
                totalPrice += subtotal;

                // 添加到订单详情
                orderDetails.append(String.format(
                        "? %-15s ×%-2d @ ?%-6.2f = ?%-7.2f\n",
                        product.getName(),
                        quantity,
                        product.getPrice(),
                        subtotal
                ));

                // 更新库存
                product.setStock(product.getStock() - quantity);
                updateProductStockDisplay(product);
            }
        }

        try {
            // 应用折扣
            DiscountCalculator.DiscountResult discount = discountCalculator.calculateBestDiscount(order);
            double discountedPrice = totalPrice - discount.discountAmount;
            order.setTotalPrice(discountedPrice);

            // 将订单加入队列处理
            simulator.getOrderQueue().addOrder(order);
            orderManager.addOrder(order);

            // 更新UI显示
            updateOrderHistoryDisplay(order, orderDetails, totalPrice, discount, discountedPrice);

            // 显示成功消息
            showSuccessMessage(orderId, discountedPrice);

        } catch (InterruptedException e) {
            handleOrderInterrupted();
        } finally {
            resetOrderUI();
        }
    }

    private void handleOrderInterrupted() {
        CoffeeShopLogger.getInstance().logEvent("订单处理被中断");
        JOptionPane.showMessageDialog(
                frame,
                "订单处理被意外中断\n请检查系统状态",
                "处理中断",
                JOptionPane.ERROR_MESSAGE
        );

        // 恢复UI状态
        resetOrderUI();

        // 尝试重新加载库存显示
        menu.getAllProducts().forEach(this::updateProductStockDisplay);
    }

    // 提取出的辅助方法
    private void updateOrderHistoryDisplay(Order order, StringBuilder orderDetails,
                                           double totalPrice,
                                           DiscountCalculator.DiscountResult discount,
                                           double discountedPrice) {
        orderDetails.append("\n────────────────────────────────\n");
        orderDetails.append(String.format("%-20s: ?%7.2f\n", "商品总价", totalPrice));
        orderDetails.append(String.format("%-20s: %-10s\n", "应用优惠", discount.description));
        orderDetails.append(String.format("%-20s: ?%7.2f\n", "折后价格", discountedPrice));
        orderDetails.append("\n════════════════════════════════\n");

        allOrderSummaries.insert(0, orderDetails.toString());
        orderSummaryArea.setText(allOrderSummaries.toString());
        orderSummaryArea.setCaretPosition(0);
    }

    private void showSuccessMessage(String orderId, double discountedPrice) {
        JOptionPane.showMessageDialog(
                frame,
                "订单提交成功！\n\n" +
                        "订单编号: " + orderId + "\n" +
                        "当前队列位置: " + (simulator.getOrderQueue().getQueueSize()) + "\n" +
                        "折后总价: ?" + String.format("%.2f", discountedPrice),
                "订单确认",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void resetOrderUI() {
        selectedProducts.clear();
        for (JLabel quantityLabel : quantityLabels) {
            quantityLabel.setText("0");
        }
        totalPriceLabel.setText("?0.00");
        discountedPriceLabel.setText("?0.00");
    }

    // 更新商品库存显示
    private void updateProductStockDisplay(Product product) {
        SwingUtilities.invokeLater(() -> {
            JLabel stockLabel = stockLabels.get(product.getId());
            if (stockLabel != null) {
                stockLabel.setText("库存: " + product.getStock());

                // 库存紧张时显示红色
                if (product.getStock() < 3) {
                    stockLabel.setForeground(Color.RED);
                } else {
                    stockLabel.setForeground(new Color(100, 100, 100));
                }
            }
        });
    }

    // 主方法
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            CoffeeShop coffeeShop = new CoffeeShop();

            // 添加窗口关闭事件
            coffeeShop.frame.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                    // 保存订单
                    try (BufferedWriter bw = new BufferedWriter(new FileWriter("orders.txt", false))) {
                        for (Order order : coffeeShop.orderManager.getOrders()) {
                            Map<Product, Integer> productQuantityMap = new HashMap<>();
                            for (Product product : order.getItems()) {
                                productQuantityMap.put(product, productQuantityMap.getOrDefault(product, 0) + 1);
                            }

                            for (Map.Entry<Product, Integer> entry : productQuantityMap.entrySet()) {
                                Product product = entry.getKey();
                                int quantity = entry.getValue();

                                if (quantity > 0) {
                                    bw.write(order.getOrderId() + "," +
                                            order.getTimeStamp() + "," +
                                            order.getCustomerId() + "," +
                                            product.getId() + "," +
                                            quantity);
                                    bw.newLine();
                                }
                            }
                        }
                    } catch (IOException e) {
                        System.out.println("写入订单文件错误: " + e.getMessage());
                    }

                    // 生成报告
                    coffeeShop.orderManager.generateReport(coffeeShop.menu);

                    // 停止模拟（如果正在运行）
                    if (coffeeShop.simulator != null) {
                        coffeeShop.simulator.stopSimulation();
                    }
                }
            });
        });
    }
}
