package main;

import util.CoffeeShopLogger;
import controller.MainController;
import javax.swing.*;

// Main.java
public class CoffeeShopApp {
    public static void main(String[] args) {
        // 确保GUI在事件分发线程中创建
        SwingUtilities.invokeLater(() -> {
            try {
                // 初始化控制器（会自动创建视图和模型）
                new MainController();

                // 日志记录初始化
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    try {
                        CoffeeShopLogger.getInstance().saveToFile("coffee_shop_log.txt");
                    } catch (Exception e) {
                        System.err.println("保存日志失败: " + e.getMessage());
                    }
                }));

            } catch (Exception e) {
                JOptionPane.showMessageDialog(
                        null,
                        "系统初始化失败: " + e.getMessage(),
                        "致命错误",
                        JOptionPane.ERROR_MESSAGE
                );
                System.exit(1);
            }
        });
    }
}
