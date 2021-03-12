/*
 * Created on Sep 29, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package net.sf.sageplugins.sageimdb;

/**
 * Role: Simple tuple for actor/part pairs
 */
public class Role {
	DbObjectRef name;
	String part;
	
	public Role (DbObjectRef name, String Part) {
		this.name=name;
		this.part=new String(Part);
	}
	public String getPart() {
		return part;
	}
	public DbObjectRef getName() {
		return name;
	}
    public String toString(){
        return "\""+name +"\" - \""+part+"\"";
    }
}
