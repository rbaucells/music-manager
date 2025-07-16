package javaprojects.musictagger;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public class SearchResultController {
    private static final Logger logger = LoggerFactory.getLogger(SearchResultController.class);
    // Texts
    @FXML
    public Text ArtistAlbumYearText;

    // Labels
    @FXML
    public Label SongLabel;

    // Image Views
    @FXML
    public ImageView AlbumCoverImageView;

    // Buttons
    @FXML
    public Button AddToListButton;
    @FXML
    public Button RemoveFromListButton;

    MP3Data mp3Data;
    public Stage stage;

    public void initialize(MP3Data data) {
        mp3Data = data;

        SongLabel.setText(data.trackName.strip());
        String artistAlbumYearString = data.artistName.strip() + " | " + data.albumName.strip() + " | " + data.recordingYear.strip();
        ArtistAlbumYearText.setText(artistAlbumYearString);
        AlbumCoverImageView.setImage(new Image(new ByteArrayInputStream(data.image)));

        logger.debug("checking if the assigned mp3Data is already in the listJSONFile");
        // if we are already in the list
        if (Application.doesListJSONContain(mp3Data) != 0) {
            logger.debug("MP3Data is already in the listJSONFile, setting button visibility");
            AddToListButton.setVisible(false);
            RemoveFromListButton.setVisible(true);
        }
    }

    public void onWriteToFile() throws IOException {
        logger.info("search result chosen to write MP3Data to file. prompting user for destination file");
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("MP3 Files", "*.mp3"));
        File selectedFile = fileChooser.showOpenDialog(stage);

        logger.debug("selected file is {}", selectedFile);

        if (selectedFile == null) {
            return;
        }

        logger.debug("sending MP3Data to be applied");

        Application.applyMP3Data(mp3Data, selectedFile);

        logger.debug("loading up success");
        FXMLLoader fxmlLoader = new FXMLLoader(Application.class.getResource("success.fxml"));
        Stage successStage = fxmlLoader.load();
        SuccessController successController = fxmlLoader.getController();
        successController.SetMP3Data(mp3Data);
        successController.thisStage = successStage;
        successStage.show();
    }

    public void onDownload() throws IOException {
        logger.info("search result chosen to download");
        logger.debug("creating progressBar");
        FXMLLoader fxmlLoader = new FXMLLoader(Application.class.getResource("progress_bar.fxml"));
        Stage progressStage = fxmlLoader.load();
        final ProgressBarController progressBarController = fxmlLoader.getController();
        progressBarController.stage = progressStage;

        File selectedFile = getSelectedFile();
        logger.debug("creating new DownloadRunnable with MP3Data {} and file {}", mp3Data, selectedFile);
        DownloadRunnable downloadRunnable = new DownloadRunnable(mp3Data, selectedFile, (message, percent) -> Platform.runLater(() -> progressBarController.SetProgressBar(message, percent)));

        logger.debug("assigning DownloadRunnable to threadPoolExecutor");
        var task = Application.threadPoolExecutor.submit((downloadRunnable));

        logger.debug("defining cancelInterface for progressBarController");
        progressBarController.onCancelInterface = () -> {
            task.cancel(true);
            progressBarController.close();
        };
    }

    File getSelectedFile() {
        logger.info("prompting user for where to save downloadedFile");
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialFileName(mp3Data.trackName);
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("MP3 Files", "*.mp3"));
        File selectedFile = fileChooser.showSaveDialog(stage);
        logger.debug("user chose file {}", selectedFile);
        return selectedFile;
    }

    public void onAddToList() {
       logger.info("adding search result to list and setting button visibility");
        AddToListButton.setVisible(false);
        RemoveFromListButton.setVisible(true);

        Application.writeMP3DataToListJSON(mp3Data);
    }

    public void onRemoveFromList() {
        logger.info("removing search result to list and setting button visibility");
        AddToListButton.setVisible(true);
        RemoveFromListButton.setVisible(false);

        Application.deleteMP3DataFromListJSON(mp3Data);
    }
}
