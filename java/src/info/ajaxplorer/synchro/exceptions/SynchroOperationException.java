package info.ajaxplorer.synchro.exceptions;

/**
 * Exception object for synchronization operation error management
 * @author WojT
 *
 */
public class SynchroOperationException extends Exception {

	public SynchroOperationException(String msg) {
		super(msg);
	}
	public SynchroOperationException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
