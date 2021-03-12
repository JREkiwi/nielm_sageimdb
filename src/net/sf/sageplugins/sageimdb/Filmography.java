/*
 * Created on Oct 2, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package net.sf.sageplugins.sageimdb;

import java.util.Vector;

/**
 * Fimography class to hold the Filmography as... name
 * and the vector of Roles
 */
public class Filmography {
	private String filographyType;
	private Vector<Role> roles;
    public Filmography(String filographyType) {
        this.filographyType=filographyType;
		roles=new Vector<Role>();
	}

    public Filmography(Filmography other) {
        this(other.filographyType);
        addRoles(other.roles);
    }

    public void addRole(Role newrole) {
		roles.add(newrole);
	}

    public void addRoles(Vector<Role> roles) {
        this.roles.addAll(roles);
    }

    /**
	 * @return Returns the filographyType.
	 */
	public String getType() {
		return filographyType;
	}
	/**
	 * @return Returns the roles.
	 */
	public java.util.Vector<Role> getRoles() {
		return roles;
	}
}
