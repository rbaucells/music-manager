package rbaucells.MusicManager;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import org.json.JSONObject;
import org.slf4j.*;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.util.function.BiConsumer;

public class DownloadRunnable implements Runnable {
    final String googleApiKey = "AIzaSyCKrAJ9TgPNJptLe0zj7_20dH-waLXLFZA";
    final String mp3DownloaderApiKey = Application.getSettings().getString("apiKey");

    MP3Data mp3Data;
    File selectedFile;
    BiConsumer<String, Integer> progressBarConsumer;

    private final Logger logger = LoggerFactory.getLogger(DownloadRunnable.class);

    public DownloadRunnable(MP3Data mp3data, File selectedFile, BiConsumer<String, Integer> progressBarConsumer) {
        logger.info("DownloadRunnable created with MP3Data {} and selectedFile {}", mp3data, selectedFile);
        this.mp3Data = mp3data;
        this.selectedFile = selectedFile;
        this.progressBarConsumer = progressBarConsumer;
    }

    @Override
    public void run() {
        logger.info("DownloadRunnable started");

        if (selectedFile == null) {
            logger.warn("selected file was null, returning");
            return;
        }

        try {
            URI progressLink = startDownloadAndGetProgressLink();

            JSONObject eventObject = waitForConversion(progressLink);
            progressBarConsumer.accept("Conversion Done, Initiating Download", 70);
            downloadFile(eventObject, selectedFile);

            Application.wipeMP3(selectedFile);
            progressBarConsumer.accept("MP3 Formatted", 97);
            Application.applyMP3Data(mp3Data, selectedFile);
            progressBarConsumer.accept("MP3 Metadata Applied", 100);
            createSuccessScreen();
        }
        catch (ExitException e) {
            logger.error("got ExitException", e);
            Platform.runLater(() -> Application.newError(e.getMessage(), () -> {
                progressBarConsumer.accept("exit", -1);
                FXMLLoader fxmlLoader = new FXMLLoader(Application.class.getResource("progress_bar.fxml"));
                Stage progressStage = fxmlLoader.load();
                final ProgressBarController progressBarController = fxmlLoader.getController();
                progressBarController.stage = progressStage;

                DownloadRunnable downloadRunnable = new DownloadRunnable(mp3Data, selectedFile, (message, percent) -> Platform.runLater(() -> progressBarController.SetProgressBar(message, percent)));

                var task = Application.threadPoolExecutor.submit((downloadRunnable));

                progressBarController.onCancelInterface = () -> {
                    task.cancel(true);
                    progressBarController.close();
                };
            }));
        }
        catch (Exception e) {
            logger.error("error in DownloadRunnable", e);
        }
    }

    void createSuccessScreen() {
        Platform.runLater(() -> {
            try {
                FXMLLoader fxmlLoader = new FXMLLoader(Application.class.getResource("success.fxml"));
                Stage successStage = fxmlLoader.load();
                SuccessController successController = fxmlLoader.getController();
                successController.SetMP3Data(mp3Data);
                successController.thisStage = successStage;
                successStage.show();
            } catch (Exception e) {
                logger.error("error creating successScreen", e);
            }
        });
    }

    void downloadFile(JSONObject eventObject, File selectedFile) throws URISyntaxException {
        URI downloadURI = new URI(eventObject.getString("download_url"));
        logger.info("starting download of file located at {} to selectedFile {}", downloadURI, selectedFile);
        progressBarConsumer.accept("Reading File", 75);
        try (InputStream inputStream = downloadURI.toURL().openStream()) {
            logger.debug("reading all bytes from downloadURI and saving it to the selectedfile");
            byte[] byteArray = inputStream.readAllBytes();
            progressBarConsumer.accept("Writing File", 85);
            try (FileOutputStream fileOutputStream = new FileOutputStream(selectedFile)) {
                fileOutputStream.write(byteArray);
                logger.info("file successfully download from {} to {}", downloadURI, selectedFile);
            }
            catch (Exception e){
                logger.error("Error writing downloaded data to file", e);
                throw(new ExitException("error writing to file while trying to download file"));
            }
        }
        catch (ExitException exitException) {
            throw(exitException);
        }
        catch (Exception e) {
            logger.error("Error reading from stream from downloadURI", e);
            throw(new ExitException("error reading from file while trying to download file"));
        }
    }

