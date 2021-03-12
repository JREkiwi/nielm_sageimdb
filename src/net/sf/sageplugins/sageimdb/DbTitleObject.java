/*
 * Created on Sep 28, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package net.sf.sageplugins.sageimdb;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Vector;

/**
 * @author Owner
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class DbTitleObject extends DbObject {

	public Vector<Role> cast;  //vector of Roles 
	public Vector<Role> originators; // vector of Roles (director/producer)
    public Vector<Role> episodes;
	public String topCredits; // writer/director
	public String tagLines;
	public String plots;
	public String summaries; 
	public String locations;
	public String novel;
	public String[] genres;
	public String runningtime;
	public String rating;
	public String year;
	public String mpaa_rating;
	public String usa_rating;
    public String airingDate;
    public DbObjectRef seriesRef;
	public Long   duration;
	public String season;
	public String episode;

	/**
	 * @param myref DbObjectRef -- database reference for this object
	 */
	public DbTitleObject(DbObjectRef myref) {
		super(myref);
		// TODO Auto-generated constructor stub
	}
	public DbTitleObject(String name) {
		super(DbObjectRef.DB_TYPE_TITLE,name);
	}	/**
	 * testing purposes -- dump to output
	 */
	public void dump(PrintStream out) {
		out.println(getObjectTypeName() + " -- " + name);
		out.println("rating:>>\n"+this.rating+"\n<<\n");
        if ( genres != null)
            out.println("genres:>>\n"+Arrays.asList(this.genres).toString()+"\n<<\n");
        else 
            out.println("genres:>>null");
        if (airingDate != null)
            out.println("Aired:" +airingDate);
        if (season != null)
            out.println("Season:" +season);
        if (episode != null)
            out.println("Episode:" +episode);
        if ( seriesRef!=null)
            out.println("Series: "+seriesRef.getName());
		out.println("summary: >>"+this.summaries+"<<\n\n");
		out.println("taglines:>>\n"+this.tagLines+"\n<<\n");
		out.println("plot:>>\n"+this.plots+"\n<<\n");
		out.println("locations:>>\n"+this.locations+"\n<<\n");
        if ( episodes != null && ! episodes.isEmpty()){
            out.println("episodes:\n");
            for ( Iterator<Role> it =episodes.iterator(); it.hasNext();){
                Role ep=it.next();
                out.println("  "+ep);
            }
            
        }
		out.println("Cast:");
		if ( this.cast != null) 
            for (int i=0; i<this.cast.size();i++ ){
				Role r=this.cast.get(i);
				out.println("  "+r.getName().getName()+" -- "+r.getPart());
			}
		
		out.println("Originators:");
        if ( this.originators!=null)
		for (int i=0; i<this.originators.size();i++ ){
				Role r=this.originators.get(i);
				out.println("  "+r.getName().getName()+" -- "+r.getPart());
			}
		
		out.println();
		out.println();
		super.dump(out);
	}
	public Vector<Role> getCast() {
		return cast;
	}
	public Vector<Role> getOriginators() {
		return originators;
	}

	/**
	 * @return Returns the plots.
	 */
	public String getPlots() {
		return plots;
	}
	/**
	 * @return Returns the summaries.
	 */
	public String getSummaries() {
		return summaries;
	}
	/**
	 * @return Returns the tagLines.
	 */
	public String getTagLines() {
		return tagLines;
	}
	/**
	 * @return Returns the rating.
	 */
	public String getRating() {
		return rating;
	}
	/**
	 * @return Returns the topCredits.
	 */
	public String getTopCredits() {
		return topCredits;
	}
	/**
	 * @return Returns the locations.
	 */
	public String getLocations() {
		return locations;
	}
	public String getRunningtime() {
		return runningtime;
	}
	public Long getDuration() {
		duration = Long.valueOf(runningtime.replaceAll("([0-9]+) .*","$1").trim());
		return duration;
	}
    public String[] getGenres() {
        return genres;
    }
    public Vector<Role> getEpisodes() {
        return episodes;
    }
    public String getAiringDate() {
        return airingDate;
    }
    public DbObjectRef getSeriesRef() {
        return seriesRef;
    }
	public String getMPAArating() {
		return mpaa_rating	;
	}
	public String getUSArating() {
		return usa_rating	;
	}
	public String getYear() {
		return year	;
	}
	public String getSeason() {
		return season	;
	}
	public String getEpisode() {
		return episode	;
	}
}
