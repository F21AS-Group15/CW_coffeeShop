package controller;

import model.*;
import util.CoffeeShopLogger;
import util.OrderQueue;
import view.MainView;

import javax.swing.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.awt.event.ActionEvent;


public class MainController {
    // region 组件声明
    public DiscountCalculator discountCalculator;
    public MainView view;
    public Menu menu;
    public OrderManager orderManager;
    public CoffeeShopSimulator simulator;
    public Map<Product, Integer> selectedProducts = new HashMap<>();
    // endregion
    public OrderQueue orderQueue;
    public MainController() {
        this.discountCalculator = new DiscountCalculator();
        // 1. 初始化模型
        this.menu = new Menu();
        this.orderManager = new OrderManager();
        this.orderQueue = new OrderQueue(100); // 设置队列容量
        this.simulator = new CoffeeShopSimulator(orderQueue, 2, 2000);
        loadInitialData();


        // 2. 初始化视图
        this.view = new MainView();
        setupEventHandlers();
        initProductDisplay();

        // 3. 显示窗口
        view.setVisible(true);

        if (orderQueue == null) throw new IllegalStateException("orderQueue未初始化");
        if (orderManager == null) throw new IllegalStateException("orderManager未初始化");
        if (menu == null) throw new IllegalStateException("menu未初始化");

        System.out.println("[DEBUG] 初始化完成，组件状态正常");
    }

    // region 初始化方法

    private void loadInitialData() {
        try {
            menu.loadFromFile("D:\\PPSTAR\\github\\untitled\\src\\menu.txt");
            orderManager.loadFromFile("D:\\PPSTAR\\github\\untitled\\src\\pre_orders.txt", menu);
        } catch (Exception e) {
            showErrorDialog("初始化错误", "加载数据失败: " + e.getMessage());
        }
    }

    private void setupEventHandlers() {
        // 订单按钮
        view.setOrderButtonListener(this::handlePlaceOrder);

        // 模拟控制按钮
        view.setStartSimulationListener(e -> startSimulation());
        view.setStopSimulationListener(e -> stopSimulation());
    }

    private void initProductDisplay() {
        menu.getAllProducts().forEach(product -> {
            view.addProductCard(
                    product,
                    e -> handleAddProduct(product),
                    e -> handleRemoveProduct(product)
            );

            // 初始化选中数量
            selectedProducts.put(product, 0);
        });
    }
    // endregion

    // region 订单处理逻辑
    private void handlePlaceOrder(ActionEvent e) {
        if (selectedProducts.values().stream().allMatch(q -> q == 0)) {
            showWarningDialog("订单错误", "请至少选择一件商品");
            return;
        }

        if (simulator == null || !simulator.isRunning()) {
            showWarningDialog("系统未就绪", "请先启动模拟系统");
            return;
        }

        try {
            // 1. 创建新订单
            Order newOrder = createNewOrder();

            // 2. 添加商品到订单
            addItemsToOrder(newOrder);

            // 3. 计算并应用折扣
            applyDiscounts(newOrder);

            // 4. 提交订单到系统
            submitOrder(newOrder);

            // 5. 更新UI状态
            resetOrderUI();
            showSuccessMessage(newOrder);

        } catch (Exception ex) {
            handleOrderException(ex);
        }
    }

    private Order createNewOrder() {
        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        return new Order(orderId, timestamp, "现场顾客", "WALK_IN");
    }

    private void addItemsToOrder(Order order) throws OutOfStockException {
        for (Map.Entry<Product, Integer> entry : selectedProducts.entrySet()) {
            Product product = entry.getKey();
            int quantity = entry.getValue();

            if (quantity > 0) {
                if (quantity > product.getStock()) {
                    throw new OutOfStockException(
                            product.getName() + "库存不足（剩余：" + product.getStock() + "）");
                }
                order.addItem(product, quantity);
                product.reduceStock(quantity);
                view.updateProductStock(product); // 改为传整个对象
            }
        }
    }

    private void applyDiscounts(Order order) {
        DiscountCalculator.DiscountResult discount =
                new DiscountCalculator().calculateBestDiscount(order);

        // 记录折扣金额
        order.setDiscountAmount(discount.discountAmount);
        order.setTotalPrice(order.calculateOriginalPrice() - discount.discountAmount);

        view.appendOrderNote(String.format(
                "应用折扣: %s (¥%.2f)",
                discount.description,
                discount.discountAmount
        ));
    }
    // endregion

