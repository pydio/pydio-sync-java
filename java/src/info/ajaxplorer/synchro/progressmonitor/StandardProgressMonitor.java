package info.ajaxplorer.synchro.progressmonitor;

import java.util.HashMap;
import java.util.Map;

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
	private Map<String, Boolean> showProgress = new HashMap<String, Boolean>();

	@Override
	public void notifyProgress(int total, int current) {
		this.total = total;
		this.current = current;
	}

	@Override
	public String getProgressString() {
		String progressString = taskName != null ? (taskName + " - ") : "";
		progressString += getProgressValue() + "%";
		return progressString;
	}

	@Override
	public String getShortProgressString() {
		return getProgressValue() + "%";
	}

	@Override
	public boolean isShowProgress(int nodeId) {
		Boolean showPrg = showProgress.get("" + nodeId);
		return showPrg != null ? showPrg : false;
	}

	@Override
	public void begin(String tcurrenNodeId, String taskName) {
		this.taskName = taskName;
		this.current = 0;
		this.total = 0;
		showProgress.put(tcurrenNodeId, true);
	}

	@Override
	public void end(String tcurrentNodeId) {
		this.current = 0;
		this.total = 0;
		showProgress.put(tcurrentNodeId, false);
	}

	private int getProgressValue() {
		int progressValue = 0;

		if (current != 0 && total != 0) {
			progressValue = (int) (((double) current / (double) total) * 100);
		}

		return progressValue;
	}

}
