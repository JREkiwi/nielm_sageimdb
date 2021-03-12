package net.sf.sageplugins.sageimdb;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.sageplugins.sageutils.Translate;



public class ImdbWebGetTitle {

	
	/** 
	 * background updater class to get episodes
	 * @author Niel
	 *
	 */
	static class UpdateEpisodes extends ImdbWebBgUpdateThread{
		private int season;
		private Vector[] seasons;
	    UpdateEpisodes(DbTitleObject obj,ImdbWebObjectRef ref,int season,Vector[] seasons) {
	        super(obj,ref,"Episodes");
	        this.season = season;
	        this.seasons = seasons;
	        start();
	    }
	    /* (non-Javadoc)
	     * @see net.sf.sageplugins.sageimdb.ImdbWebBgUpdateThread#doUpdate()
	     */
	    @SuppressWarnings("unchecked")
		void doUpdate() throws Exception {
	        Vector<Role> episodes=new Vector<Role>();
	        DbTitleObject obj=(DbTitleObject)this.obj;
	        URL url=new URL("https",ImdbWebBackend.IMDB_HOST,-1,ref.getImdbRef()+"episodes?season="+season);
	        ImdbWebPageReader reader=new ImdbWebPageReader(url);
	        if ( reader.getResponseCode()!=HttpURLConnection.HTTP_OK)
	        	throw new DbFailureException("Failed to get Title Episode List Page "+url+" - "+reader.getResponseCode()+" - "+reader.getResponseMessage());

	        Pattern contentPat=Pattern.compile(ImdbWebBackend.CONTENT_PAT_STRING,Pattern.CASE_INSENSITIVE);
	        Matcher contentMat=contentPat.matcher(reader.getPageText());
	        if ( !contentMat.find()){
	        	throw new DbFailureException("Failed to find content in Title Episode List Page" + url);
	        }
	        if ( season == 1 ) { // For the season 1 request we get the list of all seasons in the select element and then scrape those season episodes pages. 
	            Pattern bySeasonSelectPat=Pattern.compile(
	            	    "<select id=\"bySeason\".*?</select>"
	                    );
	            Matcher bySeasonSelectMat=bySeasonSelectPat.matcher(contentMat.group());
	            if (bySeasonSelectMat.find() ) {
	                String bySeasonSelectContent = bySeasonSelectMat.group();
	                Pattern optionPat=Pattern.compile(
	                	    "<option[^>]*?value=\"(-?\\d+)\">"
	                        );
	                Matcher optionMat=optionPat.matcher(bySeasonSelectContent);
                	int option;
	                while (optionMat.find()) {
	                	if ((option = Integer.parseInt(optionMat.group(1))) == -1 ||
	                		(option > 1 && option < seasons.length)) {
	                		new UpdateEpisodes(obj, ref, option, seasons);
	                	}
	                }
	            }
	        }
            Pattern episodePat=Pattern.compile(
                    // $1 = season number
                    // $2 = episode number
                    "<div>S(\\d+), Ep(\\d+)</div>.*?" +
                    // $3 = original airing date
                    "<div class=\"airdate\">\\s*([^<]*)</div>.*?" +
                    // $4 = imdbref
                    // $5=episode name
                    "<strong>[^<]*<a href=\"(/title/[^\"]*)\"[^>]*>([^<]*)</a>\\s*</strong>"
                    );

            Matcher episodeMat=episodePat.matcher(contentMat.group());
            while (episodeMat.find()){
                String episodeName=ImdbWebPageReader.deHtmlIze(episodeMat.group(5));
                
                Role role=new Role(new ImdbWebObjectRef(
                        DbObjectRef.DB_TYPE_TITLE,
                        "Episode: " + episodeName,
                        episodeMat.group(4)),
                        "S" + episodeMat.group(1)
                        + " E" + episodeMat.group(2)
                        + " - " + episodeMat.group(3).trim().replaceFirst("(\\d+) ([A-Z][a-z][a-z])\\.* (\\d+)", "$2 $1, $3")
                		);
                episodes.add(role);
	        }

            synchronized(seasons) {
            	if (season == -1) { // -1 is used for Unknown seasons. Put these episodes as season 0.
            		seasons[0] = episodes;
            	}
            	else {
            		seasons[season] = episodes;
            	}
                episodes = new Vector<Role>();
    
                for (int index = 0; index < seasons.length; index++) {
                    if (seasons[index] != null) {
                        episodes.addAll(seasons[index]);
                    }
                }

	        	obj.episodes=episodes;
            }
	    }
	}

