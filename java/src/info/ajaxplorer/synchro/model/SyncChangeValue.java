package info.ajaxplorer.synchro.model;

import info.ajaxplorer.client.model.Node;
import info.ajaxplorer.synchro.SyncJob;

public class SyncChangeValue {

	public String nodeId;
	public Node n;
	public Integer task;
	public Integer status;
	private String sep = "__";
	public boolean loaded = false;
	
	SyncChangeValue(Integer task, Node n, Integer status){
		this.task = task;
		this.n = n;
		this.status = status;
	}
	
	SyncChangeValue(String serializedString){
		String[] split = serializedString.split(sep);
		this.task = Integer.parseInt(split[0]);
		this.nodeId = split[1];
		this.status = Integer.parseInt(split[2]);
	}
	
	public String getSerializedString(){
		return this.task + sep + this.n.id + sep + this.status;
	}
	
	public String getStatusString(){
		String s = "";
		if (status == SyncJob.STATUS_TODO){
			return "To do";
		}else if (status == SyncJob.STATUS_DONE){
			return "Done";
		}else if (status == SyncJob.STATUS_CONFLICT){				
			return "Conflict";
		}else if (status.equals(SyncJob.STATUS_CONFLICT_SOLVED)){
			return "Solved";
		}else if (status == SyncJob.STATUS_PROGRESS){
			return "In progress";
		}else if (status == SyncJob.STATUS_INTERRUPTED){
			return "Interrupted";
		}
		return s;
	}
	
	public String getTaskString(){
		String s = "";
		if (task == SyncJob.TASK_DO_NOTHING){
			return "Nothing done";
		}else if (task == SyncJob.TASK_LOCAL_GET_CONTENT){
			return "Get content from server";
		}else if (task == SyncJob.TASK_LOCAL_MKDIR){				
			return "Create local folder";
		}else if (task == SyncJob.TASK_LOCAL_REMOVE){
			return "Remove locale resource";
		}else if (task == SyncJob.TASK_REMOTE_MKDIR){
			return "Create remote folder";
		}else if (task == SyncJob.TASK_REMOTE_PUT_CONTENT){
			return "Send content to server";
		}else if (task == SyncJob.TASK_REMOTE_REMOVE){
			return "Remove remote resource";
		}else if (task.equals(SyncJob.TASK_SOLVE_KEEP_BOTH)){
			return "Keep both version (rename local version)";
		}else if (task.equals(SyncJob.TASK_SOLVE_KEEP_MINE)){
			return "Keep my version (override remote version)";
		}else if (task.equals(SyncJob.TASK_SOLVE_KEEP_THEIR)){
			return "Keep remote version (override local version)";
		}
		return s;
		
	}
		
}
