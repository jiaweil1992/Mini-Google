package cis555.crawler;

public class CrawlerConstants {
	
	public static final String CHARSET = "utf-8";
	public static final int QUEUE_CAPACITY = 200000;
	public static final int NUM_HEAD_GET_THREADS = 10;
	public static final int NUM_EXTRACTOR_THREADS = 10;
	public static final int NUM_MATCHER_THREADS = 10;
	
	public static final int THREAD_JOIN_WAIT_TIME = 500;
	public static final int THREAD_SLEEP_TIME = 1000;
	
	public static final String CRAWLER_USER_AGENT = "cis455crawler";
	public static final long DEFAULT_CRAWLER_DELAY_MS = 1000;
	public static final int BYTES_IN_MEGABYTE = 1000000;
	public static final int DEFAULT_CONTENT_LENGTH = Integer.MAX_VALUE;
	
	public static final String[] REDIRECT_STATUS_CODES = {"301", "302", "303", "307"};
	
	public static final String ENCODING = "UTF-8";
	public static final int PORT = 80;
	public static final int SOCKET_TIMEOUT = 10000;

	public static final int MAX_RETRY = 20;
}
