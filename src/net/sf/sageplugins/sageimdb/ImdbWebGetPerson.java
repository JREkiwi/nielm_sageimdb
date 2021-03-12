package net.sf.sageplugins.sageimdb;


import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Container class to get person information.
 * use get() method
 * @author Niel
 *
 */
public class ImdbWebGetPerson {

    /** 
	 * background updater class to get filmographies
	 * @author Niel
	 *
	 */
	static private class UpdateEpisodeFilmog extends ImdbWebBgPersonUpdateThread{
		
	    String pageText;

        static public final Pattern episodePat=Pattern.compile(
                        "<div class=\"filmo-episodes\">\\s*" +
                        // $1 - ref
                        "(?:- )?<a href=\"([^\"]+)\"\\s*" +
                        // $2 - toggleSeeMoreEpisodes parameters
                        "(?:\\s*data-n=\"\\d+\"\\s*onclick=\"toggleSeeMoreEpisodes\\(([^)]*)\\); return false;\")?" +
                        // $3 - Episode name
                        ">(.*?)</a>\\s*" +
                        // $4 - Year
                        "(\\([^)]*\\))?\\s*" +
                        "(?:\\.{3})?\\s*" +
                        // $5 - Role
                        "(.*?)\\s*" +
                        "</div>" );
		
	    UpdateEpisodeFilmog(DbPersonObject obj,ImdbWebObjectRef ref, String pageText) {
	        super(obj,ref,"EpisodeFilmog");
	        this.pageText = pageText;
	        start();
	    }
	    /* (non-Javadoc)
	     * @see net.sf.sageplugins.sageimdb.ImdbWebBgUpdateThread#doUpdate()
	     */
	    void doUpdate() throws Exception {
	        final Pattern contentPat=Pattern.compile(ImdbWebBackend.CONTENT_PAT_STRING,Pattern.CASE_INSENSITIVE);
			Matcher contentMat=contentPat.matcher(pageText);
	        if ( !contentMat.find()){
	        	throw new DbFailureException("Failed to find content in Person Episode Filmography Page");
	        }

	        /* 
			 * Pattern that finds a series block, and puts:
			 *  Series name into $1
			 */
			final Pattern filmogPat=Pattern.compile(
                    "<div class=\"filmo-row [^>]*?>\\s*" +
                    "<span class=\"year_column\">[^<]*</span>\\s*" +
                    // $1 title ref
                    "<b><a href=\"/title/(tt\\d*)[^\"]*\"\\s*>" +
                    // $2 title
                    "([^<]*)</a></b>\\s*" +
                    // $3 type
                    "(\\([^)]*\\))*\\s*<br/>[^<]*" +
                    // $4 -- episodes
                    "((<div class=\"filmo-episodes\">.*?</div>\\s*)+)" +
                    "</div>\\s*" );

	        Matcher filmogMat=filmogPat.matcher(contentMat.group());
	        ArrayList<UpdateMoreEpisodeFilmog> updateThreads=new ArrayList<UpdateMoreEpisodeFilmog>();

	        Filmography tvEpisodesFilmog=null;
	        // copy filmographies, update copy, replace original...
	        // avoids concurrent modification exceptions
	        Vector<Filmography> newFilmographies=new Vector<Filmography>(personObj.filmographies);
        	// loop tho series
        	while ( filmogMat.find()) {
        		String seriesName=ImdbWebPageReader.deHtmlIze(filmogMat.group(2));
        		if (tvEpisodesFilmog == null) {
        		    tvEpisodesFilmog=new Filmography("TV Episodes");
        		}
        		Filmography filmog=new Filmography("TV: " + seriesName);

        		Matcher episodeMat=episodePat.matcher(filmogMat.group(4));
        		// loop thro episodes
        		while ( episodeMat.find()){
        			String toggleSeeMoreEpisodesParameters = episodeMat.group(2);
        			if (toggleSeeMoreEpisodesParameters != null) {
        				// The toggleSeeMoreEpisodes function parameters look like this:
        				//     this,'nm0000662','tt5296406','actor','nm_flmg_act_1',5,'blind','#more-episodes-tt5296406-actor',toggleContent
        				String[] parameters = toggleSeeMoreEpisodesParameters.replaceAll("'", "").split(",");
        				if (parameters.length >= 6) {
        				    updateThreads.add(new UpdateMoreEpisodeFilmog(personObj, ref, personObj.tvSeriesFilmographies.size(), filmog, seriesName,
        				                                                  parameters[1], parameters[2], parameters[3], parameters[4], parameters[5]));
        				}
        		        break;
        			}
        			Role role = createEpisodeRole(seriesName, episodeMat);
                    tvEpisodesFilmog.addRole(role);
                    filmog.addRole(role);
        		}
                personObj.tvSeriesFilmographies.add(filmog);
        	}
        	newFilmographies.add(tvEpisodesFilmog);
        	personObj.filmographies = newFilmographies;

        	Iterator<UpdateMoreEpisodeFilmog> updateThreadIter = updateThreads.iterator();
        	while (updateThreadIter.hasNext()) {
        	    UpdateMoreEpisodeFilmog updateThread = updateThreadIter.next();
        	    updateThread.start();
        	}
	    }

