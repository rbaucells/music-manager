package rbaucells.MusicManager;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class SongListController {
    private static final Logger logger = LoggerFactory.getLogger(SongListController.class);
    // label
    @FXML
    public Label InfoLabel;

    // VBox
    @FXML
    public VBox ScrollingVBox;

    public Stage stage;


    public void onInitialize() throws IOException {
        refreshList();
    }

    void refreshList() throws IOException {
        logger.info("refreshing list with new data");
        logger.debug("clearing all childern in ScrollingVBox");
        ScrollingVBox.getChildren().clear();
        ArrayList<MP3Data> list = Application.readListJSON();
        logger.debug("iterating through MP3Datas in listJSONFile and creating a ListResult for each.");
        for (MP3Data mp3Data : list) {
            FXMLLoader fxmlLoader = new FXMLLoader(Application.class.getResource("list_result.fxml"));
            AnchorPane anchorPane = fxmlLoader.load();
            ScrollingVBox.getChildren().add(anchorPane);
            ListResultController listResultController = fxmlLoader.getController();

            listResultController.setMP3Data(mp3Data);
            listResultController.songListController = this;
        }

        InfoLabel.setText("Song List : " + list.size() + " Items");
    }

    public void onDownload() throws IOException {
        logger.info("downloading all items in listJSONFile");
        ArrayList<MP3Data> list = Application.readListJSON();

        if (list.isEmpty()) {
            logger.debug("listJSONFile was empty, nothing to download");
            return;
        }
        logger.info("prompting user what folder to put all downloaded files into");
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File folder = directoryChooser.showDialog(stage);

        if (folder == null) {
            logger.debug("selected folder was null, returning");
            return;
        }

        logger.debug("iterating through each MP3Data in listJSON and creating a new DownloadRunnable, adding it to the threadPoolExecutor and defining onCancelInterface");
        for (MP3Data mp3Data : list) {
            FXMLLoader fxmlLoader = new FXMLLoader(Application.class.getResource("progress_bar.fxml"));
            Stage progressStage = fxmlLoader.load();
            final ProgressBarController progressBarController = fxmlLoader.getController();
            progressBarController.stage = progressStage;
            File file = new File(folder.getPath() + File.separator + mp3Data.trackName + ".mp3");

            DownloadRunnable downloadRunnable = new DownloadRunnable(mp3Data, file, (message, percent) -> Platform.runLater(() -> progressBarController.SetProgressBar(message, percent)));

            var task = Application.threadPoolExecutor.submit((downloadRunnable));

            progressBarController.onCancelInterface = () -> {
                task.cancel(true);
                progressBarController.close();
            };
        }
    }

    public void onCancel() {
        stage.close();
    }

    public void onClear() throws IOException {
        Application.clearListJSON();
        refreshList();
    }
}
