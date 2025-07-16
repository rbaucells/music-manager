package javaprojects.musictagger;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;

public class ErrorController {
    @FunctionalInterface
    public interface OnRetryInterface {
        void execute() throws IOException;
    }

    @FXML
    public Label InformationLabel;

    public OnRetryInterface onRetryInterface;

    public Stage stage;

    public void OnRetry() throws IOException {
        if (onRetryInterface != null) {
            onRetryInterface.execute();
            stage.close();
        }
    }

    public void OnCancel() {
        stage.close();
    }
}
