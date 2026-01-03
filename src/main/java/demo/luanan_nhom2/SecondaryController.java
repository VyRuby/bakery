package demo.luanan_nhom2;

import java.io.IOException;
import javafx.fxml.FXML;

public class SecondaryController {
    
    private int pick , price;
    private String id;

    public SecondaryController(int pick, int price, String id) {
        this.pick = pick;
        this.price = price;
        this.id = id;
    }
    
    
    
    

    @FXML
    private void switchToPrimary() throws IOException {
        App.setRoot("primary");
    }
}