	    static Role createEpisodeRole(String seriesName, Matcher episodeMat) {
	        String imdbRef=episodeMat.group(1);
            String episodeName=ImdbWebPageReader.deHtmlIze(episodeMat.group(3));
            String yearWithParens=episodeMat.group(4);
            String part=ImdbWebPageReader.deHtmlIze(episodeMat.group(5));
            if (yearWithParens != null && !episodeName.startsWith("Episode dated ")) {
                episodeName += " " + yearWithParens;
            }
            return new Role(new ImdbWebObjectRef(
                    DbObjectRef.DB_TYPE_TITLE,
                    seriesName + "\n-- " + episodeName,
                    imdbRef),
                    part);
	    }
	}

	public static class UpdateMoreEpisodeFilmog extends ImdbWebBgPersonUpdateThread{
        
        int         filmographiesIndex;
        Filmography oldFilmog;
        String      seriesName;
        String      nameRef;
        String      titleRef;
        String      category;
        String      refMarker;
        String      startIndex;
        
        UpdateMoreEpisodeFilmog(DbObject obj, ImdbWebObjectRef ref,
                                int filmographiesIndex,
                                Filmography filmog, String seriesName, String nameRef,
                                String titleRef, String category, String refMarker, String startIndex) {
            super(obj,ref,"MoreEpisodeFilmog-"+seriesName);

            this.filmographiesIndex     = filmographiesIndex;
            this.oldFilmog              = filmog;
            this.seriesName             = seriesName;
            this.nameRef                = nameRef;
            this.titleRef               = titleRef;
            this.category               = category;
            this.refMarker              = refMarker;
            this.startIndex             = startIndex;
        }

        /* (non-Javadoc)
         * @see net.sf.sageplugins.sageimdb.ImdbWebBgUpdateThread#doUpdate()
         */
        void doUpdate() throws Exception {
            DbPersonObject obj=(DbPersonObject)this.obj;

            String moreEpisodes = getMoreEpisodes(nameRef, titleRef, category, refMarker, startIndex);
            Matcher episodeMat = UpdateEpisodeFilmog.episodePat.matcher(moreEpisodes);

            // Copy oldFilmog to newFilmog
            Filmography newFilmog = new Filmography(oldFilmog);

            // Add 'more' episodes to newFilmog
            while (episodeMat.find()) {
                newFilmog.addRole(UpdateEpisodeFilmog.createEpisodeRole(seriesName, episodeMat));
            }

            obj.tvSeriesFilmographies.set(filmographiesIndex, newFilmog);
        }
        
        String getMoreEpisodes(String nameRef, String titleRef, String category, String refMarker, String startIndex) throws DbFailureException {
            try {
                URL url=new URL("https",ImdbWebBackend.IMDB_HOST,-1,"/name/"+nameRef+"/episodes/_ajax?title="+titleRef+"&category="+category+"&ref_marker="+refMarker+"&start_index="+startIndex);
    
                ImdbWebPageReader reader=new ImdbWebPageReader(url);
                if ( reader.getResponseCode()!=HttpURLConnection.HTTP_OK) {
                    System.out.println("IMDB ajax query for more episodes failed for "+url+" - "+reader.getResponseCode()+" - "+reader.getResponseMessage());
                    return "";
                }

                return reader.getPageText();

            } catch (Throwable e) {
                throw new DbFailureException(e);
            }
        }
    }

