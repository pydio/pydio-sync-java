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

	public String getShortProgressString();

	/**
	 * Denotes if progress should be shown
	 * 
	 * @return
	 */
	public boolean isShowProgress();

	/**
	 * Starts the progress for taskName
	 * 
	 * @param taskName
	 *            - task name to display
	 */
	public void begin(String taskName);

	/**
	 * Stops the progress
	 */
	public void end();

}
