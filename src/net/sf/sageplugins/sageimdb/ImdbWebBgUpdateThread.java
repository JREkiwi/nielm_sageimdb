/*
 * Created on Oct 14, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package net.sf.sageplugins.sageimdb;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author Owner
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
abstract class ImdbWebBgUpdateThread extends Thread {

    public static boolean printTimings = false; // TODO Get this from server configuration

    protected DbObject obj;
	protected ImdbWebObjectRef ref;
	/**
	 * 
	 */
	protected ImdbWebBgUpdateThread(DbObject obj,ImdbWebObjectRef ref, String name) {
		super(name);
		this.obj=obj;
		this.ref=ref;
		obj.numBgUpdatesInProgress++;
	}

    protected static URL getNameURL(ImdbWebObjectRef ref, String suffix) throws MalformedURLException {
        String imdbRef = ref.getImdbRef();
        int queryIndex = imdbRef.lastIndexOf('?');
        if (queryIndex >= 0 && suffix.startsWith("?")) {
            imdbRef += "&" + suffix.substring(1);
        }
        else {
            imdbRef += suffix;
        }

        return new URL("https",ImdbWebBackend.IMDB_HOST,-1,imdbRef);
    }

	/**
	 * Method to get and parse the data in the background.
	 * @throws Exception
	 */
	abstract void doUpdate() throws Exception;

	/**
     * Method to give derived classes a chance to do something before run() calls notifyWaiters()
     * @throws Exception
     */
    protected void preNotify() throws Exception {
    }

    /* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
	    Exception e = null;
		try{
		    long startTime = System.currentTimeMillis();
			doUpdate();
			if (printTimings) {
			    long stopTime = System.currentTimeMillis();
			    System.out.println( "IMDB Background update thread " + getName() + " took " + ( stopTime - startTime ) + " ms." );
			}
		} catch (Exception ex) {
		    e = ex;
		}
		finally {
		    try {
		        preNotify();
		    } catch (Exception ex) {
		        e = ex;
		    }
		}
		if (e != null) {
            System.out.println("IMDB Background update thread "+getName()+" failed: Exception: "+e.toString());
            e.printStackTrace();
		}
		obj.numBgUpdatesInProgress--;
		obj.notifyWaiters();
	}
}
