/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package demo.luanan_nhom2;

import java.time.LocalDateTime;

/**
 *
 * @author Admin
 */
public class CakeOrder {
    
    private int quantity , price;
    private String idcake;
    private LocalDateTime date;

    public CakeOrder(int quantity, int price, String idcake, LocalDateTime date) {
        this.quantity = quantity;
       
        this.price = price;
        
        this.idcake = idcake;
        this.date = date;
    }

    
    public double getSubTotal(){
        return price * quantity;
    }
    
    
    
    
    
    
    
    
}
