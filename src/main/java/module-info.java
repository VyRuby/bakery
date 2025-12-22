module app.projectbakery {
    requires javafx.controls;
    requires javafx.fxml;

    opens app.projectbakery to javafx.fxml;
    exports app.projectbakery;
}
