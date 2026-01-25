/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package model;

/**
 *
 * @author vy
 */
public class RestockItem {
   
    private final String importId;
    private final String productId;
    private final String productName;
    private int quantity;          // số lượng nhập
    private double costPrice;      // giá vốn nhập

    public RestockItem(String importId, String productId, String productName, int quantity, double costPrice) {
        this.importId = importId;
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.costPrice = costPrice;
    }

    public String getImportId() { return importId; }
    public String getProductId() { return productId; }
    public String getProductName() { return productName; }
    public int getQuantity() { return quantity; }
    public double getCostPrice() { return costPrice; }

    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void setCostPrice(double costPrice) { this.costPrice = costPrice; }

    @Override
    public String toString() {
        return importId + " | " + productId + " - " + productName
                + " | Qty: " + quantity + " | Cost: " + costPrice;
    }
}


