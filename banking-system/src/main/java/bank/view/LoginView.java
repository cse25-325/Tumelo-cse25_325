package bank.view;

import bank.controller.LoginController;
import bank.controller.Session;
import bank.model.Bank;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * Boundary class for the login screen (layout: fxml/login.fxml).
 * Collects credentials and shows the result; all checks happen in LoginController.
 */
public class LoginView {

    private final LoginController controller;
    private final Navigator navigator;

    @FXML private Label titleLabel;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;

    public LoginView(LoginController controller, Navigator navigator) {
        this.controller = controller;
        this.navigator = navigator;
    }

    public Parent getRoot() {
        Parent root = Ui.load("login.fxml", this);
        titleLabel.setText(Bank.NAME);
        return root;
    }

    @FXML
    private void onLogin() {
        Ui.guard(() -> {
            Session session = controller.login(usernameField.getText(), passwordField.getText());
            navigator.showHome(session);
        });
    }

    @FXML
    private void onRegister() {
        navigator.showRegister();
    }
}
