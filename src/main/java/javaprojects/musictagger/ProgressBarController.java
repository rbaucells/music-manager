package javaprojects.musictagger;

import javafx.fxml.FXML;
import javafx.scene.text.Text;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class ProgressBarController {
    @FunctionalInterface
    public interface OnCancelInterface {
        void execute();
    }

    // Texts
    @FXML
    public Text LoadingText;
    @FXML
    public Text PercentText;

    // Progress Bar
    @FXML
    public javafx.scene.control.ProgressBar ProgressBar;

    // Label
    @FXML
    public Label InfoLabel;

    // Stage
    public Stage stage;

    // Lambda
    public OnCancelInterface onCancelInterface;

    public void OnCancel() {
        if (onCancelInterface != null)
            onCancelInterface.execute();
    }

    public void SetProgressBar(String message, int percent) {
        PercentText.setText("%" + percent);
        ProgressBar.setProgress((double) percent /100);
        InfoLabel.setText(message);
    }

    public void close() {
        stage.close();
    }
}
