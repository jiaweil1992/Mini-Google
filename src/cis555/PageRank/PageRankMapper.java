package cis555.PageRank;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

public class PageRankMapper extends Mapper<LongWritable, Text, Text, Text> {

    protected void map(LongWritable key, Text value, Context context)
            throws IOException, InterruptedException {
	Text url = new Text();

	String urlStr = null;
	String urlData = null;

	String line = value.toString();
	Pattern urlFromToPat = Pattern.compile("^([^\t]+)\t(.*)");
	Matcher urlMatcher = urlFromToPat.matcher(line);
	// the structure is docID_from TAB
	// page_rank;numlinks;docID_to1;docID_to2;docID_to3...docID_ton;
	
	if (urlMatcher.matches()) {
	    urlStr = urlMatcher.group(1);
	    urlData = urlMatcher.group(2);
	}

	Pattern urlDataPat = Pattern.compile("(\\d+\\.\\d+)(E-?\\d+)?;(\\d+);(.*)");
	Matcher dataMatcher = urlDataPat.matcher(urlData);
	// page_rank;numlinks;docID_to1;docID_to2;docID_to3...docID_ton;
	if (dataMatcher.matches()) {
	    double pageRank = 0;
	    if (dataMatcher.group(2) == null) {
		System.out.println(urlData + ": not scientific notation");
		pageRank = Double.parseDouble(dataMatcher.group(1));
	    }
	    else {
		System.out.println("very much scientific notation");
		pageRank = Double.valueOf(dataMatcher.group(1) + dataMatcher.group(2));
		System.out.println("\nValue of PG (sci): " + pageRank + "\n");
	    }
	    double numLinks = Double.parseDouble(dataMatcher.group(3));
	    String listOfLinks = dataMatcher.group(4);
	    // System.out.println("\n\n\nfor url " + urlStr + " pageRank is " + pageRank + ", numLinks is " + numLinks + " and these links are " + listOfLinks + "\n\n\n");
	    if (numLinks > 0) {
		DoubleWritable weightTransfer = new DoubleWritable(pageRank / numLinks);
		String[] linkCollection = listOfLinks.split(";");
		for (String linkedTo : linkCollection) {
		    Text linkedToUrl = new Text();
		    linkedToUrl.set(linkedTo);
		    String weightStr = String.valueOf(weightTransfer);
		    context.write(linkedToUrl, new Text(weightStr));
		}
	    }
	    url.set(urlStr);
	    context.write(url, new Text("~" + urlData));
	}
    }
}