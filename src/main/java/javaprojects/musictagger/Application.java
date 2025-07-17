package javaprojects.musictagger;

import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.id3.ID3v1Tag;
import org.jaudiotagger.tag.images.StandardArtwork;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Application extends javafx.application.Application {

    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    static Path configurationPath = Paths.get(System.getenv("HOME"), ".musictagger");

    static Path listJSONFilePath = configurationPath.resolve("listjson.json");
    static File listJSONFile = listJSONFilePath.toFile();

    static Path settingsJSONFilePath = configurationPath.resolve("settings.json");
    static File settingsJSONFile = settingsJSONFilePath.toFile();

    static ThreadPoolExecutor threadPoolExecutor;

    // Controllers
    public static MainController mainController;


    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Application.class.getResource("main.fxml"));
        Stage myStage = fxmlLoader.load();
        myStage.show();
        mainController = fxmlLoader.getController();
        mainController.onInitiate();
        mainController.mainStage = myStage;
    }

    @Override
    public void stop() {
        threadPoolExecutor.close();
        JSONObject jsonObject = getSettings();
        saveSettings(jsonObject.getInt("numberOfBatchDownloads"), jsonObject.getInt("numberOfSearchResults"), jsonObject.getString("apiKey"), mainController.GetRemainingDownloadApiRequests());
    }

    public static void main(String[] args) throws IOException {
        // empty line to signify new startup
        logger.info("App Started");

        try (InputStream inputStream = new FileInputStream(settingsJSONFile)) {
            logger.debug("settingsJSONFile located at {}", settingsJSONFile);
            logger.debug("settingsJSONFile contents {}", new String(inputStream.readAllBytes()));
        }

        try (InputStream inputStream = new FileInputStream(listJSONFile)) {
            JSONArray listArray = new JSONArray(new String(inputStream.readAllBytes()));
            logger.debug("listJSONFile located at {}", listJSONFile);
            logger.debug("listJSONFile contents {}", listJSONTostring(listArray));
        }

        logger.info("verifying listJSONFile and settingsJSONFile");

        verifyListJSONFile();
        verifySettingsJSONFile();

        int numberOfBatchDownloads = getSettings().getInt("numberOfBatchDownloads");
        logger.debug("numberOfConcurrentDownloads is {}, creating threadPoolExecutor now", numberOfBatchDownloads);
        threadPoolExecutor = new ThreadPoolExecutor(numberOfBatchDownloads, numberOfBatchDownloads, 5, TimeUnit.MINUTES, new ArrayBlockingQueue<>(numberOfBatchDownloads));

        logger.info("the ui thread has begun");
        launch();
    }

    public static void verifySettingsJSONFile() throws IOException {
        logger.debug("SetUpSettingsJSON()");
        if (!settingsJSONFile.exists()) {
            logger.info("settingsJSONFile doesnt exist, making it now");
            Files.createDirectories(configurationPath);
            Files.createFile(settingsJSONFilePath);

            logger.debug("writing jsonObject with default parameters to settingsJSONFile");
            try (OutputStream outputStream = new FileOutputStream(settingsJSONFile)) {
                JSONObject jsonObject = new JSONObject();

                jsonObject.put("numberOfBatchDownloads", 5);
                jsonObject.put("numberOfSearchResults", 10);
                jsonObject.put("apiKey", "");
                jsonObject.put("remainingDownloadRequests", 300);

                outputStream.write(jsonObject.toString().getBytes());
            } catch (Exception e) {
                logger.error("Error writing jsonObject with default parameters to settingsJSONFile where it didnt exist", e);
                newError("Error While Writing While Setting Up SettingsJSON", Application::verifySettingsJSONFile);
            }
        } else {
            logger.info("settingsJSONFile does exist, verifying it now");
            try (InputStream inputStream = new FileInputStream(settingsJSONFile)) {
                String string = new String(inputStream.readAllBytes());
                if (!string.contains("{")) {
                    logger.debug("settingsJSONFile doeesnt contain a \"{\" (jsonObject), writing a jsonObject with default parameters now");
                    try (OutputStream outputStream = new FileOutputStream(settingsJSONFile)) {
                        JSONObject jsonObject = new JSONObject();

                        jsonObject.put("numberOfBatchDownloads", 5);
                        jsonObject.put("numberOfSearchResults", 10);
                        jsonObject.put("apiKey", "");
                        jsonObject.put("remainingDownloadRequests", 300);

                        outputStream.write(jsonObject.toString().getBytes());
                    } catch (Exception e) {
                        logger.error("Error writing jsonObject with default parameters to settingsJSONFile where it did exist but was empty", e);
                        newError("Error While Writing To SettingsJSON", Application::verifySettingsJSONFile);
                    }
                } else {
                    JSONObject jsonObject = new JSONObject(string);

                    logger.debug("settingsJSONFile does have a jsonObject, verifying it has all parameters now");
                    if (!jsonObject.has("numberOfBatchDownloads")) {
                        logger.debug("settingsJSONFile doesnt have \"numberOfBatchDownloads\", writing default parameter");
                        jsonObject.put("numberOfBatchDownloads", 5);
                    }
                    if (!jsonObject.has("numberOfSearchResults")) {
                        logger.debug("settingsJSONFile doesnt have \"numberOfSearchResults\", writing default parameter");
                        jsonObject.put("numberOfSearchResults", 10);
                    }
                    if (!jsonObject.has("apiKey")) {
                        logger.debug("settingsJSONFile doesnt have \"apiKey\", writing default parameter");
                        jsonObject.put("apiKey", "");
                    }
                    if (!jsonObject.has("remainingDownloadRequests")) {
                        logger.debug("settingsJSONFile doesnt have \"remainingDownloadRequests\", writing default parameter");
                        jsonObject.put("remainingDownloadRequests", 300);
                    }

                    logger.debug("writing verified jsonObject to settingsJSONFile");
                    try (OutputStream outputStream = new FileOutputStream(settingsJSONFile)) {
                        outputStream.write(jsonObject.toString().getBytes());
                    } catch (Exception e) {
                        logger.error("Error writing verified jsonObject to settingsJSONFile", e);
                        newError("Error While Writing To SettingsJSON", Application::verifySettingsJSONFile);
                    }
                }
            } catch (Exception e) {
                logger.error("Error reading settingsJSONFile", e);
                newError("Error While Reading From SettingsJSON", Application::verifySettingsJSONFile);
            }
        }
    }

    public static void verifyListJSONFile() throws IOException {
        logger.debug("SetUpListJSON()");

        if (!listJSONFile.exists()) {
            logger.info("listJSONFile doesnt exist, making it now");
            Files.createDirectories(configurationPath);
            Files.createFile(listJSONFilePath);

            logger.debug("writing empty array to listJSONFile");
            try (OutputStream outputStream = new FileOutputStream(listJSONFile)) {
                outputStream.write("[ ]".getBytes());
            } catch (Exception e) {
                logger.error("Error writing empty array to listJSONFile where it didnt exist", e);
                newError("Error Writing to ListJSON While SettingUpListJSON", Application::verifyListJSONFile);
            }
        } else {
            logger.info("listJSONFile does exist, reading from it now");
            try (InputStream inputStream = new FileInputStream(listJSONFile)) {
                String jsonString = new String(inputStream.readAllBytes());
                if (!jsonString.contains("[")) {
                    logger.debug("listJSONFile doesnt contain a \"[\" (jsonArray), writing an empty JSON array now");
                    try (OutputStream outputStream = new FileOutputStream(listJSONFile)) {
                        outputStream.write("[ ]".getBytes());
                    } catch (Exception e) {
                        logger.error("Error writing empty array to listJSONFile where it did exist", e);
                        newError("Error Writing to ListJSON While SettingUpListJSON", Application::verifyListJSONFile);
                    }
                }
            } catch (Exception e) {
                logger.error("Error reading listJSONFile while it already existed", e);
                newError("Error Reading From ListJSON While SettingUpListJSON", Application::verifyListJSONFile);
            }
        }
    }

    public static void applyMP3Data(MP3Data data, File selectedFile) {
        logger.info("applying MP3Data {} to file {}", data, selectedFile);
        try {
            logger.debug("reading selectedFile and getting fileTag");
            AudioFile musicFile = AudioFileIO.read(selectedFile);
            Tag fileTag = musicFile.getTagOrCreateDefault();

            logger.debug("setting fields of fileTag");
            fileTag.setField(FieldKey.TITLE, data.trackName);
            fileTag.setField(FieldKey.ARTIST, data.artistName);
            fileTag.setField(FieldKey.ALBUM, data.albumName);

            if (!data.recordingYear.isBlank()) {
                logger.debug("recordingYear is not blank, setting field");
                fileTag.setField(FieldKey.YEAR, data.recordingYear);
            } else {
                logger.debug("recordingYear is blank, deleting YEAR field");
                fileTag.deleteField(FieldKey.YEAR);
            }

            logger.debug("applying artwork");
            fileTag.deleteArtworkField();
            StandardArtwork standardArtwork = new StandardArtwork();
            standardArtwork.setBinaryData(data.image);
            fileTag.setField(standardArtwork);

            logger.debug("setting musicFile tag to fileTag, and commiting changes");
            musicFile.setTag(fileTag);
            musicFile.commit();

            File newMusicFile = new File(selectedFile.getParent(), data.trackName + ".mp3");
            logger.debug("renaming {} to {}", selectedFile, newMusicFile);
            selectedFile.renameTo(newMusicFile);
            logger.info("MP3Data applied, and file renamed to {}", newMusicFile);
        } catch (Exception e) {
            logger.error("Error applying metadata to MP3 file", e);
            newError("Error applying metadata to MP3 file", () -> Application.applyMP3Data(data, selectedFile));
        }
    }

    public static void wipeMP3(File selectedFile) {
        logger.info("wiping mp3 {}", selectedFile);
        try {
            logger.debug("reading selectedFile, and making a new empty ID3v1Tag");
            AudioFile musicFile = AudioFileIO.read(selectedFile);
            Tag fileTag = new ID3v1Tag();

            logger.debug("setting musicFile tag and commiting changes");
            musicFile.setTag(fileTag);
            musicFile.commit();
        } catch (Exception e) {
            logger.error("Error wiping mp3 file", e);
            newError("Error wiping mp3 file", () -> Application.wipeMP3(selectedFile));
        }
    }

    public static int doesListJSONContain(MP3Data data) {
        logger.info("checking if listJSONFile contains MP3Data {}", data);
        ArrayList<MP3Data> mp3DataArrayList = readListJSON();
        for (int i = 0; i < mp3DataArrayList.size(); i++) {
            MP3Data mp3Data = mp3DataArrayList.get(i);
            if (Objects.equals(mp3Data.trackName, data.trackName) && Objects.equals(mp3Data.artistName, data.artistName) && Objects.equals(mp3Data.albumName, data.albumName) && Objects.equals(mp3Data.recordingYear, data.recordingYear)) {
                logger.info("MP3Data at index {} from listJSONFile {} and given MP3Data {} are the same", mp3Data, i, data);
                return i;
            }
        }
        logger.info("no corresponding MP3Data was found in listJSONFile");
        return 0;
    }

    public static void writeMP3DataToListJSON(MP3Data data) {
        logger.info("writing MP3Data {} into listJSONFile", data);
        if (readListJSON().contains(data)) {
            logger.info("listJSONFile already contains given MP3Data");
            return;
        }
        logger.debug("making a jsonObject from given MP3Data");
        JSONObject item = new JSONObject()
                .put("trackName", data.trackName)
                .put("artistName", data.artistName)
                .put("albumName", data.albumName)
                .put("recordingYear", data.recordingYear)
                .put("image", Base64.getEncoder().encodeToString(data.image));

        logger.debug("reading from listJSONFile");
        try (InputStream inputStream = new FileInputStream(listJSONFile)) {
            JSONArray jsonArray = new JSONArray(new String(inputStream.readAllBytes()));
            logger.debug("putting jsonObject {} into jsonArray and writing to listJSONFile", listJSONToString(item));
            jsonArray.put(item);
            try (OutputStream outputStream = new FileOutputStream(listJSONFile)) {
                outputStream.write(jsonArray.toString(4).getBytes());
            } catch (Exception e) {
                logger.error("Error writing jsonArray with MP3Data jsonObject in it", e);
                newError("Error writing jsonArray with MP3Data jsonObject in it", () -> Application.writeMP3DataToListJSON(data));
            }
        } catch (Exception e) {
            logger.error("Error reading listJSONFile", e);
            newError("Error reading listJSONFile", () -> Application.writeMP3DataToListJSON(data));
        }
    }

    public static void deleteMP3DataFromListJSON(MP3Data data) {
        logger.info("deleting MP3Data {} from listJSONFile", data);

        int indexOfMP3Data = doesListJSONContain(data);

        if (indexOfMP3Data == 0) {
            logger.info("listJSONFile doesnt contain given MP3Data");
            return;
        }
        logger.debug("reading from listJSONFile");
        try (InputStream inputStream = new FileInputStream(listJSONFile)) {
            logger.debug("creating jsonArray from listJSONFile and removing MP3Data");
            JSONArray jsonArray = new JSONArray(new String(inputStream.readAllBytes()));
            jsonArray.remove(indexOfMP3Data);
            logger.debug("writing jsonArray with MP3Data removed to listJSONFile");
            try (OutputStream outputStream = new FileOutputStream(listJSONFile)) {
                outputStream.write(jsonArray.toString(4).getBytes());
            } catch (Exception e) {
                logger.error("Error while trying to write jsonArray with data removed from it", e);
                newError("Error Writing to ListJSON While Deleting MP3Data", () -> Application.deleteMP3DataFromListJSON(data));
            }
        } catch (Exception e) {
            logger.error("Error trying to read from listJSONFile", e);
            newError("Error Reading from ListJSON While Deleting MP3Data", () -> Application.deleteMP3DataFromListJSON(data));
        }
    }

    public static ArrayList<MP3Data> readListJSON() {
        logger.info("reading listJSONFile");
        ArrayList<MP3Data> datas = new ArrayList<>();

        try (InputStream inputStream = new FileInputStream(listJSONFile)) {
            logger.debug("creating jsonArray from listJSONFile");
            String string = new String(inputStream.readAllBytes());
            JSONArray jsonArray = new JSONArray(string);
            logger.debug("looping through jsonObjects in jsonArray and turning them into MP3Data");
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                MP3Data data = new MP3Data(jsonObject.getString("trackName"), jsonObject.getString("artistName"), jsonObject.getString("albumName"), jsonObject.getString("recordingYear"), Base64.getDecoder().decode(jsonObject.getString("image")));
                datas.add(data);
            }
        } catch (Exception e) {
            newError("Error Reading ListJSONFile", () -> {
            });
            logger.error("Error opening inputStream to read listJSONFile", e);
        }

        return datas;
    }

    public static void clearListJSON() {
        logger.info("clearing listJSONFile, writing empty JSONArray to it now");
        try (OutputStream outputStream = new FileOutputStream(listJSONFile)) {
            outputStream.write(new JSONArray().toString(4).getBytes());
        } catch (Exception e) {
            logger.error("Error writing to ListJSONFile", e);
            newError("Error Writing While Clearing ListJSON", Application::clearListJSON);
        }
    }

    public static void saveSettings(int numberOfBatchDownloads, int numberOfSearchResults, String apiKey, int remainingDownloadRequests) {
        logger.info("saving settings to settingsJSONFile");
        logger.debug("given perameters [numberOfBatchDownloads \"{}\", numberOfSearchResults \"{}\", apiKey \"{}\", remainingDownloadRequests \"{}\"]", numberOfBatchDownloads, numberOfSearchResults, apiKey, remainingDownloadRequests);
        logger.debug("reading from settingsJSONFile");
        try (InputStream inputStream = new FileInputStream(settingsJSONFile)) {
            logger.debug("getting jsonObject from settingsJSONFile and putting parameters");
            String string = new String(inputStream.readAllBytes());
            JSONObject jsonObject = new JSONObject(string);

            jsonObject.put("numberOfBatchDownloads", numberOfBatchDownloads);
            jsonObject.put("numberOfSearchResults", numberOfSearchResults);
            jsonObject.put("apiKey", apiKey);
            jsonObject.put("remainingDownloadRequests", remainingDownloadRequests);

            logger.debug("writing jsonObject to settingsJSONFile");
            try (OutputStream outputStream = new FileOutputStream(settingsJSONFile)) {
                outputStream.write(jsonObject.toString().getBytes());
            } catch (Exception e) {
                logger.error("Error writing to settingsJSONFile", e);
                newError("Error Writing While Saving Settings", () -> saveSettings(numberOfBatchDownloads, numberOfSearchResults, apiKey, remainingDownloadRequests));
            }
        } catch (Exception e) {
            logger.error("Error while reading form settingsJSONFile", e);
            newError("Error Reading While Saving Settings", () -> saveSettings(numberOfBatchDownloads, numberOfSearchResults, apiKey, remainingDownloadRequests));
        }
    }

    public static JSONObject getSettings() {
        logger.info("getting settings, reading settingsJSONFile now");
        String string = "";
        try (InputStream inputStream = new FileInputStream(settingsJSONFile)) {
            string = new String(inputStream.readAllBytes());
        } catch (Exception e) {
            newError("Error While Reading Settings File", () -> {
            });
            logger.error("Error while reading settingsJSONFile", e);
        }
        return new JSONObject(string);
    }

    public static String listJSONToString(JSONObject listEntry) {
        boolean hasImage = listEntry.getString("image").length() > 1;

        return "{" +
                "trackName='" + listEntry.getString("trackName") + "'" +
                ", artistName='" + listEntry.getString("artistName") + "'" +
                ", albumName='" + listEntry.getString("albumName") + "'" +
                ", recordingYear='" + listEntry.getString("recordingYear") + "'" +
                ", image='" + hasImage + "'" +
                "}";
    }

    public static String listJSONTostring(JSONArray list) {
        String string = "[";

        for (int i = 0; i < list.length(); i++) {
            JSONObject listEntry = list.getJSONObject(i);
            boolean hasImage = listEntry.getString("image").length() > 1;

            string += "{" +
                    "trackName='" + listEntry.getString("trackName") + "'" +
                    ", artistName='" + listEntry.getString("artistName") + "'" +
                    ", albumName='" + listEntry.getString("albumName") + "'" +
                    ", recordingYear='" + listEntry.getString("recordingYear") + "'" +
                    ", image='" + hasImage + "'" +
                    "}";
        }

        string += "]";
        return string;
    }

    public static void newError(String message, ErrorController.OnRetryInterface onRetry) {
        FXMLLoader fxmlLoader = new FXMLLoader(Application.class.getResource("error.fxml"));
        Stage stage;
        try {
            stage = fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ErrorController errorController = fxmlLoader.getController();
        errorController.onRetryInterface = onRetry;
        errorController.InformationLabel.setText(message);
        errorController.stage = stage;
        stage.show();
        logger.debug("created new Error with messege \"{}\" and retry \"{}\"", message, onRetry);
    }

    Dictionary<String, String> parseQueryString(String queryString) {
        String[] seperatedQuery = queryString.split("&");

        Dictionary<String, String> parameterValue = new Hashtable<>();

        for (String string : seperatedQuery) {
            String[] seperatedString = string.split("=");

            parameterValue.put(seperatedString[0], seperatedString[1]);
        }

        return parameterValue;
    }
}