/*
 * Copyright 2012 Charles du Jeu <charles (at) ajaxplorer.info>
 * This file is part of AjaXplorer.
 *
 * AjaXplorer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AjaXplorer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AjaXplorer.  If not, see <http://www.gnu.org/licenses/>.
 *
 * The latest code can be found at <http://www.ajaxplorer.info/>.
 *
 */
package info.ajaxplorer.synchro.model;

import info.ajaxplorer.client.model.Node;
import info.ajaxplorer.synchro.CoreManager;
import info.ajaxplorer.synchro.SyncJob;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.support.ConnectionSource;

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
				ConnectionSource cs = CoreManager.getInstance().getConnection();
				Dao<Node, String> nodeDao = DaoManager.createDao(cs, Node.class);
				value.n = nodeDao.queryForId(value.nodeId);
			}catch (Exception e) {
				Logger.getRootLogger().error("Could not load node for SyncChangeValue!");
			} finally{
				CoreManager.getInstance().releaseConnection();
			}
		}
		return value;
	}

	public void setChangeValue(SyncChangeValue changeValue) {
		this.value = changeValue;
		this.changeValue = value.getSerializedString();
	}
	
}
