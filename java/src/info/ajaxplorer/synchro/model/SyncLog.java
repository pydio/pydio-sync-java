package info.ajaxplorer.synchro.model;

import info.ajaxplorer.client.model.Node;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable
public class SyncLog {

	public static String LOG_STATUS_SUCCESS = "success";
	public static String LOG_STATUS_INTERRUPT = "interrupt";
	public static String LOG_STATUS_ERRORS = "errors";
	public static String LOG_STATUS_CONFLICTS = "conflicts";
	
	@DatabaseField(generatedId=true)
	private int id;
	@DatabaseField
	public long jobDate;
	@DatabaseField
	public String jobStatus;
	@DatabaseField
	public String jobSummary;
	@DatabaseField(foreign=true,index=true)
	public Node synchroNode;
	
}
