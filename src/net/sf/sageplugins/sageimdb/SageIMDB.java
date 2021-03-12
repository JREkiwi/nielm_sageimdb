/*
 * Created on Sep 28, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package net.sf.sageplugins.sageimdb;
import java.io.File;
import java.util.Vector;
/**
 * @author Owner
 *
 * Sage -> local IMDB main class.
 */
public class SageIMDB {
	public final static String VERSION = "2.0.2";
	public String lastError;
	public boolean isLoadedOk;
	private DbBackend backend;
	
	static String getExceptionLocation(Throwable e){
		if ( e.getCause()!= null)
			e=e.getCause();
		for (int elementNum = 0; elementNum < e.getStackTrace().length; elementNum++) {
			StackTraceElement element=e.getStackTrace()[elementNum];
			if ( element.getClassName().startsWith(SageIMDB.class.getPackage().getName())) {
				return "\nat: "+element.getClassName()+"."+element.getMethodName()+":"+element.getLineNumber();
			}
		}
		return "";
	}
	
	/**
	 * 
	 */
	public SageIMDB(String backend_class_name, String backend_options) {
		// init backend
		try {
			Class backend_class=Class.forName(backend_class_name);  
			if ( backend_class == null ){
				lastError="Failed to find DbBackend class "+backend_class_name;
				isLoadedOk=false;
				return;
			} 
			backend = (DbBackend)backend_class.newInstance();
			backend.setOptions(backend_options);
		}
		catch ( Throwable e ){
			lastError="Failed to load DbBackend class "+backend_class_name+"\n"
			+ e.toString();
			isLoadedOk=false;
			return;
		}
		isLoadedOk=true;
	}

	/**
	 * Search for a person. Returns a vector of DbObjectRef's, null on failure
	 */
	public Vector<Role> searchAll(String person) {
		try {
			return backend.searchAll(person);
		} catch ( Exception e ) {
			lastError="searching for "+person+"\n"+
			e.toString()+
			getExceptionLocation(e);
			return null;
		}
	}
	/**
	 * Search for a person. Returns a vector of DbObjectRef's, null on failure
	 */
	public Vector<Role> searchPerson(String person) {
		try {
			return backend.searchPerson(person);
		} catch ( Exception e ) {
			lastError="searching for person "+person+"\n"+
			e.toString()+
			getExceptionLocation(e);
			return null;
		}
	}
	/**
	 * Search for a title. Returns a vector of DbObjectRef's, null on failure
	 */
	public Vector<Role> searchTitle(String title) {
		try {
			return backend.searchTitle(title);
		} catch ( Exception e ) {
			lastError="searching for title "+title+"\n"+
			e.toString()+
			getExceptionLocation(e);
			return null;
		}
	}
	public Vector<Role> searchTvEpisode(String title) {
		try {
			return backend.searchTvEpisode(title);
		} catch ( Exception e ) {
			lastError="searching for TV Episode "+title+"\n"+
			e.toString()+
			getExceptionLocation(e);
			return null;
		}
	}

	/** 
	 * Convert a DbObjectRef into a DbObject, 
	 * catching exceptions and updating lastError
	 * @param DbObjectRef
	 */
	public DbObject getDbObject(DbObjectRef ref){
		try { 
			return ref.getDbObject(backend);
		} catch (Exception e) {
			System.out.println("Imdb get Object failed "+e);
			e.printStackTrace();
			lastError="reading "+ref.getObjectTypeName()+":"+ref.getName()+"\n"+
			e.toString()+
			getExceptionLocation(e);
			return null;
		}
	}
	/** 
	 * Convert a DbObjectRef into a DbObject, 
	 * catching exceptions and updating lastError
	 * and waiting for all data
	 * @param DbObjectRef
	 */
	public DbObject getDbObjectWaitAll(DbObjectRef ref){
		try {
			DbObject obj=ref.getDbObject(backend);
			while ( obj.getNumBgUpdatesInProgress()>0)
				obj.waitUpdates(0);
			return obj;
		} catch (Exception e) {
			lastError="reading "+ref.getObjectTypeName()+":"+ref.getName()+"\n"+
			e.toString()+
			getExceptionLocation(e);

			return null;
		}
	}	
	/**
	 * Main function / test harness
	 */
	public static void main(String[] args) {
		if ( args.length < 4 ) {
			System.out.println("Usage: SageIMDB DbBackendClassName BackendOptions SearchType SearchString");
			System.exit(1);
		}
		SageIMDB imdb=new SageIMDB(args[0], args[1]);
		if ( ! imdb.isLoadedOk) {
			System.out.println("Failed to load SageIMDB: \n "+imdb.lastError );
			System.exit(1);
		}

		// Enable logging of timings for IMDB requests and processing of the responses.
		ImdbWebBgUpdateThread.printTimings = true;
		ImdbWebPageReader.printTimings     = true;
		ImdbWebObjectRef.printTimings      = true;

		Vector<Role> result=null;
		if(args[2].equalsIgnoreCase("person")) { 
			result=imdb.searchPerson(args[3]);
		} else if(args[2].equalsIgnoreCase("title")) { 
			result=imdb.searchTitle(args[3]);
		} else if(args[2].equalsIgnoreCase("episode")) { 
			result=imdb.searchTvEpisode(args[3]);
		} else if(args[2].equalsIgnoreCase("all")) { 
			result=imdb.searchAll(args[3]);
		} else if(args[2].equalsIgnoreCase("imdbref")) {
			ImdbWebObjectRef ref;
			if ( args[3].startsWith("/title")) {
				ref=new ImdbWebObjectRef(DbObjectRef.DB_TYPE_TITLE,args[4],args[3]);
				result=new Vector<Role>();
				result.add(new Role(ref, ""));
			} else if ( args[3].startsWith("/name")) {
				ref=new ImdbWebObjectRef(DbObjectRef.DB_TYPE_PERSON,args[4],args[3]);
				result=new Vector<Role>();
				result.add(new Role(ref, ""));
			} 
		} else { 
			System.out.println("Usage: SageIMDB DbBackendClassName SearchType SearchString");
			return;
		}
		if ( result == null ) {
			System.out.println("Failed to search imdb: \n "+imdb.lastError );
			System.exit(1);
		}
		System.out.println("Found "+Integer.toString(result.size())+" results");
		if ( result.size() > 1 )
			for ( int i=0; i < result.size(); i++ ) {
				System.out.println((result.elementAt(i).getName()).getName());
			}
		if ( result.size() == 1 ) {
			DbObject found=(imdb.getDbObjectWaitAll(result.elementAt(0).getName()));
			if ( found != null){
				found.dump(System.out);
				try {
					found.writeImageJpg(new File("c:\\temp\\test.jpg"));
				} catch (Exception e) {
					System.out.println("Exception "+e.toString());
					e.printStackTrace();
				}

			}
			else
				System.out.println("Error -- unable to get object for reference");
		}
	}
	/**
	 * @return Returns the isLoadedOk.
	 */
	public boolean isLoadedOk() {
		return isLoadedOk;
	}
	/**
	 * @return Returns the lastError.
	 */
	public String getLastError() {
		return lastError;
	}

	public static String getVERSION() {
		return VERSION;
	}
}
