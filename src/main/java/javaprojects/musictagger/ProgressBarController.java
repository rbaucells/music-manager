package javaprojects.musictagger;

import javafx.fxml.FXML;
import javafx.scene.text.Text;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class ProgressBarController {
    @FXML
    public Text LoadingText;

    @FXML
    public Text PercentText;

    @FXML
    public javafx.scene.control.ProgressBar ProgressBar;

    @FXML
    public Label InfoLabel;

    public DownloadThread downloadThread;
    public Stage myStage;

    public void OnCancel() {
        downloadThread.interrupt();
        myStage.close();

        InfoLabel.setText("Cancelling Download");
        LoadingText.setText("Cancelling");
    }
}
