package info.ajaxplorer.synchro.progressmonitor;

/**
 * Interface for progress monitor, will be notified about all long time
 * processes
 * 
 * @author WojT
 * 
 */
public interface IProgressMonitor {

	/**
	 * Notifies monitor about progress
	 * 
	 * @param total
	 *            - total amount of work (eg. node count)
	 * @param current
	 *            - current processed item (eg. current node number)
	 */
	public void notifyProgress(int total, int current);

	/**
	 * Returns the progress string which should be displayed next to user
	 * notifications
	 * 
	 * @return
	 */
	public String getProgressString();

	/**
	 * Returns the short progress string - contains percentage value
	 * 
	 * @return
	 */
	public String getShortProgressString();

	/**
	 * Denotes if progress should be shown for syncNodeId
	 * 
	 * @param nodeId
	 * 
	 * @return
	 */
	public boolean isShowProgress(int nodeId);

	/**
	 * Starts the progress for taskName
	 * 
	 * @param currentJobNodeID
	 *            - denotes which repo node is processed, so progress start to
	 *            notify proper status item
	 * 
	 * @param taskName
	 *            - task name to display
	 */
	public void begin(String currentJobNodeID, String taskName);

	/**
	 * Stops the progress
	 * 
	 * @param currentJobNodeID
	 *            - denotes which repo node is stopped with process, so progress
	 *            stop notifying status item
	 * 
	 */
	public void end(String currentJobNodeID);

}
