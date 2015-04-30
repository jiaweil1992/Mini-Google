package cis555.searchengine;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class SearchEngine {

    private static DBWrapper db;

    public static void setDatabase(DBWrapper db) {
        SearchEngine.db = db;
    }

    /*
     * Search using Boolean Model, binary decision, no tf-idf value is used for
     * testing...
     * 
     * @param: query, from user input
     * 
     * @return: List of matching URLs
     */
    public static List<String> booleanSearch(String query) {
        Set<QueryTerm> queryTerms = SEHelper.parseQuery(query);
        if (queryTerms.size() == 0)
            return new LinkedList<String>();
        Iterator<QueryTerm> iter = queryTerms.iterator();
        Set<String> docIDSet = new HashSet<String>();
        Set<DocHitEntity> docHitSet = db.getDocHit(iter.next().getWord());
        for (DocHitEntity docHit : docHitSet) {
            docIDSet.add(docHit.getDocID());
        }
        for (QueryTerm queryTerm : queryTerms) {
            docHitSet = db.getDocHit(queryTerm.getWord());
            Set<String> newDocIDSet = new HashSet<String>();
            for (DocHitEntity docHit : docHitSet) {
                newDocIDSet.add(docHit.getDocID());
            }
            docIDSet.retainAll(newDocIDSet);
        }
        List<String> results = new LinkedList<String>();
        for (String docID : docIDSet) {
            results.add(db.getUrl(docID));
        }
        return results;
    }

    /*
     * Search using Vector Model, just a protoType
     * 
     * @param: query, from user input
     * 
     * @return: List of matching WeightedDocID, sorted by weight
     */
    public static List<WeightedDocID> vectorSearch(String query) {
        Set<QueryTerm> queryTerms = SEHelper.parseQuery(query);
        if (queryTerms.size() == 0)
            return new LinkedList<WeightedDocID>();
        List<WeightedDocID> results = new LinkedList<WeightedDocID>();
        return results;
    }

    public static void main(String... args) {
        DBWrapper db = new DBWrapper("database");
        SearchEngine.setDatabase(db);
        db.start();
        // SearchEngine se = new SearchEngine(db);
        String[] queries = new String[] {
                "Computer Science developer, hello a i world test wiki",
                "abd asd;wqekl .qwnlcasd.asd;", "computer Science.",
                "testing ", "WikiPedia", "Bank of America", "Apigee",
                "University of Pennsylvania", "UCB", "....a" };
        for (String query : queries) {
            System.out.println(SearchEngine.booleanSearch(query));
        }
        db.shutdown();
    }
}
