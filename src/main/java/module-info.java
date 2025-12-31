module app {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.base;

    opens app to javafx.fxml;
    opens controller to javafx.fxml;
    opens model;
    exports app;
}
