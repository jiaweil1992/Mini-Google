package cis555.searchengine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cis555.searchengine.utils.DocHitEntity;
import cis555.searchengine.utils.QueryTerm;
import cis555.searchengine.utils.SEHelper;
import cis555.searchengine.utils.WeightedDocID;

/**
 * Streamlined version of QueryProcessor
 * 
 * 1. Parse Query
 * 
 * 2. Prepare Phase (get docID + calculate TF-IDF value)
 * 
 * 3. PageRank Phase (combined with PageRank value)
 * 
 * 4. Position Check Phase (kinda solve the cons of bag model)
 * 
 * 5. Fancy Check Phase (add weight by Fancy Hit)
 * 
 * 6. Filter Phase (Sort + get top N result)
 * 
 */
public class QueryProcessor {

    public static void setup(String dbPath) {
        IndexTermDAO.setup(dbPath);
        UrlIndexDAO.setup(dbPath);
    }

    public static Set<QueryTerm> parseQuery(String query) {
        return SEHelper.parseQuery(query);
    }

    /**
     * prepare phase for the query processor
     *
     * 1. Get matching docID
     * 
     * 2. Calculate MD25 value for each matching docID
     * 
     * 3. Attach corresponding DocIdEntity to weightedDocId
     * 
     * @param Set
     *            of queryTerms
     * @return List of WeightedDocID
     */
    public static List<WeightedDocID> preparePhase(Set<QueryTerm> queryTerms) {
        if (queryTerms.size() == 0)
            return new LinkedList<WeightedDocID>();
        Iterator<QueryTerm> iter = queryTerms.iterator();
        QueryTerm queryTerm = iter.next();
        Map<String, WeightedDocID> weightedDocIDMap = new HashMap<String, WeightedDocID>();
        Set<DocHitEntity> docHitSet = IndexTermDAO.getDocHitEntities(queryTerm
                .getWord());
        for (DocHitEntity docHit : docHitSet) {
            WeightedDocID weightedDocID = new WeightedDocID(docHit.getDocID());
            weightedDocID.updateWeight(docHit.getTf() * queryTerm.getFreq()
                    * IndexTermDAO.getIdfValue(queryTerm.getWord()));
            weightedDocID.addDocHit(docHit);
            weightedDocIDMap.put(docHit.getDocID(), weightedDocID);
        }
        while (iter.hasNext()) {
            queryTerm = iter.next();
            docHitSet = IndexTermDAO.getDocHitEntities(queryTerm.getWord());
            for (DocHitEntity docHit : docHitSet) {
                WeightedDocID weightedDocID = weightedDocIDMap.get(docHit
                        .getDocID());
                if (weightedDocID == null) {
                    weightedDocID = new WeightedDocID(docHit.getDocID());
                    weightedDocIDMap.put(docHit.getDocID(), weightedDocID);
                }
                weightedDocID.updateWeight(docHit.getTf() * queryTerm.getFreq()
                        * IndexTermDAO.getIdfValue(queryTerm.getWord()));
                weightedDocID.addDocHit(docHit);
            }
        }

        List<WeightedDocID> results = new ArrayList<WeightedDocID>(
                weightedDocIDMap.values());
        return results;
    }

    public static List<WeightedDocID> pageRankPhase(
            List<WeightedDocID> weightedDocIDList) {
        return null;
    }

    public static List<WeightedDocID> posCheckPhase(
            List<WeightedDocID> weightedDocIDList, Set<QueryTerm> queryTerms) {
        return null;
    }

    public static List<WeightedDocID> fancyCheckPhase(
            List<WeightedDocID> weightedDocIDList) {
        return null;
    }

    /**
     * filtering phase --> sort + top N (offset-based)
     * 
     * @param: weightDocIDList, the original list
     * @param: startOffset
     * @param: N, how many results you want
     * @return filtered weightedDocIDList
     */
    public static List<WeightedDocID> filterPhase(
            List<WeightedDocID> weightedDocIDList, int startOffset, int N) {
        Collections.sort(weightedDocIDList);
        List<WeightedDocID> newList = new ArrayList<WeightedDocID>(N);
        Iterator<WeightedDocID> iter = weightedDocIDList.iterator();
        int startIndex = startOffset;
        int endIndex = Math.min(N + startOffset - 1,
                weightedDocIDList.size() - 1);
        int index = 0;
        while (iter.hasNext()) {
            if (index < startIndex) {
                iter.next();
                index++;
            } else if (index <= endIndex) {
                newList.add(iter.next());
                index++;
            } else
                break;
        }
        return newList;
    }

    /**
     * @param: weightDocIDList
     * @return URL List
     */
    public static List<String> getURLs(List<WeightedDocID> weightedDocIDList) {
        List<String> results = new ArrayList<String>(weightedDocIDList.size());
        for (WeightedDocID w : weightedDocIDList) {
            results.add(UrlIndexDAO.getUrl(w.getDocID()));
        }
        return results;
    }

    public static void main(String... args) {
        setup("database");
        String[] queries = new String[] {
                "United_Christian_Broadcasters",
                "Computer Science developer, hello a i world test wiki 12321 sd132 o98nasd what is ",
                "abd asd;wqekl .qwnlcasd.asd;", "computer Science.",
                "testing ", "WikiPedia", "Bank of America", "Apigee",
                "University of Pennsylvania", };

        for (String query : queries) {
            System.out.println(query);
            // System.out.println();
            Set<QueryTerm> terms = parseQuery(query);
            List<WeightedDocID> lst1 = preparePhase(terms);
            List<WeightedDocID> lst2 = filterPhase(lst1, 0, 10);
            List<String> URLs = getURLs(lst2);
            for (String URL : URLs) {
                System.out.println(URL);
            }
        }
    }

}