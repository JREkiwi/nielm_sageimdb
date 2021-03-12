/*
 * Created on Sep 28, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package net.sf.sageplugins.sageimdb;

/**
 * Base class for returned IMDB database references
 */
public abstract class DbObjectRef {
	public static final int DB_TYPE_PERSON = 0;
	public static final int DB_TYPE_TITLE = 1;
    public static final String typeNames[] = { "Person", "Title" };
	
	protected int type;
	protected String name;
    protected DbObjectRef(int mytype, String myname){
		type=mytype;
		name=new String(myname);
	}

	protected DbObjectRef(DbObjectRef copy){
		this(copy.type,copy.name);
	}
	
	
	/**
	 * Method for retrieving the real object from the database.
	 * Different for each backend, hence abstract.
	 * Real DbObjects just return themselves...
	 * 
	 */
	abstract public DbObject getDbObject(DbBackend myBackend) throws DbFailureException,DbNotFoundException;
	
	/** 
	 * compare two references to see if they are the same.
	 */
	public boolean equals(Object arg0) {
        if (arg0.getClass() == this.getClass()) {
            DbObjectRef other=(DbObjectRef)arg0;
            if ( other.type==type && other.name.equals(name))
                return true;
        }
		return false;
	}
	
	public String getObjectTypeName() {
		if ( type < 0 || type > typeNames.length )
			return "unknown";
		return typeNames[type];
	}
	
	public static String[] getTypeNames() {
		return typeNames;
	}
	public String getName() {
		return name;
	}
	public int getType() {
		return type;
	}

    public void setName(String name) {
        this.name = name;
    }
    
    public String toString(){
        return typeNames[type]+": "+getName();
    }
}
