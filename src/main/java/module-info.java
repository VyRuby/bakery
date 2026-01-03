module demo.luanan_nhom2 {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.base;
    requires java.sql;
    requires java.desktop;

    opens demo.luanan_nhom2 to javafx.fxml;
    exports demo.luanan_nhom2;
}
