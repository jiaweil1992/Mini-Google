package cis555.crawler;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import org.apache.log4j.Logger;
	
public class LinkQueuer implements Runnable {
	
	private static final Logger logger = Logger.getLogger(LinkQueuer.class);
	private static final String CLASSNAME = LinkQueuer.class.getName();

	
	private BlockingQueue<URL> preRedistributionNewURLQueue;
	private BlockingQueue<URL> newUrlQueue;
	private int crawlerID;
	private Map<String, String> otherCrawlerDetails;
	public static boolean active;
	
	public LinkQueuer(BlockingQueue<URL> preRedistributionNewURLQueue, BlockingQueue<URL> newUrlQueue,
			int crawlerID, Map<String, String> otherCrawlerDetails){
		this.preRedistributionNewURLQueue = preRedistributionNewURLQueue;
		this.newUrlQueue = newUrlQueue;
		this.crawlerID = crawlerID;
		this.otherCrawlerDetails = otherCrawlerDetails;
		LinkQueuer.active = true;
	}

	@Override
	public void run() {
		while(LinkQueuer.active){
			try {
				URL url = preRedistributionNewURLQueue.take();
				URL filteredURL = filter(url);
				if (null == filteredURL){
					continue;
				}
				
				try {
					if (!this.newUrlQueue.contains(filteredURL)){
						this.newUrlQueue.add(filteredURL);						
					}
				} catch (IllegalStateException e){
					logger.info(CLASSNAME + " New url queue is full, dropping " + filteredURL);					
				}
				
			} catch (InterruptedException | MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Filter out urls with particular characteristics
	 * @param url
	 * @return
	 * @throws MalformedURLException
	 */
	private URL filter(URL url) throws MalformedURLException{
		String urlString = url.toString();
		if (urlString.contains("#")){
			String newUrlString = urlString.substring(0, urlString.indexOf("#"));
			return new URL(newUrlString);
		} else if (urlString.contains("/cgi-bin/")){
			// Ignore all urls containing /cgi-bin/
			return null;
			
		}	else {

			return url;
		}
		
	}
}
