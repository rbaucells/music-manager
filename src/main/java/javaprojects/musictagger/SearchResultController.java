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
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.TagException;

import java.io.*;
import java.net.URISyntaxException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SearchResultController {
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

    public MainController mainController;

    MP3Data mp3Data;

    public Stage stage;

    public void Initialize(MP3Data data) throws IOException {
        mp3Data = data;

        SongLabel.setText(data.trackName.strip());
        String artistAlbumYearString = data.artistName.strip() + " | " + data.albumName.strip() + " | " + data.recordingYear.strip();
        ArtistAlbumYearText.setText(artistAlbumYearString);
        AlbumCoverImageView.setImage(new Image(new ByteArrayInputStream(data.image)));

        // if we are already in the list
        if (Application.DoesListJSONContain(mp3Data)) {
            AddToListButton.setVisible(false);
            RemoveFromListButton.setVisible(true);
        }
    }

    public void OnChoose() throws CannotWriteException, CannotReadException, TagException, InvalidAudioFrameException, ReadOnlyFileException, IOException {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("MP3 Files", "*.mp3"));
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile == null) {
            return;
        }

        Application.ApplyMP3Data(mp3Data, selectedFile);

        FXMLLoader fxmlLoader = new FXMLLoader(Application.class.getResource("success.fxml"));
        Stage successStage = fxmlLoader.load();
        SuccessController successController = fxmlLoader.getController();
        successController.SetMP3Data(mp3Data);
        successController.thisStage = successStage;
        successStage.show();
    }

    public void OnDownload() throws URISyntaxException, IOException, InterruptedException, CannotWriteException, CannotReadException, TagException, InvalidAudioFrameException, ReadOnlyFileException {
        FXMLLoader fxmlLoader = new FXMLLoader(Application.class.getResource("progress_bar.fxml"));
        Stage progressStage = fxmlLoader.load();
        final ProgressBarController progressBarController = fxmlLoader.getController();
        progressBarController.stage = progressStage;

        DownloadRunnable downloadRunnable = new DownloadRunnable(stage, mp3Data, GetSelectedFile(), (message, percent) -> Platform.runLater(() -> progressBarController.SetProgressBar(message, percent)));

        var task = Application.threadPoolExecutor.submit((downloadRunnable));

        progressBarController.onCancelInterface = () -> {
            task.cancel(true);
            progressBarController.close();
        };
    }

    File GetSelectedFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialFileName(mp3Data.trackName);
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("MP3 Files", "*.mp3"));
        return fileChooser.showSaveDialog(stage);
    }

    public void OnAddToList() throws IOException {
        AddToListButton.setVisible(false);
        RemoveFromListButton.setVisible(true);

        Application.WriteMP3DataToListJSON(mp3Data);
    }

    public void OnRemoveFromList() throws IOException {
        AddToListButton.setVisible(true);
        RemoveFromListButton.setVisible(false);

        Application.DeleteMP3DataFromListJSON(mp3Data);
    }
}
