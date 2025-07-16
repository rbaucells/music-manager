package javaprojects.musictagger;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class SearchListController {
    public final String SPOTIFY_CLIENT_ID = "d98205732f034eaeb1f56a0bd987eebe";
    public final String SPOTIFY_CLIENT_SECRET = "d8e239b7a26f46eb9d6806e1fb919f39";

    private static final Logger logger = LoggerFactory.getLogger(SearchListController.class);
    // Label
    @FXML
    public Label SongArtistLabel;

    // VBoxes
    @FXML
    public VBox ScrollingVBox;

    // SearchListScreenStage
    public Stage stage;

    String songName;
    String artistName;
    int page;

    public void OnInitialize(String songName, String artistName, int page) throws IOException {
        this.songName = songName;
        this.artistName = artistName;
        this.page = page;

        logger.info("Initializing SearchList with parameters [songName {}, artistName {}, page {}]", songName, artistName, page);
        String string = songName.strip() + " | " + artistName.strip();
        SongArtistLabel.setText(string);

        stage.setMinWidth(SongArtistLabel.getWidth());

        ArrayList<MP3Data> mp3Datas = GetAllSongData(songName.strip(), artistName.strip(), page);

        logger.info("Got search data, iterating through it now to add searchResults");

        for (MP3Data mp3Data : mp3Datas) {
            FXMLLoader fxmlLoader = new FXMLLoader(Application.class.getResource("search_result.fxml"));
            AnchorPane anchorPane = fxmlLoader.load();
            ScrollingVBox.getChildren().add(anchorPane);
            SearchResultController searchResultController = fxmlLoader.getController();

            searchResultController.initialize(mp3Data);
            searchResultController.stage = stage;
        }

        logger.debug("Creating \"See More\" button at bottom of ScrollingVBox");

        Button button = new Button();
        button.setText("See More");
        button.setOnAction(event -> {
            try {
                this.OnInitialize(songName, artistName, page + 1);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        Pane spacerPane = new Pane();
        spacerPane.setPrefHeight(6);
        spacerPane.setMaxHeight(6);
        spacerPane.setMinHeight(6);

        // delete any past buttons or empty spacerPanes if were on a page other than 1
        if (page > 1) {
            logger.debug("we are on a page greater than 1 so we have to remove old \"See More\" buttons and spacer panes");
            List<Node> nodesToRemove = new ArrayList<>();
            for (Node node : ScrollingVBox.getChildren()) {
                if (node instanceof Button || (node instanceof Pane && !(node instanceof AnchorPane))) {
                    nodesToRemove.add(node);
                }
            }
            ScrollingVBox.getChildren().removeAll(nodesToRemove);
        }

        ScrollingVBox.getChildren().add(spacerPane);
        ScrollingVBox.getChildren().add(button);
    }

    public ArrayList<MP3Data> GetAllSongData(String trackName, String artistName, int page) throws IOException {
        String accessToken = getAccessToken();
        JSONObject trackSearchObject = SearchTracks(accessToken, (trackName + " by " + artistName).strip(), page);

        logger.info("processing search results");

        try {
            if (trackSearchObject.has("items")) {
                JSONArray tracksArray = trackSearchObject.getJSONArray("items");
                ArrayList<MP3Data> mp3DataList = new ArrayList<>();

                logger.debug("looping through all tracks in trackArray");
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
                                    artist = artist + ", " + artistArray.getJSONObject(y).getString("name");
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
                    logger.debug("created new MP3Data from trackObject, adding it to mp3DataList {}", data);
                    mp3DataList.add(data);
                }
                return mp3DataList;
            }
        }
        catch (Exception e) {
            logger.error("Error processing search results from Spotify Web API", e);
            throw (new ExitException("Error processing search results from Spotify Web API"));
        }

        return null;
    }

    public JSONObject SearchTracks(String accessToken, String searchTerm, int page) {
        int amountPerPage = Application.getSettings().getInt("numberOfSearchResults");
        int offset = amountPerPage * (page - 1);
        logger.info("sending search request to spotify web api");
        logger.debug("amountPerPage {} and offset {}", amountPerPage, offset);
        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            URI uri = new URI("https", null, "api.spotify.com", 443, "/v1/search", "q=" + searchTerm + "&type=track&market=US&offset=" + offset + "&limit=" + amountPerPage, null);
            logger.debug("creating search request from uri {}", uri);
            HttpRequest request = HttpRequest.newBuilder()
                    .GET()
                    .uri(uri)
                    .header("Authorization", "Bearer " + accessToken)
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            int statusCode = response.statusCode();
            logger.info("response received with statusCode {}", response.statusCode());
            if (statusCode == 429) {
                logger.error("status code was 429, quota reached for spotify web api");
                throw (new ExitException("Quota for spotify web api reached"));
            }
            logger.debug("turning response into jsonObject");
            JSONObject responseObject = new JSONObject(response.body());

            return responseObject.getJSONObject("tracks");
        }
        catch (ExitException e) {
            throw (e);
        }
        catch (Exception e) {
            logger.error("Error sending search request", e);
            throw (new ExitException("Error sending search request"));
        }
    }

    public String getAccessToken() {
        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            logger.info("getting access token from spotify web api");
            URI uri = new URI("https", null, "accounts.spotify.com", 443, "/api/token", "grant_type=client_credentials&client_id=" + SPOTIFY_CLIENT_ID.strip() + "&client_secret=" + SPOTIFY_CLIENT_SECRET.strip(), null);
            HttpRequest request = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .uri(uri)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .build();
            logger.debug("sending request to spotify web api");
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            logger.debug("response received with statusCode {}", response.statusCode());
            logger.debug("making jsonObject from response and getting access_token");
            JSONObject object = new JSONObject(response.body());
            String accessToken = object.getString("access_token");
            logger.debug("returning access_token {}", accessToken);
            return accessToken;
        }
        catch (Exception e) {
            logger.error("Error getting access token", e);
            throw (new ExitException("Error getting access token from spotify"));
        }
    }

    public byte[] GetImageFromURL(String URL) throws URISyntaxException {
        logger.debug("Turning URL given to URI");
        URI uri = new URI(URL);

        logger.debug("reading from URI {} and returning byteArray", uri);
        byte[] byteArray;

        try (InputStream inputStream = uri.toURL().openStream()) {
            byteArray = inputStream.readAllBytes();
            return byteArray;
        }
        catch (Exception e) {
            logger.error("Error getting Image from URI", e);
            throw (new ExitException("Error getting Image from URI"));
        }
    }
}
