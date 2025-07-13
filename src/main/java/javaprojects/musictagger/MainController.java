package javaprojects.musictagger;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

public class MainController {
    // Text Fields
    @FXML
    public TextField songNameTextField;

    @FXML
    public TextField artistNameTextField;

    @FXML
    public TextField apiKeyTextField;

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

    @FXML
    public Text apiKeyRequiredText;

    public Stage mainStage;

    int remainingDownloadRequests;

    public void OnIntiate() throws IOException {
        SetRemainingDownloadApiRequests(Application.GetSettings().getInt("remainingDownloadRequests"));
    }

    public void OnClearButton() {
        // set requiredTexts to invisible
        songRequiredText.setVisible(false);
        artistRequiredText.setVisible(false);

        // empty the textFields
        songNameTextField.clear();
        artistNameTextField.clear();
    }

    public void OnSearchButton() throws IOException, URISyntaxException, InterruptedException {
        // show the requiredTexts and do nothing if the textFields are empty
        boolean songTextEmpty = songNameTextField.getText().isBlank();
        boolean artistTextEmpty = artistNameTextField.getText().isBlank();

        if (songTextEmpty && artistTextEmpty) {
            songRequiredText.setVisible(true);
            artistRequiredText.setVisible(true);
            return;
        }

        FXMLLoader fxmlLoader = new FXMLLoader(Application.class.getResource("search_list.fxml"));
        Stage stage = fxmlLoader.load();
        SearchListController controller = fxmlLoader.getController();
        stage.show();
        controller.mainController = this;
        controller.stage = stage;
        controller.OnInitialize(songNameTextField.getText(), artistNameTextField.getText(), 1);
    }

    public void OnEnterKeyPressedInTextField(KeyEvent event) throws IOException, URISyntaxException, InterruptedException {
        if (event.getCode() == KeyCode.ENTER) {
            OnSearchButton();
        }
    }

    public void OnViewList() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Application.class.getResource("song_list.fxml"));
        Stage stage = fxmlLoader.load();
        SongListController songListController = fxmlLoader.getController();
        songListController.mainController = this;
        songListController.stage = stage;
        songListController.OnInitialize();
        stage.show();
    }

    public void OnSettings() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Application.class.getResource("settings.fxml"));
        Stage stage = fxmlLoader.load();
        SettingsController settingsController = fxmlLoader.getController();
        settingsController.stage = stage;
        settingsController.OnLoad();
        stage.show();
    }

    public void SetRemainingDownloadApiRequests(int requests) {
        remainingDownloadApiRequestsText.setText("Remaining Download API Requests: " + requests);
        remainingDownloadRequests = requests;
    }

    public int GetRemainingDownloadApiRequests() {
        return remainingDownloadRequests;
    }
}
