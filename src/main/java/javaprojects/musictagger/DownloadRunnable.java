package javaprojects.musictagger;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

public class DownloadRunnable implements Runnable {
    Stage stage;
    MP3Data mp3Data;

    String googleApiKey = "AIzaSyCKrAJ9TgPNJptLe0zj7_20dH-waLXLFZA";
    String mp3DownloaderApiKey = Application.GetSettings().getString("apiKey");
    File selectedFile;

    BiConsumer<String, Integer> progressConsumer;

    public DownloadRunnable(Stage mainStage, MP3Data mp3data, File selectedFile, BiConsumer<String, Integer> progressConsumer) throws IOException {
        stage = mainStage;
        this.mp3Data = mp3data;
        this.selectedFile = selectedFile;
        this.progressConsumer = progressConsumer;
    }

    @Override
    public void run() {
        if (selectedFile == null) {
            return;
        }

        try {
            URI progressLink = GetProgressLink();

            JSONObject eventObject = WaitForConversion(progressLink);
            progressConsumer.accept("Conversion Done, Initiating Download", 70);
            downloadFile(eventObject, selectedFile);

            Application.WipeMP3(selectedFile);
            progressConsumer.accept("MP3 Formatted", 97);
            Application.ApplyMP3Data(mp3Data, selectedFile);
            progressConsumer.accept("MP3 Metadata Applied", 100);
            CreateSuccessScreen();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    void CreateSuccessScreen() {
        Platform.runLater(() -> {
            try {
                FXMLLoader fxmlLoader = new FXMLLoader(Application.class.getResource("success.fxml"));
                Stage successStage = fxmlLoader.load();
                SuccessController successController = fxmlLoader.getController();
                successController.SetMP3Data(mp3Data);
                successController.thisStage = successStage;
                successStage.show();
            } catch (Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        });
    }

    void downloadFile(JSONObject eventObject, File selectedFile) throws URISyntaxException, IOException {
        URI downloadURI = new URI(eventObject.getString("download_url"));

        progressConsumer.accept("Reading File", 75);
        try (InputStream inputStream = downloadURI.toURL().openStream()) {
            byte[] byteArray = inputStream.readAllBytes();
            progressConsumer.accept("Writing File", 85);
            try (FileOutputStream fileOutputStream = new FileOutputStream(selectedFile)) {
                fileOutputStream.write(byteArray);
            }
            catch (Exception e){
                throw(e);
            }
        } catch (Exception e) {
            Platform.runLater(() -> {
                try {
                    Application.NewError("Error Saving MP3 to File", () -> {
                        progressConsumer.accept("exit", -1);
                        FXMLLoader fxmlLoader = new FXMLLoader(Application.class.getResource("progress_bar.fxml"));
                        Stage progressStage = fxmlLoader.load();
                        final ProgressBarController progressBarController = fxmlLoader.getController();
                        progressBarController.stage = progressStage;

                        DownloadRunnable downloadRunnable = new DownloadRunnable(stage, mp3Data, selectedFile, (message, percent) -> Platform.runLater(() -> progressBarController.SetProgressBar(message, percent)));

                        var task = Application.threadPoolExecutor.submit((downloadRunnable));

                        progressBarController.onCancelInterface = () -> {
                            task.cancel(true);
                            progressBarController.close();
                        };
                    });
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
            });
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

    URI GetProgressLink() throws URISyntaxException, IOException, InterruptedException {
        progressConsumer.accept("Getting ID From Youtube-Data API", 5);
        String id = GetVideoID();

        progressConsumer.accept("Initiating Conversion of Audio", 10);
        URI uri = new URI("https", null, "youtube-mp3-2025.p.rapidapi.com", 443, "/v1/social/youtube/audio", "id=" + id + "&quality=128kbps", null);

        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .GET()
                    .header("x-rapidapi-key", mp3DownloaderApiKey)
                    .uri(uri)
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            final int remainingDownloadApiRequests = Integer.parseInt(response.headers().allValues("X-RateLimit-Requests-Remaining").getFirst());

            // Let the main controller know how many DownloadApiRequests are remaining
            Platform.runLater(() -> {
                Application.mainController.SetRemainingDownloadApiRequests(remainingDownloadApiRequests);
            });

            JSONObject jsonResponse = new JSONObject(response.body());

            progressConsumer.accept("Conversion Started", 15);

            if (!jsonResponse.has("linkDownloadProgress")) {
                Platform.runLater(() -> {
                    try {
                        Application.NewError("Youtube to MP3 2025 API Failed", () -> {
                            progressConsumer.accept("exit", -1);
                            stage.close();
                            FXMLLoader fxmlLoader = new FXMLLoader(Application.class.getResource("progress_bar.fxml"));
                            Stage progressStage = fxmlLoader.load();
                            final ProgressBarController progressBarController = fxmlLoader.getController();
                            progressBarController.stage = progressStage;

                            DownloadRunnable downloadRunnable = new DownloadRunnable(stage, mp3Data, selectedFile, (message, percent) -> Platform.runLater(() -> progressBarController.SetProgressBar(message, percent)));

                            var task = Application.threadPoolExecutor.submit((downloadRunnable));

                            progressBarController.onCancelInterface = () -> {
                                task.cancel(true);
                                progressBarController.close();
                            };
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }

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
                    .build();

            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        }

        JSONObject jsonResponse = new JSONObject(response.body());

        if (response.statusCode() == 403) {
            Platform.runLater(() -> {
                try {
                    Application.NewError("Quota Exceeded for Youtube Data API for the Day/Minute", () -> {
                        progressConsumer.accept("exit", -1);
                        FXMLLoader fxmlLoader = new FXMLLoader(Application.class.getResource("progress_bar.fxml"));
                        Stage progressStage = fxmlLoader.load();
                        final ProgressBarController progressBarController = fxmlLoader.getController();
                        progressBarController.stage = progressStage;

                        DownloadRunnable downloadRunnable = new DownloadRunnable(stage, mp3Data, selectedFile, (message, percent) -> Platform.runLater(() -> progressBarController.SetProgressBar(message, percent)));

                        var task = Application.threadPoolExecutor.submit((downloadRunnable));

                        progressBarController.onCancelInterface = () -> {
                            task.cancel(true);
                            progressBarController.close();
                        };
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }

        JSONObject firstItem = jsonResponse.getJSONArray("items").getJSONObject(0);
        JSONObject idObject = firstItem.getJSONObject("id");
        return idObject.getString("videoId");
    }
}
