/*
 * Created on Sep 28, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package net.sf.sageplugins.sageimdb;

import java.io.PrintStream;
import java.util.Vector;

/**
 * abstract class for a Person
 */
public class DbPersonObject extends DbObject {

	/** 
	 * Filmography as xx,yy,zz
	 * Stored as 
	 * Vector of Strings containing Filmography Labels
	 * Vector of Vectors of Strings containing Films
	 */
    public Vector<Filmography> tvSeriesFilmographies=new Vector<Filmography>(); // Vector of "TV: <series-name>" Filmographies
	public Vector<Filmography> filmographies=new Vector<Filmography>();  // Vector of Filmographies
	public String biography;
	public String birthname;
	public String nickname;
	public String dob;
	public String height;
	public String married; // (may be several lines) 
	public String trademark; // (may be several lines) 
	/**
	 * remaining items stored like Filmography...
	 */
	public Vector<?> miscLabels; // Vector of Strings
	public Vector<?> miscItems;  // Vector of Vector of Strings
	public String pob;
	public String dod;
	public String pod;
	
	
	/**
	 * @param myref DbObjectRef - Database reference to this person
	 */
	public DbPersonObject(DbObjectRef myref) {
		super(myref);
	}
	public DbPersonObject(String name) {
		super(DbObjectRef.DB_TYPE_PERSON,name);
	}
	
	
	
	/**
	 * testing purposes -- dump to output
	 */
	public void dump(PrintStream out) {
		out.println(getObjectTypeName() + " -- " + name);
		out.println("Biography:>>"+this.biography+"<<\n\n");
		out.println("BirthDate:>>\n"+this.dob+"\n<<\n");
		out.println("Height:>>\n"+this.height+"\n<<\n");
		out.println("Married:>>\n"+this.married+"\n<<\n");
		out.println("Trademark:>>\n"+this.trademark+"\n<<\n");
		for (int i=0; i<this.filmographies.size();i++ ){
			Filmography f=filmographies.get(i);
			out.println("Filmography as "+f.getType());
			for (int j=0; j<f.getRoles().size();j++ ){
				Role r=f.getRoles().get(j);
				out.println("  "+r.getName().getName()+" -- "+r.getPart());
			}
			out.println();
		}
		out.println();
		out.println();
		super.dump(out);
	}
	/**
	 * @return Returns the biography.
	 */
	public String getBiography() {
		return biography;
	}
	/**
	 * @return Returns the birthname.
	 */
	public String getBirthname() {
		return birthname;
	}
	/**
	 * @return Returns the dob.
	 */
	public String getDob() {
		return dob;
	}
	/**
	 * @return Returns the pob.
	 */
	public String getPob() {
		return pob;
	}
	/**
	 * @return Returns the dod.
	 */
	public String getDod() {
		return dod;
	}
	/**
	 * @return Returns the pod.
	 */
	public String getPod() {
		return pod;
	}
	/**
	 * @return Returns the filmographies.
	 */
	public Vector<Filmography> getFilmographies() {
		return filmographies;
	}
	/**
	 * @return Returns the height.
	 */
	public String getHeight() {
		return height;
	}
	/**
	 * @return Returns the married.
	 */
	public String getMarried() {
		return married;
	}
	/**
	 * @return Returns the nickname.
	 */
	public String getNickname() {
		return nickname;
	}
	/**
	 * @return Returns the trademark.
	 */
	public String getTrademark() {
		return trademark;
	}
}
