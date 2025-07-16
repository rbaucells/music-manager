package javaprojects.musictagger;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class SettingsController {
    private static final Logger logger = LoggerFactory.getLogger(SettingsController.class);
    // TextFields
    @FXML
    public TextField numberOfBatchDownloadThreadsTextField;
    @FXML
    public TextField apiKeyTextField;
    @FXML
    public TextField numberOfSearchResultsTextField;

    public Stage stage;

    public void onLoad() {
        logger.info("loadingSettings, getting info from settingsJSONFile");
        JSONObject jsonObject = Application.getSettings();

        logger.debug("putting received info into textFields");
        numberOfBatchDownloadThreadsTextField.setText(String.valueOf(jsonObject.getInt("numberOfBatchDownloads")));
        numberOfSearchResultsTextField.setText(String.valueOf(jsonObject.getInt("numberOfSearchResults")));
        apiKeyTextField.setText(String.valueOf(jsonObject.getString("apiKey")));
    }

    public void onSave() {
        logger.info("Saving information from textFields into settingsJSONFile");
        Application.saveSettings(Integer.parseInt(numberOfBatchDownloadThreadsTextField.getText().strip()), Integer.parseInt(numberOfSearchResultsTextField.getText().strip()), apiKeyTextField.getText().strip(), Application.mainController.remainingDownloadRequests);
        logger.debug("closing stage");
        stage.close();
    }

    public void onCancel() {
        stage.close();
    }

    public void onApiLink() throws IOException, URISyntaxException {
        logger.info("sending user web browser to link to get an apiKey");
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                URI uri = new URI("https://rapidapi.com/nguyenmanhict-MuTUtGWD7K/api/youtube-mp3-2025");
                desktop.browse(uri);
            }
        }
    }
}
