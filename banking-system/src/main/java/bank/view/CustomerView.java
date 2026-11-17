package bank.view;

import bank.controller.AccountController;
import bank.controller.Session;
import bank.model.Account;
import bank.model.AccountType;
import bank.model.Bank;
import bank.model.Transaction;
import bank.util.Money;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;

/**
 * Boundary class for a logged-in customer (the "AccountView"; layout: fxml/customer.fxml).
 * It only collects input and displays results; every rule (who may withdraw, minimum deposits...)
 * lives in the controller/model.
 */
public class CustomerView {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final AccountController controller;
    private final Session session;
    private final Navigator navigator;

    @FXML private Label welcomeLabel;
    @FXML private Label balanceLabel;

    @FXML private TableView<Account> accountTable;
    @FXML private TableColumn<Account, String> accountNumberColumn;
    @FXML private TableColumn<Account, String> accountTypeColumn;
    @FXML private TableColumn<Account, String> accountBalanceColumn;

    @FXML private TableView<Transaction> historyTable;
    @FXML private TableColumn<Transaction, String> whenColumn;
    @FXML private TableColumn<Transaction, String> txTypeColumn;
    @FXML private TableColumn<Transaction, String> amountColumn;
    @FXML private TableColumn<Transaction, String> balanceAfterColumn;
    @FXML private TableColumn<Transaction, String> descriptionColumn;

    @FXML private TextField depositAmount;
    @FXML private TextField withdrawAmount;

    @FXML private ComboBox<AccountType> typeBox;
    @FXML private ComboBox<String> branchBox;
    @FXML private TextField openingDeposit;
    @FXML private VBox employerBox;
    @FXML private TextField companyName;
    @FXML private TextField companyAddress;

    public CustomerView(AccountController controller, Session session, Navigator navigator) {
        this.controller = controller;
        this.session = session;
        this.navigator = navigator;
    }

    public Parent getRoot() {
        Parent root = Ui.load("customer.fxml", this);
        welcomeLabel.setText("Welcome, " + session.getDisplayName() + "  (" + session.getCustomerId() + ")");
        refreshAccounts();
        return root;
    }

    /** Called by the FXML loader once the layout is loaded: wires columns, lists and listeners. */
    @FXML
    private void initialize() {
        accountNumberColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getAccountNumber()));
        accountTypeColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getAccountType().getLabel()));
        accountBalanceColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(Money.format(c.getValue().getBalance())));
        accountTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        accountTable.setPlaceholder(new Label("You have no accounts yet. Open one on the right."));
        accountTable.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> onAccountSelected());

        whenColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getTimestamp().format(WHEN)));
        txTypeColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getType().name()));
        amountColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(Money.format(c.getValue().getAmount())));
        balanceAfterColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(Money.format(c.getValue().getBalanceAfter())));
        descriptionColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getDescription()));
        historyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        historyTable.setPlaceholder(new Label("No transactions to show."));

        typeBox.getItems().addAll(AccountType.values());
        branchBox.getItems().addAll(Bank.BRANCHES);
        branchBox.getSelectionModel().selectFirst();
        typeBox.valueProperty().addListener((o, a, b) -> {
            boolean cheque = b == AccountType.CHEQUE; // purely which fields to show
            employerBox.setVisible(cheque);
            employerBox.setManaged(cheque);
        });
    }

    // ---------------- button handlers ----------------

    @FXML
    private void onLogout() {
        navigator.showLogin();
    }

    @FXML
    private void onDeposit() {
        Ui.guard(() -> {
            Transaction t = controller.deposit(session.getCustomerId(), selectedNumber(), depositAmount.getText());
            depositAmount.clear();
            refreshAccounts();
            Ui.info("Deposit of " + Money.format(t.getAmount()) + " successful. New balance: "
                    + Money.format(t.getBalanceAfter()));
        });
    }

    @FXML
    private void onWithdraw() {
        Ui.guard(() -> {
            Transaction t = controller.withdraw(session.getCustomerId(), selectedNumber(), withdrawAmount.getText());
            withdrawAmount.clear();
            refreshAccounts();
            Ui.info("Withdraw of " + Money.format(t.getAmount()) + " successful. New balance: "
                    + Money.format(t.getBalanceAfter()));
        });
    }

    @FXML
    private void onOpenAccount() {
        Ui.guard(() -> {
            Account a = controller.openAccount(session.getCustomerId(), typeBox.getValue(), openingDeposit.getText(),
                    branchBox.getValue(), companyName.getText(), companyAddress.getText());
            openingDeposit.clear();
            companyName.clear();
            companyAddress.clear();
            refreshAccounts();
            Ui.info("Your " + a.getAccountType().getLabel() + " account " + a.getAccountNumber() + " is open.");
        });
    }

    // ---------------- display only ----------------

    private String selectedNumber() {
        Account a = accountTable.getSelectionModel().getSelectedItem();
        return a == null ? null : a.getAccountNumber();
    }

    private void refreshAccounts() {
        String keep = selectedNumber();
        Ui.guard(() -> {
            accountTable.getItems().setAll(controller.getAccounts(session.getCustomerId()));
            if (keep != null) {
                accountTable.getItems().stream().filter(a -> a.getAccountNumber().equals(keep)).findFirst()
                        .ifPresent(a -> accountTable.getSelectionModel().select(a));
            }
        });
        onAccountSelected();
    }

    private void onAccountSelected() {
        String number = selectedNumber();
        if (number == null) {
            balanceLabel.setText("Select an account");
            historyTable.getItems().clear();
            return;
        }
        Ui.guard(() -> {
            balanceLabel.setText("Balance: " + Money.format(controller.getBalance(session.getCustomerId(), number)));
            historyTable.getItems().setAll(controller.getHistory(session.getCustomerId(), number));
        });
    }
}
