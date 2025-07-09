package javaprojects.musictagger;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
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

        FXMLLoader fxmlLoader = new FXMLLoader(Application.class.getResource("metadata_written_screen.fxml"));
        Stage successStage = fxmlLoader.load();
        SuccessController successController = fxmlLoader.getController();
        successController.SetMP3Data(mp3Data);
        successController.thisStage = successStage;
        successController.SearchScreenStage = stage;
        successController.mainController = mainController;
        successStage.show();
    }

    public void OnDownload() throws URISyntaxException, IOException, InterruptedException, CannotWriteException, CannotReadException, TagException, InvalidAudioFrameException, ReadOnlyFileException {
        DownloadThread downloadThread = new DownloadThread(stage, mp3Data, GetSelectedFile());
        downloadThread.start();

//        URI progressLink = GetProgressLink();
//
//        HttpClient httpClient = HttpClient.newHttpClient();
//
//        HttpRequest request = HttpRequest.newBuilder()
//                .GET()
//                .uri(progressLink)
//                .build();
//
//        HttpResponse response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
//
//        String completedEventString = response.body().toString().substring(response.body().toString().indexOf("event:completed") + 15).replace("data:", "");
//
//        JSONObject eventObject = new JSONObject(completedEventString);
//
//        URI downloadURI = new URI(eventObject.getString("download_url"));
//
//
//        try (InputStream inputStream = downloadURI.toURL().openStream()) {
//            byte[] byteArray = inputStream.readAllBytes();
//
//            try (FileOutputStream fileOutputStream = new FileOutputStream(selectedFile)) {
//                fileOutputStream.write(byteArray);
//            }
//        }
//
//        WipeMP3(selectedFile);
//        ApplyMP3Data(mp3Data, selectedFile);
    }

    File GetSelectedFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialFileName(mp3Data.trackName);
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("MP3 Files", "*.mp3"));
        return fileChooser.showSaveDialog(stage);
    }
//
//    URI GetProgressLink() throws URISyntaxException, IOException, InterruptedException {
//        String id = GetVideoID();
//
//        URI uri = new URI("https", null, "youtube-mp3-2025.p.rapidapi.com", 443, "/v1/social/youtube/audio", "id=" + id + "&quality=128kbps", null);
//        HttpClient httpClient = HttpClient.newHttpClient();
//
//        HttpRequest request = HttpRequest.newBuilder()
//                .GET()
//                .header("x-rapidapi-key", mp3DownloaderApiKey)
//                .uri(uri)
//                .build();
//
//        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
//
//        JSONObject jsonResponse = new JSONObject(response.body());
//
//        URI linkDownloadProgress = new URI(jsonResponse.getString("linkDownloadProgress"));
//
//
//        return linkDownloadProgress;
//    }
//
//    String GetVideoID() throws URISyntaxException, IOException, InterruptedException {
//        HttpClient httpClient = HttpClient.newHttpClient();
//
//        String query = "key=" + googleApiKey + "&" + "part=snippet&maxResults=1&type=video&q=" + mp3Data.trackName + " " + mp3Data.artistName + " lyrics";
//
//        URI uri = new URI("https", null, "www.googleapis.com", 443, "/youtube/v3/search", query, null);
//
//        HttpRequest request = HttpRequest.newBuilder()
//                .GET()
//                .uri(uri)
////                .header("Authorization", "Bearer " + googleApiKey)
//                .build();
//
//        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
//
//        JSONObject jsonResponse = new JSONObject(response.body());
//
//        JSONObject firstItem = jsonResponse.getJSONArray("items").getJSONObject(0);
//        JSONObject idObject = firstItem.getJSONObject("id");
//        String id = idObject.getString("videoId");
//        return id;
//    }
}
