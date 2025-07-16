package javaprojects.musictagger;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class ListResultController {
    // text
    @FXML
    public Text ArtistAlbumYearText;

    // label
    @FXML
    public Label SongLabel;

    // imageView
    @FXML
    public ImageView AlbumCoverImageView;

    public SongListController songListController;
    public MP3Data data;

    public void setMP3Data(MP3Data data) {
        this.data = data;

        SongLabel.setText(data.trackName.strip());
        String artistAlbumYearString = data.artistName.strip() + " | " + data.albumName.strip() + " | " + data.recordingYear.strip();
        ArtistAlbumYearText.setText(artistAlbumYearString);
        AlbumCoverImageView.setImage(new Image(new ByteArrayInputStream(data.image)));
    }

    public void onRemoveFromList() throws IOException {
        Application.deleteMP3DataFromListJSON(data);
        songListController.refreshList();
    }
}
