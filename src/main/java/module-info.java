module app.bakery {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.base;

    opens app.bakery to javafx.fxml;
    opens controller to javafx.fxml;
    opens model;
    exports app.bakery;
}
