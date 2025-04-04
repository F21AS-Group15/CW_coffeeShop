// 在model包下创建 OutOfStockException.java
package model;

public class OutOfStockException extends Exception {
    public OutOfStockException(String message) {
        super(message);
    }
}