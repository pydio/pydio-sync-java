package info.ajaxplorer.synchro.model;

import info.ajaxplorer.client.model.Node;

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
		
}
