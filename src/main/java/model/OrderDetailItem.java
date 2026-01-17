/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package model;

import model.Product;

/**
 *
 * @author Admin
 */
public class OrderDetailItem {
    private Product product;
    private int quantity;
    private float price;
    private String promoID;
    private float discountAmount;
        

    public OrderDetailItem(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
        this.price = product.getPrice();
        this.promoID=null;
        this.discountAmount=0;
        
        
    }

    public String getPromoID() {
        return promoID;
    }

    public float getDiscountAmount() {
        return discountAmount;
    }

    public void setPromoID(String promoID) {
        this.promoID = promoID;
    }

    public void setDiscountAmount(float discountAmount) {
        this.discountAmount = discountAmount;
    }

    
            
    public Product getProduct() {
        return product;
    }
    
    public String getProductName(){
        return product.getProductName();
    }

    public void setPrice(float price) {
        this.price = price;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getQuantity() {
        return quantity;
    }

    public float getPrice() {
        return price;
    }
    
    public float getTotal(){
        return quantity * price;
    }
    
    public void addQuantity(int qty){
        this.quantity += qty;
    }
}
