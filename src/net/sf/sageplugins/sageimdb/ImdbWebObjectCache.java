/*
 * Created on Oct 12, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package net.sf.sageplugins.sageimdb;

import java.util.Vector;


/**
 * @author Owner
 *
 * Cache containing last N objects -- to be used by the backend when required
 */
public class ImdbWebObjectCache {

	Vector<DbObject> objCache;
	Vector<DbObjectRef> refCache;
	static final public int CACHE_SIZE=20; 
	/**
	 * 
	 */
	public ImdbWebObjectCache() {
		objCache=new Vector<DbObject>();
		refCache=new Vector<DbObjectRef>();
	}
	
	public void addObject(DbObject obj,DbObjectRef ref) {
		Object cacheObj=getObject(ref);
		
		if ( cacheObj!=null) {
			// remove element, so it will be put at end
			objCache.removeElement(cacheObj);
			refCache.removeElement(ref);
		} else {
			// not already existing, check size
			if ( objCache.size()>=CACHE_SIZE) {
				// too big, remove first
				System.out.println("Removing old obj");
				objCache.removeElementAt(0);
				refCache.removeElementAt(0);
			}
		}
		objCache.add(obj);
		refCache.add(ref);
	}
	public DbObject getObject(DbObjectRef ref){
		for ( int i=refCache.size()-1 ; i >=0 ; i -- ){
            DbObjectRef cacheRef=refCache.elementAt(i);
			if ( cacheRef.equals(ref)){
				return objCache.elementAt(i);
			}
		}
		return null;
	}
}
