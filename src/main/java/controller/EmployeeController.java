package controller;

import model.Employee;
import DAO_Employee.EmployeeDAO;

import java.net.URL;
import java.sql.Date;
import java.time.LocalDate;
import java.time.Period;
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
import javafx.scene.text.Text;
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
        
         styleInactive(colId);
styleInactive(colName);
styleInactive(colDob);
styleInactive(colGender);
styleInactive(colPhone);
styleInactive(colEmail);
styleInactive(colPosition);
styleInactive(colStatus);
styleInactive(colSalary);


        loadData();

        // ✅ SEARCH REALTIME
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> autoSearch());

        // ✅ FILTER REALTIME
        cbFilterStatus.valueProperty().addListener((obs, oldVal, newVal) -> autoSearch());
    }
    
private <T> void styleInactive(TableColumn<Employee, T> col) {
    col.setCellFactory(column -> new TableCell<>() {

        private final Text text = new Text();

        @Override
        protected void updateItem(T item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setGraphic(null);
                setText(null);
                return;
            }

            text.setText(item.toString());

            Employee emp = getTableView().getItems().get(getIndex());

            if ("Inactive".equalsIgnoreCase(emp.getStatus())) {
                text.setFill(javafx.scene.paint.Color.web("#B91C1C"));
                text.setStrikethrough(true);   // ✅ GẠCH NGANG THẬT
                text.setOpacity(0.8);
            } else {
                text.setFill(javafx.scene.paint.Color.BLACK);
                text.setStrikethrough(false);
                text.setOpacity(1);
            }

            setText(null);      // ❌ KHÔNG DÙNG setText
            setGraphic(text);   // ✅ DÙNG GRAPHIC
        }
    });
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
      // 🚫 ĐÃ INACTIVE → KHÔNG CHO DEACTIVATE
    if ("Inactive".equalsIgnoreCase(e.getStatus())) {
        showAlert("This employee is already inactive!");
        return;
    }

    try {
        dao.deactivate(e.getEmployeeID());
        loadData();
        showSuccess("Employee deactivated successfully!");
    } catch (Exception ex) {
        showError("Deactivate employee failed!");
    }
}


    // ================= ALERT =================
    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
    private void showSuccess(String msg) {
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.setHeaderText(null);
    alert.setContentText(msg);
    alert.showAndWait();
}

private void showError(String msg) {
    Alert alert = new Alert(Alert.AlertType.ERROR);
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

    String inputStyle =
            "-fx-background-color:#FFFDF8;" +
            "-fx-border-color:#C19A6B;" +
            "-fx-border-radius:8;" +
            "-fx-background-radius:8;" +
            "-fx-padding:6 8;";

    // ===== INPUT =====
    TextField txtId = new TextField();
    TextField txtName = new TextField();
    TextField txtPhone = new TextField();
    TextField txtEmail = new TextField();
    TextField txtAddress = new TextField();
    TextField txtSalary = new TextField();

    DatePicker dpDob = new DatePicker();
    DatePicker dpHire = new DatePicker();

    ComboBox<String> cbGender = new ComboBox<>();
    cbGender.getItems().addAll("male", "female");

    ComboBox<String> cbStatus = new ComboBox<>();
    cbStatus.getItems().addAll("Active", "Inactive");

    ComboBox<String> cbPosition = new ComboBox<>();
    cbPosition.getItems().addAll("Manager", "Staff");

    txtId.setStyle(inputStyle);
    txtName.setStyle(inputStyle);
    txtPhone.setStyle(inputStyle);
    //    Không gõ được chữ
//,Không paste ký tự đặc biệt,Giống hệt Salary
    txtPhone.setTextFormatter(new TextFormatter<>(c ->
        c.getControlNewText().matches("\\d*") ? c : null));


    txtEmail.setStyle(inputStyle);
    txtAddress.setStyle(inputStyle);
    txtSalary.setStyle(inputStyle);

    txtSalary.setTextFormatter(new TextFormatter<>(c ->
            c.getControlNewText().matches("\\d*") ? c : null));

    // ===== ERROR LABELS =====
    Label errId = new Label();
    Label errName = new Label();
    Label errEmail = new Label();
    Label errSalary = new Label();
    Label errPhone = new Label();
    Label errDob = new Label();


    Button btnSave = new Button(isUpdate ? "Update" : "Add");
    btnSave.setDisable(true);

    // ===== LOAD UPDATE DATA =====
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
        cbPosition.setValue(emp.getPosition());
        cbStatus.setValue(emp.getStatus());
        txtSalary.setText(String.valueOf(emp.getBaseDailySalary()));
    }

    // ===== VALIDATION =====
    Runnable validate = () -> {

        boolean ok = true;

        // ID
        if (!isUpdate) {
            if (txtId.getText().isBlank()) {
                markError(txtId, errId, "Required");
                ok = false;
            } else if (dao.exists(txtId.getText().trim())) {
                markError(txtId, errId, "ID already exists");
                ok = false;
            } else {
                clearError(txtId, errId);
            }
        }

        // Name
        if (txtName.getText().isBlank()) {
            markError(txtName, errName, "Required");
            ok = false;
        } else {
            clearError(txtName, errName);
        }
        // DOB – validate age >= 18
if (dpDob.getValue() == null) {
    markError(dpDob, errDob, "Required");
    ok = false;
} else {
    LocalDate dob = dpDob.getValue();
    LocalDate today = LocalDate.now();
    int age = Period.between(dob, today).getYears();

    if (age < 18) {
        markError(dpDob, errDob, "Must be at least 18 years old");
        ok = false;
    } else {
        clearError(dpDob, errDob);
    }
}

        // Phone
if (txtPhone.getText().isBlank()) {
    markError(txtPhone, errPhone, "Required");
    ok = false;
} else if (!txtPhone.getText().matches("\\d{9,11}")) {
    markError(txtPhone, errPhone, "9–11 digits");
    ok = false;
} else {
    clearError(txtPhone, errPhone);
}


        // Email
        if (!isValidEmail(txtEmail.getText())) {
            markError(txtEmail, errEmail, "Invalid email");
            ok = false;
        } else if (isUpdate
                ? dao.existsEmailExceptId(txtEmail.getText(), txtId.getText())
                : dao.existsEmail(txtEmail.getText())) {
            markError(txtEmail, errEmail, "Email already exists");
            ok = false;
        } else {
            clearError(txtEmail, errEmail);
        }

        // Salary
        try {
            int s = Integer.parseInt(txtSalary.getText());
            if (s <= 0) throw new Exception();
            clearError(txtSalary, errSalary);
        } catch (Exception e) {
            markError(txtSalary, errSalary, "Invalid salary");
            ok = false;
        }

        btnSave.setDisable(!ok);
    };

    txtId.textProperty().addListener((o, a, b) -> validate.run());
    txtName.textProperty().addListener((o, a, b) -> validate.run());
    txtEmail.textProperty().addListener((o, a, b) -> validate.run());
    txtSalary.textProperty().addListener((o, a, b) -> validate.run());
    txtPhone.textProperty().addListener((o, a, b) -> validate.run());
    dpDob.valueProperty().addListener((o, a, b) -> validate.run());


    // ===== GRID =====
    GridPane grid = new GridPane();
    grid.setHgap(10);
    grid.setVgap(6);
    grid.setPadding(new Insets(20));

    int r = 0;
    grid.add(new Label("Employee ID"), 0, r);
    grid.add(txtId, 1, r);
    grid.add(errId, 2, r++);

    grid.add(new Label("Full Name"), 0, r);
    grid.add(txtName, 1, r);
    grid.add(errName, 2, r++);

    grid.add(new Label("DOB"), 0, r);
