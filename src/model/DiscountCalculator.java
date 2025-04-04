package model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DiscountCalculator {
    private static final double CAKE_PRICE = 4.0; // 蛋糕单价

    public static class DiscountResult {
        public static final DiscountResult NO_DISCOUNT =
                new DiscountResult(false, "无可用折扣", 0.0);

        public final boolean applied;
        public final String description;
        public final double discountAmount;

        public DiscountResult(boolean applied, String description, double discountAmount) {
            this.applied = applied;
            this.description = description;
            this.discountAmount = discountAmount;
        }
    }

    /**
     * 计算所有可能的折扣，返回优惠力度最大的一个
     */
    public DiscountResult calculateBestDiscount(Order originalOrder) {
        List<DiscountResult> allDiscounts = new ArrayList<>();

        // 1. 计算蛋糕折扣
        allDiscounts.add(applyCakeDiscount(originalOrder));

        // 2. 计算套餐折扣（不包含蛋糕的商品）
        Order orderWithoutCakes = createOrderWithoutCakes(originalOrder);
        allDiscounts.add(applyMealDealDiscount(orderWithoutCakes));

        // 3. 计算默认折扣（不包含蛋糕的商品）
        allDiscounts.add(applyDefaultDiscount(orderWithoutCakes));

        // 4. 选择折扣金额最大的方案
        return allDiscounts.stream()
                .max(Comparator.comparingDouble(d -> d.discountAmount))
                .orElse(DiscountResult.NO_DISCOUNT);
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

    // 应用套餐折扣规则：2份食品(不包含蛋糕)+1份饮料打8折
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
