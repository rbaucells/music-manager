package rbaucells.MusicManager;

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

    @Override
    public String toString() {
        return "{" +
                "trackName='" + trackName + '\'' +
                ", artistName='" + artistName + '\'' +
                ", albumName='" + albumName + '\'' +
                ", recordingYear='" + recordingYear + '\'' +
                ", image=" + (image.length > 0) +
                '}';
    }
}
