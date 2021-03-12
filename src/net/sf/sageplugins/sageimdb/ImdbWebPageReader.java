package net.sf.sageplugins.sageimdb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.sageplugins.sageutils.Translate;


public class ImdbWebPageReader implements Serializable {
	
	private static final long serialVersionUID = -5581656822643451965L;
    public static boolean printTimings = false; // TODO Get this from server configuration 
	
	// setup and use cache file for debugging to avoid multiple page gets from IMDB 
	static final boolean USE_CACHE=false;
	static final File CACHE_FILE=new File("imdbwebsearch.cache");
	static Map<URL, ImdbWebPageReader> cache;
	static {
		if ( USE_CACHE ) {
			if ( CACHE_FILE.canRead()) {
				System.out.println("Reading web page cache");
				// read cache
				try {
					ObjectInputStream ois=new ObjectInputStream(new FileInputStream(CACHE_FILE));
					Map<URL, ImdbWebPageReader> readObject = extracted(ois);
					cache=readObject;
					ois.close();
				} catch (Exception e){
					System.out.println(e);
					cache=null;
				}
			} else {
				cache=new java.util.HashMap<URL, ImdbWebPageReader>();
			}
		} else {
			cache=null;
		}
	}

	@SuppressWarnings("unchecked")
	private static Map<URL, ImdbWebPageReader> extracted(ObjectInputStream ois)
			throws IOException, ClassNotFoundException {
		return (Map<URL, ImdbWebPageReader>)ois.readObject();
	}
	
	// variables for throttling
	// throttles requests for new title/person pages
	// so that 1 page every 10 secs is requested
	static Object lastReqMutex=new Object();
	static long lastReqTime=0;
	static final long MIN_REQ_INTERVAL=10000L;
	static String lastReqObjId=null; 
	
	private final int responseCode;
	private final String responseMessage;
	private final Map headerFields;
	private final String pageText;
	
	/**
	 * Constructor reads entire webpage into a single string.
	 * All CR/LF characters are stripped.
	 * 
	 * Requests are throttled
	 * 
	 * @param url
	 * @throws IOException
	 */
	ImdbWebPageReader(URL url) throws IOException {
		ImdbWebPageReader cached=null;
		if ( USE_CACHE && cache!= null ){
			synchronized (cache) {
				cached=cache.get(url);
			}
		}
		if ( cached != null){
			System.out.println("IMDB cached URL:"+url.toString());
			this.responseCode=cached.responseCode;
			this.headerFields=cached.headerFields;
			this.pageText=cached.pageText;
			this.responseMessage=cached.responseMessage;
		} else {
			System.out.println("IMDB getting URL:"+url.toString());
			
			synchronized (lastReqMutex) {
				Pattern pat=Pattern.compile(".*(/[a-z]{2}[0-9]{4,}/).*");
				Matcher mat=pat.matcher(url.getPath());
				if ( mat.matches() ){
					// throttle requests for people/titles...
					String objId=mat.group(1);
					long now=System.currentTimeMillis();
					if ( lastReqObjId==null)
						lastReqObjId=objId;
				
					if ( now-lastReqTime<MIN_REQ_INTERVAL && !objId.equals(lastReqObjId) ){
						try {
							long waitTime=lastReqTime+MIN_REQ_INTERVAL-now;
							System.out.println("Throttling "+waitTime+" for "+objId);
							Thread.sleep(waitTime);
						} catch (InterruptedException e){}
					} else {
						System.out.println("not Throttling for "+objId);
					}
					lastReqTime=System.currentTimeMillis();
					lastReqObjId=objId;
				}
			}
			
			HttpURLConnection connection;
			long startTime = System.currentTimeMillis();
			connection=(HttpURLConnection)url.openConnection();
			connection.setInstanceFollowRedirects(false);
			connection.addRequestProperty("User-Agent","sageimdb/"+SageIMDB.VERSION+" ");
			connection.setDefaultUseCaches(true);
			connection.setUseCaches(true);
			responseCode=connection.getResponseCode();
			responseMessage=connection.getResponseMessage();
			headerFields=connection.getHeaderFields();
	
			BufferedReader br = 
				new BufferedReader(
						new InputStreamReader(
								connection.getInputStream(), "UTF-8"));
			StringBuffer buf=new StringBuffer();
			String line;
			while (null != (line = br.readLine())) {
				buf.append(line);
				buf.append(' ');
			}
			br.close();
			connection.disconnect();
			if (printTimings) {
			    long stopTime = System.currentTimeMillis();
			    System.out.println("IMDB web request took " + ( stopTime - startTime ) + " ms. for "+url);
			}
			pageText=buf.toString();
			
			// write updated cache
			if ( USE_CACHE && cache!= null ){
				synchronized (cache) {
					cache.put(url, this);
					try {
						System.out.println("Writing web page cache");
						ObjectOutputStream oos=new ObjectOutputStream(new FileOutputStream(CACHE_FILE));
						oos.writeObject(cache);
						oos.close();
					} catch (Exception e){
						System.out.println(e);
						cache=null;
					}
				}					

			}
		}
	}
		
	String getPageText() {
		return pageText;
	}
	
	String getResponseMessage() {
		return responseMessage;
	}	
	int getResponseCode() {
		return responseCode;
	}
	
	String getHeaderField(String Header){
		return (String)(((List)headerFields.get(Header)).get(0));
	}
	List getHeaderFields(String Header){
		return ((List)headerFields.get(Header));
	}
	
	static String deHtmlIze(String input){
		Pattern deHtmlPat=Pattern.compile("<[^>]+>");
		if (input== null)
			return "";
		String out=deHtmlPat.matcher(input).replaceAll("");
		out=out.replaceAll("&#160;", " ");
		out=out.replaceAll("&#[Xx][Aa]0;", " ");
		out=out.replaceAll("&nbsp;", " ");
		out=Translate.decode(out);
		out=out.replaceAll("[\t \f]+", " ");
		out=out.trim();
		return out;
	}
}
