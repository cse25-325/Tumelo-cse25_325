package bank.view;

import bank.controller.EmployeeController;
import bank.controller.Session;
import bank.model.Account;
import bank.model.Customer;
import bank.util.Money;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Boundary class for the bank employee's back-office screen. */
public class EmployeeView {

    private final EmployeeController controller;
    private final Session session;
    private final Navigator navigator;

    private final TableView<Customer> customerTable = new TableView<>();
    private final TableView<Account> accountTable = new TableView<>();

    public EmployeeView(EmployeeController controller, Session session, Navigator navigator) {
        this.controller = controller;
        this.session = session;
        this.navigator = navigator;
    }

    public Parent getRoot() {
        Label title = new Label("Bank Employee - " + session.getUsername());
        title.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button logout = new Button("Log out");
        logout.setOnAction(e -> navigator.showLogin());
        HBox bar = new HBox(10, title, spacer, logout);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(14, 20, 14, 20));
        bar.setStyle("-fx-background-color: #12355b;");

        TabPane tabs = new TabPane(customersTab(), accountsTab(), interestTab());
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        BorderPane root = new BorderPane();
        root.setTop(bar);
        root.setCenter(tabs);
        refresh();
        return root;
    }

    private Tab customersTab() {
        TableColumn<Customer, String> id = new TableColumn<>("Customer no.");
        id.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getCustomerId()));
        TableColumn<Customer, String> name = new TableColumn<>("Name");
        name.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getFullName()));
        TableColumn<Customer, String> addr = new TableColumn<>("Address");
        addr.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getAddress()));
        TableColumn<Customer, String> count = new TableColumn<>("Accounts");
        count.setCellValueFactory(c -> new ReadOnlyStringWrapper(String.valueOf(c.getValue().getAccounts().size())));
        customerTable.getColumns().addAll(id, name, addr, count);
        customerTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        return paddedTab("Customers", customerTable);
    }

    private Tab accountsTab() {
        TableColumn<Account, String> no = new TableColumn<>("Account no.");
        no.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getAccountNumber()));
        TableColumn<Account, String> owner = new TableColumn<>("Owner");
        owner.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getOwner().toString()));
        TableColumn<Account, String> type = new TableColumn<>("Type");
        type.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getAccountType().getLabel()));
        TableColumn<Account, String> branch = new TableColumn<>("Branch");
        branch.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getBranch()));
        TableColumn<Account, String> bal = new TableColumn<>("Balance");
        bal.setCellValueFactory(c -> new ReadOnlyStringWrapper(Money.format(c.getValue().getBalance())));
        accountTable.getColumns().addAll(no, owner, type, branch, bal);
        accountTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        return paddedTab("All accounts", accountTable);
    }

    private Tab interestTab() {
        Button run = new Button("Credit this month's interest");
        Ui.primary(run);
        run.setOnAction(e -> Ui.guard(() -> {
            EmployeeController.InterestSummary s = controller.creditMonthlyInterest(session);
            refresh();
            Ui.info("Interest credited to " + s.getAccountsCredited() + " account(s), total "
                    + Money.format(s.getTotalInterest()) + ". Already paid this month: " + s.getAccountsSkipped() + ".");
        }));
        VBox box = new VBox(12, Ui.hint("Savings accounts earn 0.05% and Investment accounts 5% per month. "
                + "An account that has already received interest this month is skipped."), run);
        box.setPadding(new Insets(16));
        return new Tab("Monthly interest", box);
    }

    private static Tab paddedTab(String name, TableView<?> table) {
        VBox box = new VBox(table);
        box.setPadding(new Insets(12));
        VBox.setVgrow(table, Priority.ALWAYS);
        return new Tab(name, box);
    }

    private void refresh() {
        Ui.guard(() -> {
            customerTable.getItems().setAll(controller.listCustomers(session));
            accountTable.getItems().setAll(controller.listAccounts(session));
        });
    }
}
