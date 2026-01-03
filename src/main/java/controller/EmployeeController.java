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
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

public class EmployeeController implements Initializable {

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

    // ===== FORM =====
    @FXML private TextField txtId;
    @FXML private TextField txtName;
    @FXML private TextField txtPhone;
    @FXML private TextField txtEmail;
    @FXML private TextField txtAddress;
    @FXML private TextField txtPosition;

    @FXML private DatePicker dpDob;
    @FXML private DatePicker dpHireDate;

    @FXML private ComboBox<String> cbGender;
    @FXML private ComboBox<String> cbStatus;
    @FXML private ComboBox<String> cbFilterStatus;

    private EmployeeDAO dao = new EmployeeDAO();
    private ObservableList<Employee> empList;

    // ================= INITIALIZE =================
    @Override
    public void initialize(URL url, ResourceBundle rb) {

        // ComboBox
        cbGender.getItems().addAll("Male", "Female");
        cbStatus.getItems().addAll("Active", "Inactive");
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

        // Load data
        loadData();

        // Click table -> fill form
        tblEmployee.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldVal, newVal) -> {
                    if (newVal != null) {
                        fillForm(newVal);
                    }
                });
    }

    // ================= LOAD DATA =================
    private void loadData() {
        List<Employee> list = dao.getEmployees(cbFilterStatus.getValue());
        empList = FXCollections.observableArrayList(list);
        tblEmployee.setItems(empList);
    }

    @FXML
    private void handleLoad() {
        loadData();
    }

    // ================= ADD =================
    @FXML
    private void handleAdd() {

        if (!validateForm()) return;

        Employee e = getEmployeeFromForm();
        dao.insert(e);
        loadData();
        clearForm();
    }

    // ================= UPDATE =================
    @FXML
    private void handleUpdate() {

        if (tblEmployee.getSelectionModel().getSelectedItem() == null) {
            showAlert("Please select an employee to update!");
            return;
        }

        if (!validateForm()) return;

        Employee e = getEmployeeFromForm();
        dao.update(e);
        loadData();
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
        clearForm();
    }

    // ================= HELPERS =================
    private Employee getEmployeeFromForm() {

        return new Employee(
                txtId.getText(),
                txtName.getText(),
                Date.valueOf(dpDob.getValue()),
                cbGender.getValue(),
                txtPhone.getText(),
                txtEmail.getText(),
                txtAddress.getText(),
                Date.valueOf(dpHireDate.getValue()),
                txtPosition.getText(),
                cbStatus.getValue()
        );
    }
    
 


    private void fillForm(Employee e) {

        txtId.setText(e.getEmployeeID());
        txtId.setDisable(true); // không cho sửa ID

        txtName.setText(e.getFullName());
        dpDob.setValue(e.getDob().toLocalDate());
        cbGender.setValue(e.getGender());
        txtPhone.setText(e.getPhone());
        txtEmail.setText(e.getEmail());
        txtAddress.setText(e.getAddress());
        dpHireDate.setValue(e.getHireDate().toLocalDate());
        txtPosition.setText(e.getPosition());
        cbStatus.setValue(e.getStatus());
    }

    private void clearForm() {

        txtId.clear();
        txtId.setDisable(false);

        txtName.clear();
        dpDob.setValue(null);
        cbGender.setValue(null);
        txtPhone.clear();
        txtEmail.clear();
        txtAddress.clear();
        dpHireDate.setValue(null);
        txtPosition.clear();
        cbStatus.setValue(null);

        tblEmployee.getSelectionModel().clearSelection();
    }

    private boolean validateForm() {

        if (txtId.getText().isEmpty()
                || txtName.getText().isEmpty()
                || dpDob.getValue() == null
                || cbGender.getValue() == null
                || dpHireDate.getValue() == null
                || cbStatus.getValue() == null) {

            showAlert("Please fill all required fields!");
            return false;
        }
        return true;
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
