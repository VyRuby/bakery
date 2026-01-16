/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package controller;

import model.Customer;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import DAO_Customer_Order.CustomerDao;
import DAO_Customer_Order.CustomerDao;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import model.Product;
import model.OrderDetailItem;
import model.OrderDetailItem;
import DAO_Customer_Order.productDao;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import DAO_Customer_Order.OrderDao;
import DAO_Customer_Order.OrderDetailDao;
import controller.BacktoHomeController;
import controller.CustomerController;
import controller.ProductDetailController;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import java.io.FileOutputStream;
import java.io.File;
import org.w3c.dom.Document;
import model.Promotion;
import DAO_Product.PromotionDAO;
/**
 * FXML Controller class
 *
 * @author Admin
 */
public class OrderController extends BacktoHomeController implements Initializable {


    @FXML
    private Button addcus;
    @FXML
    private Label customer;
    @FXML
    private TextField phonefind;
//    private ListView<Product> listProduct;
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
    @FXML
    private Label dateOrder;
    @FXML
    private CheckBox Transfer;
    @FXML
    private Button btnBack;
    @FXML
    private GridPane productGrid;
    @FXML
    private TableColumn<OrderDetailItem, Void> DelColum;
    
    
    
    
    
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
        DelColum.setCellFactory(param -> new javafx.scene.control.TableCell<OrderDetailItem, Void>(){
            private final Button btn = new Button ("Del");
            {
                btn.setStyle("-fx-background-color: #ff4444; -fx-text-fill: white;");
                btn.setOnAction(event -> {
                    OrderDetailItem item = getTableView().getItems().get(getIndex());
                orderList.remove(item);
                calculateTotal();
                orderDetail.refresh();
                });            
            }
            @Override
                protected void updateItem(Void item, boolean empty){
        super.updateItem(item, empty);
        if(empty){
            setGraphic(null);
        }else{
            setGraphic(btn);
        }
    }
            
        });
        
        
        phonefind.textProperty().addListener((observable,oldValue,newValue) -> {
            
            if(!newValue.matches("\\d*")){
                phonefind.setText(oldValue);
            }
            
            if(newValue == null||newValue.trim().isEmpty()){
                customer.setText("Visitor");
                customer.setStyle("-fx-text-fill: black;");
            }
            
            else if(newValue !=null&& newValue.length()>=10){
                findCustomerByPhone();
            }
        });
        
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String formatteDate= now.format(formatter);
        dateOrder.setText(formatteDate);
//        listProduct.setOnMouseClicked(e->{
//            if(e.getClickCount() ==2){
//                Product product =listProduct.getSelectionModel().getSelectedItem();
//                if(product != null){
//                    openProductDetail(product);
//                }
//            }
//        }
        
//        );
        
//        listProduct.setCellFactory(param -> new ListCell <Product>(){
//        private ImageView imageView = new ImageView();
//        private Label nameLabel = new Label();
//        private Label priceLabel = new Label();
//        private VBox vbox = new VBox (nameLabel, priceLabel);
//        private HBox hbox = new HBox (imageView, vbox);
//        {
//            imageView.setFitHeight(70);
//            imageView.setFitWidth(70);
//            imageView.setPreserveRatio(true);
//            
//            vbox.setSpacing(10);
//            hbox.setSpacing(10);
//            
//        }
//        @Override
//        protected void updateItem (Product item,boolean empty){
//            super.updateItem(item,empty);
//            if(empty|| item == null){
//                setGraphic(null);
//            }else{
//                nameLabel.setText(item.getProductName());
//                priceLabel.setText(item.getPrice() + "USD");
//            
//            try{
//                Image img = new Image(
//                getClass().getResourceAsStream("/" + item.getImage()));
//                imageView.setImage(img);
//            }catch(Exception e){
//                imageView.setImage(null);
//            }
//            setGraphic(hbox);
//            }  
//        }    
//    })
                
                ;
     orderDetail.setItems(orderList);
        
    }    
    
