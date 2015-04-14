package cis555.crawler;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import cis555.crawler.database.Dao;

public class GETWorker implements Runnable {
	
	private static final Logger logger = Logger.getLogger(GETWorker.class);
	private static final String CLASSNAME = GETWorker.class.getName();
	
	private ConcurrentHashMap<String, SiteInfo> siteInfoMap;
	private BlockingQueue<URL> crawlQueue;
	private BlockingQueue<URL> newUrlQueue;
	private int id;
	private Dao dao;
	protected static boolean active = true;
	
	public GETWorker(ConcurrentHashMap<String, SiteInfo> siteInfoMap, BlockingQueue<URL> crawlQueue, 
			BlockingQueue<URL> newUrlQueue, int id, Dao dao){
		this.siteInfoMap = siteInfoMap;
		this.crawlQueue = crawlQueue;
		this.newUrlQueue = newUrlQueue;
		this.id = id;
		this.dao = dao;
	}
	
	
	@Override
	public void run() {
		while (GETWorker.active){

			URL url = null;
			
			try {
				
				url = crawlQueue.take();
				
				String domain = url.getHost();
				
				if (!siteInfoMap.containsKey(domain)){
					// Missing Site info ... return to the new URL queue
					this.newUrlQueue.add(url);
					continue;
				}
				
				SiteInfo info = siteInfoMap.get(domain);		
				String agentName = CrawlerUtils.extractAgent(info);
				
				if (info.canCrawl(agentName)){
					crawl(info, domain, url);
				} else {
					// Need to wait 
					this.crawlQueue.add(url);
				}

				
			} catch (InterruptedException e) {
				logger.error(CLASSNAME + ": Unabble to get URL");
				logger.error(CLASSNAME + e.getMessage());
			} catch (IOException e){
				System.out.println("Unable to crawl " + url + ", skipping." );
			}

		}
		logger.info(CLASSNAME + ": GETWorker " + this.id + " has shut down");
	}
		
	/**
	 * Does a crawl, extracts links, updates with new links, and updates the site info map
	 * @param info
	 * @param domain
	 * @param url
	 * @throws IOException 
	 * @throws MalformedURLException 
	 */
	private void crawl(SiteInfo info, String domain, URL url) throws MalformedURLException, IOException{
		
		
		try {
			Response response = new Response();
			String ifModifiedSinceString = ""; // Not relevant for GET requests
			
			if (url.toString().startsWith("https")){
				response = CrawlerUtils.retrieveHttpsResource(url, CrawlerUtils.Method.GET, ifModifiedSinceString);
			} else {
				response = CrawlerUtils.retrieveHttpResource(url, CrawlerUtils.Method.GET, ifModifiedSinceString);
			} 
			
			updateSiteInfo(info, domain);
			
			String contents = response.getResponseBody();
			
			if (contents.isEmpty()){
				System.out.println("Unable to crawl " + url + ", skipping." );
				return;
			}
			System.out.println("Crawled " + url);				
			String contentType = response.getContentType().name();
			
			// Let Dao know that the last crawl date has changed
			this.dao.setLastCrawlDate(new Date());
			
			this.dao.addNewCrawledDocument(url.toString(), contents, new Date(), contentType);
			
			if (CrawlLimitCounter.getCounterAndDecrement() < 0){
				logger.info(CLASSNAME + ": Crawl limit reached");
				GETWorker.active = false;
			}
			
			if (response.getContentType() == Response.ContentType.HTML){
				addLinksToQueue(contents, url);
			}
				
		} catch (CrawlerException e){
			System.out.println("Unable to crawl " + url + " because of " + e.getMessage() + ", skipping." );
		} 
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
	 * Updates the site info with the newest crawl date, and puts it back into the siteInfoMap
	 * @param info
	 * @param crawlDate
	 * @param domain
	 */
	private void updateSiteInfo(SiteInfo info, String domain){
		info.setLastCrawledDate(new Date());
		this.siteInfoMap.put(domain, info);
	}
	
}