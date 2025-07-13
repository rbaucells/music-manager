package javaprojects.musictagger;

import javafx.fxml.FXMLLoader;
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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Application extends javafx.application.Application {

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
        mainController.OnIntiate();
        mainController.mainStage = myStage;
    }

    @Override
    public void stop() throws IOException {
        threadPoolExecutor.close();
        JSONObject jsonObject = GetSettings();
        SaveSettings(jsonObject.getInt("numberOfBatchDownloads"), jsonObject.getInt("numberOfSearchResults"), jsonObject.getString("apiKey"), mainController.GetRemainingDownloadApiRequests());
    }

    public static void main(String[] args) throws IOException {
        SetUpListJSON();
        SetUpSettingsJSON();

        int numberOfBatchDownloads = GetSettings().getInt("numberOfBatchDownloads");
        threadPoolExecutor = new ThreadPoolExecutor(numberOfBatchDownloads, numberOfBatchDownloads, 5, TimeUnit.MINUTES, new ArrayBlockingQueue<>(numberOfBatchDownloads));

        launch();
    }

    public static void SetUpSettingsJSON() throws IOException {
        if (!settingsJSONFile.exists()) {
            Files.createDirectories(configurationPath);
            Files.createFile(settingsJSONFilePath);

            try (OutputStream outputStream = new FileOutputStream(settingsJSONFile)) {
                JSONObject jsonObject = new JSONObject();

                jsonObject.put("numberOfBatchDownloads", 5);
                jsonObject.put("numberOfSearchResults", 10);
                jsonObject.put("apiKey", "");
                jsonObject.put("remainingDownloadRequests", 300);

                outputStream.write(jsonObject.toString().getBytes());
            } catch (Exception e) {
                NewError("Error While Writting While Setting Up SettingsJSON", Application::SetUpSettingsJSON);
            }
        }
        else {
            try (InputStream inputStream = new FileInputStream(settingsJSONFile)) {
                String string = new String(inputStream.readAllBytes());

                if (!string.contains("{")) {
                    try (OutputStream outputStream = new FileOutputStream(settingsJSONFile)) {
                        JSONObject jsonObject = new JSONObject();

                        jsonObject.put("numberOfBatchDownloads", 5);
                        jsonObject.put("numberOfSearchResults", 10);
                        jsonObject.put("apiKey", "");
                        jsonObject.put("remainingDownloadRequests", 300);

                        outputStream.write(jsonObject.toString().getBytes());
                    } catch (Exception e) {
                        NewError("Error While Writing To SettingsJSON", Application::SetUpSettingsJSON);
                    }
                }

                JSONObject jsonObject = new JSONObject(string);

                if (!jsonObject.has("numberOfBatchDownloads")) {
                    jsonObject.put("numberOfBatchDownloads", 5);
                }
                if (!jsonObject.has("numberOfSearchResults")) {
                     jsonObject.put("numberOfSearchResults", 10);
                }
                if (!jsonObject.has("apiKey")) {
                    jsonObject.put("apiKey", "");
                }
                if (!jsonObject.has("remainingDownloadRequests")) {
                    jsonObject.put("remainingDownloadRequests", 300);
                }

                try (OutputStream outputStream = new FileOutputStream(settingsJSONFile)) {
                    outputStream.write(jsonObject.toString().getBytes());
                }
                catch (Exception e) {
                    NewError("Error While Writing To SettingsJSON", Application::SetUpSettingsJSON);
                }
            }
            catch (Exception e) {
                NewError("Error While Reading From SettingsJSON", Application::SetUpSettingsJSON);
            }
        }
    }

    public static void SetUpListJSON() throws IOException {
        if (!listJSONFile.exists()) {
            Files.createDirectories(configurationPath);
            Files.createFile(listJSONFilePath);

            try (OutputStream outputStream = new FileOutputStream(listJSONFile)) {
                outputStream.write("[ ]".getBytes());
            }
            catch (Exception e) {
                NewError("Error Writing to ListJSON While SettingUpListJSON", Application::SetUpListJSON);
                e.printStackTrace();
            }
        }
        else {
            try (InputStream inputStream = new FileInputStream(listJSONFile)) {
                String jsonString = new String(inputStream.readAllBytes());

                if (!jsonString.contains("[")) {
                    try (OutputStream outputStream = new FileOutputStream(listJSONFile)) {
                        outputStream.write("[ ]".getBytes());
                    }
                    catch (Exception e) {
                        NewError("Error Writing to ListJSON While SettingUpListJSON", Application::SetUpListJSON);
                        e.printStackTrace();
                    }
                }
            }
            catch (Exception e) {
                e.printStackTrace();
                NewError("Error Reading From ListJSON While SettingUpListJSON", Application::SetUpListJSON);
            }
        }
    }

    public static void ApplyMP3Data(MP3Data data, File selectedFile) throws CannotReadException, TagException, InvalidAudioFrameException, ReadOnlyFileException, IOException, CannotWriteException {
        AudioFile musicFile = AudioFileIO.read(selectedFile);
        Tag fileTag = musicFile.getTagOrCreateDefault();

        fileTag.deleteArtworkField();

        fileTag.setField(FieldKey.TITLE, data.trackName);
        fileTag.setField(FieldKey.ARTIST, data.artistName);
        fileTag.setField(FieldKey.ALBUM, data.albumName);
        if (!data.recordingYear.isBlank())
            fileTag.setField(FieldKey.YEAR, data.recordingYear);
        else
            fileTag.deleteField(FieldKey.YEAR);

        StandardArtwork standardArtwork = new StandardArtwork();
        standardArtwork.setBinaryData(data.image);
        fileTag.setField(standardArtwork);

        musicFile.setTag(fileTag);
        musicFile.commit();

        File newMusicFilePath = new File(selectedFile.getParent(), data.trackName + ".mp3");

        selectedFile.renameTo(newMusicFilePath);
    }

    public static void WipeMP3(File selectedFile) throws CannotReadException, TagException, InvalidAudioFrameException, ReadOnlyFileException, IOException, CannotWriteException {
        AudioFile musicFile = AudioFileIO.read(selectedFile);
        Tag fileTag = musicFile.getTagOrCreateDefault();
        fileTag = new ID3v1Tag();
        musicFile.setTag(fileTag);
        musicFile.commit();
    }

    public static Boolean DoesListJSONContain(MP3Data data) throws IOException {
        ArrayList<MP3Data> mp3DataArrayList = ReadListJSON();
        for (MP3Data mp3Data : mp3DataArrayList) {
            if (Objects.equals(mp3Data.trackName, data.trackName) && Objects.equals(mp3Data.artistName, data.artistName) && Objects.equals(mp3Data.albumName, data.albumName) && Objects.equals(mp3Data.recordingYear, data.recordingYear)) {
                return true;
            }
        }

        return false;
    }

    public static void WriteMP3DataToListJSON(MP3Data data) throws IOException {
        if (ReadListJSON().contains(data))
            return;

        JSONObject item = new JSONObject()
                .put("trackName", data.trackName)
                .put("artistName", data.artistName)
                .put("albumName", data.albumName)
                .put("recordingYear", data.recordingYear)
                .put("image", Base64.getEncoder().encodeToString(data.image));

        try (InputStream inputStream = new FileInputStream(listJSONFile)) {
            JSONArray jsonArray = new JSONArray(new String(inputStream.readAllBytes()));
            jsonArray.put(item);

            try (OutputStream outputStream = new FileOutputStream(listJSONFile)) {
                outputStream.write(jsonArray.toString(4).getBytes());
            }
        }
    }

    public static void DeleteMP3DataFromListJSON(MP3Data data) throws IOException {
        try (InputStream inputStream = new FileInputStream(listJSONFile)) {
            JSONArray jsonArray = new JSONArray(new String(inputStream.readAllBytes()));
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);

                // are the 2 the same
                if (Objects.equals(jsonObject.getString("trackName"), data.trackName) && Objects.equals(jsonObject.getString("artistName"), data.artistName) && Objects.equals(jsonObject.getString("albumName"), data.albumName) && Objects.equals(jsonObject.getString("recordingYear"), data.recordingYear)) {
                    jsonArray.remove(i);
                }
            }

            try (OutputStream outputStream = new FileOutputStream(listJSONFile)) {
                outputStream.write(jsonArray.toString(4).getBytes());
            }
            catch (Exception e) {
                e.printStackTrace();
                NewError("Error Writing to ListJSON While Deleting MP3Data", () -> {
                    Application.DeleteMP3DataFromListJSON(data);
                });
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            NewError("Error Reading from ListJSON While Deleting MP3Data", () -> {
                Application.DeleteMP3DataFromListJSON(data);
            });
        }
    }

    public static ArrayList<MP3Data> ReadListJSON() throws IOException {
        ArrayList<MP3Data> datas = new ArrayList<>();

        try (InputStream inputStream = new FileInputStream(listJSONFile)) {
            JSONArray jsonArray = new JSONArray(new String(inputStream.readAllBytes()));
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);

                MP3Data data = new MP3Data(jsonObject.getString("trackName"), jsonObject.getString("artistName"), jsonObject.getString("albumName"), jsonObject.getString("recordingYear"), Base64.getDecoder().decode(jsonObject.getString("image")));

                datas.add(data);
            }
        }
        catch (Exception e) {
            NewError("Error Reading ListJSON", () -> {});
            e.printStackTrace();
        }

        return datas;
    }

    public static void ClearListJSON() throws IOException {
        try (OutputStream outputStream = new FileOutputStream(listJSONFile)) {
            outputStream.write(new JSONArray().toString(4).getBytes());
        }
        catch (Exception e) {
            NewError("Error Writing While Clearing ListJSON", Application::ClearListJSON);
        }
    }

    public static void SaveSettings(int numberOfBatchDownloads, int numberOfSearchResults, String apiKey, int remainingDownloadRequests) throws IOException {
        try (InputStream inputStream = new FileInputStream(settingsJSONFile)) {
            byte[] bytes = inputStream.readAllBytes();
            JSONObject jsonObject = new JSONObject(new String(bytes));

            jsonObject.put("numberOfBatchDownloads", numberOfBatchDownloads);
            jsonObject.put("numberOfSearchResults", numberOfSearchResults);
            jsonObject.put("apiKey", apiKey);
            jsonObject.put("remainingDownloadRequests", remainingDownloadRequests);

            try (OutputStream outputStream = new FileOutputStream(settingsJSONFile)) {
                outputStream.write(jsonObject.toString().getBytes());
            }
            catch (Exception e) {
                e.printStackTrace();
                NewError("Error Writing While Saving Settings", () -> {
                    SaveSettings(numberOfBatchDownloads, numberOfSearchResults, apiKey, remainingDownloadRequests);
                });
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            NewError("Error Reading While Saving Settings", () -> {
                SaveSettings(numberOfBatchDownloads, numberOfSearchResults, apiKey, remainingDownloadRequests);
            });
        }
    }

    public static JSONObject GetSettings() throws IOException {
        try (InputStream inputStream = new FileInputStream(settingsJSONFile)) {
            String string = new String(inputStream.readAllBytes());
            return new JSONObject(string);
        }
        catch (Exception e) {
            NewError("Error While Reading Settings File", () -> {});
            e.printStackTrace();
            return null;
        }
    }

    public static void NewError(String message, ErrorController.OnRetryInterface onRetry) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Application.class.getResource("error.fxml"));
        Stage stage = fxmlLoader.load();
        ErrorController errorController = fxmlLoader.getController();
        errorController.onRetryInterface = onRetry;
        errorController.InformationLabel.setText(message);
        errorController.stage = stage;
        stage.show();
    }
}