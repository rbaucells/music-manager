package javaprojects.musictagger;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class ListResultController {
    public MainController mainController;
    public SongListController songListController;
    public MP3Data data;

    @FXML
    public Text ArtistAlbumYearText;

    @FXML
    public Label SongLabel;

    @FXML
    public ImageView AlbumCoverImageView;

    public Stage stage;

    public void OnRemoveFromList() throws IOException {
        mainController.RemoveFromList(data);
        songListController.RefreshList();
    }

    public void SetMP3Data(MP3Data data) {
        this.data = data;

        SongLabel.setText(data.trackName.strip());
        String artistAlbumYearString = data.artistName.strip() + " | " + data.albumName.strip() + " | " + data.recordingYear.strip();
        ArtistAlbumYearText.setText(artistAlbumYearString);
        AlbumCoverImageView.setImage(new Image(new ByteArrayInputStream(data.image)));
    }
}
