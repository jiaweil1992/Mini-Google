package cis555.PageRank;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;


public class NumLinksMapper extends Mapper<LongWritable, Text, IntWritable, Text> {

    private Text url = new Text();
    private String linksStr = null;
    
    protected void map(LongWritable key, Text value, Context context)
            throws IOException, InterruptedException {
	
	    String line = value.toString();
	    // System.out.println(line);
	    // below assumes good input
	    // Pattern urlFromTo = Pattern.compile("^([^\t]+)\t(.*)");
	    Pattern urlFromTo = Pattern.compile("^([A-F0-9]{32})\t(.*)");
	    Matcher urlMatcher = urlFromTo.matcher(line);
	    if (urlMatcher.matches()) {
		// System.out.println("urlMatcher.group(1): " + urlMatcher.group(1));
		// System.out.println("urlMatcher.group(2): " + urlMatcher.group(2));
		url.set(urlMatcher.group(1));
		linksStr = urlMatcher.group(2);
	    }
	    else {
		System.out.println("\n\n\nno good: " + line + "\n\n\n");
	    }
	    
	    int numLinks = 0;
	    if (!linksStr.equals("")) {
		String[] linkCollection = linksStr.split(";");
		numLinks = linkCollection.length;
	    }
	    
	    context.write(new IntWritable(numLinks), url);
    }
}