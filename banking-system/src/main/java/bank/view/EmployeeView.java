package bank.view;

import bank.controller.EmployeeController;
import bank.controller.Session;
import bank.model.Account;
import bank.model.Customer;
import bank.util.Money;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/** Boundary class for the bank employee's back-office screen (layout: fxml/employee.fxml). */
public class EmployeeView {

    private final EmployeeController controller;
    private final Session session;
    private final Navigator navigator;

    @FXML private Label titleLabel;

    @FXML private TableView<Customer> customerTable;
    @FXML private TableColumn<Customer, String> customerIdColumn;
    @FXML private TableColumn<Customer, String> customerNameColumn;
    @FXML private TableColumn<Customer, String> customerAddressColumn;
    @FXML private TableColumn<Customer, String> customerAccountsColumn;

    @FXML private TableView<Account> accountTable;
    @FXML private TableColumn<Account, String> accountNumberColumn;
    @FXML private TableColumn<Account, String> ownerColumn;
    @FXML private TableColumn<Account, String> accountTypeColumn;
    @FXML private TableColumn<Account, String> branchColumn;
    @FXML private TableColumn<Account, String> balanceColumn;

    public EmployeeView(EmployeeController controller, Session session, Navigator navigator) {
        this.controller = controller;
        this.session = session;
        this.navigator = navigator;
    }

    public Parent getRoot() {
        Parent root = Ui.load("employee.fxml", this);
        titleLabel.setText("Bank Employee - " + session.getUsername());
        refresh();
        return root;
    }

    /** Called by the FXML loader once the layout is loaded: wires the table columns. */
    @FXML
    private void initialize() {
        customerIdColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getCustomerId()));
        customerNameColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getFullName()));
        customerAddressColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getAddress()));
        customerAccountsColumn.setCellValueFactory(
                c -> new ReadOnlyStringWrapper(String.valueOf(c.getValue().getAccounts().size())));
        customerTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        accountNumberColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getAccountNumber()));
        ownerColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getOwner().toString()));
        accountTypeColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getAccountType().getLabel()));
        branchColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getBranch()));
        balanceColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(Money.format(c.getValue().getBalance())));
        accountTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    @FXML
    private void onLogout() {
        navigator.showLogin();
    }

    @FXML
    private void onCreditInterest() {
        Ui.guard(() -> {
            EmployeeController.InterestSummary s = controller.creditMonthlyInterest(session);
            refresh();
            Ui.info("Interest credited to " + s.getAccountsCredited() + " account(s), total "
                    + Money.format(s.getTotalInterest()) + ". Already paid this month: " + s.getAccountsSkipped() + ".");
        });
    }

    private void refresh() {
        Ui.guard(() -> {
            customerTable.getItems().setAll(controller.listCustomers(session));
            accountTable.getItems().setAll(controller.listAccounts(session));
        });
    }
}
