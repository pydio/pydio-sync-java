package info.ajaxplorer.synchro.progressmonitor;

/**
 * Implementation of progress monitor
 * 
 * @author WojT
 * 
 */
public class StandardProgressMonitor implements IProgressMonitor {

	private int total;
	private int current;
	private String taskName;
	private boolean showProgress = false;

	@Override
	public void notifyProgress(int total, int current) {
		this.total = total;
		this.current = current;
	}

	@Override
	public String getProgressString() {
		String progressString = "";
		if (showProgress) {
			progressString += taskName != null ? (taskName + " - ") : "";
			progressString += getProgressValue() + "%";
		}
		return progressString;
	}

	@Override
	public String getShortProgressString() {
		return getProgressValue() + "%";
	}

	@Override
	public boolean isShowProgress() {
		return showProgress;
	}

	@Override
	public void begin(String taskName) {
		this.taskName = taskName;
		this.current = 0;
		this.total = 0;
		this.showProgress = true;
	}

	@Override
	public void end() {
		this.current = 0;
		this.total = 0;
		this.showProgress = false;
	}

	private int getProgressValue() {
		int progressValue = 0;

		if (current != 0 && total != 0) {
			progressValue = (int) (((double) current / (double) total) * 100);
		}

		return progressValue;
	}

}
