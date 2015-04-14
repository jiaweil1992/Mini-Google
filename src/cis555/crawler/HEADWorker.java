package cis555.crawler;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import cis555.crawler.database.CrawledDocument;
import cis555.crawler.database.Dao;


/**
 * Responsible for doing HEAD requests to determine whether URL satisfies criteria
 * @author cis455
 *
 */
public class HEADWorker implements Runnable {
	
	private static final Logger logger = Logger.getLogger(HEADWorker.class);
	private static final String CLASSNAME = HEADWorker.class.getName();
	
	private ConcurrentHashMap<String, SiteInfo> siteInfoMap;
	private BlockingQueue<URL> headCrawlQueue;
	private BlockingQueue<URL> getCrawlQueue;
	private BlockingQueue<URL> newUrlQueue; // Only used for re-directs
	private int id;
	private int maxDocSize;
	private Dao dao;
	private Vector<URL> sitesCrawledThisSession;
	
	public HEADWorker(ConcurrentHashMap<String, SiteInfo> siteInfoMap, BlockingQueue<URL> headCrawlQueue, Dao dao, BlockingQueue<URL> getCrawlQueue, 
			int id, int maxDocSize, BlockingQueue<URL> newUrlQueue, Vector<URL> sitesCrawledThisSession){
		this.siteInfoMap = siteInfoMap;
		this.headCrawlQueue = headCrawlQueue;
		this.dao = dao;
		this.getCrawlQueue = getCrawlQueue;
		this.id = id;
		this.maxDocSize = maxDocSize;
		this.newUrlQueue = newUrlQueue;
		this.sitesCrawledThisSession = sitesCrawledThisSession;
	}
	
	
	@Override
	public void run() {
		while (GETWorker.active){

			URL url = null;
			
			try {
				url = headCrawlQueue.take();
								
				String domain = url.getHost();
				
				if (!siteInfoMap.containsKey(domain)){
					// Missing Site info ... return to the new URL queue
					
					logger.info(CLASSNAME + ": Missing site info map for " + domain);
					this.newUrlQueue.add(url);
					continue;
				}
				
				SiteInfo info = siteInfoMap.get(domain);		
				String agentName = CrawlerUtils.extractAgent(info);
				
				if (info.canCrawl(agentName)){
					crawl(info, domain, url);
				} else {
					// Returning to the queue
					headCrawlQueue.add(url);
				}
				
			} catch (CrawlerException e){
				logger.debug(CLASSNAME + ": URL rejected because " + e.getMessage());
				
			}	catch (InterruptedException e) {
				logger.error(CLASSNAME + ": Unabble to get URL");
				logger.error(CLASSNAME + e.getMessage());
			} catch (IOException e){
				System.out.println("Unable to crawl " + url + ", skipping." );
			}

		}
		logger.info(CLASSNAME + ": HEADWorker " + this.id + " has shut down");
	}
	