    static class UpdateGoofs extends ImdbWebBgUpdateThread {
        UpdateGoofs(DbTitleObject obj, ImdbWebObjectRef ref) {
            super(obj, ref, "Goofs");
            this.start();
        }

        void doUpdate() throws Exception {
            DbTitleObject obj = (DbTitleObject)this.obj;
            URL url = new URL("https", ImdbWebBackend.IMDB_HOST, -1, this.ref.getImdbRef() + "goofs");
            ImdbWebPageReader reader = new ImdbWebPageReader(url);
            if (reader.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new DbFailureException("Failed to get Title goofs page " + url + " - " + reader.getResponseCode() + " - " + reader.getResponseMessage());
            } else {
                Pattern goofscontentPat = Pattern.compile("<div id=\"gf.*?</div>.*?</div>\\s*</div>\\s*</div>\\s*</div>\\s*</div>");
                Matcher goofscontentMat = goofscontentPat.matcher(reader.getPageText());
                if (!goofscontentMat.find()) {
                    throw new DbFailureException("Failed to find content in Title goofs Page "+url);
                } else {
                    StringBuffer goofs = new StringBuffer();
                    Pattern goofsItemPat = Pattern.compile("class=\"sodatext\">\\s*(.*?)\\s*</div>");
                    Matcher goofsItemMat = goofsItemPat.matcher(goofscontentMat.group());

                    while(goofsItemMat.find()) {
                        goofs.append(ImdbWebPageReader.deHtmlIze(goofsItemMat.group(1)));
                        goofs.append("\n ---- \n");
                    }

                    if (goofs.length() > 0) {
                        obj.goofs = goofs.toString();
                    }
                }
            }
        }
    }
	    
	/** 
	 * background updater class to get locations
	 * @author Niel
	 *
	 */
	static class UpdateLocations extends ImdbWebBgUpdateThread{


		UpdateLocations(DbTitleObject obj,ImdbWebObjectRef ref) {
			super(obj,ref,"Locations");
			start();
		}
		/* (non-Javadoc)
		 * @see net.sf.sageplugins.sageimdb.ImdbWebBgUpdateThread#doUpdate()
		 */
		void doUpdate() throws Exception {
			DbTitleObject obj=(DbTitleObject)this.obj;
			URL url=new URL("https",ImdbWebBackend.IMDB_HOST,-1,ref.getImdbRef()+"locations");

			ImdbWebPageReader reader=new ImdbWebPageReader(url);
			if ( reader.getResponseCode()!=HttpURLConnection.HTTP_OK)
				throw new DbFailureException("Failed to get Title Locations Page "+url+" - "+reader.getResponseCode()+" - "+reader.getResponseMessage());

	        Pattern contentPat=Pattern.compile(ImdbWebBackend.CONTENT_PAT_STRING,Pattern.CASE_INSENSITIVE);
			Matcher contentMat=contentPat.matcher(reader.getPageText());
			if ( !contentMat.find()){
				throw new DbFailureException("Failed to find content in Title Locations Page "+url);
			}
			
			Pattern locationsPat=Pattern.compile(
					// $1 -- location
					"<dt>\\s*<a href=\"/search[^\"]*\"[^>]*>(.*?)</a>\\s*</dt>" +
					// $2 location extra inf
					"\\s*<dd>(.*?)</dd>"	
			);
			StringBuffer locations=new StringBuffer();
			Matcher locationsMat=locationsPat.matcher(contentMat.group());
			while (locationsMat.find()){
				locations.append(ImdbWebPageReader.deHtmlIze(locationsMat.group(1)));

				String extrainf=ImdbWebPageReader.deHtmlIze(locationsMat.group(2));
				if ( extrainf.length()>0) {
					locations.append(" - ");
					locations.append(extrainf);
				}
				locations.append("\n ---- \n");
			}
			if ( locations.length()>0)
				obj.locations=locations.toString();
		}
	}

