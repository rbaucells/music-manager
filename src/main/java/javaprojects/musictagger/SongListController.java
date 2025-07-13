package javaprojects.musictagger;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.*;

public class SongListController {
    public MainController mainController;
    public Stage stage;

    @FXML
    public Label InfoLabel;

    @FXML
    public VBox ScrollingVBox;

    boolean downloading;

    public void OnInitialize() throws IOException {
        RefreshList();
    }

    void RefreshList() throws IOException {
        ScrollingVBox.getChildren().clear();
        ArrayList<MP3Data> list = Application.ReadListJSON();

        for (MP3Data mp3Data : list) {
            FXMLLoader fxmlLoader = new FXMLLoader(Application.class.getResource("list_result.fxml"));
            AnchorPane anchorPane = fxmlLoader.load();
            ScrollingVBox.getChildren().add(anchorPane);
            ListResultController listResultController = fxmlLoader.getController();

            listResultController.SetMP3Data(mp3Data);
            listResultController.stage = stage;
            listResultController.mainController = mainController;
            listResultController.songListController = this;
        }

        InfoLabel.setText("Song List : " + list.size() + " Items");
    }

    public void OnDownload() throws IOException, InterruptedException {
        ArrayList<MP3Data> list = Application.ReadListJSON();

        if (list.isEmpty())
            return;
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File folder = directoryChooser.showDialog(stage);

        if (folder == null)
            return;

        for (MP3Data mp3Data : list) {
            FXMLLoader fxmlLoader = new FXMLLoader(Application.class.getResource("progress_bar.fxml"));
            Stage progressStage = fxmlLoader.load();
            final ProgressBarController progressBarController = fxmlLoader.getController();
            progressBarController.stage = progressStage;
            File file = new File(folder.getPath() + File.separator + mp3Data.trackName + ".mp3");

            DownloadRunnable downloadRunnable = new DownloadRunnable(stage, mp3Data, file, (message, percent) -> Platform.runLater(() -> progressBarController.SetProgressBar(message, percent)));

            var task = Application.threadPoolExecutor.submit((downloadRunnable));

            progressBarController.onCancelInterface = () -> {
                task.cancel(true);
                progressBarController.close();
            };
        }

        System.out.println("Number of threads: " + Application.threadPoolExecutor.getPoolSize());
    }

    public void OnCancel() {
        stage.close();
    }

    public void OnClear() throws IOException {
        if (!downloading) {
            Application.ClearListJSON();
            Application.ClearListJSON();
            RefreshList();
        }
    }
}
