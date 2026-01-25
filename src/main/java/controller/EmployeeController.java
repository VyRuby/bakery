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

public class EmployeeController extends BacktoHomeController implements Initializable {

    // ===== TABLE =====
    @FXML
    private TableView<Employee> tblEmployee;
    @FXML
    private TableColumn<Employee, String> colId;
    @FXML
    private TableColumn<Employee, String> colName;
    @FXML
    private TableColumn<Employee, Date> colDob;
    @FXML
    private TableColumn<Employee, String> colGender;
    @FXML
    private TableColumn<Employee, String> colPhone;
    @FXML
    private TableColumn<Employee, String> colEmail;
    @FXML
    private TableColumn<Employee, String> colPosition;
    @FXML
    private TableColumn<Employee, String> colStatus;
    @FXML
    private TableColumn<Employee, Integer> colSalary;
    @FXML
    private TextField txtSearch;

    // ===== FILTER =====
    @FXML
    private ComboBox<String> cbFilterStatus;

    private EmployeeDAO dao = new EmployeeDAO();
    private ObservableList<Employee> empList;

    // ================= INITIALIZE =================
    @Override
    public void initialize(URL url, ResourceBundle rb) {

        cbFilterStatus.getItems().addAll("ALL", "Active", "Inactive");
        cbFilterStatus.setValue("ALL");

        colId.setCellValueFactory(new PropertyValueFactory<>("employeeID"));
        colName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colDob.setCellValueFactory(new PropertyValueFactory<>("dob"));
        colGender.setCellValueFactory(new PropertyValueFactory<>("gender"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colPosition.setCellValueFactory(new PropertyValueFactory<>("position"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colSalary.setCellValueFactory(new PropertyValueFactory<>("baseDailySalary"));

        loadData();

        // ✅ SEARCH REALTIME
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> autoSearch());

        // ✅ FILTER REALTIME
        cbFilterStatus.valueProperty().addListener((obs, oldVal, newVal) -> autoSearch());
    }

    // ================= LOAD DATA =================
    private void loadData() {
        try {
            empList = FXCollections.observableArrayList(dao.getEmployees("ALL"));
            tblEmployee.setItems(empList);
        } catch (Exception e) {
            showAlert("Cannot load employee data!");
            e.printStackTrace();
        }
    }

    // ❌ KHÔNG DÙNG NỮA – GIỮ ĐỂ KHỎI LỖI FXML
    @FXML
    private void handleLoad() {
    }

    @FXML
    private void handleSearch() {
    }

    // ================= REALTIME SEARCH + FILTER =================
    private void autoSearch() {

        try {
            String keyword = txtSearch.getText() == null ? "" : txtSearch.getText().trim();
            String status = cbFilterStatus.getValue() == null ? "ALL" : cbFilterStatus.getValue();

            List<Employee> list;

            if (keyword.isEmpty()) {
                list = dao.getEmployees("ALL");
            } else {
                list = dao.search(keyword);
            }

            if (!"ALL".equalsIgnoreCase(status)) {
                list.removeIf(e -> e.getStatus() == null
                        || !e.getStatus().equalsIgnoreCase(status));
            }

            tblEmployee.setItems(FXCollections.observableArrayList(list));

        } catch (Exception ex) {
            showAlert("Error while searching employees!");
            ex.printStackTrace(); // để debug
        }
    }

    // ================= ADD / UPDATE =================
    @FXML
    private void handleAdd() {
        openEmployeeForm(null, false);
    }

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

    private boolean isValidEmail(String email) {
        return email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");
    }

    // ================= POPUP FORM =================
    private void openEmployeeForm(Employee emp, boolean isUpdate) {

        Stage stage = new Stage();
        stage.setTitle(isUpdate ? "Update Employee" : "Add Employee");
        stage.initModality(Modality.APPLICATION_MODAL);

        String inputStyle
                = "-fx-background-color: #FFFDF8;"
                + "-fx-border-color: #C19A6B;"
                + "-fx-border-radius: 8;"
                + "-fx-background-radius: 8;"
                + "-fx-padding: 6 8 6 8;";

        TextField txtId = new TextField();
        TextField txtName = new TextField();
        TextField txtPhone = new TextField();
        TextField txtEmail = new TextField();
        TextField txtAddress = new TextField();
        TextField txtPosition = new TextField();
        TextField txtSalary = new TextField();
        txtSalary.setTextFormatter(new TextFormatter<>(change -> {
            if (change.getControlNewText().matches("\\d*")) {
                return change;
            }
            return null;
        }));

        DatePicker dpDob = new DatePicker();
        DatePicker dpHire = new DatePicker();

        ComboBox<String> cbGender = new ComboBox<>();
        cbGender.getItems().addAll("male", "female");

        ComboBox<String> cbStatus = new ComboBox<>();
        cbStatus.getItems().addAll("Active", "Inactive");

        txtId.setStyle(inputStyle);
        txtName.setStyle(inputStyle);
        txtPhone.setStyle(inputStyle);
        txtEmail.setStyle(inputStyle);
        txtAddress.setStyle(inputStyle);
        txtPosition.setStyle(inputStyle);
        txtSalary.setStyle(inputStyle);

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
            txtSalary.setText(String.valueOf(emp.getBaseDailySalary()));
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        int r = 0;
        grid.add(new Label("Employee ID:"), 0, r);
        grid.add(txtId, 1, r++);
        grid.add(new Label("Full Name:"), 0, r);
        grid.add(txtName, 1, r++);
        grid.add(new Label("DOB:"), 0, r);
        grid.add(dpDob, 1, r++);
        grid.add(new Label("Gender:"), 0, r);
        grid.add(cbGender, 1, r++);
        grid.add(new Label("Phone:"), 0, r);
        grid.add(txtPhone, 1, r++);
        grid.add(new Label("Email:"), 0, r);
        grid.add(txtEmail, 1, r++);
        grid.add(new Label("Address:"), 0, r);
        grid.add(txtAddress, 1, r++);
        grid.add(new Label("Hire Date:"), 0, r);
        grid.add(dpHire, 1, r++);
        grid.add(new Label("Position:"), 0, r);
        grid.add(txtPosition, 1, r++);
        grid.add(new Label("Status:"), 0, r);
        grid.add(cbStatus, 1, r++);
        grid.add(new Label("Daily Salary:"), 0, r);
        grid.add(txtSalary, 1, r++);

        Button btnSave = new Button(isUpdate ? "Update" : "Add");
        Button btnClose = new Button("Close");

        btnClose.setOnAction(e -> stage.close());

        btnSave.setOnAction(e -> {
            try {
                if (txtId.getText().isBlank()
                        || txtName.getText().isBlank()
                        || dpDob.getValue() == null
                        || cbGender.getValue() == null
                        || txtPhone.getText().isBlank()
                        || txtEmail.getText().isBlank()
                        || dpHire.getValue() == null
                        || cbStatus.getValue() == null
                        || txtSalary.getText().isBlank()) {

                    showAlert("Please fill in all required fields!");
                    return;
                }

                // ✅ CHECK ID TRÙNG
                if (!isUpdate && dao.exists(txtId.getText().trim())) {
                    showAlert("Employee ID already exists!");
                    return;
                }

                // ✅ EMAIL
                if (!isValidEmail(txtEmail.getText().trim())) {
                    showAlert("Invalid email format!");
                    return;
                }

                // ✅ SALARY
                int salary;
                try {
                    salary = Integer.parseInt(txtSalary.getText());
                    if (salary <= 0) {
                        throw new Exception();
                    }
                } catch (Exception ex) {
                    showAlert("Daily salary must be a positive number!");
                    return;
                }

                Employee newEmp = new Employee(
                        txtId.getText().trim(),
                        txtName.getText().trim(),
                        Date.valueOf(dpDob.getValue()),
                        cbGender.getValue(),
                        txtPhone.getText().trim(),
                        txtEmail.getText().trim(),
                        txtAddress.getText().trim(),
                        Date.valueOf(dpHire.getValue()),
                        txtPosition.getText().trim(),
                        cbStatus.getValue(),
                        salary
                );

                if (isUpdate) {
                    dao.update(newEmp);
                } else {
                    dao.insert(newEmp);
                }

                loadData();
                stage.close();

            } catch (Exception ex) {
                showAlert("Error while saving employee!");
                ex.printStackTrace();
            }
        });

        HBox hbox = new HBox(10, btnSave, btnClose);
        hbox.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(15, grid, hbox);
        root.setPadding(new Insets(12));

        stage.setScene(new Scene(root, 430, 600));
        stage.showAndWait();
    }
}