	static class UpdateQuotes extends ImdbWebBgUpdateThread{
		
		
		UpdateQuotes(DbTitleObject obj,ImdbWebObjectRef ref) {
			super(obj,ref,"Quotes");
			start();
		}
		/* (non-Javadoc)
		 * @see net.sf.sageplugins.sageimdb.ImdbWebBgUpdateThread#doUpdate()
		 */
		void doUpdate() throws Exception {
			DbTitleObject obj=(DbTitleObject)this.obj;
			URL url=new URL("https",ImdbWebBackend.IMDB_HOST,-1,ref.getImdbRef()+"quotes");
			ImdbWebPageReader reader=new ImdbWebPageReader(url);
			if ( reader.getResponseCode()!=HttpURLConnection.HTTP_OK)
				throw new DbFailureException("Failed to get Title quotes Page "+url+" - "+reader.getResponseCode()+" - "+reader.getResponseMessage());

	        Pattern contentPat=Pattern.compile(ImdbWebBackend.CONTENT_PAT_STRING,Pattern.CASE_INSENSITIVE);
			Matcher contentMat=contentPat.matcher(reader.getPageText());
			if ( !contentMat.find()){
				throw new DbFailureException("Failed to find content in Title Quotes Page "+url);
			}
			
			Pattern quotesPat=Pattern.compile(
                    "<div class=\"sodatext\">(.*?)</div>"
					);
			StringBuffer quotes=new StringBuffer();
			Matcher quotesMat=quotesPat.matcher(contentMat.group());
			while (quotesMat.find()){
				String quote=ImdbWebPageReader.deHtmlIze(
						quotesMat.group(1).replaceAll("\\s*</p><p>\\s*", "\n"));
				quotes.append(quote.trim());
				quotes.append("\n ---- \n");
			}
			if ( quotes.length()>0)
				obj.quotes=quotes.toString();
		}
	}

	static class UpdateSummary extends ImdbWebBgUpdateThread{
		
		UpdateSummary(DbTitleObject obj,ImdbWebObjectRef ref) {
			super(obj,ref,"Summary");
			start();
		}
		/* (non-Javadoc)
		 * @see net.sf.sageplugins.sageimdb.ImdbWebBgUpdateThread#doUpdate()
		 */
		void doUpdate() throws Exception {
			DbTitleObject obj=(DbTitleObject)this.obj;
			URL url=new URL("https",ImdbWebBackend.IMDB_HOST,-1,ref.getImdbRef()+"plotsummary");
			ImdbWebPageReader reader=new ImdbWebPageReader(url);
			if ( reader.getResponseCode()!=HttpURLConnection.HTTP_OK)
				throw new DbFailureException("Failed to get Title summaries Page "+url+" - "+reader.getResponseCode()+" - "+reader.getResponseMessage());

	        Pattern contentPat=Pattern.compile(ImdbWebBackend.CONTENT_PAT_STRING,Pattern.CASE_INSENSITIVE);
			Matcher contentMat=contentPat.matcher(reader.getPageText());
			if ( !contentMat.find()){
				throw new DbFailureException("Failed to find content in Title plotsummary Page "+url);
			}

			Pattern synopsisPat=Pattern.compile(
			        "<h4 id=\"synopsis\" class=\"ipl-list-title\">Synopsis</h4>\\s*" +
			        "<ul class=\"ipl-zebra-list\" id=\"plot-synopsis-content\">\\s*" +
			        "<li class=\"ipl-zebra-list__item\"[^>]*>\\s*" +
			        "(.*?)</li>\\s*</ul>");

			Pattern summaryPat=Pattern.compile(
					"<li class=\"ipl-zebra-list__item\" id=\"summary[^>]*>\\s*" +
			        "<p>(.*?)</p>\\s*" +
					"(<div class=\"author-container\">[\\s\\S]*?</div>)?"
					);

			
			StringBuffer summaries=new StringBuffer();

			// first get small synopsis, then full summary
			Matcher synopMat=synopsisPat.matcher(contentMat.group());
			if ( synopMat.find()){
				String synop=ImdbWebPageReader.deHtmlIze(synopMat.group(1));
				if ( ! synop.startsWith("It looks like we don't have a Synopsis") ) {
					summaries.append(synop);
					summaries.append("\n ----\n");
				}
			}

			// now add each summary...
			Matcher summaryMat=summaryPat.matcher(contentMat.group());
			while (summaryMat.find()){
				summaries.append(ImdbWebPageReader.deHtmlIze(
						summaryMat.group(1).replaceAll("<br ?/?>", "\n")));
				summaries.append('\n');
				summaries.append(ImdbWebPageReader.deHtmlIze(
									summaryMat.group(2)));
				summaries.append("\n ---- \n");
			}
			if ( summaries.length()>0)
				obj.summaries=obj.summaries+summaries.toString();
		}
	}