	/**
	 * Background updater class to get Bio page
	 * @author Niel
	 *
	 */
	static private class UpdateBio extends ImdbWebBgPersonUpdateThread{
		
	    
		UpdateBio(DbPersonObject obj,ImdbWebObjectRef ref) {
	        super(obj,ref,"Bio");
	        start();
	    }
	    /* (non-Javadoc)
	     * @see net.sf.sageplugins.sageimdb.ImdbWebBgUpdateThread#doUpdate()
	     */
	    void doUpdate() throws Exception {
	        DbPersonObject obj=(DbPersonObject)this.obj;
	        URL url=getNameURL(ref, "bio");
	        ImdbWebPageReader reader=new ImdbWebPageReader(url);

	        if ( reader.getResponseCode()!=HttpURLConnection.HTTP_OK)
	        	throw new DbFailureException("Failed to get Person Episode Filmography Page: ");
	        
	        final Pattern contentPat=Pattern.compile(ImdbWebBackend.CONTENT_PAT_STRING,Pattern.CASE_INSENSITIVE);
			Matcher contentMat=contentPat.matcher(reader.getPageText());
	        if ( !contentMat.find()){
	        	throw new DbFailureException("Failed to find content in Person Episode Filmography Page");
	        }
	        
	        // Bio sections start with <h5> and end with <br/>
			final Pattern sectionPat=Pattern.compile(
					"<h4 class=\"li_group\">" +
					// $1 section title 
                    "(.*?)\\(\\d+\\)" +
					"</h4>" +
					// $2 section content
					"(.*?)" +
					"(<a name=|<script>)");

            final Pattern overviewRowPat=Pattern.compile(
                            "<tr class=\"[odevn]+\">\\s*" +
                            "<td class=\"label\">" +
                            // $1 label
                            "([^<]*)</td>\\s*" +
                            // $2 value
                            "<td>(.*?)</td>\\s*" +
                            "</tr>" );

			Matcher sectionMat=sectionPat.matcher(contentMat.group());
	        while ( sectionMat.find()){
	        	String title=ImdbWebPageReader.deHtmlIze(sectionMat.group(1));
	        	String sectionValue=sectionMat.group(2);
                if ( title.equalsIgnoreCase("Overview")) {
                    Matcher overviewRowMat = overviewRowPat.matcher(sectionValue);
                    while ( overviewRowMat.find() ) {
                        String label = overviewRowMat.group(1);
                        String mvalue = overviewRowMat.group(2);
                        if ( label.equalsIgnoreCase("Nicknames")) {
        	        		obj.nickname=mvalue.replaceAll("<br>", "\r\n ");
        	        	} 
                        
                        String value = ImdbWebPageReader.deHtmlIze(overviewRowMat.group(2));
                        if ( label.equalsIgnoreCase("Birth Name")) {
        	        		obj.birthname=value;
        	        	} else if ( label.equalsIgnoreCase("Nickname")) {
        	        		obj.nickname=value;
        	        	} else if ( label.equalsIgnoreCase("Height")) {
        	        		obj.height=value;
        	        	}
                    }
	        	} else if ( title.equalsIgnoreCase("Spouse")) {
                    // spouses listed in table...
                    obj.married=ImdbWebPageReader.deHtmlIze(sectionValue.replaceAll("\\s*</tr>\\s*","\r\n"));
	        	} else if ( title.equalsIgnoreCase("Mini bio")) {
	        	    sectionValue = ImdbWebPageReader.deHtmlIze(sectionValue.replaceAll("<br ?/?>","\r\n"));
                    if ( obj.biography == null ) {
                        obj.biography=sectionValue;
                    } else {
                        obj.biography += "\r\n ----\r\n" + sectionValue;
                    }
	        	} else {
	        	    sectionValue = ImdbWebPageReader.deHtmlIze(sectionValue.replaceAll("<br ?/?>","[br]")).replaceAll("\\s*\\[br\\]\\s*", "\r\n ----\r\n");
    	        	if ( title.equalsIgnoreCase("Trade mark")) {
    	        		obj.trademark=sectionValue;
    	        	} else if ( title.equalsIgnoreCase("Trivia")) {
    	        		obj.trivia=sectionValue;
    	        	} else if ( title.equalsIgnoreCase("Personal quotes")) {
    	        		obj.quotes=sectionValue;
    	        	}
	        	}
	        }
	    }
	}

