package javaprojects.musictagger;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class DownloadThread extends Thread {

    ProgressBarController progressBarController;
    Stage stage;
    MP3Data mp3Data;

    String googleApiKey = "AIzaSyCKrAJ9TgPNJptLe0zj7_20dH-waLXLFZA";
    String mp3DownloaderApiKey = "a5dc7a14fdmsh5a9b1cb95974c3cp141925jsnc22815ca997f";

    File selectedFile;

    public DownloadThread(Stage mainStage, MP3Data mp3data, File selectedFile) {
        stage = mainStage;
        this.mp3Data = mp3data;
        this.selectedFile = selectedFile;
    }

    void startLoadingBar() {
        Platform.runLater(() -> {
            try {
                FXMLLoader fxmlLoader = new FXMLLoader(Application.class.getResource("progress_bar.fxml"));
                Stage loadingStage = fxmlLoader.load();
                progressBarController = fxmlLoader.getController();
                progressBarController.myStage = loadingStage;
                progressBarController.downloadThread = this;
                loadingStage.show();
            }
            catch (Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        });
    }

    @Override
    public void run() {
        if (selectedFile == null) {
            return;
        }

        try {
            startLoadingBar();
            URI progressLink = GetProgressLink();

            JSONObject eventObject = WaitForConversion(progressLink);
            SetProgressBar("Conversion Done, Initiating Download", 70);
            downloadFile(eventObject, selectedFile);

            Application.WipeMP3(selectedFile);
            SetProgressBar("MP3 Formatted", 97);
            Application.ApplyMP3Data(mp3Data, selectedFile);
            SetProgressBar("MP3 Metadata Applied", 100);
            CloseProgressBar();
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    void CloseProgressBar() {
        Platform.runLater(() -> {
            progressBarController.myStage.close();
        });
    }

    void downloadFile(JSONObject eventObject, File selectedFile) throws URISyntaxException, IOException {
        URI downloadURI = new URI(eventObject.getString("download_url"));

        SetProgressBar("Reading File", 75);
        try (InputStream inputStream = downloadURI.toURL().openStream()) {
            byte[] byteArray = inputStream.readAllBytes();
            SetProgressBar("Writing File", 85);
            try (FileOutputStream fileOutputStream = new FileOutputStream(selectedFile)) {
                fileOutputStream.write(byteArray);
            }
        }
    }

    JSONObject WaitForConversion(URI progressLink) throws IOException, InterruptedException {
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(progressLink)
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        String completedEventString = response.body().substring(response.body().indexOf("event:completed") + 15).replace("data:", "");
        return new JSONObject(completedEventString);
    }

    void SetProgressBar(String message, int percent) {
        Platform.runLater(() -> {
            progressBarController.PercentText.setText("%" + percent);
            progressBarController.ProgressBar.setProgress((double) percent /100);
            progressBarController.InfoLabel.setText(message);
        });
    }

    URI GetProgressLink() throws URISyntaxException, IOException, InterruptedException {
        SetProgressBar("Getting ID From Youtube-Data API", 5);
        String id = GetVideoID();

        SetProgressBar("Initiating Conversion of Audio", 10);
        URI uri = new URI("https", null, "youtube-mp3-2025.p.rapidapi.com", 443, "/v1/social/youtube/audio", "id=" + id + "&quality=128kbps", null);

        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .GET()
                    .header("x-rapidapi-key", mp3DownloaderApiKey)
                    .uri(uri)
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            JSONObject jsonResponse = new JSONObject(response.body());


            if (jsonResponse.getBoolean("error") || jsonResponse.has("status")) {
                SetProgressBar("Error Starting Conversion, Auto Trying again", 15);
                CloseProgressBar();
                start();
            }

            SetProgressBar("Conversion Started", 15);
            return new URI(jsonResponse.getString("linkDownloadProgress"));
        }
    }

    String GetVideoID() throws URISyntaxException, IOException, InterruptedException {
        HttpResponse<String> response;
        try (HttpClient httpClient = HttpClient.newHttpClient()) {

            String query = "key=" + googleApiKey + "&" + "part=snippet&maxResults=1&type=video&q=" + mp3Data.trackName + " " + mp3Data.artistName + " lyrics";

            URI uri = new URI("https", null, "www.googleapis.com", 443, "/youtube/v3/search", query, null);

            HttpRequest request = HttpRequest.newBuilder()
                    .GET()
                    .uri(uri)
                    //                .header("Authorization", "Bearer " + googleApiKey)
                    .build();

            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        }

        JSONObject jsonResponse = new JSONObject(response.body());

        JSONObject firstItem = jsonResponse.getJSONArray("items").getJSONObject(0);
        JSONObject idObject = firstItem.getJSONObject("id");
        return idObject.getString("videoId");
    }
}