    // region 模拟控制逻辑
    private void startSimulation() {
        new Thread(() -> {
            try {
                // 1. 验证组件
                if (orderQueue == null || orderManager == null) {
                    throw new IllegalStateException("核心组件未初始化");
                }

                // 2. 创建模拟器
                this.simulator = new CoffeeShopSimulator(
                        orderQueue,
                        2,
                        view.getSimulationSpeed(),
                        orderManager
                );

                // 3. 设置观察者
                simulator.addObserver(view);

                // 4. 启动模拟
                simulator.startSimulation();

                // 5. 更新UI（必须在EDT线程）
                SwingUtilities.invokeLater(() -> {
                    view.setSimulationControlsEnabled(true);
                    view.showMessage("成功", "模拟已启动", JOptionPane.INFORMATION_MESSAGE);
                });

            } catch (Exception e) {
                // 打印完整错误
                e.printStackTrace();

                // 显示友好错误（EDT线程）
                SwingUtilities.invokeLater(() ->
                        showErrorDialog("启动失败",
                                "原因: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName())
                        )
                );
            }
        }).start();
    }

    private void stopSimulation() {
        if (simulator != null) {
            simulator.stopSimulation();
            view.setSimulationControlsEnabled(false);
            CoffeeShopLogger.getInstance().logEvent("模拟停止");

            try {
                CoffeeShopLogger.getInstance().saveToFile("coffee_shop_log.txt");
            } catch (Exception e) {
                showErrorDialog("日志错误", "保存日志失败: " + e.getMessage());
            }
        }
    }
    // endregion

    // region 商品选择处理
    private void handleAddProduct(Product product) {
        try {
            int current = selectedProducts.get(product);
            if (current >= product.getStock()) {
                throw new OutOfStockException("已达到库存上限");
            }

            selectedProducts.put(product, current + 1);
            updatePriceDisplay();

        } catch (OutOfStockException e) {
            showWarningDialog("库存警告", e.getMessage());
        }
    }

    private void handleRemoveProduct(Product product) {
        int current = selectedProducts.get(product);
        if (current > 0) {
            selectedProducts.put(product, current - 1);
            updatePriceDisplay();
        }
    }

    private void updatePriceDisplay() {
        // 创建临时订单用于计算
        Order tempOrder = createTempOrder();

        // 使用DiscountCalculator计算折扣
        DiscountCalculator.DiscountResult discountResult = calculateDiscount(tempOrder);

        double total = tempOrder.calculateOriginalPrice();
        double discountedPrice = total - discountResult.discountAmount;

        view.updatePriceDisplay(
                total,
                discountedPrice,
                discountResult.description // 显示折扣描述
        );
    }

    private Order createTempOrder() {
        Order tempOrder = new Order("TEMP", "TEMP", "TEMP", "TEMP");
        selectedProducts.forEach((product, quantity) -> {
            if (quantity > 0) {
                tempOrder.addItem(product, quantity);
            }
        });
        return tempOrder;
    }
    private String getDiscountInfo(double total, double discount) {
        if (discount <= 0) {
            return "无折扣";
        }
        double discountRate = (discount / total) * 100;
        return String.format("优惠: ¥%.2f (%.0f%%)", discount, discountRate);
    }
    // endregion

    // region 辅助方法
    private double calculateTotalPrice() {
        return selectedProducts.entrySet().stream()
                .mapToDouble(e -> e.getKey().getPrice() * e.getValue())
                .sum();
    }

    private DiscountCalculator.DiscountResult calculateDiscount(Order order) {
        // 使用DiscountCalculator计算最佳折扣
        return discountCalculator.calculateBestDiscount(order);
    }

    private void submitOrder(Order order) throws InterruptedException {
        orderManager.addOrder(order);
        simulator.getOrderQueue().addOrder(order);
        CoffeeShopLogger.getInstance().logEvent(
                "新订单提交: " + order.getOrderId() + " 总价: " + order.getTotalPrice());
    }

    private void resetOrderUI() {
        selectedProducts.replaceAll((p, q) -> 0);
        view.resetProductSelections();
        view.resetOrderSummariesSelections();
    }

    private void showSuccessMessage(Order order) {
        view.showMessage(
                "订单提交成功",
                String.format("订单号: %s\n总价: ¥%.2f", order.getOrderId(), order.getTotalPrice()),
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void handleOrderException(Exception ex) {
        CoffeeShopLogger.getInstance().logEvent("订单错误: " + ex.getMessage());
        if (ex instanceof OutOfStockException) {
            showWarningDialog("库存错误", ex.getMessage());
        } else {
            showErrorDialog("系统错误", ex.getMessage());
        }
    }
    // endregion

    // region UI工具方法
    private void showWarningDialog(String title, String message) {
        view.showMessage(title, message, JOptionPane.WARNING_MESSAGE);
    }

    private void showErrorDialog(String title, String message) {
        view.showMessage(title, message, JOptionPane.ERROR_MESSAGE);
    }
    // endregion
}
