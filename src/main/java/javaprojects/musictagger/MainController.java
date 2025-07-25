package javaprojects.musictagger;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class MainController {
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);
    // Text Fields
    @FXML
    public TextField songNameTextField;

    @FXML
    public TextField artistNameTextField;
    // Buttons
    @FXML
    public Button searchButton;

    @FXML
    public Button clearButton;

    // Text
    @FXML
    public Text songRequiredText;

    @FXML
    public Text artistRequiredText;

    @FXML
    public Text remainingDownloadApiRequestsText;

    public Stage mainStage;

    int remainingDownloadRequests;

    public void onInitiate() {
        SetRemainingDownloadApiRequests(Application.getSettings().getInt("remainingDownloadRequests"));
    }

    public void OnClearButton() {
        logger.info("clear button pressed");
        // set requiredTexts to invisible
        songRequiredText.setVisible(false);
        artistRequiredText.setVisible(false);

        // empty the textFields
        songNameTextField.clear();
        artistNameTextField.clear();
    }

    public void OnSearchButton() throws IOException {
        // show the requiredTexts and do nothing if the textFields are empty
        boolean songTextEmpty = songNameTextField.getText().isBlank();
        boolean artistTextEmpty = artistNameTextField.getText().isBlank();

        if (songTextEmpty && artistTextEmpty) {
            logger.info("both song and artist are empty, showing the requiredTexts");
            songRequiredText.setVisible(true);
            artistRequiredText.setVisible(true);
            return;
        }

        logger.debug("creating search_list");

        FXMLLoader fxmlLoader = new FXMLLoader(Application.class.getResource("search_list.fxml"));
        Stage stage = fxmlLoader.load();
        SearchListController controller = fxmlLoader.getController();
        stage.show();
        controller.stage = stage;
        controller.OnInitialize(songNameTextField.getText(), artistNameTextField.getText(), 1);
    }

    public void OnEnterKeyPressedInTextField(KeyEvent event) throws IOException {
        if (event.getCode() == KeyCode.ENTER) {
            logger.info("enter key pressed, starting search");
            OnSearchButton();
        }
    }

    public void OnViewList() throws IOException {
        logger.info("view list button pressed, creating new song_list");
        FXMLLoader fxmlLoader = new FXMLLoader(Application.class.getResource("song_list.fxml"));
        Stage stage = fxmlLoader.load();
        SongListController songListController = fxmlLoader.getController();
        songListController.stage = stage;
        songListController.onInitialize();
        stage.show();
    }

    public void OnSettings() throws IOException {
        logger.info("view settings button pressed, creating new settings");
        FXMLLoader fxmlLoader = new FXMLLoader(Application.class.getResource("settings.fxml"));
        Stage stage = fxmlLoader.load();
        SettingsController settingsController = fxmlLoader.getController();
        settingsController.stage = stage;
        settingsController.onLoad();
        stage.show();
    }

    public void SetRemainingDownloadApiRequests(int requests) {
        logger.info("setting remaining DownloadApiRequests to {}", requests);
        remainingDownloadApiRequestsText.setText("Remaining Download API Requests: " + requests);
        remainingDownloadRequests = requests;
    }

    public int GetRemainingDownloadApiRequests() {
        return remainingDownloadRequests;
    }
}
