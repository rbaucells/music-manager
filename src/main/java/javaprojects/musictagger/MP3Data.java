package javaprojects.musictagger;

import org.jaudiotagger.tag.images.Artwork;

import java.awt.*;

public class MP3Data {
    public String trackName;
    public String artistName;
    public String albumName;
    public String recordingYear;
    public byte[] image;

    public MP3Data(String trackName, String artistName, String albumName, String recordingYear, byte[] image) {
        this.trackName = trackName;
        this.artistName = artistName;
        this.albumName = albumName;
        this.recordingYear = recordingYear;
        this.image = image;
    }
}
