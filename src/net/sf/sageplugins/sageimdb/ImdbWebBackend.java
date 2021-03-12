/*
 * Created on Oct 7, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package net.sf.sageplugins.sageimdb;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Vector;

/**
 * @author Owner
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class ImdbWebBackend extends DbBackend {
    static final String IMDB_HOST="www.imdb.com";
	static final int MAX_SEARCH_RESULTS = 200;
/* static final String CONTENT_PAT_STRING = "<div\\s*id=\"main.*?<div\\s*id=\"sidebar\""; */
	static final String CONTENT_PAT_STRING = "<div\\s*id=\"main.*?class=\"article contribute\"";
    static final String INFO_BLOCK_PAT_STRING = "<tr class=\"ipl-zebra-list__item\"[^>]*>\\s*" +
                                                "<td class=\"ipl-zebra-list__label\"[^>]*>\\s*" +
                                                // $1 info title
                                                "(.*?)\\s*</td>\\s*<td>\\s*" + 
                                                // $2 info content
                                                "(.*?)" +
                                                "\\s*</td>\\s*</tr>";

    /* (non-Javadoc)
	 * @see net.sf.sageplugins.sageimdb.DbBackend#setOptions(java.lang.String)
	 */
	public void setOptions(String Options) throws DbFailureException {
		// no options for web backend
	}

    public Vector<Role> searchTvEpisode(String Episode) throws DbNotFoundException, DbFailureException {
        try {
            return ImdbWebSearch.search("find?s=ep&mx="+MAX_SEARCH_RESULTS+"&q="+URLEncoder.encode(Episode,"utf-8"));
        }catch (UnsupportedEncodingException e) {
            throw new DbFailureException(e);
        }
    }
	/* (non-Javadoc)
	 * @see net.sf.sageplugins.sageimdb.DbBackend#searchPerson(java.lang.String)
	 */
	public Vector<Role> searchPerson(String person) throws DbNotFoundException,
			DbFailureException {
		// download and parse page from:
		// https://www.imdb.com/find?nm=on;mx=50;q=Bruce%20Willis
        try {
            Vector<Role> results=ImdbWebSearch.search("find?s=nm&mx="+MAX_SEARCH_RESULTS+"&q="+URLEncoder.encode(person,"utf-8"));
            return results;
            
        }catch (UnsupportedEncodingException e) {
            throw new DbFailureException(e);
        }
	}

	/* (non-Javadoc)
	 * @see net.sf.sageplugins.sageimdb.DbBackend#searchTitle(java.lang.String)
	 */
	public Vector<Role> searchTitle(String title) throws DbNotFoundException,
			DbFailureException {
		// download and parse page from:
		// https://IMDB_HOST/find?tt=on;mx=50;q=die+Hard
        try {
            return ImdbWebSearch.search("find?s=tt&mx="+MAX_SEARCH_RESULTS+"&q="+URLEncoder.encode(title,"utf-8"));
        }catch (UnsupportedEncodingException e) {
            throw new DbFailureException(e);
        }
	}

	/* (non-Javadoc)
	 * @see net.sf.sageplugins.sageimdb.DbBackend#searchAll(java.lang.String)
	 */
	public Vector<Role> searchAll(String text) throws DbNotFoundException,
			DbFailureException {
		//		 download and parse page from:
		// https://IMDB_HOST/find?tt=on;nm=on;mx=20;q=xxx
		try {
            return ImdbWebSearch.search("find?s=all&mx="+MAX_SEARCH_RESULTS+"&q="+URLEncoder.encode(text,"utf-8"));
        }catch (UnsupportedEncodingException e) {
            throw new DbFailureException(e);
        }
	}
}