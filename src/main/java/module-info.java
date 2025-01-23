module com.czaplicki.lab06 {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;

    opens com.czaplicki.lab06 to javafx.fxml;
    exports com.czaplicki.lab06;
    exports com.czaplicki.lab06.interfaces;
    opens com.czaplicki.lab06.interfaces to javafx.fxml;
}