    static class UpdateFullCredits extends ImdbWebBgUpdateThread{
        
        UpdateFullCredits(DbTitleObject obj,ImdbWebObjectRef ref) {
            super(obj,ref,"FullCredits");
            start();
        }
        void doUpdate() throws Exception {
            DbTitleObject obj=(DbTitleObject)this.obj;
            URL url=new URL("https",ImdbWebBackend.IMDB_HOST,-1,ref.getImdbRef()+"fullcredits");
            ImdbWebPageReader reader=new ImdbWebPageReader(url);
            if ( reader.getResponseCode()!=HttpURLConnection.HTTP_OK)
                throw new DbFailureException("Failed to get Title full credits Page "+url+" - "+reader.getResponseCode()+" - "+reader.getResponseMessage());

            // Split from cast starting point to end of text, 
            // then split from cast ending point, which is first table def after cast table
            String content = reader.getPageText();
            int cast_start=content.indexOf("<table class=\"cast_list\"");
            int cast_end=content.indexOf("<table",cast_start+1);
            if ( cast_start>0 && cast_end > 0){
                Pattern castMemberPat=Pattern.compile(
                        "<tr\\s*class=\"[^\"]*\"[^>]*>" +
                        "\\s*<td\\s*class=\"primary_photo\"[^>]*>[\\s\\S]*?</td>" +
                        "\\s*<td>[\\s\\S]*?" +
                        // $1 = imdb ref
                        // $2 = cast name
                        "<a href=\"([^\"]*)\"[^>]*>\\s([\\s\\S]*?)</a>\\s*</td>" +
                        "\\s*<td\\s*class=\"ellipsis\"[^>]*>[\\s\\S]*?</td>" +
                        // $3 = part name
                        "\\s*<td\\s*class=\"character\"[^>]*>([\\s\\S]*?)</td>" +
                        "\\s*</tr>"
                        );
                Matcher castMemberMat=castMemberPat.matcher(content.substring(cast_start, cast_end));
                Vector<Role> personList=new Vector<Role>();
                while ( castMemberMat.find()){
                    personList.add(
                            new Role(
                                    new ImdbWebObjectRef(DbObjectRef.DB_TYPE_PERSON,
                                            ImdbWebPageReader.deHtmlIze(castMemberMat.group(2)), // name
                                            castMemberMat.group(1)), // imdb ref
                                            ImdbWebPageReader.deHtmlIze(castMemberMat.group(3)))); // role
                }
                obj.cast=personList;
            } else {
            	System.out.println("Failed to find content in Title fullcredits Page "+url);
            }

            // get originators
            // person group is a table where first row is title, remaining rows are people
            Pattern origGroupPat=Pattern.compile(
                    // $1 group name
                    "class=\"dataHeaderWithBorder\">([\\s\\S]*?)</h4>\\s*" +
                    "<table class=\"simpleTable simpleCreditsTable\">\\s*" +
                    "<colgroup>\\s*" +
                    "<col class=\"column1\">\\s*" +
                    "<col class=\"column2\">\\s*" +
                    "<col class=\"column3\">\\s*" +
                    "</colgroup>\\s*" +
                    "<tbody>" +
                    // $2 rows with IMDB ref, name and role
                    "([\\s\\S]*?)" +
                    "</tbody>\\s*" +
                    "</table>"
                    );
                
            // match people with IMDB ref, name and role
            Pattern origPersonPat=Pattern.compile(
                    "<tr[^>]*>" +
                    // $1 imdb ref, $2 name
                    "\\s*<td[^>]*>\\s*<a href=\"(/name[^\"]*)\"[^>]*>([\\s\\S]*?)</a>\\s*</td>" +
                    "(?:\\s*<td[^>]*>[.]*?</td>)?" +
                    // $3 role (info)
                    "\\s*<td[^>]*>(.*?)</td>"
                    );
                
            Vector<Role> personList=new Vector<Role>();
            
            Matcher origGroupMat=origGroupPat.matcher(content);
            // for each group
            while ( origGroupMat.find() ){
                String groupName=ImdbWebPageReader.deHtmlIze(origGroupMat.group(1));
                Matcher origPersonMat=origPersonPat.matcher(origGroupMat.group(2));
                
                // for each person
                while ( origPersonMat.find()){
                    String name=ImdbWebPageReader.deHtmlIze(origPersonMat.group(2));
                    String role=ImdbWebPageReader.deHtmlIze(origPersonMat.group(3));
                    if (role.endsWith( " &" )) {
                        role = role.substring(0, role.length() - 2);
                    }
                    if ( role.isEmpty() ) {
                        role="("+groupName+")";
                    }
                    personList.add(
                            new Role(
                                    new ImdbWebObjectRef(DbObjectRef.DB_TYPE_PERSON,
                                            name,
                                            origPersonMat.group(1)), // imdb ref
                                            role)); // role
                }
            }
            if ( personList.size()>0)
                obj.originators=personList;
        }
    }

