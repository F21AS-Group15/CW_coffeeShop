package com.itjava;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class CoffeeShopTest {
    // ---------------------------
    // DefaultDiscount 测试
    // ---------------------------

    // 当原价小于20时，不打折
    @Test
    public void testDefaultDiscount_NoDiscount() {
        DefaultDiscount discount = new DefaultDiscount();
        Order order = new Order("order1", "timestamp", "customer1");
        double originalPrice = 19.0;
        double discountedPrice = discount.applyDiscount(order, originalPrice);
        assertEquals(19.0, discountedPrice, 0.001);
    }

    // 当原价在20~29之间时，减2元
    @Test
    public void testDefaultDiscount_Above20() {
        DefaultDiscount discount = new DefaultDiscount();
        Order order = new Order("order2", "timestamp", "customer1");
        double originalPrice = 22.0;
        double discountedPrice = discount.applyDiscount(order, originalPrice);
        assertEquals(20.0, discountedPrice, 0.001);
    }

    // 当原价在30~49之间时，减5元
    @Test
    public void testDefaultDiscount_Above30() {
        DefaultDiscount discount = new DefaultDiscount();
        Order order = new Order("order3", "timestamp", "customer1");
        double originalPrice = 35.0;
        double discountedPrice = discount.applyDiscount(order, originalPrice);
        assertEquals(30.0, discountedPrice, 0.001);
    }

    // 当原价大于或等于50时，打8折
    @Test
    public void testDefaultDiscount_Above50() {
        DefaultDiscount discount = new DefaultDiscount();
        Order order = new Order("order4", "timestamp", "customer1");
        double originalPrice = 100.0;
        double discountedPrice = discount.applyDiscount(order, originalPrice);
        assertEquals(80.0, discountedPrice, 0.001);
    }

    // ---------------------------
    // FoodAndBeverageDiscount 测试
    // ---------------------------
    @Test
    public void testFoodAndBeverageDiscount_NoDiscount() {
        FoodAndBeverageDiscount discount = new FoodAndBeverageDiscount();
        Order order = new Order("order5", "timestamp", "customer1");
        Product food = new Product("p1", "Burger", "Delicious", "Food", 8.0, 10);
        order.addItem(food, 2);
        double originalPrice = 16;
        double discountedPrice = discount.applyDiscount(order, originalPrice);
        assertEquals(16, discountedPrice, 0.001);

        Product beverage = new Product("p2", "Coke", "Refreshing", "Beverage", 3.0, 10);
        order.addItem(beverage, 1);
        originalPrice = 19.0;
        discountedPrice = discount.applyDiscount(order, originalPrice);
        assertEquals(19*0.8, discountedPrice, 0.001);
    }
    // ---------------------------
    // FreeCakeDiscount 测试
    // ---------------------------
    @Test
    public void testFreeCakeDiscount() {
        FreeCakeDiscount discount = new FreeCakeDiscount();
        Order order = new Order("order6", "timestamp", "customer1");
        // 添加2个 Cake 商品
        Product cake = new Product("p3", "Cake", "Sweet", "Food", 4.0, 10);
        order.addItem(cake, 2);

        double originalPrice = 8.0;
        double discountedPrice = discount.applyDiscount(order, originalPrice);
        assertEquals(originalPrice, discountedPrice, 0.001);

        order.addItem(cake, 1);
        originalPrice = 12.0;
        discountedPrice = discount.applyDiscount(order, originalPrice);
        assertEquals(8.0, discountedPrice, 0.001);
    }



    // ---------------------------
    // DiscountManager 测试
    // ---------------------------

    // 如果传入的折扣名称为 null 或空串或无效的折扣名称时，应使用默认的 "default" 规则
    @Test
    public void testDiscountManager_DefaultIfNullOrEmpty() {
        DiscountManager manager = new DiscountManager();
        Order order = new Order("order7", "timestamp", "customer1");
        double originalPrice = 50.0;

        // 折扣名称为 null
        double discountedPrice = manager.applyDiscount(null, order, originalPrice);
        assertEquals(40.0, discountedPrice, 0.001);

        // 折扣名称为空串
        discountedPrice = manager.applyDiscount("", order, originalPrice);
        assertEquals(40.0, discountedPrice, 0.001);

        // 折扣名称为无效折扣名
        discountedPrice = manager.applyDiscount("nonexistent", order, originalPrice);
        assertEquals(40.0, discountedPrice, 0.001);
    }

    // ---------------------------
    // Order 总价计算测试
    // ---------------------------

    @Test
    public void testOrderTotalPriceCalculation() {
        Order order = new Order("order8", "timestamp", "customer1");
        Product product1 = new Product("p4", "Sandwich", "Tasty", "Food", 5.0, 10);
        Product product2 = new Product("p5", "Juice", "Fresh", "Beverage", 3.5, 10);
        // 添加 2 个 Sandwich，总价 10.0
        order.addItem(product1, 2);
        // 添加 3 个 Juice，总价 10.5
        order.addItem(product2, 3);
        // 未调用 setTotalPrice 时，getTotalPrice 根据商品价格计算
        assertEquals(20.5, order.getTotalPrice(), 0.001);

        // 调用 setTotalPrice 后，返回 set 的值
        order.setTotalPrice(18.0);
        assertEquals(18.0, order.getTotalPrice(), 0.001);
    }

    @Test
    void testCakeAndFoodBeverageCombination() {
        DiscountManager discountManager = new DiscountManager();
        Order order = new Order("order9", "timestamp", "customer1");
        Product product1 = new Product("p6", "Cake", "Sweet", "Food", 4.0, 10);
        Product product2 = new Product("p7", "Coffee", "Arabica", "Beverage", 8.0, 10);
        // 添加商品：3蛋糕 + 1饮料
        order.addItem(product1, 3);   // 4*3 = 12
        order.addItem(product2, 1); // 8*1 = 8
        double originalPrice = 20.0;

        // 应用折扣策略
        double finalPrice = discountManager.applyDiscount("free_cake", order, originalPrice);
        finalPrice = discountManager.applyDiscount("food_and_beverage", order, finalPrice);
        assertEquals(16*0.8, finalPrice, 0.001);
    }


    // 测试折扣应用优先级
    @Test
    void testDiscountPriority() {
        DiscountManager discountManager = new DiscountManager();
        Order order = new Order("order10", "timestamp", "customer1");
        Product product1 = new Product("c1", "Cake", "Sweet", "Food", 4.0, 10);
        Product product2 = new Product("f1", "Burger", "Juicy", "Food", 15.0, 10);
        Product product3 = new Product("b1", "Coffee", "Arabica", "Beverage", 8.0, 10);
        // 添加商品：3蛋糕 + 2食品 + 1饮料 + 其他商品
        order.addItem(product1, 3);      // 4*3 = 12
        order.addItem(product2, 2);      // 15*2 = 30
        order.addItem(product3, 1); // 8*1 = 8
        Product other = new Product("o1", "Notebook", "Moleskine", "Stationery", 30.0, 10);
        order.addItem(other, 1);     // 30*1 = 30
        double originalPrice = 80.0;

        // 模拟placeOrder方法逻辑
        double tempPrice = originalPrice;

        // 1. 检查蛋糕折扣
        if (countCakes(order) >= 3) {
            tempPrice = discountManager.applyDiscount("free_cake", order, tempPrice);
        }

        // 2. 检查食品饮料组合
        if (hasFoodBeverageCombo(order)) {
            tempPrice = discountManager.applyDiscount("food_and_beverage", order, tempPrice);
        } else {
            tempPrice = discountManager.applyDiscount("default", order, tempPrice);
        }
        assertEquals(60.8, tempPrice, 0.001);
    }

    private int countCakes(Order order) {
        return (int) order.getItems().stream().filter(p -> "Cake".equals(p.getName())).count();
    }

    private boolean hasFoodBeverageCombo(Order order) {
        int foodCount = 0;
        int beverageCount = 0;
        for (Product p : order.getItems()) {
            if ("Food".equals(p.getCategory())) {
                foodCount++;
            }
            if ("Beverage".equals(p.getCategory())) {
                beverageCount++;
            }
        }
        return foodCount >= 2 && beverageCount >= 1;
    }

    @Test
    void testOutOfStockExceptionThrown() {
        Order order = new Order("order11", "timestamp", "customer1");
        Product product1 = new Product("p1", "Cake", "Sweet", "Food", 4.0, 2);
        Map<Product, Integer> selectedProducts = new HashMap<>();
        // 先将商品数量加到库存上限
        selectedProducts.put(product1, 2);

        // 模拟点击按钮时的逻辑
        Exception exception = assertThrows(OutOfStockException.class, () -> {
            int quantity = selectedProducts.getOrDefault(product1, 0);
            if (quantity >= product1.getStock()) {
                throw new OutOfStockException("Out of stock!");
            }
            quantity++;
            selectedProducts.put(product1, quantity);
        });

        // 验证异常信息是否正确
        assertEquals("Out of stock!", exception.getMessage());

        // 确保数量没有被增加
        assertEquals(2, selectedProducts.get(product1));
    }

}
