package info.ajaxplorer.synchro.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import info.ajaxplorer.client.model.Node;
import info.ajaxplorer.synchro.Manager;
import info.ajaxplorer.synchro.SyncJob;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DatabaseField;

public class SyncChange {

	@DatabaseField(generatedId=true)
	int id;
	@DatabaseField
	String jobId;
	@DatabaseField
	String key;
	@DatabaseField
	String changeValue;
	
	SyncChangeValue value;
	
	SyncChange(){
		
	}
	
	public static List<SyncChange> MapToSyncChanges(Map<String, Object[]> map, String jobId){
		ArrayList<SyncChange> changes = new ArrayList<SyncChange>();
		Iterator<Map.Entry<String, Object[]>> it = map.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry<String, Object[]> entry = it.next();
			Object[] ob = entry.getValue();
			SyncChange s = new SyncChange();
			SyncChangeValue v = new SyncChangeValue((Integer)ob[0], (Node)ob[1], (Integer)ob[2]);
			s.setKey(entry.getKey());
			try{
				s.setChangeValue(v);				
			}catch(Exception e){
				continue;
			}
			s.setJobId(jobId);
			changes.add(s);
		}
		return changes;
	}
	
	public static boolean syncChangesToTreeMap(List<SyncChange> changes, Map<String, Object[]> tree){
		boolean detectConflicts = false;
		 //= new TreeMap<String, Object[]>();
		for(int i=0;i<changes.size();i++){
			Object[] value = new Object[3];
			value[0] = changes.get(i).getChangeValue().task;
			value[1] = changes.get(i).getChangeValue().n;
			value[2] = changes.get(i).getChangeValue().status;
			if(value[2] == SyncJob.STATUS_CONFLICT) detectConflicts = true;
			tree.put(changes.get(i).getKey(), value);
		}
		return detectConflicts;
	}

	public String getJobId() {
		return jobId;
	}

	public void setJobId(String jobId) {
		this.jobId = jobId;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public SyncChangeValue getChangeValue() {
		if(value == null){
			value = new SyncChangeValue(changeValue);
			try{
				Dao<Node, String> nodeDao = Manager.getInstance().getNodeDao();
				value.n = nodeDao.queryForId(value.nodeId);
			}catch (Exception e) {
				System.out.println("Could not load node for SyncChangeValue!");
			}
		}
		return value;
	}

	public void setChangeValue(SyncChangeValue changeValue) {
		this.value = changeValue;
		this.changeValue = value.getSerializedString();
	}
	
}