    static class UpdateParentalGuide extends ImdbWebBgUpdateThread {
        
        UpdateParentalGuide(DbTitleObject obj,ImdbWebObjectRef ref) {
            super(obj,ref,"ParentalGuide");
            start();
        }
        void doUpdate() throws Exception {
            DbTitleObject obj=(DbTitleObject)this.obj;
            URL url=new URL("https",ImdbWebBackend.IMDB_HOST,-1,ref.getImdbRef()+"parentalguide");
            ImdbWebPageReader reader=new ImdbWebPageReader(url);

            if ( reader.getResponseCode()!=HttpURLConnection.HTTP_OK)
                throw new DbFailureException("Failed to get Title parental guide page "+url+" - "+reader.getResponseCode()+" - "+reader.getResponseMessage());
        
            //
            // $1 info title
            // $2 info content
            //
            // <tr class="ipl-zebra-list__item" id="mpaa-rating">
            // <td class="ipl-zebra-list__label">MPAA</td>
            // <td>Rated PG-13 for sci-fi action violence, some language and brief suggestive comments</td>
            // </tr>
            //
            Pattern infoBlockPat=Pattern.compile(ImdbWebBackend.INFO_BLOCK_PAT_STRING);
            Matcher infoBlockMat=infoBlockPat.matcher(reader.getPageText());

            while (infoBlockMat.find()){
                String infoTitle=ImdbWebPageReader.deHtmlIze(infoBlockMat.group(1));
                if (infoTitle.indexOf("MPAA") >= 0) {
                    String infoContent=infoBlockMat.group(2).trim();
                    String rated = "Rated ";
                    int pos1 = infoContent.indexOf(rated);
                    if (pos1 >= 0) {
                        obj.mpaa_rating = ImdbWebPageReader.deHtmlIze(infoContent.substring(pos1 + rated.length() - 1));
                        break;
                    }
                }
            }
        }
    }

