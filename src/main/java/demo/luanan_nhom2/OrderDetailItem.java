/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package demo.luanan_nhom2;

/**
 *
 * @author Admin
 */
public class OrderDetailItem {
    private Product product;
    private int quantity;
    private float price;
        

    public OrderDetailItem(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
        this.price = product.getPrice();
    }

    public Product getProduct() {
        return product;
    }
    
    public String getProductName(){
        return product.getProductName();
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
