package cis555.searchengine;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import cis555.searchengine.utils.DocHitEntity;
import cis555.searchengine.utils.IndexTerm;
import cis555.utils.DocIdUrlInfo;

import com.sleepycat.je.CursorConfig;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.persist.EntityCursor;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;
import com.sleepycat.persist.SecondaryIndex;
import com.sleepycat.persist.StoreConfig;

public class DocHitEntityIndexTermDBDAO {
	private static EntityStore store;
	private static PrimaryIndex<Long, DocHitEntity> docHitEntityById;
	private static SecondaryIndex<String, Long, DocHitEntity> docHitEntityByWord;
	private static PrimaryIndex<String, IndexTerm> termIndex;
	
	/** Initialize all the static variables
	 *	@param dbPath The file path of the db  
	 */
	public static void setup(String dbPath) {
		// Create the directory in which this store will live.
		System.out.println("Setting up DocHitEntityIndexTermDB.");
	    File dir = new File(dbPath, "DocHitEntityIndexTermDB");
	    if (dir.mkdirs()) {
	    	System.out.println("Created DocHitEntityIndexTermDB directory.");
	    }
	    
 	    
	    EnvironmentConfig envConfig = new EnvironmentConfig();
		StoreConfig storeConfig = new StoreConfig();
		envConfig.setAllowCreate(true);
		storeConfig.setAllowCreate(true);
		
	    Environment env = new Environment(dir,  envConfig);
	    store = new EntityStore(env, "DocHitEntityStore", storeConfig);
	    
	    termIndex = store.getPrimaryIndex(String.class, IndexTerm.class);
	    docHitEntityById = store.getPrimaryIndex(Long.class, DocHitEntity.class);
	    docHitEntityByWord = store.getSecondaryIndex(docHitEntityById, String.class, "word");
	    
	    ShutdownHook hook = new ShutdownHook(env, store);
	    Runtime.getRuntime().addShutdownHook(hook);
		
	}
	
	/**
	 * Store the given DocHitEntity in the database.
	 * @param DocHitEntity 
	 */
	public static void putDocHitEntity(DocHitEntity docHitEntity) {
		docHitEntityById.put(docHitEntity);
	}
	
	/**
	 * Store the given word and line as DocHitEntity in the database.
	 * @param word
	 * @param line 
	 */
	public static void putDocHitEntity(String word, String line, double avgWord) {
		docHitEntityById.put(new DocHitEntity(word, line, avgWord));
    }
	  
	/**
	 * Retrieve a DocHitEntity from the database given its key.
	 * @param id The primary key for the DocHitEntity.
	 * @return The DocHitEntity instance. 
	 */
	public static DocHitEntity getDocHitEntity(long id) {
		return docHitEntityById.get(id);	
	}
	
	/**
     * Given a word return a Set<DocHit>; This function would automatically do
     * a checking: if the URL of docId is not saved in database, then it will
     * not be returned.
     * 
     * TODO: query word process should be done explicitly outside this function.
     */
    public static Set<DocHitEntity> getDocHitEntities(String word) {
        EntityCursor<DocHitEntity> cursor = docHitEntityByWord.entities(word, true,
                word, true);
        Set<DocHitEntity> set = new HashSet<DocHitEntity>();
        for (DocHitEntity docHit : cursor) {
            if (getUrl(docHit.getDocID()) != null) {
            	set.add(docHit);
            }
        }
        cursor.close();
        return set;
    }
	
	/**
	 * Returns cursor that iterates through the DocHitEntities.
	 * @return A DocHitEntity cursor. 
	 */
	public static EntityCursor<Long> getDocHitEntityCursor() {
		CursorConfig cursorConfig = new CursorConfig();
		cursorConfig.setReadUncommitted(true);
		return docHitEntityById.keys(null, cursorConfig);
	}
	  
	/**
	 * Removes the DocHitEntity instance with the specified host.
	 * @param id
	 */
	public static void deleteDocHitEntity(long id) {
		docHitEntityById.delete(id);
	}
	
	
	public static String getUrl(String docID) {
        DocIdUrlInfo docIdUrlInfo = UrlIndexDAO.getUrlInfoIndex().get(docID);
        if (docIdUrlInfo == null)
            return null;
        return docIdUrlInfo.getURL();
    }
	
	
	
	
	
	
	
	/**
	 * Store the given indexTerm in the database.
	 * @param indexTerm 
	 */
	public static void putIndexTerm(IndexTerm IndexTerm) {
		termIndex.put(IndexTerm);
	}
	
	/**
	 * Store the given word as an indexTerm in the database.
	 * @param word 
	 */
	public static void putIndexTerm(String word) {
        termIndex.put(new IndexTerm(word));
    }
	  
	/**
	 * Retrieve a indexTerm from the database given its docID.
	 * @param docID The primary key for the indexTerm.
	 * @return The indexTerm instance. 
	 */
	public static IndexTerm getIndexTerm(String docID) {
		return termIndex.get(docID);	
	}
	
	/**
	 * Returns cursor that iterates through the indexTerm.
	 * @return A indexTerm cursor. 
	 */
	public static EntityCursor<String> getUrlInfoCursor() {
		CursorConfig cursorConfig = new CursorConfig();
		cursorConfig.setReadUncommitted(true);
		return termIndex.keys(null, cursorConfig);
	}
	  
	/**
	 * Removes the indexTerm instance with the docID.
	 * @param docID
	 */
	public static void deleteUrlInfo(String docID) {
		termIndex.delete(docID);
	}
	
}