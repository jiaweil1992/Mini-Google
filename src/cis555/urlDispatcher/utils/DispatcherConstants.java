package cis555.urlDispatcher.utils;

public class DispatcherConstants {
	
	public static final String IP_FORMAT = "\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}";
	public static final int TIMER_TASK_FREQUENCY_MS = 10000;

	
	// GET / PUT URL paths
	
	public static final String STATUS_URL = "status";
	public static final String WORKER_STATUS_URL = "workerstatus";
	
	
	public static final String START_URL = "start";
	public static final String STOP_URL = "stop";
	
	public static final String ADD_URLS_URL = "addUrls";
	
	// URL Parameters
	
	public static final String PORT_PARAM = "port";
	public static final String PAGES_CRAWLED_PARAM = "pagesCrawled";

	public static final String STARTING_URL_PARAM = "startingUrl";
	public static final String CRAWLER_NUMBER_PARAM = "crawlerNumber";
	
	public static final String NEW_URLS_PARAM = "newUrls";

	// Worker Web.xml
	
	public static final String MASTER_KEY_XML = "master";
	public static final String PORT_KEY_XML = "port";
	
}