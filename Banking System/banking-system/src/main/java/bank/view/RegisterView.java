package bank.view;

import bank.controller.CustomerController;
import bank.model.Bank;
import bank.model.Customer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Boundary class for customer registration. Validation is done by CustomerController. */
public class RegisterView {

    private final CustomerController controller;
    private final Navigator navigator;

    public RegisterView(CustomerController controller, Navigator navigator) {
        this.controller = controller;
        this.navigator = navigator;
    }

    public Parent getRoot() {
        TextField first = new TextField();
        TextField surname = new TextField();
        TextArea address = new TextArea();
        address.setPrefRowCount(2);
        address.setWrapText(true);
        TextField username = new TextField();
        PasswordField password = new PasswordField();

        Button register = new Button("Register");
        Ui.primary(register);
        register.setOnAction(e -> Ui.guard(() -> {
            Customer c = controller.register(first.getText(), surname.getText(), address.getText(),
                    username.getText(), password.getText());
            Ui.info("Welcome, " + c.getFullName() + "! Your customer number is " + c.getCustomerId()
                    + ". You can now log in.");
            navigator.showLogin();
        }));
        Button back = new Button("Back to login");
        back.setOnAction(e -> navigator.showLogin());

        VBox card = Ui.card(420);
        card.getChildren().addAll(Ui.title("Register with " + Bank.NAME),
                Ui.hint("Password: at least 8 characters with a letter and a digit."),
                new Label("First name"), first, new Label("Surname"), surname, new Label("Address"), address,
                new Label("Choose a username"), username, new Label("Choose a password"), password,
                new HBox(10, register, back));

        StackPane root = new StackPane(card);
        root.setPadding(new Insets(24));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #e8eef5;");
        return root;
    }
}
