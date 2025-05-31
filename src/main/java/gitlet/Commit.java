package gitlet;



import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

public class Commit implements Serializable {

    private String message;
    private String timestamp;
    private String parent;
    private String secondParent;
    private Map<String, String> blobs;



    public Commit() {
        this.message = "initial commit";
        this.timestamp = "Thu Jan 1 00:00:00 1970 +0000";
        this.parent = null;
        this.secondParent = null;
        this.blobs = new TreeMap<>();
    }
    public Commit(String message, String parent, Map<String, String> blobs) {
        this.message = message;
        this.timestamp = getCurrentTimestamp();
        this.parent = parent;
        this.secondParent = null;
        this.blobs = new TreeMap<>(blobs);
    }
    private String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z");
        return sdf.format(new Date());
    }

    public String getMessage() {
        return message;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getParent() {
        return parent;
    }

    public Map<String, String> getBlobs() {
        return blobs;
    }

    public String getSecondParent() {
        return secondParent;
    }

    public void setSecondParent(String secondParent) {
        this.secondParent = secondParent;
    }
}
