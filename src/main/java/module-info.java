
module app.projectbakery {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.base;
    requires itextpdf;

    
    
    opens app.projectbakery to javafx.fxml;
    opens controller to javafx.fxml;
    opens model;
    exports app.projectbakery;
}
