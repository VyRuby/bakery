/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package controller;

import app.App;
import java.io.IOException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

/**
 *
 * @author vy
 */
public class BacktoHomeController {
    @FXML
    protected void goBack(ActionEvent event) throws IOException {
       System.out.println("CLICK Back");
    try {
        App.setRoot("Home"); 
        System.out.println("NAVIGATED TO Home");
    } catch (Exception e) {
        e.printStackTrace(); 
    } 
    }
}
