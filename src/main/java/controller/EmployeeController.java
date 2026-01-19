package controller;

import model.Employee;
import DAO_Employee.EmployeeDAO;

import java.net.URL;
import java.sql.Date;
import java.util.List;
import java.util.ResourceBundle;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class EmployeeController extends BacktoHomeController  implements Initializable {

    // ===== TABLE =====
    @FXML private TableView<Employee> tblEmployee;
    @FXML private TableColumn<Employee, String> colId;
    @FXML private TableColumn<Employee, String> colName;
    @FXML private TableColumn<Employee, Date> colDob;
    @FXML private TableColumn<Employee, String> colGender;
    @FXML private TableColumn<Employee, String> colPhone;
    @FXML private TableColumn<Employee, String> colEmail;
    @FXML private TableColumn<Employee, String> colPosition;
    @FXML private TableColumn<Employee, String> colStatus;
    @FXML private TextField txtSearch;


    // ===== FILTER =====
    @FXML private ComboBox<String> cbFilterStatus;

    private EmployeeDAO dao = new EmployeeDAO();
    private ObservableList<Employee> empList;

    // ================= INITIALIZE =================
    @Override
    public void initialize(URL url, ResourceBundle rb) {

        // Filter
        cbFilterStatus.getItems().addAll("ALL", "Active", "Inactive");
        cbFilterStatus.setValue("ALL");

        // Table columns
        colId.setCellValueFactory(new PropertyValueFactory<>("employeeID"));
        colName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colDob.setCellValueFactory(new PropertyValueFactory<>("dob"));
        colGender.setCellValueFactory(new PropertyValueFactory<>("gender"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colPosition.setCellValueFactory(new PropertyValueFactory<>("position"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        loadData();
    }

    // ================= LOAD DATA =================
    private void loadData() {

        String status = cbFilterStatus.getValue();
        if (status == null || status.isEmpty()) {
            status = "ALL";
        }

        List<Employee> list = dao.getEmployees(status);
        empList = FXCollections.observableArrayList(list);
        tblEmployee.setItems(empList);
    }

    @FXML
    private void handleLoad() {
        loadData();
    }
    
    @FXML
private void handleSearch() {

    String key = txtSearch.getText().trim();

    if (key.isEmpty()) {
        showAlert("Please enter keyword to search!");
        return;
    }

    ObservableList<Employee> list =
        FXCollections.observableArrayList(dao.search(key));

    tblEmployee.setItems(list);
}


    // ================= ADD =================
    @FXML
    private void handleAdd() {
        openEmployeeForm(null, false);
    }

    // ================= UPDATE =================
    @FXML
    private void handleUpdate() {

        Employee e = tblEmployee.getSelectionModel().getSelectedItem();
        if (e == null) {
            showAlert("Please select an employee to update!");
            return;
        }

        openEmployeeForm(e, true);
    }

    // ================= DEACTIVATE =================
    @FXML
    private void handleDeactivate() {

        Employee e = tblEmployee.getSelectionModel().getSelectedItem();
        if (e == null) {
            showAlert("Please select an employee to deactivate!");
            return;
        }

        dao.deactivate(e.getEmployeeID());
        loadData();
    }

    // ================= ALERT =================
    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    // ================= POPUP FORM =================
   private void openEmployeeForm(Employee emp, boolean isUpdate) {

    Stage stage = new Stage();
    stage.setTitle(isUpdate ? "Update Employee" : "Add Employee");
    stage.initModality(Modality.APPLICATION_MODAL);

    // ===== COMMON STYLES =====
    String inputStyle =
        "-fx-background-color: #FFFDF8;" +
        "-fx-border-color: #C19A6B;" +
        "-fx-border-radius: 8;" +
        "-fx-background-radius: 8;" +
        "-fx-padding: 6 8 6 8;";

    String labelStyle =
        "-fx-font-weight: bold;" +
        "-fx-text-fill: #3E2723;";

    // ===== INPUT CONTROLS =====
    TextField txtId = new TextField();
    TextField txtName = new TextField();
    TextField txtPhone = new TextField();
    TextField txtEmail = new TextField();
    TextField txtAddress = new TextField();
    TextField txtPosition = new TextField();

    DatePicker dpDob = new DatePicker();
    DatePicker dpHire = new DatePicker();

    ComboBox<String> cbGender = new ComboBox<>();
    cbGender.getItems().addAll("male", "female");

    ComboBox<String> cbStatus = new ComboBox<>();
    cbStatus.getItems().addAll("Active", "Inactive");

    // Apply input style
    txtId.setStyle(inputStyle);
    txtName.setStyle(inputStyle);
    txtPhone.setStyle(inputStyle);
    txtEmail.setStyle(inputStyle);
    txtAddress.setStyle(inputStyle);
    txtPosition.setStyle(inputStyle);
    dpDob.setStyle(inputStyle);
    dpHire.setStyle(inputStyle);
    cbGender.setStyle(inputStyle);
    cbStatus.setStyle(inputStyle);

    // ===== LOAD DATA (UPDATE) =====
    if (isUpdate && emp != null) {
        txtId.setText(emp.getEmployeeID());
        txtId.setDisable(true);

        txtName.setText(emp.getFullName());
        dpDob.setValue(emp.getDob().toLocalDate());
        cbGender.setValue(emp.getGender());
        txtPhone.setText(emp.getPhone());
        txtEmail.setText(emp.getEmail());
        txtAddress.setText(emp.getAddress());
        dpHire.setValue(emp.getHireDate().toLocalDate());
        txtPosition.setText(emp.getPosition());
        cbStatus.setValue(emp.getStatus());
    }

    // ===== GRID =====
    GridPane grid = new GridPane();
    grid.setHgap(10);
    grid.setVgap(10);
    grid.setPadding(new Insets(20));
    grid.setStyle(
        "-fx-background-color: #FFF8F0;" +
        "-fx-border-color: #8B5E3C;" +
        "-fx-border-radius: 12;" +
        "-fx-background-radius: 12;"
    );

    int r = 0;

    Label lbId = new Label("Employee ID:");
    lbId.setStyle(labelStyle);
    grid.add(lbId, 0, r);
    grid.add(txtId, 1, r++);

    Label lbName = new Label("Full Name:");
    lbName.setStyle(labelStyle);
    grid.add(lbName, 0, r);
    grid.add(txtName, 1, r++);

    Label lbDob = new Label("DOB:");
    lbDob.setStyle(labelStyle);
    grid.add(lbDob, 0, r);
    grid.add(dpDob, 1, r++);

    Label lbGender = new Label("Gender:");
    lbGender.setStyle(labelStyle);
    grid.add(lbGender, 0, r);
    grid.add(cbGender, 1, r++);

    Label lbPhone = new Label("Phone:");
    lbPhone.setStyle(labelStyle);
    grid.add(lbPhone, 0, r);
    grid.add(txtPhone, 1, r++);

    Label lbEmail = new Label("Email:");
    lbEmail.setStyle(labelStyle);
    grid.add(lbEmail, 0, r);
    grid.add(txtEmail, 1, r++);

    Label lbAddress = new Label("Address:");
    lbAddress.setStyle(labelStyle);
    grid.add(lbAddress, 0, r);
    grid.add(txtAddress, 1, r++);

    Label lbHire = new Label("Hire Date:");
    lbHire.setStyle(labelStyle);
    grid.add(lbHire, 0, r);
    grid.add(dpHire, 1, r++);

    Label lbPosition = new Label("Position:");
    lbPosition.setStyle(labelStyle);
    grid.add(lbPosition, 0, r);
    grid.add(txtPosition, 1, r++);

    Label lbStatus = new Label("Status:");
    lbStatus.setStyle(labelStyle);
    grid.add(lbStatus, 0, r);
    grid.add(cbStatus, 1, r++);

    // ===== BUTTONS =====
    Button btnSave = new Button(isUpdate ? "Update" : "Add");
    Button btnClose = new Button("Close");

    btnSave.setStyle(
        "-fx-background-color: #8B5E3C;" +
        "-fx-text-fill: white;" +
        "-fx-font-weight: bold;" +
        "-fx-background-radius: 8;"
    );

    btnSave.setOnMouseEntered(e ->
        btnSave.setStyle(
            "-fx-background-color: #6D4C41;" +
            "-fx-text-fill: white;" +
            "-fx-font-weight: bold;" +
            "-fx-background-radius: 8;"
        )
    );

    btnSave.setOnMouseExited(e ->
        btnSave.setStyle(
            "-fx-background-color: #8B5E3C;" +
            "-fx-text-fill: white;" +
            "-fx-font-weight: bold;" +
            "-fx-background-radius: 8;"
        )
    );

    btnClose.setStyle(
        "-fx-background-color: #999;" +
        "-fx-text-fill: white;" +
        "-fx-background-radius: 8;"
    );

    btnClose.setOnAction(e -> stage.close());

    btnSave.setOnAction(e -> {

        Employee newEmp = new Employee(
            txtId.getText(),
            txtName.getText(),
            Date.valueOf(dpDob.getValue()),
            cbGender.getValue(),
            txtPhone.getText(),
            txtEmail.getText(),
            txtAddress.getText(),
            Date.valueOf(dpHire.getValue()),
            txtPosition.getText(),
            cbStatus.getValue()
        );

        if (isUpdate) {
            dao.update(newEmp);
        } else {
            if (dao.exists(newEmp.getEmployeeID())) {
                showAlert("Employee ID already exists!");
                return;
            }
            dao.insert(newEmp);
        }

        loadData();
        stage.close();
    });

    HBox hbox = new HBox(10, btnSave, btnClose);
    hbox.setAlignment(Pos.CENTER_RIGHT);

    VBox root = new VBox(15, grid, hbox);
    root.setPadding(new Insets(12));
    root.setStyle("-fx-background-color: #F5E6D3;");

    stage.setScene(new Scene(root, 430, 560));
    stage.showAndWait();
}

}
