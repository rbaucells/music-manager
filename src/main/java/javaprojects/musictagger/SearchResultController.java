package javaprojects.musictagger;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.id3.ID3v1Tag;
import org.jaudiotagger.tag.images.StandardArtwork;
import org.json.JSONObject;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

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

    public void SetMP3Data(MP3Data data) {
        mp3Data = data;

        SongLabel.setText(data.trackName.strip());
        String artistAlbumYearString = data.artistName.strip() + " | " + data.albumName.strip() + " | " + data.recordingYear.strip();
        ArtistAlbumYearText.setText(artistAlbumYearString);
        AlbumCoverImageView.setImage(new Image(new ByteArrayInputStream(data.image)));
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
        DownloadThread downloadThread = new DownloadThread(stage, mp3Data, GetSelectedFile());
        downloadThread.start();
    }

    File GetSelectedFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialFileName(mp3Data.trackName);
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("MP3 Files", "*.mp3"));
        return fileChooser.showSaveDialog(stage);
    }

    public void OnAddToList() {
        AddToListButton.setVisible(false);
        RemoveFromListButton.setVisible(true);

        mainController.AddToList(mp3Data);
    }

    public void OnRemoveFromList() {
        AddToListButton.setVisible(true);
        RemoveFromListButton.setVisible(false);

        mainController.RemoveFromList(mp3Data);
    }
}
