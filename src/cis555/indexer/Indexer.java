package cis555.indexer;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class Indexer {

    protected static final String DELIMITER = ", \t\n\r\f|:/.![]{}()*^&%~'\\<>?=+\"";

    protected long docID;
    protected String URL;
    protected FileInputStream fis;
    protected Stemmer stemmer;
    protected Map<String, Map<Long, DocHit>> map;

    public Indexer(File file, String URL, long docID) throws Exception {
        this.URL = URL;
        this.docID = docID;
        this.stemmer = new Stemmer();
        this.fis = new FileInputStream(file);
        this.map = new HashMap<String, Map<Long, DocHit>>();
    }

    public static Indexer getInstance(File file, String URL, long docID,
            String contentType) {
        try {
            if (contentType.equals("text/html"))
                return new HTMLIndexer(file, URL, docID);
            if (contentType.equals("text/plain"))
                return new TXTIndexer(file, URL, docID);
            if (contentType.equals("application/pdf"))
                return new PDFIndexer(file, URL, docID);
            if (contentType.endsWith("xml"))
                return new XMLIndexer(file, URL, docID);
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    protected String getStem(String word) {
        word = word.trim();
        stemmer.add(word.toCharArray(), word.length());
        stemmer.stem();
        return stemmer.toString();
    }

    // TODO: get docID for given URL
    protected static long getDocID(String url) {
        url = url.trim();
        return 111;
    }

    // parse method should be implemented by extended class
    public void parse() {
    }

    public void displayResult() {
        for (Entry<String, Map<Long, DocHit>> e1 : map.entrySet()) {
            for (Entry<Long, DocHit> e2 : e1.getValue().entrySet()) {
                System.out.println(e1.getKey() + " : " + e2.getValue());
            }
        }
    }
}