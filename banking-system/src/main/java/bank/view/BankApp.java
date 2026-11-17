package bank.view;

import bank.controller.BankingController;
import bank.controller.SampleDataLoader;
import bank.controller.Session;
import bank.model.Bank;
import bank.model.Role;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** JavaFX application: owns the window and switches between screens. */
public class BankApp extends Application implements Navigator {

    private static final String DB_URL = "jdbc:h2:./data/bankdb";

    private Stage stage;
    private BankingController bank;

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        bank = BankingController.create(DB_URL);
        if (!bank.hasCustomers()) {
            SampleDataLoader.load(bank); // 10 sample records for demonstration
        }
        stage.setTitle(Bank.NAME);
        stage.setWidth(1000);
        stage.setHeight(640);
        showLogin();
        stage.show();
    }

    @Override
    public void stop() {
        if (bank != null) {
            bank.close();
        }
    }

    @Override
    public void showLogin() {
        show(new LoginView(bank.login(), this).getRoot());
    }

    @Override
    public void showRegister() {
        show(new RegisterView(bank.customers(), this).getRoot());
    }

    @Override
    public void showHome(Session session) {
        if (session.getRole() == Role.EMPLOYEE) {
            show(new EmployeeView(bank.employee(), session, this).getRoot());
        } else {
            show(new CustomerView(bank.accounts(), session, this).getRoot());
        }
    }

    private void show(Parent root) {
        stage.setScene(new Scene(root, stage.getWidth() > 0 ? stage.getWidth() : 1000,
                stage.getHeight() > 0 ? stage.getHeight() : 640));
    }
}
