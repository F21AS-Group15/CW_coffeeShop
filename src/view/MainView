package view;

import model.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class MainView extends JFrame implements QueueObserver {
    // 核心组件
    private final JTabbedPane tabbedPane = new JTabbedPane();
    private final JPanel productPanel = new JPanel();
    private final JTextArea orderSummaryArea = new JTextArea();
    private final JTextArea queueTextArea = new JTextArea();
    private final JTextArea serversTextArea = new JTextArea();
    private final JButton placeOrderButton = new JButton("确认下单");
    private final JButton startSimulationButton = new JButton("开始模拟");
    private final JButton stopSimulationButton = new JButton("停止模拟");
    private final JSlider speedSlider = new JSlider(500, 5000, 2000);
    private final JLabel totalPriceLabel = new JLabel("¥0.00");
    private final JLabel discountedPriceLabel = new JLabel("¥0.00");
    private final JLabel discountInfoLabel = new JLabel("无折扣");

    // 状态跟踪
    private final Map<String, JLabel> stockLabels = new HashMap<>();
    private final Map<Product, JLabel> quantityLabels = new HashMap<>();
    private final StringBuilder allOrderSummaries = new StringBuilder();

    public MainView() {
        setupUI();
    }

    private void setupUI() {
        // 1. 主窗口配置
        setTitle("咖啡店订单管理系统");
        setSize(900, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

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
        setContentPane(backgroundPanel);

        // 2. 顶部价格显示
        add(createPricePanel(), BorderLayout.NORTH);

        // 3. 主选项卡
        initProductTab();
        initOrderTab();
        initDiscountTab();
        initSimulationTab();
        backgroundPanel.add(tabbedPane, BorderLayout.CENTER);

        // 4. 下单按钮
        configureOrderButton();
        backgroundPanel.add(placeOrderButton, BorderLayout.SOUTH);
    }


    private JPanel createPricePanel() {
        JPanel panel = new JPanel(new GridLayout(1, 5, 10, 0));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JLabel totalLabel = new JLabel("商品总价:");
        totalLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 16));
        panel.add(totalLabel);

        totalPriceLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 16));
        totalPriceLabel.setForeground(new Color(0, 100, 0));  // 深绿色
        panel.add(totalPriceLabel);

        JLabel discountLabel = new JLabel("折后价格:");
        discountLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 16));
        panel.add(discountLabel);

        discountedPriceLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 16));
        discountedPriceLabel.setForeground(new Color(200, 0, 0));  // 深红色
        panel.add(discountedPriceLabel);

        discountInfoLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        discountInfoLabel.setForeground(new Color(150, 0, 150));
        panel.add(discountInfoLabel);

        return panel;
    }

    private void initProductTab() {
        productPanel.setLayout(new GridLayout(0, 2, 15, 15));
        productPanel.setOpaque(false);
        productPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JScrollPane productScroll = new JScrollPane(productPanel);
        productScroll.setOpaque(false);
        productScroll.getViewport().setOpaque(false);
        tabbedPane.addTab("商品菜单", productScroll);
    }

    private void initOrderTab() {
        orderSummaryArea.setEditable(false);
        orderSummaryArea.setOpaque(false);
        orderSummaryArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        orderSummaryArea.setLineWrap(true);
        orderSummaryArea.setWrapStyleWord(true);

        JScrollPane orderScroll = new JScrollPane(orderSummaryArea);
        orderScroll.setOpaque(false);
        orderScroll.getViewport().setOpaque(false);
        tabbedPane.addTab("订单详情", orderScroll);
    }

    private void initDiscountTab() {
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
                "   - 满¥20减¥2\n" +
                "   - 满¥30减¥5\n" +
                "   - 满¥50享8折优惠\n\n" +
                "注：优惠不可叠加使用，系统将自动应用最优折扣";

        discountTextArea.setText(discountInfo);

        JScrollPane discountScroll = new JScrollPane(discountTextArea);
        discountScroll.setOpaque(false);
        discountScroll.getViewport().setOpaque(false);
        discountPanel.add(discountScroll, BorderLayout.CENTER);

        tabbedPane.addTab("优惠信息", discountPanel);
    }

    private void initSimulationTab() {
        JPanel simulationPanel = new JPanel(new BorderLayout());
        simulationPanel.setOpaque(false);

        // 控制面板
        JPanel controlPanel = new JPanel(new FlowLayout());
        controlPanel.setOpaque(false);

        // 初始化按钮
        startSimulationButton.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        stopSimulationButton.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        stopSimulationButton.setEnabled(false);

        // 速度调节滑块
        speedSlider.setMajorTickSpacing(1000);
        speedSlider.setPaintTicks(true);
        speedSlider.setPaintLabels(true);

        controlPanel.add(startSimulationButton);
        controlPanel.add(stopSimulationButton);
        controlPanel.add(new JLabel("模拟速度:"));
        controlPanel.add(speedSlider);

        // 状态显示区域
        queueTextArea.setEditable(false);
        queueTextArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        serversTextArea.setEditable(false);
        serversTextArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));

        JPanel displayPanel = new JPanel(new GridLayout(1, 2));
        displayPanel.add(new JScrollPane(queueTextArea));
        displayPanel.add(new JScrollPane(serversTextArea));

        simulationPanel.add(controlPanel, BorderLayout.NORTH);
        simulationPanel.add(displayPanel, BorderLayout.CENTER);

        tabbedPane.addTab("模拟控制", simulationPanel);
    }

    private void configureOrderButton() {
        placeOrderButton.setFont(new Font("Microsoft YaHei", Font.BOLD, 16));
        placeOrderButton.setBackground(new Color(70, 130, 180));
        placeOrderButton.setForeground(Color.WHITE);
        placeOrderButton.setFocusPainted(false);
        placeOrderButton.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
    }

