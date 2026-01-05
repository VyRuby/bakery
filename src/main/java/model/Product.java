/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package model;

/**
 *
 * @author Admin
 */
public class Product {
    
    private String productId, productName , categoryId, unit,description,image ;
    private int quantity;
    private float price;

    public Product(String productId, String productName, String categoryId, int quantity, String unit, float price, String description, String image ) {
        this.productId = productId;
        this.productName = productName;
        this.categoryId = categoryId;
        this.quantity = quantity;
        this.unit = unit;
        this.price = price;
        this.description = description;
        this.image = image;
    }

    
    
    
    
    
    public String getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public String getUnit() {
        return unit;
    }

    public String getDescription() {
        return description;
    }

    public String getImage() {
        return image;
    }

    public int getQuantity() {
        return quantity;
    }

    public float getPrice() {
        return price;
    }

    @Override
    public String toString() {
        return "Product{" + "productId=" + productId + ", productName=" + productName + ", categoryId=" + categoryId + ", unit=" + unit + ", description=" + description + ", image=" + image + ", quantity=" + quantity + ", price=" + price + '}';
    }
    
}