    static class UpdateTrivia extends ImdbWebBgUpdateThread {
		
		UpdateTrivia(DbTitleObject obj,ImdbWebObjectRef ref) {
			super(obj,ref,"Trivia");
			start();
		}
		/* (non-Javadoc)
		 * @see net.sf.sageplugins.sageimdb.ImdbWebBgUpdateThread#doUpdate()
		 */
		void doUpdate() throws Exception {
			DbTitleObject obj=(DbTitleObject)this.obj;
			URL url=new URL("https",ImdbWebBackend.IMDB_HOST,-1,ref.getImdbRef()+"trivia");
			ImdbWebPageReader reader=new ImdbWebPageReader(url);
			if ( reader.getResponseCode()!=HttpURLConnection.HTTP_OK)
				throw new DbFailureException("Failed to get Title trivia Page "+url+" - "+reader.getResponseCode()+" - "+reader.getResponseMessage());

			Pattern triviaItemPat=Pattern.compile("<div\\s*class=\"sodatext\">(.*?)</div>");
			Matcher triviaItemMat=triviaItemPat.matcher(reader.getPageText());
			StringBuffer trivia=new StringBuffer();
			while (triviaItemMat.find()){
				trivia.append(ImdbWebPageReader.deHtmlIze(
						triviaItemMat.group(1).replaceAll("<br ?/?>", "\n")));
				trivia.append("\n ---- \n");
			}
			if ( trivia.length()>0) {
				obj.trivia=trivia.toString();
			} else {
			    throw new DbFailureException("Failed to find content in Title trivia Page "+url);
			}
		}
	}