	public static DbPersonObject get(ImdbWebObjectRef ref) throws DbFailureException {
		DbPersonObject obj=new DbPersonObject(ref);
        boolean getEpisodeFilmography=false;

        try {
			final URL url=ImdbWebBgUpdateThread.getNameURL(ref, "?nmdp=1");
			obj.imdbUrl=url.toString();
			
			final ImdbWebPageReader reader=new ImdbWebPageReader(url);
	        if ( reader.getResponseCode()!=HttpURLConnection.HTTP_OK) {
                throw new DbFailureException("Failed to get Person Episode Filmography Page "+url+" - "+reader.getResponseCode()+" - "+reader.getResponseMessage());
	        }
	        // got text, need to get...
			// Name
			// Picture
			// DOB
			// Filmographies
			

			// then launch background updaters for
			// Tv Episode Filmographies
			// Bio

	        
		    final Pattern contentPat=Pattern.compile(ImdbWebBackend.CONTENT_PAT_STRING,Pattern.CASE_INSENSITIVE);
			Matcher matcher = contentPat.matcher(reader.getPageText());
	        if ( !matcher.find()){
	        	throw new DbFailureException("Failed to find content in Person Episode Filmography Page");
	        }
	        String content=matcher.group();

	    	final Pattern dobPat=Pattern.compile(
	    			"bth_monthday\"[^>]*>([^<]*)</a>(,)[^<]*<a.+_bth_year\"[^>]*>([^<]*)</a>");

	    	final Pattern pobPat=Pattern.compile(
	    			"bth_place\"[^>]*>([^<]*)<");

	    	final Pattern dodPat=Pattern.compile(
	    			"dth_monthday\"[^>]*>([^<]*)</a>(,)[^<]*<a.+_dth_year\"[^>]*>([^<]*)</a>");

	    	final Pattern podPat=Pattern.compile(
	    			"dth_place\"[^>]*>([^<]*)<");

	        final Pattern namePat=Pattern.compile(
	    			"<meta property='og:title' content=\"([^\"]*?)\"");
	    	
	    	final Pattern episodeListLinkPat=Pattern.compile(
   	                "<div class=\"filmo-episodes\">");
	    	
	    	final Pattern picturePat=Pattern.compile(
	    			"<meta property='og:image' content=\"([^\"]*?)\"");
	    	
	    	
	    	/** 
	    	 * pattern to match a group of filmographies
	    	 */
	    	final Pattern filmogGroupPat=Pattern.compile(
	    	        "\"filmo-head-[^>]*>.*?" +
	    	        "(?:<a name=\"[^\"]*\"></a>\\s*)?" +
                    // $1 - filmog type
                    "<a name=\"([^\"]*)\"> *" +
                    // $2 - Filmog type printable
                    "(.*?)" +
                    "</div>\\s*" +
                    "<div class=\"filmo-category-section\"[^>]*>\\s*" +
                    // $3 group items
	    			"(<div class=\"filmo-row [odevn]*\" id=\"[^\"]*?\">\\s*" +
	    	        "<span class=\"[^\"]*\">\\s*[^<]*?</span>\\s*" +
	    			"<b>\\s*" +
	    			"<a href=\"[^\"]*\"\\s*>\\s*" +
	    			// $4 title
                    "([^<]*)" +
	    			"</a></b>\\s*" + 
                    // $5 - Filmog type printable
                    "(.*?)" +
                    "\\s*<br/>[^<]*?" +
	    			// $6 -- episodes
//	    			"((<div class=\"filmo-episodes\">\\s*-\\s*<a\\s*href=\"/title/[^\"]*\"[^>]*>\\s*.*?</div>\\s*)*)" +
//	    			"</div>\\s*)*" );
                    "(.*?)" +
                    "(</div>\\s*</div>\\s*<div id=|<script>))" );

	    	/**
	    	 * Pattern to match a filmography item
	    	 */
	    	final Pattern filmogItemPat=Pattern.compile(
	                "<div class=\"filmo-row [^>]*?>\\s*" +
                    // $1 year
	                "<span class=\"year_column\">([^<]*)</span>\\s*" +
	                "<b>\\s*" +
                    // $2 imdbref
	                "<a href=\"([^\"]*)\"\\s*>\\s*" +
                    // $3 title/TV/Credit
	                "(.*?<br/>[^<]*?)" +
	                // $4 -- episodes
	                "((<div class=\"filmo-episodes\">.*?</div>\\s*)*)" +
	                "</div>\\s*" );
	     
			// DOB
	        matcher=dobPat.matcher(reader.getPageText());
	        if ( matcher.find()){
	        	obj.dob=matcher.group(1) + matcher.group(2) + " " + matcher.group(3);
	        }

	        matcher=pobPat.matcher(reader.getPageText());
	        if ( matcher.find()){
	        	obj.pob=matcher.group(1);
	        }

	        matcher=dodPat.matcher(reader.getPageText());
	        if ( matcher.find()){
	        	obj.dod=matcher.group(1) + matcher.group(2) + " " + matcher.group(3);
	        }

	        matcher=podPat.matcher(reader.getPageText());
	        if ( matcher.find()){
	        	obj.pod=matcher.group(1);
	        }

	        matcher=namePat.matcher(reader.getPageText());
	        if ( matcher.find())
	        	obj.name=ImdbWebPageReader.deHtmlIze(matcher.group(1));
	        
	        matcher=picturePat.matcher(reader.getPageText());
	        if ( matcher.find())
	        	try {
	        		obj.imageURL=new URL(matcher.group(1));
	        	} catch (MalformedURLException e) {
	        		System.out.println("IMDB invalid image URL for "+obj.name+" - "+matcher.group(1));
	        	}
		    matcher=episodeListLinkPat.matcher(reader.getPageText());
		    getEpisodeFilmography=matcher.find();
		        

	    	/**
	    	 * Pattern to get the details for an item within a group
	    	 */
	    	final Pattern filmogItemDetailsPat=Pattern.compile(
	    			// $1 title
	    			"^([^<]*)</a></b>\\s*" +
	    	        // $2 optional (TV Series/Mini Series/etc)
	    	        "(\\([^)]*\\))?\\s*" +
	    			// $3 optional imdbpro info
	    			"(\\(<a href=\"https?://pro.imdb.com/[^\"]*\">.*?</a>\\))?\\s*" +
	    			// $4 optional (credit)
	    			"(\\([^)]*\\))?\\s*" +
	    			// $5 optional episode info
	    			"(\\s*\\(\\d+ episodes?, [^)]*\\))?" +
	    			// $6 optional extra info -- episode, aka etc
	    			"(\\s*<br/>.*)?");

            final Pattern filmogEpisodeCountPat = Pattern.compile(
                    "<div class=\"filmo-episodes\">\\s*<a href=\"#\" data-n=\"(\\d+)\"" );
	    	final Pattern filmogDetailsEpisodePat = Pattern.compile(
	    	        "<div class=\"filmo-episodes\">.*?</div>\\s*" );
	    	
	        // Filmographies.
	        Matcher filmogGroupMatcher=filmogGroupPat.matcher(content);
	        while (filmogGroupMatcher.find()){
	        	// got a filmography group
	        	Filmography filmog=new Filmography(ImdbWebPageReader.deHtmlIze(filmogGroupMatcher.group(2)));
	        	obj.filmographies.add(filmog);

	        	// now look at each filmography item 
	        	Matcher filmogItemMatcher=filmogItemPat.matcher(filmogGroupMatcher.group());
	        	while ( filmogItemMatcher.find()) {
                    // $1 is year
	        	    String year = ImdbWebPageReader.deHtmlIze(filmogItemMatcher.group(1)).trim();
                    // $2 is ref
	        		String imdbRef=filmogItemMatcher.group(2);
	        		int urlRequestOffset = imdbRef.indexOf( '?' );
	        		if ( urlRequestOffset >= 0 && imdbRef.startsWith( "/title/tt" ) )
	        		{
	        		    imdbRef = imdbRef.substring( 0, urlRequestOffset ) /* + "reference" */;
	        		}
	        		// $3 is info -- to be parsed
        			Matcher itemDetailsMatcher=filmogItemDetailsPat.matcher(filmogItemMatcher.group(3));
        			if ( itemDetailsMatcher.find()) {
        				// $1 title, $2 (TV Series/etc), $6 Credit

        				String title=ImdbWebPageReader.deHtmlIze(itemDetailsMatcher.group(1));
        				if (!year.isEmpty())
        					title=title+" ("+year+")";
        				if (itemDetailsMatcher.group(2) !=null )
        					title=title+" "+ImdbWebPageReader.deHtmlIze(itemDetailsMatcher.group(2));
        				String rolePlayed;
        				if ( itemDetailsMatcher.group(6) != null )
        					rolePlayed=ImdbWebPageReader.deHtmlIze(itemDetailsMatcher.group(6));
        				else
        					rolePlayed="";

        				// check for episodes...
        				int episodeCount = 0;
        				String episodeTags = filmogItemMatcher.group(4);
                        if (episodeTags.length() > 0) {
                            Matcher episodeCountMatcher = filmogEpisodeCountPat.matcher(episodeTags);
                            
                            if (episodeCountMatcher.find()) {
                                episodeCount = Integer.parseInt(episodeCountMatcher.group(1));
                            } else {
                                Matcher episodeMatcher = filmogDetailsEpisodePat.matcher(episodeTags);
                                while ( episodeMatcher.find() ) {
                                    episodeCount++;
                                }
                            }
                            title += " (" + episodeCount + " episode" + (episodeCount > 1 ? "s" : "") + ")";
        				}

                        Role role=new Role(new ImdbWebObjectRef(
    							DbObjectRef.DB_TYPE_TITLE,
    							title,
    							imdbRef),
    							rolePlayed);
    					filmog.addRole(role);
        			} else {
        				System.out.println("Failed to match filmography details for "+obj.name+" - "+filmogItemMatcher.group(3));
        			}
	        	}
	        }

            // launch BG updaters
	        if (getEpisodeFilmography)
	            new UpdateEpisodeFilmog(obj,ref, reader.getPageText());
            new UpdateBio(obj,ref);

        } catch (Throwable e){
        	System.out.println(e);
        	e.printStackTrace();
        	throw new DbFailureException("Failed to get details for person ID "+ref.getImdbRef(),e);        	
        }

        return obj;
	}

