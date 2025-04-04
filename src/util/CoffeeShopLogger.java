package util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

// 新增的日志类（单例模式）
public class CoffeeShopLogger {
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
