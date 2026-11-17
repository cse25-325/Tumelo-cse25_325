package bank.view;

import bank.controller.AccountController;
import bank.controller.Session;
import bank.model.Account;
import bank.model.AccountType;
import bank.model.Bank;
import bank.model.Transaction;
import bank.util.Money;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;

/**
 * Boundary class for a logged-in customer (the "AccountView"). It only collects input and
 * displays results; every rule (who may withdraw, minimum deposits...) lives in the controller/model.
 */
public class CustomerView {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final AccountController controller;
    private final Session session;
    private final Navigator navigator;

    private final TableView<Account> accountTable = new TableView<>();
    private final TableView<Transaction> historyTable = new TableView<>();
    private final Label balanceLabel = new Label("Select an account");

    public CustomerView(AccountController controller, Session session, Navigator navigator) {
        this.controller = controller;
        this.session = session;
        this.navigator = navigator;
    }

    public Parent getRoot() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #e8eef5;");
        root.setTop(header());
        root.setLeft(accountsPane());
        root.setCenter(actionsPane());
        refreshAccounts();
        return root;
    }

    // ---------------- layout ----------------

    private Parent header() {
        Label welcome = new Label("Welcome, " + session.getDisplayName() + "  (" + session.getCustomerId() + ")");
        welcome.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button logout = new Button("Log out");
        logout.setOnAction(e -> navigator.showLogin());
        HBox bar = new HBox(10, welcome, spacer, logout);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(14, 20, 14, 20));
        bar.setStyle("-fx-background-color: #12355b;");
        return bar;
    }

    private Parent accountsPane() {
        TableColumn<Account, String> number = new TableColumn<>("Account no.");
        number.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getAccountNumber()));
        TableColumn<Account, String> type = new TableColumn<>("Type");
        type.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getAccountType().getLabel()));
        TableColumn<Account, String> balance = new TableColumn<>("Balance");
        balance.setCellValueFactory(c -> new ReadOnlyStringWrapper(Money.format(c.getValue().getBalance())));
        accountTable.getColumns().addAll(number, type, balance);
        accountTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        accountTable.setPlaceholder(new Label("You have no accounts yet. Open one on the right."));
        accountTable.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> onAccountSelected());

        VBox box = new VBox(8, new Label("Your accounts"), accountTable);
        box.setPadding(new Insets(16));
        box.setPrefWidth(400);
        VBox.setVgrow(accountTable, Priority.ALWAYS);
        return box;
    }

    private Parent actionsPane() {
        balanceLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #12355b;");
        TabPane tabs = new TabPane(transactTab("Deposit", true), transactTab("Withdraw", false),
                historyTab(), openAccountTab());
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox box = new VBox(10, balanceLabel, tabs);
        box.setPadding(new Insets(16));
        VBox.setVgrow(tabs, Priority.ALWAYS);
        return box;
    }

    private Tab transactTab(String name, boolean isDeposit) {
        TextField amount = new TextField();
        amount.setPromptText("Amount in BWP, e.g. 250.00");
        Button go = new Button(name);
        Ui.primary(go);
        go.setOnAction(e -> Ui.guard(() -> {
            String acc = selectedNumber();
            Transaction t = isDeposit
                    ? controller.deposit(session.getCustomerId(), acc, amount.getText())
                    : controller.withdraw(session.getCustomerId(), acc, amount.getText());
            amount.clear();
            refreshAccounts();
            Ui.info(name + " of " + Money.format(t.getAmount()) + " successful. New balance: "
                    + Money.format(t.getBalanceAfter()));
        }));
        Label note = Ui.hint(isDeposit
                ? "Deposit into the selected account."
                : "Withdraw from the selected account (Investment and Cheque accounts only).");
        VBox content = new VBox(10, note, new Label("Amount"), amount, go);
        content.setPadding(new Insets(16));
        return new Tab(name, content);
    }

    private Tab historyTab() {
        TableColumn<Transaction, String> when = new TableColumn<>("Date");
        when.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getTimestamp().format(WHEN)));
        TableColumn<Transaction, String> type = new TableColumn<>("Type");
        type.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getType().name()));
        TableColumn<Transaction, String> amt = new TableColumn<>("Amount");
        amt.setCellValueFactory(c -> new ReadOnlyStringWrapper(Money.format(c.getValue().getAmount())));
        TableColumn<Transaction, String> after = new TableColumn<>("Balance after");
        after.setCellValueFactory(c -> new ReadOnlyStringWrapper(Money.format(c.getValue().getBalanceAfter())));
        TableColumn<Transaction, String> desc = new TableColumn<>("Description");
        desc.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getDescription()));
        historyTable.getColumns().addAll(when, type, amt, after, desc);
        historyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        historyTable.setPlaceholder(new Label("No transactions to show."));
        VBox content = new VBox(8, Ui.hint("Transaction history of the selected account (newest first)."), historyTable);
        content.setPadding(new Insets(16));
        VBox.setVgrow(historyTable, Priority.ALWAYS);
        return new Tab("History", content);
    }

    private Tab openAccountTab() {
        ComboBox<AccountType> type = new ComboBox<>();
        type.getItems().addAll(AccountType.values());
        type.setPromptText("Account type");
        ComboBox<String> branch = new ComboBox<>();
        branch.getItems().addAll(Bank.BRANCHES);
        branch.getSelectionModel().selectFirst();
        TextField deposit = new TextField();
        deposit.setPromptText("Opening deposit (Investment: min BWP 500.00)");
        TextField company = new TextField();
        company.setPromptText("Company name");
        TextField companyAddress = new TextField();
        companyAddress.setPromptText("Company address");

        VBox employer = new VBox(6, Ui.hint("Cheque accounts need your employer's details."), company, companyAddress);
        employer.setVisible(false);
        employer.setManaged(false);
        type.valueProperty().addListener((o, a, b) -> {
            boolean cheque = b == AccountType.CHEQUE; // purely which fields to show
            employer.setVisible(cheque);
            employer.setManaged(cheque);
        });

        Button open = new Button("Open account");
        Ui.primary(open);
        open.setOnAction(e -> Ui.guard(() -> {
            Account a = controller.openAccount(session.getCustomerId(), type.getValue(), deposit.getText(),
                    branch.getValue(), company.getText(), companyAddress.getText());
            deposit.clear();
            company.clear();
            companyAddress.clear();
            refreshAccounts();
            Ui.info("Your " + a.getAccountType().getLabel() + " account " + a.getAccountNumber() + " is open.");
        }));

        VBox content = new VBox(10, Ui.hint("Open another Savings, Investment or Cheque account."),
                new Label("Account type"), type, new Label("Branch"), branch,
                new Label("Opening deposit"), deposit, employer, open);
        content.setPadding(new Insets(16));
        return new Tab("Open account", content);
    }

    // ---------------- behaviour (display only) ----------------

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
