package bank;

import bank.view.BankApp;
import javafx.application.Application;

/** Plain entry point (keeps JavaFX launching simple from IDEs, Maven and Codespaces). */
public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        Application.launch(BankApp.class, args);
    }
}
