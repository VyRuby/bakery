/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package demo.luanan_nhom2;

import java.time.LocalDate;

/**
 *
 * @author Admin
 */
public class Customer {
    
    private String address,email ,phone,  name;
    private int id;
    private LocalDate dob;
    private Gender gender;
    
    public enum Gender{
        Male,
        Female
    }
    

    public Customer(String address, String email, String phone, String name, int id, LocalDate dob) {
        this.address = address;
        this.email = email;
        this.phone = phone;
        this.name = name;
        this.id = id;
        this.dob = dob;
    }

    public Customer(int id, String phone, String name ) {
        this.id = id;
        this.phone = phone;
        this.name = name;
        
    }

    public Customer( String name,String phone,Gender gender, LocalDate dob, String email, String address,  int id  ) {
        this.address = address;
        this.email = email;
        this.phone = phone;
        this.name = name;
        this.id = id;
        this.dob = dob;
        this.gender = gender;
    }
    
    
   
    public int getId(){
        return id;
    }
    
    public String getName(){
        return  name;
        
    }
    
    public String getPhone(){
        return phone;
    }

    public String getAddress() {
        return address;
    }

    public String getEmail() {
        return email;
    }

    public LocalDate getDob() {
        return dob;
    }

    public Gender getGender() {
        return gender;
    }
    
}