private CustomerDao customerDao = new CustomerDao();
private PromotionDAO promotionDAO = new PromotionDAO();


    private void findCustomerByPhone(){
    try{
    String phone= phonefind.getText();
    
    if(phone.isEmpty()){
        customer.setText("Visitor");
        return;
    }
    
    Customer c =customerDao.findPhone(phone);
    
    if(c!=null){
        customer.setText(c.getName());
        customer.setStyle("-fx-text-fill: black;");
        
    }else{
//        Customer visitor =customerDao.findByID(3);
            customer.setText("Not Found");
            customer.setStyle("-fx-text-fill: red;");
//            phonefind.setText(visitor.getPhone());
            
    }
    }catch(Exception e){
        System.out.println("Error!");
    }
    }
    private productDao productDao = new productDao();
    
    private void loadProducts(){
        ObservableList<Product> data =
        FXCollections.observableArrayList(productDao.findAll());
//        listProduct.setItems(data);
productGrid.getChildren().clear();
int column =0;
int row =0;
int MAX_COLUMN=3;

for(Product product : data){
    VBox productCard = createProductCard(product);
    
    productGrid.add(productCard, column, row);
    
    column++;
    if(column == MAX_COLUMN){
        column = 0;
        row++;
    }
}
          

    }
    
    private ObservableList<OrderDetailItem> orderList = FXCollections.observableArrayList();
    
    private void addOrder(Product product, int quantity){
        float originalPrice =product.getPrice();
        float unitPrice = originalPrice;
        float discountAmount = 0;
        String promoId = null;
        
        Promotion activePromo = promotionDAO.getActivePromoByProduct(product.getProductId());
        if(activePromo !=null){
            promoId= activePromo.getPromoId();
            if("Percent".equalsIgnoreCase(activePromo.getPromoType())){
                discountAmount =(float) (originalPrice * (activePromo.getValue()/100));
            }else{
                discountAmount = (float)activePromo.getValue();
                
            }
            unitPrice = originalPrice - discountAmount;
            
        }
        
        
        
        for(OrderDetailItem item : orderList){
            if (item.getProduct().getProductId().equals(product.getProductId())){
                item.addQuantity(quantity);
                orderDetail.refresh();
                calculateTotal();
                return;
            }
        }
        
        OrderDetailItem newItem= new OrderDetailItem(product,quantity);
        
        newItem.setPrice(unitPrice);
        newItem.setPromoID(promoId);
        newItem.setDiscountAmount(discountAmount);
        
        orderList.add(newItem);
        
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

    @FXML
    private void addcus(ActionEvent event) {
    try{
        FXMLLoader loader = new FXMLLoader(
        getClass().getResource("fxml/Customer.fxml")
        );
        
        Parent root = loader.load();
        CustomerController controller = loader.getController();
        
        Stage stage = new Stage();
        stage.setScene(new Scene(root));
        stage.setTitle("Add new Customer");
        stage.showAndWait();
        
//        Customer newCus=
        
    }catch(Exception e){
        System.out.println("Error !");
        
    }
    
    }
    
    
    private VBox createProductCard(Product product){
        ImageView imageView = new ImageView();
        imageView.setFitWidth(120);
        imageView.setFitHeight(120);
        imageView.setPreserveRatio(true);
        
        try{
            Image img = new Image(
            getClass().getResourceAsStream("/" + product.getImage()));
                imageView.setImage(img);
            }catch(Exception e){
                imageView.setImage(null);
        }
        Label name = new Label(product.getProductName());
        Label price = new Label(product.getPrice() +"USD");
        
        VBox box = new VBox(10, imageView, name, price );
        box.setAlignment(javafx.geometry.Pos.CENTER);
        box.setStyle("-fx-border-color: #ccc; -fx-padding: 3;-fx-cursor: hand;");
        
        box.setPickOnBounds(true);
        
        box.setOnMouseClicked(e -> {
            if(e.getClickCount() == 2){
                openProductDetail(product);
                
            }
        });
        return box;
    }
    
    private OrderDao orderDao = new OrderDao();
    private OrderDetailDao orderDetailDao = new OrderDetailDao();

    @FXML
    private void handleOrder(ActionEvent event) throws SQLException {
        if(orderList.isEmpty()){
            System.out.println("No item to order !");
            return ;
        }
        String phone = phonefind.getText();
        Customer c =customerDao.findPhone(phone);
        int customerId;
        
        if(c!=null){
            customerId =c.getId();
            
        }else{
            customerId=3;
        }
        
        String paymentMethod = Transfer.isSelected() ?"TRANSFER" : "CASH";
        
        
        
        float total=0;
        for(OrderDetailItem item : orderList){
            total += item.getTotal();
        }
        
        int orderId= orderDao.insertOrder(customerId, total , paymentMethod);
        if(orderId == -1){
            System.out.println("Order failt !");
            return;
        }
        
        for(OrderDetailItem item : orderList){
            orderDetailDao.insertDetail(orderId, item);
        }
        
        System.out.println("order Saved !");
        
        orderList.clear();
        orderDetail.refresh();
        totalOrder.setText("0 USD");
        
        
        
    }


    
    private void exportToPDF(int orderId, String customerName, String paymentMethod, float total){
        com.itextpdf.text.Document document = new com.itextpdf.text.Document() {};
        try{
            String fileName= "Invoid_Order" + orderId + ".pdf";
            PdfWriter.getInstance(document, new FileOutputStream(fileName));
            document.open();
            
            com.itextpdf.text.Font titleFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA,18 ,com.itextpdf.text.Font.BOLD);
            com.itextpdf.text.Font  boldFont= new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 12 , com.itextpdf.text.Font.BOLD);
            
            Paragraph title= new Paragraph("Invoice", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph("*****************"));
            
            document.add(new Paragraph("Order ID: " + orderId));
            document.add(new Paragraph("Customer: " + customerName));
            document.add(new Paragraph("Date: " + dateOrder.getText()));
            document.add(new Paragraph("Payment Method: " + paymentMethod));
            document.add(new Paragraph(" "));
            
            
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);
            
            table.addCell(new Paragraph("Product Name", boldFont));
            table.addCell(new Paragraph("Quantity", boldFont));
            table.addCell(new Paragraph("Price", boldFont));
            table.addCell(new Paragraph("Total", boldFont ));
            
            
            for(OrderDetailItem item : orderList){
                String pName= item.getProduct().getProductName();
            
            if(item.getPromoID() != null){ 
                pName +="\n(Promo: "+ item.getPromoID()+")";}
            table.addCell(new Phrase(pName));
            table.addCell(String.valueOf(item.getQuantity()));
            table.addCell(item.getPrice() + "USD");
            table.addCell(item.getTotal() + "USD");
            
            }
            document.add(table);
            
            
            
            
        }catch(Exception e){
            System.out.println("Error Print PDF!" + e);
            e.printStackTrace();
        }
    }

    

    
}