    static void collapseEpisodes(DbPersonObject obj) {
        // Create a new filmographies vector which starts with the non-episode filmographies.
        // Add a new "TV Episodes" filmography which combines all "TV: <SeriesName>" filmographies.
        // Point to the new filmographies vector.  This avoids concurrent modification exceptions.
        Vector<Filmography> newFilmographies=new Vector<Filmography>();
        for (int filmographyIndex = 0; filmographyIndex < obj.filmographies.size(); filmographyIndex++) {
            Filmography filmography=obj.filmographies.get(filmographyIndex);
            if (filmography.getType().equals("TV Episodes")) {
                break;
            }
            newFilmographies.add(filmography);
        }
        Vector<Role> roles=new Vector<Role>();
        for (int filmographyIndex = 0; filmographyIndex < obj.tvSeriesFilmographies.size(); filmographyIndex++) {
            Filmography filmography=obj.tvSeriesFilmographies.get(filmographyIndex);
            roles.addAll(filmography.getRoles());
        }
        Filmography episodesFilmography = new Filmography("TV Episodes (" + roles.size() + ")");
        episodesFilmography.addRoles(roles);
        newFilmographies.add(episodesFilmography);
        obj.filmographies=newFilmographies;
        obj.tvSeriesFilmographies=null;
    }
}
abstract class ImdbWebBgPersonUpdateThread extends ImdbWebBgUpdateThread {
    protected DbPersonObject personObj;

    protected ImdbWebBgPersonUpdateThread(DbObject obj, ImdbWebObjectRef ref, String name) {
        super(obj, ref, name);
        personObj = (DbPersonObject) obj;
    }
    /**
     * Method to get and parse the data in the background.
     * @throws Exception
     */
    abstract void doUpdate() throws Exception;

    protected void preNotify() {
        if (obj.numBgUpdatesInProgress == 1) {
            ImdbWebGetPerson.collapseEpisodes(personObj);
        }
    }
}
