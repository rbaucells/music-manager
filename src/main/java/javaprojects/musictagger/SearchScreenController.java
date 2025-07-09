package javaprojects.musictagger;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;

public class SearchScreenController {
    // Labels
    @FXML
    public Label SongArtistLabel;

    // Vboxes
    @FXML
    public VBox ScrollingVBox;

    public Stage stage;

    public MainController mainController;

    public String client_id = "d98205732f034eaeb1f56a0bd987eebe";
    public String client_secret = "d8e239b7a26f46eb9d6806e1fb919f39";

    public HttpClient httpClient = HttpClient.newHttpClient();

    public void OnInitialize(String songName, String artistName) throws URISyntaxException, IOException, InterruptedException {
        String string = songName.strip() + " | " + artistName.strip();
        SongArtistLabel.setText(string);

        stage.setMinWidth(SongArtistLabel.getWidth());

        ArrayList<MP3Data> mp3Datas = GetAllSongData(songName.strip(), artistName.strip());

        for (int i = 0; i < mp3Datas.size(); i++) {
            FXMLLoader fxmlLoader = new FXMLLoader(Application.class.getResource("search_result.fxml"));
            AnchorPane anchorPane = fxmlLoader.load();
            ScrollingVBox.getChildren().add(anchorPane);
            SearchResultController searchResultController = fxmlLoader.getController();

            searchResultController.SetMP3Data(mp3Datas.get(i));
            searchResultController.stage = stage;
            searchResultController.mainController = mainController;
        }
    }

    public ArrayList<MP3Data> GetAllSongData(String trackName, String artistName) throws URISyntaxException, IOException, InterruptedException {
        String accessToken = GetAccessToken();

        JSONObject trackSearchObject = SearchTracks(accessToken, (trackName + " " + artistName).strip());

        if (trackSearchObject == null)
        {
            System.out.println("trackSearchObject was null. Exiting");
            System.exit(1);
        }

        if (trackSearchObject.has("items")) {
            JSONArray tracksArray = trackSearchObject.getJSONArray("items");

            ArrayList<MP3Data> mp3DataList = new ArrayList<>();

            for (int i = 0; i < tracksArray.length(); i++) {
                JSONObject track = tracksArray.getJSONObject(i);

                String name = "";

                if (track.has("name")) {
                    name = track.getString("name");
                }

                String artist = "";

                if (track.has("artists")) {
                    JSONArray artistArray = track.getJSONArray("artists");

                    if (!artistArray.isEmpty()) {
                        for (int y = 0; y < artistArray.length(); y++) {
                            if (artist.length() < 2)
                                artist = artistArray.getJSONObject(y).getString("name");
                            else
                                artist = artist + "," + artistArray.getJSONObject(y).getString("name");
                        }
                    }
                }

                String album = "";
                String year = "";
                byte[] artworkImage = null;

                if (track.has("album")) {
                    JSONObject albumObject = track.getJSONObject("album");

                    if (albumObject.has("album_type")) {
                        String album_type = albumObject.getString("album_type");

                        if (album_type.equals("album")) {
                            if (albumObject.has("name")) {
                                album = albumObject.getString("name");
                            }

                            if (albumObject.has("release_date")) {
                                String release_date = albumObject.getString("release_date");

                                year = release_date.substring(0, 4);
                            }
                        }
                    }

                    if (albumObject.has("images")) {
                        JSONArray images = albumObject.getJSONArray("images");

                        if (!images.isEmpty()) {
                            JSONObject image = images.getJSONObject(0);

                            if (image.has("url")) {
                                String imageURL = image.getString("url");

                                artworkImage = GetImageFromURL(imageURL);
                            }
                        }
                    }
                }

                if (name.isBlank()) {
                    name = trackName.strip();
                }

                if (artist.isBlank()) {
                    artist = artistName.strip();
                }

                MP3Data data = new MP3Data(name, artist, album, year, artworkImage);
                mp3DataList.add(data);
                System.out.println("Track name: " + name);
                System.out.println("Artist name: " + artist);
                System.out.println("Album name: " + album);
                System.out.println("Year: " + year);
            }
            return mp3DataList;
        }
        return null;
    }

    public JSONObject SearchTracks(String accessToken, String searchTerm) throws URISyntaxException, IOException, InterruptedException {
        URI uri = new URI("https", null, "api.spotify.com", 443, "/v1/search", "q=" + searchTerm + "&type=track&market=US", null);

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(uri)
                .header("Authorization", "Bearer " + accessToken)
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        JSONObject responseObject = new JSONObject(response.body());

        if (responseObject.has("tracks")) {
            return responseObject.getJSONObject("tracks");
        }

        return null;
    }

    public String GetAccessToken() throws URISyntaxException, IOException, InterruptedException {
        URI uri = new URI("https", null, "accounts.spotify.com", 443, "/api/token", "grant_type=client_credentials&client_id=" + client_id.strip() + "&client_secret=" + client_secret.strip(), null);
        HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.noBody())
                .uri(uri)
                .header("Content-Type","application/x-www-form-urlencoded")
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        JSONObject object = new JSONObject(response.body());
        if (object.has("access_token"))
            return object.getString("access_token");
        else
            return null;
    }

    public byte[] GetImageFromURL(String URL) throws URISyntaxException, IOException {
        URI uri = new URI(URL);

        byte[] byteArray;

        try (InputStream inputStream = uri.toURL().openStream()) {
            byteArray = inputStream.readAllBytes();
        }

        return byteArray;
    }
}
