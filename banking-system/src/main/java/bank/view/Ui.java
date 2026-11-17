package bank.view;

import bank.model.BankException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;

import java.io.IOException;
import java.io.UncheckedIOException;

/** Small helpers shared by all views: FXML loading and dialogs. No business logic. */
final class Ui {

    private Ui() {
    }

    /** Loads a layout from src/main/resources/fxml and makes {@code controller} its FXML controller. */
    static Parent load(String fxmlFile, Object controller) {
        FXMLLoader loader = new FXMLLoader(Ui.class.getResource("/fxml/" + fxmlFile));
        loader.setController(controller);
        try {
            return loader.load();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not load screen " + fxmlFile, e);
        }
    }

    static void info(String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, message);
        a.setHeaderText("Success");
        a.showAndWait();
    }

    static void error(String message) {
        Alert a = new Alert(Alert.AlertType.ERROR, message);
        a.setHeaderText("Something went wrong");
        a.showAndWait();
    }

    /** Runs a user action; any business failure is shown as a friendly dialog instead of crashing. */
    static void guard(Runnable action) {
        try {
            action.run();
        } catch (BankException e) {
            error(e.getMessage());
        } catch (RuntimeException e) {
            error("An unexpected problem occurred. Please try again. (" + e.getMessage() + ")");
        }
    }
}
