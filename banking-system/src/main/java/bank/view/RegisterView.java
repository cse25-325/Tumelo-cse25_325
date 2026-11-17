package bank.view;

import bank.controller.CustomerController;
import bank.model.Bank;
import bank.model.Customer;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

/** Boundary class for customer registration (layout: fxml/register.fxml). Validation is in CustomerController. */
public class RegisterView {

    private final CustomerController controller;
    private final Navigator navigator;

    @FXML private Label titleLabel;
    @FXML private TextField firstNameField;
    @FXML private TextField surnameField;
    @FXML private TextArea addressField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;

    public RegisterView(CustomerController controller, Navigator navigator) {
        this.controller = controller;
        this.navigator = navigator;
    }

    public Parent getRoot() {
        Parent root = Ui.load("register.fxml", this);
        titleLabel.setText("Register with " + Bank.NAME);
        return root;
    }

    @FXML
    private void onRegister() {
        Ui.guard(() -> {
            Customer c = controller.register(firstNameField.getText(), surnameField.getText(),
                    addressField.getText(), usernameField.getText(), passwordField.getText());
            Ui.info("Welcome, " + c.getFullName() + "! Your customer number is " + c.getCustomerId()
                    + ". You can now log in.");
            navigator.showLogin();
        });
    }

    @FXML
    private void onBack() {
        navigator.showLogin();
    }
}