    JSONObject waitForConversion(URI progressLink) {
        try {
            HttpClient httpClient = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .GET()
                    .uri(progressLink)
                    .build();

            logger.info("sending request to wait for conversion to finish");
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            logger.debug("response arrived with statusCode {}", response.statusCode());

            String completedEventString = response.body().substring(response.body().indexOf("event:completed") + 15).replace("data:", "");
            logger.debug("returning jsonObject from completed event found at {}", completedEventString);
            return new JSONObject(completedEventString);
        } catch (Exception e) {
            logger.error("Error sending request to wait for conversion to finish", e);
            throw (new ExitException("error while waiting for Youtube to MP3 2025 API to return download link"));
        }
    }

    URI startDownloadAndGetProgressLink() throws URISyntaxException {
        progressBarConsumer.accept("Getting ID From Youtube-Data API", 5);
        String id = GetVideoID();
        logger.info("starting download on id {}", id);

        progressBarConsumer.accept("Starting Conversion of Audio", 10);
        URI uri = new URI("https", null, "youtube-mp3-2025.p.rapidapi.com", 443, "/v1/social/youtube/audio", "id=" + id + "&quality=128kbps&ext=mp3", null);
        logger.debug("uri for starting audio conversion {}", uri);

        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .GET()
                    .header("x-rapidapi-key", mp3DownloaderApiKey)
                    .uri(uri)
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            logger.debug("response arrived with statusCode {}, getting remainingDownloadApiRequests from response header", response.statusCode());
            final int remainingDownloadApiRequests = Integer.parseInt(response.headers().allValues("X-RateLimit-Requests-Remaining").getFirst());

            logger.debug("telling the mainController how many downloadRequests are left {}", remainingDownloadApiRequests);
            Platform.runLater(() -> Application.mainController.SetRemainingDownloadApiRequests(remainingDownloadApiRequests));

            logger.debug("creating jsonObject from response {}", response);
            JSONObject jsonResponse = new JSONObject(response.body());

            progressBarConsumer.accept("Conversion Started", 15);

            if (!jsonResponse.has("linkDownloadProgress")) {
                logger.error("jsonObject didnt have \"linkDownloadProgress\"");
                throw (new ExitException("error trying to get progress link"));
            }

            URI linkDownloadProgress = new URI(jsonResponse.getString("linkDownloadProgress"));

            logger.debug("returning linkDownloadProgress {}", linkDownloadProgress);
            return linkDownloadProgress;
        }
        catch (ExitException e) {
            throw(e);
        }
        catch (Exception e) {
            logger.error("Error starting conversion and getting progress link", e);
            throw (new ExitException("Error starting conversion and getting progress link"));
        }
    }

    String GetVideoID() {
        logger.info("getting youtube id for MP3Data {}", mp3Data);
        try (HttpClient httpClient = HttpClient.newHttpClient()) {

            String query = "part=snippet&maxResults=1&type=video&q=" + mp3Data.trackName + " " + mp3Data.artistName + " lyrics";

            URI uri = new URI("https", null, "www.googleapis.com", 443, "/youtube/v3/search", query, null);
            logger.debug("created googleapi uri {}, sending request now", uri);
            HttpRequest request = HttpRequest.newBuilder()
                    .GET()
                    .uri(uri)
                    .header("X-goog-api-key", googleApiKey)
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            logger.debug("response gave statusCode {}", response.statusCode());

            if (response.statusCode() == 403) {
                logger.error("quota for Youtube Data API exceeded");
                throw (new ExitException("quota for Youtube Data API exceeded"));
            }

            logger.debug("creating jsonObject from response and getting video id inside (items -> index 0 -> id -> videoId)");
            JSONObject jsonResponse = new JSONObject(response.body());
            JSONObject firstItem = jsonResponse.getJSONArray("items").getJSONObject(0);
            JSONObject idObject = firstItem.getJSONObject("id");
            String id = idObject.getString("videoId");
            logger.debug("returning video id \"{}\"", id);
            return id;
        }
        catch (ExitException exitException) {
            throw (exitException);
        }
        catch (Exception e) {
            logger.error("error getting video id from youtube data api", e);
            throw (new ExitException("error getting video id from Youtube Data API"));
        }
    }
}