//    public void addProductCard(Product product, ActionListener addAction, ActionListener removeAction) {
//        // 创建商品卡片面板
//        JPanel cardPanel = new JPanel(new BorderLayout()) {
//            @Override
//            protected void paintComponent(Graphics g) {
//                super.paintComponent(g);
//                Graphics2D g2d = (Graphics2D) g;
//                g2d.setColor(new Color(255, 255, 255, 220));
//                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
//                g2d.setColor(new Color(200, 200, 200, 100));
//                g2d.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 15, 15);
//            }
//        };
//        cardPanel.setOpaque(false);
//        cardPanel.setPreferredSize(new Dimension(380, 180));
//
//        // 商品信息面板
//        JPanel infoPanel = new JPanel();
//        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
//        infoPanel.setOpaque(false);
//        infoPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
//
//        // 商品名称
//        JLabel nameLabel = new JLabel("<html><b><font size=+1>" + product.getName() + "</font></b></html>");
//        nameLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
//        infoPanel.add(nameLabel);
//        infoPanel.add(Box.createVerticalStrut(5));
//
//        // 商品描述
//        JTextArea descriptionArea = new JTextArea(product.getDescription());
//        descriptionArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
//        descriptionArea.setLineWrap(true);
//        descriptionArea.setWrapStyleWord(true);
//        descriptionArea.setEditable(false);
//        descriptionArea.setOpaque(false);
//        descriptionArea.setMaximumSize(new Dimension(350, 40));
//        infoPanel.add(descriptionArea);
//        infoPanel.add(Box.createVerticalStrut(5));
//
//        // 商品分类和价格
//        JPanel detailPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
//        detailPanel.setOpaque(false);
//
//        JLabel categoryLabel = new JLabel("分类: " + product.getCategory());
//        categoryLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
//        detailPanel.add(categoryLabel);
//
//        JLabel priceLabel = new JLabel("价格: ¥" + product.getPrice());
//        priceLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 13));
//        priceLabel.setForeground(new Color(0, 100, 0));
//        detailPanel.add(priceLabel);
//
//        infoPanel.add(detailPanel);
//
//        // 库存信息
//        JLabel stockLabel = new JLabel("库存: " + product.getStock());
//        stockLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
//        stockLabel.setForeground(new Color(100, 100, 100));
//        infoPanel.add(stockLabel);
//        stockLabels.put(product.getId(), stockLabel);
//
//        // 数量选择面板
//        JPanel quantityPanel = new JPanel();
//        quantityPanel.setOpaque(false);
//        quantityPanel.setLayout(new BoxLayout(quantityPanel, BoxLayout.X_AXIS));
//        quantityPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
//
//        JButton minusButton = new JButton("-");
//        minusButton.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
//        minusButton.setPreferredSize(new Dimension(30, 30));
//        minusButton.setBackground(new Color(255, 150, 150));
//        minusButton.setForeground(Color.WHITE);
//        minusButton.setFocusPainted(false);
//        minusButton.addActionListener(removeAction);
//
//        JLabel quantityLabel = new JLabel("0");
//        quantityLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
//        quantityLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
//        quantityLabels.put(product, quantityLabel);
//
//        JButton plusButton = new JButton("+");
//        plusButton.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
//        plusButton.setPreferredSize(new Dimension(30, 30));
//        plusButton.setBackground(new Color(150, 200, 150));
//        plusButton.setForeground(Color.WHITE);
//        plusButton.setFocusPainted(false);
//        plusButton.addActionListener(addAction);
//
//        quantityPanel.add(minusButton);
//        quantityPanel.add(quantityLabel);
//        quantityPanel.add(plusButton);
//        infoPanel.add(quantityPanel);
//
//        cardPanel.add(infoPanel, BorderLayout.CENTER);
//        productPanel.add(cardPanel);
//    }
    public void addProductCard(Product product, ActionListener addAction, ActionListener removeAction) {
        // 创建商品卡片面板（保持不变）
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

        // 商品信息面板（保持不变）
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);
        infoPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        // 商品名称、描述、分类和价格（保持不变）
        // ...（原有代码保持不变）
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

        JLabel priceLabel = new JLabel("价格: ¥" + product.getPrice());
        priceLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 13));
        priceLabel.setForeground(new Color(0, 100, 0));
        detailPanel.add(priceLabel);

        infoPanel.add(detailPanel);

        // 库存信息（保持不变）
        JLabel stockLabel = new JLabel("库存: " + product.getStock());
        stockLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        stockLabel.setForeground(new Color(100, 100, 100));
        infoPanel.add(stockLabel);
        stockLabels.put(product.getId(), stockLabel);

        // 数量选择面板
        JPanel quantityPanel = new JPanel();
        quantityPanel.setOpaque(false);
        quantityPanel.setLayout(new BoxLayout(quantityPanel, BoxLayout.X_AXIS));
        quantityPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));

        // 减号按钮
        JButton minusButton = new JButton("-");
        minusButton.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        minusButton.setPreferredSize(new Dimension(30, 30));
        minusButton.setBackground(new Color(255, 150, 150));
        minusButton.setForeground(Color.WHITE);
        minusButton.setFocusPainted(false);
        minusButton.addActionListener(e -> {
            int currentQty = Integer.parseInt(quantityLabels.get(product).getText());
            if (currentQty > 0) {
                quantityLabels.get(product).setText(String.valueOf(currentQty - 1));
                removeAction.actionPerformed(e); // 通知控制器数量减少
            } else {
                showMessage("操作无效", "购买数量不能小于零", JOptionPane.WARNING_MESSAGE);
            }
        });

        // 数量显示标签
        JLabel quantityLabel = new JLabel("0");
        quantityLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        quantityLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        quantityLabels.put(product, quantityLabel);

        // 加号按钮
        JButton plusButton = new JButton("+");
        plusButton.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        plusButton.setPreferredSize(new Dimension(30, 30));
        plusButton.setBackground(new Color(150, 200, 150));
        plusButton.setForeground(Color.WHITE);
        plusButton.setFocusPainted(false);
        plusButton.addActionListener(e -> {
            int currentQty = Integer.parseInt(quantityLabels.get(product).getText());
            if (currentQty < product.getStock()) {
                quantityLabels.get(product).setText(String.valueOf(currentQty + 1));
                addAction.actionPerformed(e); // 通知控制器数量增加
            } else {
                showMessage("库存不足",
                        "库存不足！\n" + product.getName() +
                                " 剩余: " + product.getStock() +
                                "\n最大可添加数量: " + product.getStock(),
                        JOptionPane.WARNING_MESSAGE);
            }
        });

        quantityPanel.add(minusButton);
        quantityPanel.add(quantityLabel);
        quantityPanel.add(plusButton);
        infoPanel.add(quantityPanel);

        cardPanel.add(infoPanel, BorderLayout.CENTER);
        productPanel.add(cardPanel);
    }

    public void updatePriceDisplay(double totalPrice, double discountedPrice, String discountInfo) {
        SwingUtilities.invokeLater(() -> {
            totalPriceLabel.setText(String.format("¥%.2f", totalPrice));
            discountedPriceLabel.setText(String.format("¥%.2f", discountedPrice));
            discountInfoLabel.setText(discountInfo);
        });
    }

    public void updateProductStock(Product product) {
        SwingUtilities.invokeLater(() -> {
            JLabel label = stockLabels.get(product.getId());
            if (label != null) {
                label.setText("库存: " + product.getStock());
                if (product.getStock() < 3) {
                    label.setForeground(Color.RED);
                } else {
                    label.setForeground(new Color(100, 100, 100));
                }

                // 更新数量标签
                JLabel qtyLabel = quantityLabels.get(product);
                if (qtyLabel != null) {
                    int currentQty = Integer.parseInt(qtyLabel.getText());
                    if (currentQty > product.getStock()) {
                        qtyLabel.setText(String.valueOf(product.getStock()));
                    }
                }
            }
        });
    }

    public void appendOrderNote(String note) {
        SwingUtilities.invokeLater(() -> {
            allOrderSummaries.insert(0, note + "\n");
            orderSummaryArea.setText(allOrderSummaries.toString());
            orderSummaryArea.setCaretPosition(0);
        });
    }

    public void resetProductSelections() {
        SwingUtilities.invokeLater(() -> {
            quantityLabels.values().forEach(label -> label.setText("0"));
        });
    }


    public void resetOrderSummariesSelections() {
        totalPriceLabel.setText("¥0.00");
        discountedPriceLabel.setText("¥0.00");
        discountInfoLabel.setText("无折扣");
    }

    public void showMessage(String title, String message, int messageType) {
        SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(this, message, title, messageType)
        );
    }

    public Map<Product, Integer> getSelectedProducts() {
        Map<Product, Integer> selections = new HashMap<>();
        quantityLabels.forEach((product, label) -> {
            int qty = Integer.parseInt(label.getText());
            if (qty > 0) selections.put(product, qty);
        });
        return selections;
    }

    public void setOrderButtonListener(ActionListener listener) {
        placeOrderButton.addActionListener(listener);
    }

    public void setStartSimulationListener(ActionListener listener) {
        startSimulationButton.addActionListener(listener);
    }

    public void setStopSimulationListener(ActionListener listener) {
        stopSimulationButton.addActionListener(listener);
    }

    public int getSimulationSpeed() {
        return speedSlider.getValue();
    }

    public void setSimulationControlsEnabled(boolean running) {
        startSimulationButton.setEnabled(!running);
        stopSimulationButton.setEnabled(running);
        placeOrderButton.setEnabled(running);
    }

    @Override
    public void updateQueue(List<Order> orders) {
        SwingUtilities.invokeLater(() -> {
            // 按订单ID分组，合并相同订单的显示
            Map<String, List<Order>> groupedOrders = orders.stream()
                    .collect(Collectors.groupingBy(Order::getOrderId));

            StringBuilder queueText = new StringBuilder();
            queueText.append("=== 当前队列状态 ===\n");
            queueText.append("当前队列中有");
            queueText.append(orders.size());
            queueText.append("个订单待处理\n");

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
            sb.append(String.format("  %-15s ×%-2d @ ¥%-5.2f\n",
                    product.getName(), count, product.getPrice()));
        });

        double totalPrice = orders.stream()
                .mapToDouble(Order::getTotalPrice)
                .sum();

        sb.append(String.format("总价: ¥%.2f\n\n", totalPrice));
    }

    @Override
    public void updateServers(List<ServerThread> servers) {
        SwingUtilities.invokeLater(() -> {
            StringBuilder sb = new StringBuilder();
            sb.append("服务员状态:\n\n");

            for (ServerThread server : servers) {
                sb.append(server.getName()).append(": ");
                Order currentOrder = server.getCurrentOrder();
                if (currentOrder != null) {
                    sb.append(server.getCurrentOrderDetails());
//                    sb.append("正在处理订单 ")
//                            .append(currentOrder.getOrderId())
//                            .append(" (")
//                            .append(currentOrder.getItems().size())
//                            .append(" 个商品)\n");
                } else {
                    sb.append("空闲中\n");
                }
            }

            serversTextArea.setText(sb.toString());
        });
    }
}
