package cis555.crawler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import cis555.database.Dao;
import cis555.utils.CrawlerConstants;

public class LinkExtractorWorker implements Runnable {

	private static final Logger logger = Logger.getLogger(LinkExtractorWorker.class);
	private static final String CLASSNAME = LinkExtractorWorker.class.getName();
	
	private BlockingQueue<RawCrawledItem> contentForLinkExtractor;
	private BlockingQueue<URL> preRedistributionNewURLQueue;
	private int id;
	private Dao dao;
	private DocIDGenerator counterGenerator;
	private String storageDirectory;
	private String urlStorageDirectory;
	public static boolean active;
	
	public LinkExtractorWorker(BlockingQueue<RawCrawledItem> contentForLinkExtractor, 
			BlockingQueue<URL> preRedistributionNewURLQueue, int id, Dao dao, DocIDGenerator counterGenerator,
			String storageDirectory, String urlStorageDirectory) {
		this.contentForLinkExtractor = contentForLinkExtractor;
		this.preRedistributionNewURLQueue = preRedistributionNewURLQueue;
		this.id = id;
		this.dao = dao;
		this.counterGenerator = counterGenerator;
		this.storageDirectory = storageDirectory;
		this.urlStorageDirectory = urlStorageDirectory;
		LinkExtractorWorker.active = true;
	}

	/**
	 * Cleans the document, adds meta data to database, and store file to permanent storage (if it's a newly crawled file)

	 */
	@Override
	public void run() {
		while (LinkExtractorWorker.active){
			try {
				RawCrawledItem content = contentForLinkExtractor.take();
				
				URL url = content.getURL();
				String rawContents = content.getRawContents();
				String contentType = content.getContentType();
				
				Document cleansedDoc = Jsoup.parse(rawContents);
				
				if (content.isNew()){
					long docID = getDocID(url);
					this.dao.addNewCrawledDocument(docID, url.toString(), new Date(), contentType);
					storeCrawledContentsFile(cleansedDoc.toString(), docID);
					logger.info(CLASSNAME + " stored " + url.toString() + " to database");
				}
				if (contentType.equals("HTML")){
					addAHrefLinks(cleansedDoc, content, url);
					addImgSrcLinks(cleansedDoc, url);
				}
									
			} catch (InterruptedException | MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnsupportedEncodingException | IllegalStateException | IllegalArgumentException e1) {
				logger.info(CLASSNAME + " Unable to decode, skipping " + e1.getMessage());
			}
		}
	}
	
	/**
	 * Stores the source and target links to file (if it's a newly crawled file)
	 * and adds meta data for uncrawled new links
	 * @param cleansedDoc
	 * @param content
	 * @param sourceUrl
	 * @throws UnsupportedEncodingException
	 * @throws MalformedURLException
	 */
	private void addAHrefLinks(Document cleansedDoc, RawCrawledItem content, URL sourceUrl) throws IllegalArgumentException, UnsupportedEncodingException, MalformedURLException{
		Elements links = cleansedDoc.select("a[href]");
		
		List<URL> newUrls = new ArrayList<URL>();
		
		for (Element link : links){
			String urlString = link.attr("href");
			if (!urlString.isEmpty()){
				if (!urlString.contains("mailto:")){ // We're ignoring urls with 'mailto's
					URL newUrl = null;
					try {
						String cleansedString = URLDecoder.decode(urlString, CrawlerConstants.CHARSET);
						newUrl = CrawlerUtils.convertToUrl(cleansedString, sourceUrl);	
						this.preRedistributionNewURLQueue.add(newUrl);
						this.dao.addNewDocumentMeta(newUrl.toString(), getDocID(newUrl), new Date(), false);
						newUrls.add(newUrl);
					} catch (IllegalStateException e){
						logger.info(CLASSNAME + " Queue is full, dropping " + newUrl);
						
					} 
				}
			}
		} 
		
		if (content.isNew()){
			storeUrlsToFile(newUrls, sourceUrl);						
		}		
	}
	
	/**
	 * Add img links to database
	 * @param cleasnedDoc
	 * @param sourceUrl
	 * @throws IllegalArgumentException
	 * @throws UnsupportedEncodingException
	 * @throws MalformedURLException
	 */
	private void addImgSrcLinks(Document cleasnedDoc, URL sourceUrl) throws IllegalArgumentException, UnsupportedEncodingException, MalformedURLException{
		Elements links = cleasnedDoc.select("img[src]");
		
		List<String> addedImgLinksForOnePage = new ArrayList<String>();
		
		for (Element link : links){
			String urlString = link.attr("src");
			if (!urlString.isEmpty()){
				if (!addedImgLinksForOnePage.contains(urlString)){
					URL newUrl = null;
					try {
						String cleansedString = URLDecoder.decode(urlString, CrawlerConstants.CHARSET);
						newUrl = CrawlerUtils.convertToUrl(cleansedString, sourceUrl);		
						this.dao.addNewDocumentMeta(newUrl.toString(), getDocID(newUrl), new Date(), false);
						addedImgLinksForOnePage.add(urlString);
					} catch (IllegalStateException e){
						logger.info(CLASSNAME + " Queue is full, dropping " + newUrl);
						
					} catch (IllegalArgumentException e){
						logger.info(CLASSNAME + " " + e.getMessage() + ", dropping");
					}					
				}
			}
		}
	}
	
	/**
	 * Get the docID from a url. If the url doesn't exist in the database, create a new ID
	 * @param url
	 * @return
	 */
	private long getDocID(URL url){
		// Document hasn't previously been retrieved
		if (this.dao.doesDocumentMetaExist(url.toString())){
			// Previously crawled document
			return this.dao.getDocIDFromURL(url.toString());
		} else {
			// New document
			return counterGenerator.getDocIDAndIncrement();
		}

	}
	
	/**
	 * Store the crawled document in a file
	 * @param contents
	 * @param docID
	 */
	private void storeCrawledContentsFile(String contents, long docID){
		String fileName = Long.toString(docID) + ".txt";
		File storageFile = new File(this.storageDirectory + "/" + fileName);
		BufferedWriter writer = null;
		try {
			if (!storageFile.exists()){
				storageFile.createNewFile();
			}		
			writer = new BufferedWriter(new FileWriter(storageFile.getAbsoluteFile()));
			writer.write(contents);
		} catch (IOException e){
			logger.error(CLASSNAME + ": Unable to store file " + fileName +  ", skipping");
		} finally {
			if (null != writer){
				try {
					writer.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * Appends the new urls to a flat file
	 * @param newUrls
	 * @param sourceUrl
	 */
	private void storeUrlsToFile(List<URL> newUrls, URL sourceUrl){
		String fileName = CrawlerConstants.URL_STORAGE_FILENAME;
		File urlStorageFile = new File(this.urlStorageDirectory + "/" + fileName);
		BufferedWriter writer = null;
		try {
			if (!urlStorageFile.exists()){
				urlStorageFile.createNewFile();
			}
			writer = new BufferedWriter(new FileWriter(urlStorageFile.getAbsoluteFile(), true));
			if (newUrls.size() > 0){
				writer.write(sourceUrl.toString() + "\t");
				for (URL newUrl : newUrls){
					writer.write(newUrl.toString() + ";");
				}				
			}
			writer.write("\n");
			
		} catch (IOException e){
			logger.error(CLASSNAME + ": Unable to store file " + fileName +  ", skipping");
		} finally {
			if (null != writer){
				try {
					writer.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
}
