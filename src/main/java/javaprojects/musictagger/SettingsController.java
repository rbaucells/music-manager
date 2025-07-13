package javaprojects.musictagger;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.json.JSONObject;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class SettingsController {
    @FXML
    public TextField numberOfBatchDownloadThreadsTextField;

    @FXML
    public TextField apiKeyTextField;

    @FXML
    public TextField numberOfSearchResultsTextField;

    public Stage stage;

    public void OnLoad() throws IOException {
        JSONObject jsonObject = Application.GetSettings();

        numberOfBatchDownloadThreadsTextField.setText(String.valueOf(jsonObject.getInt("numberOfBatchDownloads")));
        numberOfSearchResultsTextField.setText(String.valueOf(jsonObject.getInt("numberOfSearchResults")));
        apiKeyTextField.setText(String.valueOf(jsonObject.getString("apiKey")));
    }

    public void OnSave() throws IOException {
        Application.SaveSettings(Integer.parseInt(numberOfBatchDownloadThreadsTextField.getText().strip()), Integer.parseInt(numberOfSearchResultsTextField.getText().strip()), apiKeyTextField.getText().strip(), Application.mainController.remainingDownloadRequests);
        stage.close();
    }

    public void OnCancel() {
        stage.close();
    }

    public void OnApiLink() throws IOException, URISyntaxException {
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                URI uri = new URI("https://rapidapi.com/nguyenmanhict-MuTUtGWD7K/api/youtube-mp3-2025");
                desktop.browse(uri);
            }
        }
    }
}
