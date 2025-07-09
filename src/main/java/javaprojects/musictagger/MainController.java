package javaprojects.musictagger;

import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

public class MainController {
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

    public Stage mainStage;

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

        FXMLLoader fxmlLoader = new FXMLLoader(Application.class.getResource("search_screen.fxml"));
        Stage stage = fxmlLoader.load();
        SearchScreenController controller = fxmlLoader.getController();
        stage.show();
        controller.mainController = this;
        controller.stage = stage;
        controller.OnInitialize(songNameTextField.getText(), artistNameTextField.getText());
    }

    public void OnEnterKeyPressedInArtistTextField(KeyEvent event) throws IOException, URISyntaxException, InterruptedException {
        if (event.getCode() == KeyCode.ENTER) {
            OnSearchButton();
        }
    }
}
