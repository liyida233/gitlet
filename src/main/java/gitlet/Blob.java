package gitlet;

import java.io.File;
import java.io.Serializable;
import static gitlet.Utils.*;

public class Blob implements Serializable {
    private final String filename;
    private final byte[] contents;
    private final String id;
    public Blob(File file) {
        this.filename = file.getName();
        this.contents = readContents(file);
        this.id = Utils.sha1((Object)contents);
    }

    public byte[] getContents() {
        return contents;
    }

    public String getId() {
        return id;
    }

    public void save() {
        File blobFile = join(Repository.BLOBS_DIR, id);
        writeObject(blobFile, this);
    }
    public static Blob fromId(String blobId) {
        File blobFile = join(Repository.BLOBS_DIR, blobId);
        return readObject(blobFile, Blob.class);
    }
}
