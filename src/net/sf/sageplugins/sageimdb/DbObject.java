/*
 * Created on Sep 28, 2004
 */
package net.sf.sageplugins.sageimdb;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import javax.imageio.ImageIO;

/**
 * Base class for a DBObject
 */
public abstract class DbObject extends DbObjectRef {

	public String imdbUrl;
	
	// for backends that can do background updates
	public int numBgUpdatesInProgress=0;
	

	public String trivia; // Vector of Strings
    public String goofs; // Vector of Strings
	public String quotes; // Vector of Strings
	public BufferedImage image; // poster/headshot
    public java.net.URL imageURL; // poster/headshot
	
	synchronized public void notifyWaiters(){
		notify();
	}
	synchronized public void waitUpdates(long timeout) throws InterruptedException{
		wait(timeout);
	}
	protected DbObject(DbObjectRef myref) {
		super(myref);
	}
	protected DbObject(int type, String name) {
		super(type,name);
	}
	
	public DbObject getDbObject(DbBackend backend){
		return this;
	}
	
	public String getQuotes() {
		return quotes;
	}
	public String getTrivia() {
		return trivia;
	}
    public String getGoofs() {
        return this.goofs;
    }
	public String getImdbUrl() {
		return imdbUrl;
	}
	
	/**
	 * testing purposes -- dump to output
	 */
	protected void dump(PrintStream out){
		out.println("URL: >>"+this.imdbUrl+"<<\n\n");
		out.println("Quotes:>>\n"+this.quotes+"\n<<\n");
		out.println("Trivia:>>\n"+this.trivia+"\n<<\n");
        out.println("Goofs:>>\n" + this.goofs + "\n<<\n");
	}
	/**
	 * @param imdbUrl The imdbUrl to set.
	 */
	public void setImdbUrl(String imdbUrl) {
		this.imdbUrl = imdbUrl;
	}
	/**
	 * @param quotes The quotes to set.
	 */
	public void setQuotes(String quotes) {
		this.quotes = quotes;
	}
	/**
	 * @param trivia The trivia to set.
	 */
	public void setTrivia(String trivia) {
		this.trivia = trivia;
	}
    public void setGoofs(String goofs) {
        this.goofs = goofs;
    }
	/**
	 * @return Returns the image.
	 */
	public BufferedImage getImage() {
		if ( image == null && imageURL != null )
			try {
				image=ImageIO.read(imageURL);
			} catch (Exception e) {
				System.out.println("Failed to get image from url "+imageURL+" -- "+e);
			}
		return image;
	}
    public boolean writeImageJpg(File outfile) 
    throws IOException {
    	getImage();
        if ( image == null )
            return false;

        FileOutputStream outputstream=null;
        try {
            outputstream=new FileOutputStream(outfile); 
            ImageIO.write(image,"jpg",outputstream);
        } finally {
            if ( outputstream != null)
                outputstream.close();
        }
        return true;
    }
	/**
	 * @return Returns the numBgUpdatesInProgress.
	 */
	public int getNumBgUpdatesInProgress() {
		return numBgUpdatesInProgress;
	}
    public java.net.URL getImageURL() {
        return imageURL;
    }
}
