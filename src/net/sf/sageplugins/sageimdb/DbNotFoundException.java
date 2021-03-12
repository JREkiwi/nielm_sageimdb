/*
 * Created on Sep 28, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package net.sf.sageplugins.sageimdb;

/**
 * Exception thrown when something is not found
 */
public class DbNotFoundException extends Exception {

	/**
     * 
     */
    private static final long serialVersionUID = -3672695867632846534L;

    /**
	 * 
	 */
	public DbNotFoundException() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param arg0
	 */
	public DbNotFoundException(String arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param arg0
	 */
	public DbNotFoundException(Throwable arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param arg0
	 * @param arg1
	 */
	public DbNotFoundException(String arg0, Throwable arg1) {
		super(arg0, arg1);
		// TODO Auto-generated constructor stub
	}

}
