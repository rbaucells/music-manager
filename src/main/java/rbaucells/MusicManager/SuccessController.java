package rbaucells.MusicManager;

import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;

public class SuccessController {
    @FXML
    public Text Song;

    @FXML
    public Text Artist;

    @FXML
    public Text Album;

    @FXML
    public Text Year;

    @FXML
    public ImageView imageView;

    public Stage thisStage;

    public void SetMP3Data(MP3Data data) {
        Song.setText(data.trackName);
        Artist.setText(data.artistName);
        Album.setText(data.albumName);
        Year.setText(data.recordingYear);
        imageView.setImage(new Image(new ByteArrayInputStream(data.image)));
    }

    public void OnClose() {
        thisStage.close();
    }
}
