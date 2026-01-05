/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt
 * to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java
 * to edit this template
 */
package model;

import java.io.Serializable;
import java.sql.Date;

/**
 *
 * @author Quoc Anh
 */
public class Employee implements Serializable {

    private String employeeID;
    private String fullName;
    private Date dob;
    private String gender;
    private String phone;
    private String email;
    private String address;
    private Date hireDate;
    private String position;
    private String status; // Active / Inactive

    public Employee() {
    }

    public Employee(String employeeID, String fullName, Date dob, String gender,
                    String phone, String email, String address,
                    Date hireDate, String position, String status) {
        this.employeeID = employeeID;
        this.fullName = fullName;
        this.dob = dob;
        this.gender = gender;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.hireDate = hireDate;
        this.position = position;
        this.status = status;
    }

    // =====================
    // GETTERS & SETTERS
    // =====================

    public String getEmployeeID() {
        return employeeID;
    }

    public void setEmployeeID(String employeeID) {
        this.employeeID = employeeID;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Date getDob() {
        return dob;
    }

    public void setDob(Date dob) {
        this.dob = dob;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Date getHireDate() {
        return hireDate;
    }

    public void setHireDate(Date hireDate) {
        this.hireDate = hireDate;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
