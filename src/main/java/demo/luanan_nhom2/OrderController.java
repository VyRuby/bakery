/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package demo.luanan_nhom2;

import demo.luanan_nhom2.Customer;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import demo.luanan_nhom2.CustomerDao;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import demo.luanan_nhom2.Product;
import demo.luanan_nhom2.productDao;
import demo.luanan_nhom2.OrderDetailItem;
import demo.luanan_nhom2.ProductDetailController;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * FXML Controller class
 *
 * @author Admin
 */
public class OrderController implements Initializable {


    @FXML
    private Button addcus;
    @FXML
    private Label customer;
    @FXML
    private TextField phonefind;
    @FXML
    private ListView<Product> listProduct;
    @FXML
    private TableView<OrderDetailItem> orderDetail;
    @FXML
    private Label totalOrder;
    @FXML
    private TableColumn<OrderDetailItem, String> ProductName;
    @FXML
    private TableColumn<OrderDetailItem, Integer> Number;
    @FXML
    private TableColumn<OrderDetailItem, Float> Price;
    @FXML
    private TableColumn<OrderDetailItem, Float> TotalPrice;
    @FXML
    private Button buttonOrder;
    
    
    
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
        loadProducts();
        ProductName.setCellValueFactory(new PropertyValueFactory<>("productName"));
        Number.setCellValueFactory( new PropertyValueFactory<>("quantity"));
        Price.setCellValueFactory(new PropertyValueFactory<>("price"));
        TotalPrice.setCellValueFactory(new PropertyValueFactory<>("total"));
        
        
        listProduct.setOnMouseClicked(e->{
            if(e.getClickCount() ==2){
                Product product =listProduct.getSelectionModel().getSelectedItem();
                if(product != null){
                    openProductDetail(product);
                }
            }
        });
        
        listProduct.setCellFactory(param -> new ListCell <Product>(){
        private ImageView imageView = new ImageView();
        private Label nameLabel = new Label();
        private Label priceLabel = new Label();
        private VBox vbox = new VBox (nameLabel, priceLabel);
        private HBox hbox = new HBox (imageView, vbox);
        {
            imageView.setFitHeight(70);
            imageView.setFitWidth(70);
            imageView.setPreserveRatio(true);
            
            vbox.setSpacing(10);
            hbox.setSpacing(10);
            
        }
        @Override
        protected void updateItem (Product item,boolean empty){
            super.updateItem(item,empty);
            if(empty|| item == null){
                setGraphic(null);
            }else{
                nameLabel.setText(item.getProductName());
                priceLabel.setText(item.getPrice() + "USD");
            
            try{
                Image img = new Image(
                getClass().getResourceAsStream("/" + item.getImage()));
                imageView.setImage(img);
            }catch(Exception e){
                imageView.setImage(null);
            }
            setGraphic(hbox);
            }  
        }    
    });
     orderDetail.setItems(orderList);
        
    }    
    
private CustomerDao customerDao = new CustomerDao();

    private void findCustomerByPhone(){
    try{
    String phone= phonefind.getText();
    
    Customer c =customerDao.findPhone(phone);
    
    if(c!=null){
        customer.setText(c.getName());
    }else{
        customer.setText("khong tim thay khach hang ");
    }
    }catch(Exception e){
        System.out.println("Error! not found phone");
    }
    }
    private productDao productDao = new productDao();
    
    private void loadProducts(){
        ObservableList<Product> data =
        FXCollections.observableArrayList(productDao.findAll());
        listProduct.setItems(data);
                
    }
    
    private ObservableList<OrderDetailItem> orderList = FXCollections.observableArrayList();
    
    private void addOrder(Product product, int quantity){
        for(OrderDetailItem item : orderList){
            if (item.getProduct().getProductId().equals(product.getProductId())){
                item.addQuantity(quantity);
                orderDetail.refresh();
                calculateTotal();
                return;
            }
        }
        orderList.add(new OrderDetailItem(product,quantity));
        calculateTotal();    
    }
    
    private void calculateTotal(){
        float total=0 ;
        for(OrderDetailItem item : orderList){
            total += item.getTotal();
        }
        totalOrder.setText(total + "USD");
    }
    private void openProductDetail(Product product){
        try{
            FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/fxml/ProductDetail.fxml")
            );
            Parent root = loader.load();
            
            ProductDetailController controller = loader.getController();
            controller.setProduct(product);
            
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.showAndWait();
            
            int qty = controller.getSelectedQuantity();
            if(qty > 0){
                addOrder(product,qty);
            }
        }catch(Exception e){
            System.out.println("Error !"+ e);
        }
    }
    
}