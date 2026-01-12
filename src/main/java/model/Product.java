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

//    =======constructor============
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

   public Product() {
    }
   
//    =========GETTER============
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
    
    //==========SETTER=================

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public void setPrice(float price) {
        this.price = price;
    }

    public Object getCategory() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    

    }