	public static DbTitleObject get(ImdbWebObjectRef ref) throws DbFailureException{
		DbTitleObject obj=new DbTitleObject(ref);
		try {
			URL url=new URL("https",ImdbWebBackend.IMDB_HOST,-1,ref.getImdbRef()+"reference");

			ImdbWebPageReader reader=new ImdbWebPageReader(url);
			if ( reader.getResponseCode()!=HttpURLConnection.HTTP_OK)
				throw new DbFailureException("Failed to get Title Page "+url+" - "+reader.getResponseCode()+" - "+reader.getResponseMessage());

	        Pattern contentPat=Pattern.compile(ImdbWebBackend.CONTENT_PAT_STRING,Pattern.CASE_INSENSITIVE);
			Matcher contentMat=contentPat.matcher(reader.getPageText());
			if ( !contentMat.find()){
				throw new DbFailureException("Failed to find content in Title reference Page "+url);
			}
			
			String content=contentMat.group();
			
			// get LHS for photo etc
			boolean getSummary=false;
			boolean getTrivia=false;
            boolean getGoofs = false;
			boolean getQuotes=false;
			boolean getEpisodes=false;
			boolean getLocations=false;
			boolean getFullCredits=false;
			boolean getParentalGuide=false;
			
			Pattern lhsPat=Pattern.compile("<div class=\"aux-content-widget-2 links subnav\" div=\"quicklinks\">(.*?)<div class=\"aux-content-widget-2\"");
			Matcher lhsMat=lhsPat.matcher(reader.getPageText());
			if ( lhsMat.find()) {
				String lhs=lhsMat.group(1);
				// check for sub-pages
				String hrefPrefix = "class=\"link\" >"; //"href=\"" + ref.getImdbRef();
				getSummary=lhs.indexOf(hrefPrefix + "Plot Summary")>0;
				getGoofs=lhs.indexOf(hrefPrefix + "Goofs")>0;
				getTrivia=lhs.indexOf(hrefPrefix + "Trivia")>0;
				getQuotes=lhs.indexOf(hrefPrefix + "Quotes")>0;
				getEpisodes=lhs.indexOf(hrefPrefix + "Episode List")>0;
				getLocations=lhs.indexOf(hrefPrefix + "Filming & Production")>0;
				getFullCredits=lhs.indexOf(hrefPrefix + "Full Cast and Crew")>0;
				getParentalGuide=lhs.indexOf(hrefPrefix + "Parents Guide")>0;
				
				// get photo URL
				Pattern photoPat=Pattern.compile(
                        "<div\\s+class=\"titlereference-header\">\\s*?" +
                        "<div>\\s*?" +
						"<a [^>]*>\\s*?" +
						"<img [^>]*?\\s*?src=\"([^\"]*)\"",Pattern.CASE_INSENSITIVE);
				Matcher photoMat=photoPat.matcher(content);
				if ( photoMat.find())
					try {
						obj.imageURL=new URL(photoMat.group(1));
					} catch ( MalformedURLException e){
						System.out.println("Failed to get URL of photo from title page: "+photoMat.group(1));
					}
			} else {
				System.out.println("Failed to get LHS from title page");
			}
			obj.imdbUrl=url.toString();

			obj.summaries="";

			// get name, year from title tag
			Pattern titlePat=Pattern.compile("<title>(.*?)(\\([0-9?]{4}[^)]*\\))?</title>");
			Matcher titleMat=titlePat.matcher(reader.getPageText());
			if ( titleMat.find()){
				// remove quotes from title
				obj.name=ImdbWebPageReader.deHtmlIze(titleMat.group(1));
				if (obj.name.startsWith("\"")) {
				    obj.name = obj.name.substring(1).replaceFirst("\"", " -");
				}
                obj.name = obj.name.replaceFirst("TV Episode ","");
				obj.name = obj.name.replaceFirst(" - Reference View - IMDb", "");

				if ( titleMat.group(2)!=null && ! titleMat.group(2).equals("????") )
					// remove brackets from year.
					obj.year=titleMat.group(2).replaceAll("[^0-9?]*", "");
			}
			
			// get user rating
			Pattern userRatingPat=Pattern.compile(
					// $1 rating
					"<span class=\"ipl-rating-star__rating\">([0-9.]+)<");
			Matcher userRatingMat=userRatingPat.matcher(content);
			if ( userRatingMat.find()){
				obj.rating=userRatingMat.group(1) + "/10";
			}

			int latestSeason = 0;
			if (getEpisodes) {
	            //
	            // Get the latest season number following "?season=":
	            //
	            //     <a href="/title/tt0098904/episodes?season=9">9</a>
	            //
                Pattern latestSeasonPat=Pattern.compile("<a href=\"/title/tt\\d+/episodes\\?season=(\\d+)\">");
                Matcher latestSeasonMat=latestSeasonPat.matcher(content);
                if (latestSeasonMat.find()) {
                    latestSeason = Integer.parseInt(latestSeasonMat.group(1));
                }
			}

			//
			// Get the original airing date:
			//
			//     <a href="/title/tt7642386/releaseinfo">20 Mar 2018</a>
			//
			Pattern originalAirDatePat=Pattern.compile("<a href=\"/title/tt\\d+/releaseinfo\">([^<]*)</a>");
            Matcher originalAirDateMat=originalAirDatePat.matcher(content);
            if (originalAirDateMat.find()) {
                obj.airingDate = originalAirDateMat.group(1);
            }
			//
			// Get season and Episode numbers:
			//
            //		<li class="ipl-inline-list__item">
            // 			Season 1
            // 		</li> 
            // 		<li class="ipl-inline-list__item">
            //    		Episode 5
			//
			Pattern SeasonEpisodePat=Pattern.compile("<li class=\"ipl-inline-list__item\">.*Season ([0-9]*)[^<]*</li>.*<li class=\"ipl-inline-list__item\">.*Episode ([0-9]*)");
            Matcher SeasonEpisodeMat=SeasonEpisodePat.matcher(content);
            if (SeasonEpisodeMat.find()) {
                obj.season = SeasonEpisodeMat.group(1);
                obj.episode = SeasonEpisodeMat.group(2);            }

            //
            // Get series reference $1 and name $2
            //
            //     <h4 itemprop="name">
            //     <a href="/title/tt6437276/?ref_=tt_rv" itemprop='url'>For the People</a>
            //     <span class="titlereference-parent-title-year">(TV Series)</span>
            //     </h4>
            //
            Pattern seriesPat=Pattern.compile("<h4 itemprop=\"name\">\\s*<a href=\"(/title/tt\\d+/)[^>]*>([^<]*)</a>");
            Matcher seriesMat=seriesPat.matcher(content);
            if (seriesMat.find()) {
                obj.seriesRef=new ImdbWebObjectRef(
                                DbObject.DB_TYPE_TITLE,
                                ImdbWebPageReader.deHtmlIze(seriesMat.group(2)),
                                seriesMat.group(1)); 
            }

            //
            // $1 info title
            // $2 info content
            //
            // <tr class="ipl-zebra-list__item" id="mpaa-rating">
            // <td class="ipl-zebra-list__label">MPAA</td>
            // <td>Rated PG-13 for sci-fi action violence, some language and brief suggestive comments</td>
            // </tr>
            //
			Pattern infoBlockPat=Pattern.compile(ImdbWebBackend.INFO_BLOCK_PAT_STRING);
			Matcher infoBlockMat=infoBlockPat.matcher(content);

			while (infoBlockMat.find()){
				String infoTitle=ImdbWebPageReader.deHtmlIze(infoBlockMat.group(1));
				String infoContent=infoBlockMat.group(2).trim();
				int moreIndex=infoContent.indexOf("See more &raquo;</a>");
				if ( moreIndex>0)
					infoContent=infoContent.substring(0, moreIndex).trim();
				if ( infoContent.length()>0){
						
					if ( infoTitle.indexOf("Tagline")>=0) {
						obj.tagLines=ImdbWebPageReader.deHtmlIze(infoContent);
		
					} else if (infoTitle.indexOf("Certification")>=0) {
						obj.usa_rating=Translate.decode(infoContent.replaceAll(".*United States:([^<]+)<.*","$1").trim());					
					
					}else if (infoTitle.indexOf("Plot Summary")>=0) {
						obj.summaries=ImdbWebPageReader.deHtmlIze(infoContent)+"\n ----\n";
					
					}else if (infoTitle.indexOf("Runtime")>=0) {
						obj.runningtime=ImdbWebPageReader.deHtmlIze(infoContent);
					
					}else if (infoTitle.indexOf("Genres")>=0) {
						obj.genres=ImdbWebPageReader.deHtmlIze(infoContent).split(" \\| ");

					}else if ( infoContent.startsWith("<a href=\"/name/")){
						// top credits
						infoContent=ImdbWebPageReader.deHtmlIze(infoContent.replaceAll("<br/?>", ", "));
						if ( obj.topCredits==null)
							obj.topCredits="";
						obj.topCredits=obj.topCredits+infoTitle+" - "+infoContent+"\n";
					}
				}
			}

			if ( getSummary )
				new UpdateSummary(obj,ref);
			else if (obj.summaries==null)
				obj.summaries="";
			if (getFullCredits)
			    new UpdateFullCredits(obj,ref);
            else
                obj.cast=null;
			if ( getTrivia )
				new UpdateTrivia(obj,ref);
			else
				obj.trivia=null;
            if (getGoofs) {
                new ImdbWebGetTitle.UpdateGoofs(obj, ref);
            } else {
                obj.goofs = null;
            }
			if ( getQuotes )
				new UpdateQuotes(obj,ref);
			else
				obj.quotes=null;
			if ( getLocations )
				new UpdateLocations(obj,ref);
			else
				obj.locations=null;
			if ( getEpisodes ) {
			    Vector[] seasons = new Vector[1 + latestSeason]; // seasons[0] reserved for unknown season episodes
		        new UpdateEpisodes(obj,ref,1,seasons);
			} else
				obj.episodes=null;
            if (getParentalGuide)
                new UpdateParentalGuide(obj,ref);
            else
                obj.mpaa_rating=null;
		} catch (DbFailureException e){
			throw e;
		} catch (Throwable e) {
			throw new DbFailureException(e);
		}
		return obj;
	}
}
