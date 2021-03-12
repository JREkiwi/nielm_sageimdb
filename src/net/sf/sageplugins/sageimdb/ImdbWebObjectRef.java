/*
 * Created on Oct 7, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package net.sf.sageplugins.sageimdb;



/**
 * @author Owner
 * 
 * TODO To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Style - Code Templates
 */
public class ImdbWebObjectRef 
extends DbObjectRef 
{
    public static boolean printTimings = false; // TODO Get this from server configuration

	/**
	 * IMDB- web reference -- root of all other imdb pages eg name/nm0268626/
	 */
	private String imdbRef;
	static ImdbWebObjectCache objectCache = new ImdbWebObjectCache();

	/**
	 * @param mytype
	 * @param myname
	 */
	public ImdbWebObjectRef(int mytype, String myname, String imdbRef) {
		super(mytype, myname);
        imdbRef = imdbRef.replaceAll("https?://www.imdb.com", "");
        imdbRef = imdbRef + "/";
        imdbRef = imdbRef.replaceAll("//", "/");
        imdbRef = imdbRef.replaceAll("\\?.*$", "");
		this.imdbRef = imdbRef;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.sf.sageplugins.sageimdb.DbObjectRef#getDbObject(net.sf.sageplugins.sageimdb.DbBackend)
	 */
	public DbObject getDbObject(DbBackend myBackend) throws DbFailureException,
			DbNotFoundException {

		DbObject searchret = objectCache.getObject(this);
		if (searchret == null) {
            long startTime = System.currentTimeMillis();
			switch (type) {
			case DB_TYPE_PERSON:
				searchret = ImdbWebGetPerson.get(this);
                if (printTimings) {
                    System.out.println( "IMDB person query and processing took " + ( System.currentTimeMillis() - startTime ) + " ms." );
                }
				break;
			case DB_TYPE_TITLE:
				searchret = ImdbWebGetTitle.get(this);
                if (printTimings) {
				    System.out.println( "IMDB title query and processing took " + ( System.currentTimeMillis() - startTime ) + " ms." );
				}
				break;
			default:
				throw new DbNotFoundException("Not a valid DbObjectRef");
			}
			if (searchret == null) {
				throw new DbNotFoundException(getObjectTypeName() + " - "
						+ name + " not found");
			}
		}
		objectCache.addObject(searchret,this);
		return searchret;
	}

	/**
	 * @return Returns the imdbRef.
	 */
	public String getImdbRef() {
		return imdbRef;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object arg0) {
		if (arg0.getClass() == this.getClass()) {
			ImdbWebObjectRef obj = (ImdbWebObjectRef) arg0;
			return (obj.imdbRef.equals(this.imdbRef));
		}
		return false;
	}
}