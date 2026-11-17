package bank.view;

import bank.model.BankException;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Small helpers shared by all views: dialogs and consistent styling. No business logic. */
final class Ui {

    private Ui() {
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

    static Label title(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #12355b;");
        return l;
    }

    static Label hint(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #5b6770;");
        l.setWrapText(true);
        return l;
    }

    static VBox card(double width) {
        VBox box = new VBox(10);
        box.setPadding(new Insets(24));
        box.setMaxWidth(width);
        box.setStyle("-fx-background-color: white; -fx-background-radius: 10;"
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 12, 0, 0, 3);");
        return box;
    }

    static void primary(javafx.scene.control.Button b) {
        b.setStyle("-fx-background-color: #12355b; -fx-text-fill: white; -fx-font-weight: bold;"
                + "-fx-background-radius: 6; -fx-padding: 8 18 8 18;");
        b.setMinWidth(Region.USE_PREF_SIZE);
    }
}
