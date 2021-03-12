package net.sf.sageplugins.sageimdb;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Vector;

public class ImdbWebSearchCache {

    class CacheItem {
        CacheItem(String search, Vector<Role> results){
            this.search=search;
            this.results=results;
        }
        String search;
        Vector<Role> results;
    }
    LinkedList<CacheItem> cache=new LinkedList<CacheItem>();
    static final int MAX_CACHE_SIZE=20;
    
    public void addSearch(String search,Vector<Role> results){
        for ( Iterator<CacheItem> it = cache.iterator();
              it.hasNext();){
            CacheItem item=it.next();
            if ( item.search.equals(search)) {
                // remove it -- it will be added to the end
                it.remove();
            }
        }
        while ( cache.size() >= MAX_CACHE_SIZE){
            cache.remove(0);
        }
        cache.add(new CacheItem(search,results));
    }
    public Vector<Role> getSearch(String search){
        for ( Iterator<CacheItem> it = cache.iterator();
              it.hasNext();){
            CacheItem item=it.next();
            if ( item.search.equals(search)) {
System.out.println("search cache hit");                
                return item.results;
            }
        }
        return null;
    }
   
}
