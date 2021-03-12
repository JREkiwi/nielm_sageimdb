package net.sf.sageplugins.sageimdb;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


//import net.sf.sageplugins.sageimdb.ImdbWebBackend;
//import net.sf.sageplugins.sageimdb.ImdbWebObjectRef;
import net.sf.sageplugins.sageutils.Translate;

public class ImdbWebSearch {

	static ImdbWebSearchCache searchCache = new ImdbWebSearchCache();
	
	// precompiled regexps...
//	static Pattern contentPat=Pattern.compile("<div *id=\"pagecontent\".*?<div *?id=\"footer\"",Pattern.CASE_INSENSITIVE);
	static Pattern contentPat=Pattern.compile("<div *id=\"pagecontent\".*?<div *?id=\"sidebar\"",Pattern.CASE_INSENSITIVE);
	static Pattern patResult=Pattern.compile("<td.*?>\\s*<a href=\"(/title|/name)[^>]*?>[^<].*?</td>",Pattern.CASE_INSENSITIVE);
	static Pattern patTitle = Pattern.compile("<a href=\"(/title/tt[^/]+?/)[^\"]*?\"[^>]*?>([^<]+?)</a>([^<]*)<",Pattern.CASE_INSENSITIVE);
    static Pattern patEpisode = Pattern.compile("<a href=\"(/title/tt[^/]+?/)[^\"]*?\"[^>]*?>([^<]+?)</a>([^<]*)<br>(.*?)</td>", Pattern.CASE_INSENSITIVE);
	static Pattern patPerson = Pattern.compile("<a href=\"(/name/nm[^/]+?/)[^\"]*?\"[^>]*?>([^<]+?</a>[ ()IVX]+?)(<small>.*?</small>)",Pattern.CASE_INSENSITIVE);
	static Pattern moreResPat=Pattern.compile("<a +href=\"/(find?[^\"]+?)\".*?>See all the matches</a>",Pattern.CASE_INSENSITIVE);
	
	static Vector<Role> search(String searchstring) throws DbFailureException{
		Vector<Role> results;
	    results=searchCache.getSearch(searchstring);
	    if ( results==null) {
	    	results=new Vector<Role>();
			search(searchstring,results);
	    	if ( results != null && results.size()>0 )
	    		searchCache.addSearch(searchstring,results);
	    }
		return results;
	}

	static private void search(String searchstring,Vector<Role> results) throws DbFailureException {
		URL url;
		try {
			url=new URL("https://"+ImdbWebBackend.IMDB_HOST+"/"+searchstring);
		} catch (MalformedURLException e){
			throw new DbFailureException(e.toString(),e);
		}
		ImdbWebPageReader webReader;
		try {
			webReader=new ImdbWebPageReader(url);
		} catch (IOException e) {
			throw new DbFailureException("Failed to get URL "+ url + " - " +e);
		}

		switch (webReader.getResponseCode()) {
		case HttpURLConnection.HTTP_OK:
			// OK
			break;

		case HttpURLConnection.HTTP_MOVED_PERM:
		case HttpURLConnection.HTTP_MOVED_TEMP:
			// redirect to movie/character name... 
			String location=webReader.getHeaderField("Location");
			String searchText="";
			try {
				searchText=URLDecoder.decode(searchstring.replaceAll(".*q=",""),"utf-8");
			} catch (UnsupportedEncodingException e) {
				System.out.println("IMDB Web:"+ e.toString());
			}
				
			if ( location != null ) {
				if ( location.matches("^https?://"+ImdbWebBackend.IMDB_HOST+"/name/nm")) {
					results.add(
							new Role(
									new ImdbWebObjectRef(
											DbObjectRef.DB_TYPE_PERSON,
											searchText,
											location.replace("^https?://"+ImdbWebBackend.IMDB_HOST+"(/name/nm[^/]+/).*","$1")),
							""));
					return;
				} 
				else if ( location.matches("^https?://"+ImdbWebBackend.IMDB_HOST+"/title/tt")) {
					results.add(
							new Role(
									new ImdbWebObjectRef(
											DbObjectRef.DB_TYPE_TITLE,
											searchText,
											location.replace("^https?://"+ImdbWebBackend.IMDB_HOST+"(/title/tt[^/]+/).*","$1")),
							""));
					return;
				}
			}
			// if we got here, no valid results from redirect
			throw new DbFailureException("Search failed, redirected to location="+location);

		default:
			throw new DbFailureException("Search failed with status "+webReader.getResponseCode()+" "+webReader.getResponseMessage());
		}


		// if we got here, then HTTP_OK was returned, and we should have some text 


		// Main content is between
		// <table id="outerbody"
		// and
		// <div id="footer"
		Matcher contentMatch=contentPat.matcher(webReader.getPageText());
		if ( ! contentMatch.find() )
			throw new DbFailureException("Failed to find search results content in "+url);

		String content=contentMatch.group();

		// matches are: 
		// <a href="/title/tt0371881/">sdadadsa (2003)</a>
		Matcher matResult=patResult.matcher(content);

		while( matResult.find() && results.size()<ImdbWebBackend.MAX_SEARCH_RESULTS){
			String res=matResult.group();
			String type=matResult.group(1);
            String name1;
            String name2;
			if ( type.equals("/title")) {
                if (searchstring.startsWith("find?s=ep")) {
				     Matcher matchepisode=patEpisode.matcher(res);
				     if ( matchepisode.find()){
					     name1=Translate.decode(
						      matchepisode.group(2).trim()+
						      matchepisode.group(3));
                         name2=Translate.decode(ImdbWebPageReader.deHtmlIze(
                              matchepisode.group(4).trim()));
                         String name = name1 + name2;
					     results.add(new Role ( new ImdbWebObjectRef(
						      DbObjectRef.DB_TYPE_TITLE,
						      Translate.decode(name),
						      matchepisode.group(1)),
					     ""));
                    }
                } else {
                    Matcher matchtitle = patTitle.matcher(res);
                    if (matchtitle.find()) {
                        name1 = Translate.decode(matchtitle.group(2).trim() + matchtitle.group(3).trim());
                        results.add(new Role(new ImdbWebObjectRef(1, Translate.decode(name1), matchtitle.group(1)), ""));
                    } else {
                        System.out.println("IMDB failed search title match: " + res);
                    }
				}
			} else if ( type.equals("/name")) {
				Matcher matchperson=patPerson.matcher(res);
				if ( matchperson.find()) {
					String name=Translate.decode(
							matchperson.group(2)
							.replaceAll("<[^>]+>","")
							.trim());
					String part=Translate.decode(
							matchperson.group(3)
							.replaceAll("<[^>]+>","")
							.trim());
					results.add(new Role ( new ImdbWebObjectRef(DbObjectRef.DB_TYPE_PERSON,
							Translate.decode(name),
							matchperson.group(1)),
							part));
				} else {
					System.out.println("IMDB failed search person match: "+res);
				}
			}
		} // loop thro matches

		if ( results.size()<ImdbWebBackend.MAX_SEARCH_RESULTS ) {
			// if not many results, and 'more matches' present in search results, search more
			Matcher moreResMat=moreResPat.matcher(content);
			if ( moreResMat.find()){
				search(moreResMat.group(1),results);
			}
		}
	}
}
