package io.pyd.synchro.exceptions;

/**
 * Exception object for errors with file operations during synchronization
 * @author WojT
 *
 */
public class SynchroFileOperationException extends Exception {

	public SynchroFileOperationException(String msg) {
		super(msg);
	}
	public SynchroFileOperationException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
