package cis555.utils;

import java.io.File;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;

/**
 * Class that provides a mapping from URL to docID, as well as from docID to URL
 * Involves generating a local database
 *
 */
public class UrlDocIDMapper {

    private String envDirectory;

    private static EntityStore store;
    private PrimaryIndex<Long, DocIdUrlInfo> docIdIndex;

    public UrlDocIDMapper(String envDirectory) {
        this.envDirectory = envDirectory;
        File directory = new File(envDirectory);
        if (!directory.exists()) {
            directory.mkdir();
        }
    }

    public void start() {
        try {
            store = DBWrapper.setupDatabase(envDirectory, false);
            docIdIndex = store.getPrimaryIndex(Long.class, DocIdUrlInfo.class);
        } catch (DatabaseException dbe) {
            dbe.printStackTrace();
        }
    }

    public void shutdown() {
        DBWrapper.shutdown();
    }

    public void sync() {
        DBWrapper.sync();
    }

    private void saveInfo(String url, String docId) {
        docIdIndex.put(new DocIdUrlInfo(url, docId));
    }

    /**
     * Returns the url for a particular docID
     * 
     * @param docId
     * @return
     */
    public String getUrl(long docId) {
        DocIdUrlInfo item = docIdIndex.get(docId);
        if (item == null)
            return null;
        return item.getURL();
    }

    // FOR TESTING PURPOSES ONLY
    // public static void main(String... args) {
    // UrlDocIDMapper db = new UrlDocIDMapper("test");
    // db.start();
    // // System.out.println(db.getDocId("https://www.yahoo.com/"));
    // // System.out.println(db.getDocId("https://www.yahoo.com123/"));
    // System.out.println(db.getUrl(356));
    // System.out.println(Hit.getHitType(-1610612718));
    // }

}