grid.add(dpDob, 1, r);
grid.add(errDob, 2, r++);


    grid.add(new Label("Gender"), 0, r);
    grid.add(cbGender, 1, r++);

    grid.add(new Label("Phone"), 0, r);
grid.add(txtPhone, 1, r);
grid.add(errPhone, 2, r++);


    grid.add(new Label("Email"), 0, r);
    grid.add(txtEmail, 1, r);
    grid.add(errEmail, 2, r++);

    grid.add(new Label("Address"), 0, r);
    grid.add(txtAddress, 1, r++);

    grid.add(new Label("Hire Date"), 0, r);
    grid.add(dpHire, 1, r++);

    grid.add(new Label("Position"), 0, r);
    grid.add(cbPosition, 1, r++);

    grid.add(new Label("Status"), 0, r);
    grid.add(cbStatus, 1, r++);

    grid.add(new Label("Daily Salary"), 0, r);
    grid.add(txtSalary, 1, r);
    grid.add(errSalary, 2, r++);

    // ===== SAVE =====
   btnSave.setOnAction(e -> {
    try {
        Employee newEmp = new Employee(
                txtId.getText().trim(),
                txtName.getText().trim(),
                Date.valueOf(dpDob.getValue()),
                cbGender.getValue(),
                txtPhone.getText().trim(),
                txtEmail.getText().trim(),
                txtAddress.getText().trim(),
                Date.valueOf(dpHire.getValue()),
                cbPosition.getValue(),
                cbStatus.getValue(),
                Integer.parseInt(txtSalary.getText())
        );

        if (isUpdate) {
            dao.update(newEmp);
            showSuccess("Employee updated successfully!");
        } else {
            dao.insert(newEmp);
            showSuccess("Employee added successfully!");
        }

        loadData();
        stage.close();

    } catch (Exception ex) {
        showError(ex.getMessage() != null
                ? ex.getMessage()
                : "Operation failed!");
    }
});


    Button btnClose = new Button("Close");
    btnClose.setOnAction(e -> stage.close());

    HBox hbox = new HBox(10, btnSave, btnClose);
    hbox.setAlignment(Pos.CENTER_RIGHT);

    VBox root = new VBox(15, grid, hbox);
    root.setPadding(new Insets(12));

    stage.setScene(new Scene(root, 520, 560));
    stage.showAndWait();
}

    private void markError(Control field, Label errorLabel, String message) {
    field.setStyle("-fx-border-color:red; -fx-border-radius:8;");
    errorLabel.setText(message);
    errorLabel.setStyle("-fx-text-fill:red; -fx-font-size:11;");
}

private void clearError(Control field, Label errorLabel) {
    field.setStyle("-fx-border-color:#C19A6B; -fx-border-radius:8;");
    errorLabel.setText("");
}

}
