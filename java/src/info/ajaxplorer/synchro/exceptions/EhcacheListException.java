package info.ajaxplorer.synchro.exceptions;

/**
 * Exception object for all ehcache list implementation/access
 * 
 * @author WojT
 * 
 */
public class EhcacheListException extends Exception {

	public EhcacheListException(String msg) {
		super(msg);
	}
	public EhcacheListException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
