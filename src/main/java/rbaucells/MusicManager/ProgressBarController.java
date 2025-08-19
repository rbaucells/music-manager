package rbaucells.MusicManager;

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
        if (percent == 100) {
            close();
            return;
        }
        else if (percent > 0 && !stage.isShowing()) {
            stage.show();
            System.out.println("Progress stage shown");
        }
        else if (percent == -1) {
            stage.close();
        }
        PercentText.setText("%" + percent);
        ProgressBar.setProgress((double) percent /100);
        InfoLabel.setText(message);
    }

    public void close() {
        stage.close();
    }
}