	/**
	 * Performs a HEAD request, and queues the result appropriately
	 * Also updates the site info map with the latest crawl time
	 * @param info
	 * @param url
	 * @param url
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	private void crawl(SiteInfo info, String domain, URL url) throws MalformedURLException, IOException{
		
		Response response = new Response();
		
		String ifModifiedSinceString = generateIfModifiedSinceString(url);
		
		if (url.toString().startsWith("https")){
			response = CrawlerUtils.retrieveHttpsResource(url, CrawlerUtils.Method.HEAD, ifModifiedSinceString);
		} else {
			response = CrawlerUtils.retrieveHttpResource(url, CrawlerUtils.Method.HEAD, ifModifiedSinceString);
		}
		
		updateSiteInfo(info, domain);
		// Now deal with response
		
		if (isRedirected(response)){
			URL redirectedURL = response.getLocation();
			// Adds the URL to the new URL queue
			logger.debug(CLASSNAME + ": Redirected URL " + redirectedURL + " added to newUrlQueue");
			this.newUrlQueue.add(redirectedURL);
		} 	else if (isNotModified(response)){
			System.out.println(CLASSNAME + ": " + url + " not modified, using database version");
			
			String contents = retrieveDocumentFromDatabase(url);
			
			if (!contents.isEmpty()){ // This means that it's an HTML document, not an XML document			
				addLinksToQueue(contents, url);
			}
			
			if (CrawlLimitCounter.getCounterAndDecrement() < 0){
				logger.info(CLASSNAME + ": Crawl limit reached");
				GETWorker.active = false;
			}
			
		} else if (isTooBig(response.getContentLength())){
			System.out.println("File exceeds maximum file length " + response.getContentLength() + ", skipping");
		} else if (!isValidResponseType(response)){
			logger.debug(CLASSNAME + ": URL " + url + " ignored as is neither HTML nor XML");
		} else {
			
			this.getCrawlQueue.add(url);
			
			this.sitesCrawledThisSession.add(url);
//			System.out.println("Adding " + url + " to the GetCrawlQueue");

		}
	}
	
	/**
	 * Checks if response is redirected or not
	 * @param response
	 * @return
	 */
	private boolean isRedirected(Response response){
		String responseCode = response.getResponseCode();
		if (Arrays.asList(CrawlerConstants.REDIRECT_STATUS_CODES).contains(responseCode)){
			if (null != response.getLocation()){
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Determines whether the content type of the response is either HTML or XML
	 * @param response
	 * @return
	 */
	private boolean isValidResponseType(Response response){
		return (response.getContentType() == Response.ContentType.HTML || response.getContentType() == Response.ContentType.XML);
	}
		
	/**
	 * Ensures that the size of the response will be smaller than the maximum tolerated document size
	 * @param response
	 */
	private boolean isTooBig(int responseSize){
		// If response doesn't have a content length then ... response size would be 0
		
		int maxDocSizeInBytes = maxDocSize * CrawlerConstants.BYTES_IN_MEGABYTE;
		return (responseSize > maxDocSizeInBytes);
		
	}
	
	
	/**
	 * Updates the site info with the newest crawl date, and puts it back into the siteInfoMap
	 * @param info
	 * @param crawlDate
	 * @param domain
	 */
	private void updateSiteInfo(SiteInfo info, String domain){
		info.setLastCrawledDate(new Date());
		this.siteInfoMap.put(domain, info);
	}
	
	/**
	 * Add links from a document to the queue for future crawling
	 * @param contents
	 * @param url
	 */
	private void addLinksToQueue(String contents, URL url){
		List<URL> links = CrawlerUtils.extractUrls(contents, url);
		for (URL link : links){
//			logger.info("Added " + link + " to the new url queue");
			this.newUrlQueue.add(link);
		}
		
	}
	
	/**
	 * Retrieve a document from the database, if the document is HTML
	 * @param url
	 * @return
	 */
	private String retrieveDocumentFromDatabase(URL url){
		
		if (this.sitesCrawledThisSession.contains(url)){
			
			// We've already crawled this site, and added the links, so we don't want to add them again
			
			return "";
		}
		
		CrawledDocument document = this.dao.getCrawledDocument(url.toString());
		if (document.getContentType().equals("HTML")){
			return document.getContents();
		}
		else {		
			// Not an HTML document, so ignore
			return "";
		}
	}
	
	
	/**
	 * Generates an "If-Modified-Since: " string if file already exists in the database. If not, returns empty string
	 * @param url
	 * @return
	 */
	private String generateIfModifiedSinceString(URL url){
		
		if (!this.dao.doesDocumentExist(url.toString())){
			return "";
		}
		
		CrawledDocument document = this.dao.getCrawledDocument(url.toString());
		Date lastCrawlDate = document.getLastCrawledDate();
		SimpleDateFormat format = new SimpleDateFormat();
		format.applyPattern("EEE, dd MMM yyyy HH:mm:ss zzz");
		String dateString = format.format(lastCrawlDate);
		
//		logger.info(CLASSNAME + ": " + url + " already exists in database with latest crawl date " + dateString);
		return dateString;
		
	}
	
	/**
	 * Checks whether it's a not-modified response
	 * @param response
	 * @return
	 */
	private boolean isNotModified(Response response){
		String responseCode = response.getResponseCode();
		return responseCode.equals("304");
	}
		
}