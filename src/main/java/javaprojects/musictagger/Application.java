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

import java.io.*;

public class Application extends javafx.application.Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Application.class.getResource("main.fxml"));
        Stage myStage = fxmlLoader.load();
        myStage.show();
        MainController mainController = fxmlLoader.getController();
        mainController.mainStage = myStage;
    }

    public static void main(String[] args) {
        launch();
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
}