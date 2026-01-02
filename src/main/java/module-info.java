module app.projectbakery {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.base;

    opens app.projectbakery to javafx.fxml;
    opens controller to javafx.fxml;

    exports app.projectbakery;
}
