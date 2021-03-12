/*
 * Created on Sep 28, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package net.sf.sageplugins.sageimdb;

import java.util.Vector;

/**
 * base class for the backend DB engine.
 * 
 * All search items should perform searches in the following order:
 * Search for exact match
 * If none found, search initial strings
 * If none found, search substrings
 * If none found, search approximate strings (optional)
 * 
 * All searches should be case-insensitive
 * 
 * All searches can return either an empty vector or
 * thow a DbNotFoundException if nothing is found.
 * They should not return null.
 * 
 */
public abstract class DbBackend {

	/**
	 * Pass any options to the backend via a String.
	 * 
	 * @param Options
	 */
	abstract public void setOptions(String Options)  throws DbFailureException;
	
	/**
	 * Search for a person. Returns a vector of Role's * 
	 * 
	 * If no exact match, no initial string match, and no substring match 
	 * is found, Person searches should remove the first word from the
	 * beginning of the string, and append it to the end with a comma:
	 * 
	 * "Bruce Willis" becomes "Willis, Bruce"
	 * 
	 * If still nothing is found, Approximate searches (if supported)
	 * should be done first on the supplied string, then on the modified 
	 * string if nothing is found on the supplied string  
	 *
	 */
	public abstract Vector<Role> searchPerson(String person) throws DbNotFoundException,DbFailureException;
	/**
	 * Search for a title. Returns a vector of Role's
	 * 
	 * If no exact match, no initial string match, and no substring match 
	 * is found, Title searches should remove the first words from the
	 * beginning of the string, and append it to the end, with a comma
	 * before any year suffix, then repeat the exact and initial string
	 * and substring searches
	 *  
	 * If still nothing is found, Approximate searches (if supported)
	 * should be done first on the supplied string, then on the modified 
	 * string if nothing is found on the supplied string  
	 * 
	 * so:
	 * "The Truman Show" becomes "Truman Show, The"
	 * "Das Boot" becomes "Boot, Das"
	 * "les 3 tambours (1946)" becomes "3 tambours, Les (1946)"
	 * "garbage search" becomes "search, garbage"
	 * 
	 */
	public abstract Vector<Role> searchTitle(String title) throws DbNotFoundException,DbFailureException;

    /**
     * Search for a TV Episode name. Returns a vector of Role's * 
     * 
     * If no exact match, no initial string match, and no substring match 
     * is found, Person searches should remove the first word from the
     * beginning of the string, and append it to the end with a comma:
     * 
     * "Bruce Willis" becomes "Willis, Bruce"
     * 
     * If still nothing is found, Approximate searches (if supported)
     * should be done first on the supplied string, then on the modified 
     * string if nothing is found on the supplied string  
     *
     */
    public abstract Vector<Role> searchTvEpisode(String Episode) throws DbNotFoundException,DbFailureException;
	
	/**
	 * Search for anything. Returns a vector of Role's
	 */
	public abstract Vector<Role> searchAll(String text) throws DbNotFoundException,DbFailureException;
}
