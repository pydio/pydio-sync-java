package info.ajaxplorer.synchro;

import info.ajaxplorer.client.http.AjxpAPI;
import info.ajaxplorer.client.http.RestRequest;
import info.ajaxplorer.client.http.RestStateHolder;
import info.ajaxplorer.client.model.Node;
import info.ajaxplorer.client.model.Property;
import info.ajaxplorer.client.model.Server;
import info.ajaxplorer.synchro.model.SyncChange;
import info.ajaxplorer.synchro.model.SyncChangeValue;
import info.ajaxplorer.synchro.model.SyncLog;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.http.HttpEntity;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.InterruptableJob;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.j256.ormlite.dao.CloseableIterator;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.support.ConnectionSource;

@DisallowConcurrentExecution
public class SyncJob implements InterruptableJob {

	public static Integer NODE_CHANGE_STATUS_FILE_CREATED = 2;
	public static Integer NODE_CHANGE_STATUS_FILE_DELETED = 4;
	public static Integer NODE_CHANGE_STATUS_MODIFIED = 8;
	public static Integer NODE_CHANGE_STATUS_DIR_CREATED = 16;
	public static Integer NODE_CHANGE_STATUS_DIR_DELETED = 32;
	
	public static Integer TASK_DO_NOTHING = 1;
	public static Integer TASK_REMOTE_REMOVE = 2;
	public static Integer TASK_REMOTE_PUT_CONTENT = 4;
	public static Integer TASK_REMOTE_MKDIR = 8;
	public static Integer TASK_LOCAL_REMOVE = 16;
	public static Integer TASK_LOCAL_MKDIR = 32;
	public static Integer TASK_LOCAL_GET_CONTENT = 64;
	
	public static Integer TASK_SOLVE_KEEP_MINE = 128;
	public static Integer TASK_SOLVE_KEEP_THEIR = 256;
	public static Integer TASK_SOLVE_KEEP_BOTH = 512;
	
	public static Integer STATUS_TODO = 2;
	public static Integer STATUS_DONE = 4;
	public static Integer STATUS_ERROR = 8;
	public static Integer STATUS_CONFLICT = 16;
	public static Integer STATUS_CONFLICT_SOLVED = 128;
	public static Integer STATUS_PROGRESS = 32;
	public static Integer STATUS_INTERRUPTED = 64;
	
	Node currentRepository ;
	Dao<Node, String> nodeDao;
	Dao<SyncChange, String> syncChangeDao;
	Dao<SyncLog, String> syncLogDao;
	Dao<Property, String> propertyDao;
	
	
	private String currentJobNodeID;
	private boolean clearSnapshots = false;
	private String direction;
	private File currentLocalFolder;
	
	boolean interruptRequired = false;
	
	private int countResourcesSynchronized = 0;
	private int countFilesUploaded = 0;
	private int countFilesDownloaded = 0;
	private int countResourcesInterrupted = 0;
	private int countResourcesErrors = 0;
	private int countConflictsDetected = 0;
	
	@Override
	public void execute(JobExecutionContext ctx) throws JobExecutionException {
		try {
			currentJobNodeID = ctx.getMergedJobDataMap().getString("node-id");
			if(ctx.getMergedJobDataMap().containsKey("clear-snapshots") && ctx.getMergedJobDataMap().getBooleanValue("clear-snapshots")){
				clearSnapshots = true;
			}else{
				clearSnapshots = false;
			}
			this.run();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void interrupt(){
		interruptRequired = true;
	}
	
	
	public SyncJob() throws URISyntaxException, Exception{
		
        //nodeDao = Manager.getInstance().getNodeDao();		
		
	}
	
	public void run()  throws URISyntaxException, Exception{
		
		Manager.getInstance().updateSynchroState(currentJobNodeID, true);
		
		// instantiate the daos
		ConnectionSource connectionSource = Manager.getInstance().getConnection();
		nodeDao = DaoManager.createDao(connectionSource, Node.class);				
		syncChangeDao = DaoManager.createDao(connectionSource, SyncChange.class);
		syncLogDao = DaoManager.createDao(connectionSource, SyncLog.class);
		propertyDao = DaoManager.createDao(connectionSource, Property.class);
		
		
		currentRepository = Manager.getInstance().getSynchroNode(currentJobNodeID);
		currentRepository.setStatus(Node.NODE_STATUS_LOADING);
		nodeDao.update(currentRepository);
		Server s = new Server(currentRepository.getParent());
		RestStateHolder.getInstance().setServer(s);		
		RestStateHolder.getInstance().setRepository(currentRepository);
		AjxpAPI.getInstance().setServer(s);		
		
		currentLocalFolder = new File(currentRepository.getPropertyValue("target_folder"));
		direction = currentRepository.getPropertyValue("synchro_direction");

        //Manager.getInstance().notifyUser(Manager.getMessage("job_running"), "Synchronizing " + d.getPath() + " against " + s.getUrl());

    	List<SyncChange> previouslyRemaining = syncChangeDao.queryForEq("jobId", currentJobNodeID);
    	Map<String, Object[]> previousChanges = new TreeMap<String, Object[]>();
		boolean unsolvedConflicts = SyncChange.syncChangesToTreeMap(previouslyRemaining, previousChanges);
		Map<String, Object[]> again = null;
		if(unsolvedConflicts){
			Manager.getInstance().notifyUser(Manager.getMessage("job_blocking_conflicts_title"), Manager.getMessage("job_blocking_conflicts"));
			currentRepository.setStatus(Node.NODE_STATUS_ERROR);	        
	        Manager.getInstance().updateSynchroState(currentJobNodeID, false);
	        Manager.getInstance().releaseConnection();
			return;
		}
		
		if(clearSnapshots) {
			this.clearSnapshot("local_snapshot");
			this.clearSnapshot("remote_snapshot");
		}
		List<Node> localSnapshot = new ArrayList<Node>();
		List<Node> remoteSnapshot = new ArrayList<Node>();
		Node localRootNode = loadRootAndSnapshot("local_snapshot", localSnapshot, currentLocalFolder);
		Node remoteRootNode = loadRootAndSnapshot("remote_snapshot", remoteSnapshot, null);
		
		Map<String, Object[]> localDiff = loadLocalChanges(localSnapshot);
		Map<String, Object[]> remoteDiff = loadRemoteChanges(remoteSnapshot);

		if(previousChanges.size() > 0){
			Logger.getRootLogger().debug("Getting previous tasks");
			again = applyChanges(previousChanges);
			syncChangeDao.delete(previouslyRemaining);
			// TODO NOT GOOD : THE NODES REFERENCES BY "again" ARE DELETED
			// AND THE CONFLICTS ARE LOST!
			this.clearSnapshot("remaining_nodes");
		}
		
        Map<String, Object[]> changes = mergeChanges(remoteDiff, localDiff);
        Map<String, Object[]> remainingChanges = applyChanges(changes);
        if(again != null && again.size() > 0){
        	remainingChanges.putAll(again);
        }
        if(remainingChanges.size() > 0){
        	List<SyncChange> c = SyncChange.MapToSyncChanges(remainingChanges, currentJobNodeID);
        	Node remainingRoot = loadRootAndSnapshot("remaining_nodes", null, null);
        	for(int i=0;i<c.size();i++){
        		SyncChangeValue cv = c.get(i).getChangeValue();
        		Node changeNode = cv.n;
        		changeNode.setParent(remainingRoot);
        		if(changeNode.id == 0){ // Not yet created!
        			nodeDao.create(changeNode);
        			Map<String, String> pValues = new HashMap<String, String>();
        			for(Property p:changeNode.properties){
        				pValues.put(p.getName(), p.getValue());
        			}
        			propertyDao.delete(changeNode.properties);
        			Iterator<Map.Entry<String, String>> it = pValues.entrySet().iterator();
        			while(it.hasNext()){
        				Map.Entry<String, String> ent = it.next();
        				changeNode.addProperty(ent.getKey(), ent.getValue());
        			}
        			c.get(i).setChangeValue(cv);
        		}else{
        			nodeDao.update(changeNode);
        		}
        		syncChangeDao.create(c.get(i));
        	}
        }
        
        // handle DL / UP failed! 
        takeLocalSnapshot(localRootNode, null, true);
        takeRemoteSnapshot(remoteRootNode, null, true);
        
		cleanDB();
        
        // INDICATES THAT THE JOB WAS CORRECTLY SHUTDOWN
		currentRepository.setStatus(Node.NODE_STATUS_LOADED);
		currentRepository.setLastModified(new Date());
		nodeDao.update(currentRepository);
        
		SyncLog sl = new SyncLog();
		String status;
		String summary = "";
		if(countConflictsDetected > 0) {
			status = SyncLog.LOG_STATUS_CONFLICTS;
			summary = Manager.getMessage("job_status_conflicts").replace("%d", countConflictsDetected+"");
		}
		else if(countResourcesErrors > 0) {
			status = SyncLog.LOG_STATUS_ERRORS;
			summary = Manager.getMessage("job_status_errors").replace("%d", countResourcesErrors + "");
		}else {
			if(countResourcesInterrupted > 0) status = SyncLog.LOG_STATUS_INTERRUPT;
			else status = SyncLog.LOG_STATUS_SUCCESS;
			if(countFilesDownloaded > 0){
				summary = Manager.getMessage("job_status_downloads").replace("%d", countFilesDownloaded + "" );
			}
			if(countFilesUploaded > 0){
				summary += Manager.getMessage("job_status_uploads").replace("%d", countFilesUploaded + "" );
			}
			if(countResourcesSynchronized > 0){
				summary += Manager.getMessage("job_status_resources").replace("%d", countResourcesSynchronized+ "" );
			}
			if(summary.equals("")){
				summary = Manager.getMessage("job_status_nothing");
			}
		}
		sl.jobDate = (new Date()).getTime();
		sl.jobStatus = status;
		sl.jobSummary = summary;
		sl.synchroNode = currentRepository;
		syncLogDao.create(sl);
		
        Manager.getInstance().updateSynchroState(currentJobNodeID, false);
        Manager.getInstance().releaseConnection();
		
	}
	
	protected Map<String, Object[]> applyChanges(Map<String, Object[]> changes) throws Exception{
		Iterator<Map.Entry<String, Object[]>> it = changes.entrySet().iterator();
		Map<String, Object[]> notApplied = new TreeMap<String, Object[]>();
		RestRequest rest = new RestRequest();
		while(it.hasNext()){
			Map.Entry<String, Object[]> entry = it.next();
			String k = entry.getKey();
			Object[] value = entry.getValue().clone();
			Integer v = (Integer)value[0];
			Node n = (Node)value[1];
			if(this.interruptRequired){
				value[2] = STATUS_INTERRUPTED;
				notApplied.put(k, value);
				continue;
			}
			//Thread.sleep(2000);
			try{
				if(n.isLeaf() && value[2].equals(STATUS_CONFLICT_SOLVED)){
					if(v.equals(TASK_SOLVE_KEEP_MINE)) {
						v = TASK_REMOTE_PUT_CONTENT;
					}
					else if(v.equals(TASK_SOLVE_KEEP_THEIR)) {
						v = TASK_LOCAL_GET_CONTENT;
					}
					else if(v.equals(TASK_SOLVE_KEEP_BOTH)){
						// COPY LOCAL FILE AND GET REMOTE COPY
						File origFile = new File(currentLocalFolder, k);
						File targetFile = new File(currentLocalFolder, k + ".mine");
						InputStream in = new FileInputStream(origFile);
						OutputStream out = new FileOutputStream(targetFile);
						byte[] buf = new byte[1024];
						int len;
						while ((len = in.read(buf)) > 0){
							out.write(buf, 0, len);
						}
						in.close();
						out.close();
						v = TASK_LOCAL_GET_CONTENT;
					}
				}
				
				if(v == TASK_LOCAL_GET_CONTENT){
					
					Node node = new Node(Node.NODE_TYPE_ENTRY, "", null);
					node.setPath(k);
					File targetFile = new File(currentLocalFolder, k);
					this.logChange(Manager.getMessage("job_log_downloading"), k);
					this.synchronousDL(node, targetFile);
					if(!targetFile.exists() || targetFile.length() != Integer.parseInt(n.getPropertyValue("bytesize"))){
						throw new Exception("Error while downloading file from server");
					}
					countFilesDownloaded++;
					
				}else if(v == TASK_LOCAL_MKDIR){
					
					File f = new File(currentLocalFolder, k);
					if(!f.exists()) {
						this.logChange(Manager.getMessage("job_log_mkdir"), k);
						boolean res = f.mkdirs();
						if(!res){
							throw new Exception("Error while creating local folder");
						}
						countResourcesSynchronized++;
					}
					
				}else if(v == TASK_LOCAL_REMOVE){
					
					this.logChange(Manager.getMessage("job_log_rmlocal"), k);
					File f = new File(currentLocalFolder, k);
					if(f.exists()){
						boolean res = f.delete();
						if(!res){
							throw new Exception("Error while removing local resource");
						}
						countResourcesSynchronized++;
					}
					
				}else if(v == TASK_REMOTE_MKDIR){
					
					this.logChange(Manager.getMessage("job_log_mkdir_remote"), k);
					Node currentDirectory = new Node(Node.NODE_TYPE_ENTRY, "", null);
					int lastSlash = k.lastIndexOf("/");
					currentDirectory.setPath(k.substring(0, lastSlash));
					RestStateHolder.getInstance().setDirectory(currentDirectory);
					rest.getStatusCodeForRequest(AjxpAPI.getInstance().getMkdirUri(k.substring(lastSlash+1)));
					JSONObject object = rest.getJSonContent(AjxpAPI.getInstance().getStatUri(k));
					if(!object.has("mtime")){
						throw new Exception("Could not create remote folder");
					}
					countResourcesSynchronized ++;
					
				}else if(v == TASK_REMOTE_PUT_CONTENT){
	
					this.logChange(Manager.getMessage("job_log_uploading"), k);
					Node currentDirectory = new Node(Node.NODE_TYPE_ENTRY, "", null);
					int lastSlash = k.lastIndexOf("/");
					currentDirectory.setPath(k.substring(0, lastSlash));
					RestStateHolder.getInstance().setDirectory(currentDirectory);
					this.synchronousUP(currentDirectory, new File(currentLocalFolder, k));
					JSONObject object = rest.getJSonContent(AjxpAPI.getInstance().getStatUri(k));
					if(!object.has("size") || object.getInt("size") != Integer.parseInt(n.getPropertyValue("bytesize"))){
						throw new Exception("Could not upload file to the server");
					}
					countFilesUploaded ++;
					
				}else if(v == TASK_REMOTE_REMOVE){
					
					this.logChange(Manager.getMessage("job_log_rmremote"), k);
					Node currentDirectory = new Node(Node.NODE_TYPE_ENTRY, "", null);
					int lastSlash = k.lastIndexOf("/");
					currentDirectory.setPath(k.substring(0, lastSlash));
					RestStateHolder.getInstance().setDirectory(currentDirectory);
					rest.getStatusCodeForRequest(AjxpAPI.getInstance().getDeleteUri(k));
					JSONObject object = rest.getJSonContent(AjxpAPI.getInstance().getStatUri(k));
					if(object.has("mtime")){ // Still exists, should be empty!
						throw new Exception("Could not remove the resource from the server");
					}	
					countResourcesSynchronized ++;
					
				}else if(v == TASK_DO_NOTHING && value[2] == STATUS_CONFLICT){
					
					this.logChange(Manager.getMessage("job_log_conflict"), k);
					notApplied.put(k, value);
					countConflictsDetected ++;
					
				}			
			}catch(Exception e){
				e.printStackTrace();
				countResourcesErrors ++;
				value[2] = STATUS_ERROR;
				notApplied.put(k, value);
			}				
		}
		return notApplied;
	}
	
	protected void logChange(String action, String path){
		Manager.getInstance().notifyUser(Manager.getMessage("job_log_balloontitle"), action+ " : "+path);
	}
	
	protected boolean ignoreTreeConflict(Integer remoteChange, Integer localChange, Node remoteNode, Node localNode){
		if(remoteChange != localChange){
			return false;
		}
		if(localChange == NODE_CHANGE_STATUS_DIR_CREATED ||
				localChange == NODE_CHANGE_STATUS_DIR_DELETED
				|| localChange == NODE_CHANGE_STATUS_FILE_DELETED){
			return true;
		}else if(remoteNode.getPropertyValue("md5") != null){
			// Get local node md5
			File f = new File(currentLocalFolder, localNode.getPath());
			String localMd5 = SyncJob.computeMD5(f);
			if(remoteNode.getPropertyValue("md5").equals(localMd5)){
				return true;
			}
			Logger.getRootLogger().debug("MD5 differ " + remoteNode.getPropertyValue("md5") + " - " + localMd5);
		}
		return false;
	}
	
	protected Map<String, Object[]> mergeChanges(Map<String, Object[]> remoteDiff, Map<String, Object[]> localDiff){
		Map<String, Object[]> changes = new TreeMap<String, Object[]>();
		Iterator<Map.Entry<String, Object[]>> it = remoteDiff.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry<String, Object[]> entry = it.next();
			String k = entry.getKey();
			Object[] value = entry.getValue();
			Integer v = (Integer)value[0];
			
			if(localDiff.containsKey(k)){
				Object[] localValue = localDiff.get(k);
				Integer localChange = (Integer)localValue[0];
				if(ignoreTreeConflict(v, localChange, (Node)value[1], (Node)localValue[1])){
					localDiff.remove(k);
					continue;
				}else{				
					value[0] = TASK_DO_NOTHING;
					value[2] = STATUS_CONFLICT;
					localDiff.remove(k);				
					changes.put(k, value);
				}
				continue;
			}
			if(v == NODE_CHANGE_STATUS_FILE_CREATED || v == NODE_CHANGE_STATUS_MODIFIED){
				if(direction.equals("up")){					
					if(v == NODE_CHANGE_STATUS_MODIFIED) localDiff.put(k, value);
				}else{
					value[0] = TASK_LOCAL_GET_CONTENT;
					changes.put(k, value);
				}
			}else if(v == NODE_CHANGE_STATUS_FILE_DELETED || v == NODE_CHANGE_STATUS_DIR_DELETED){
				if(direction.equals("up")){
					if(v == NODE_CHANGE_STATUS_FILE_DELETED) value[0] = NODE_CHANGE_STATUS_MODIFIED;
					else value[0] = NODE_CHANGE_STATUS_DIR_CREATED;
					localDiff.put(k, value);
				}else{
					value[0] = TASK_LOCAL_REMOVE;
					changes.put(k, value);
				}
			}else if(v == NODE_CHANGE_STATUS_DIR_CREATED && !direction.equals("up")){
				value[0] = TASK_LOCAL_MKDIR;
				changes.put(k, value);
			}
		}
		it = localDiff.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry<String, Object[]> entry = it.next();
			String k = entry.getKey();
			Object[] value = entry.getValue();
			Integer v = (Integer)value[0]; 
			if(v == NODE_CHANGE_STATUS_FILE_CREATED || v == NODE_CHANGE_STATUS_MODIFIED){
				if(direction.equals("down")){
					if(v == NODE_CHANGE_STATUS_FILE_CREATED) value[0] = TASK_LOCAL_REMOVE;
					else value[0] = TASK_LOCAL_GET_CONTENT;
				}else{
					value[0] = TASK_REMOTE_PUT_CONTENT;
				}
				changes.put(k, value);
			}else if(v == NODE_CHANGE_STATUS_FILE_DELETED || v == NODE_CHANGE_STATUS_DIR_DELETED){
				if(direction.equals("down")){
					if(v == NODE_CHANGE_STATUS_FILE_DELETED) value[0] = TASK_LOCAL_GET_CONTENT;
					else value[0] = TASK_LOCAL_MKDIR;
				}else{
					value[0] = TASK_REMOTE_REMOVE;
				}
				changes.put(k, value);
			}else if(v == NODE_CHANGE_STATUS_DIR_CREATED){
				if(direction.equals("down")){
					value[0] = TASK_LOCAL_REMOVE;
				}else{				
					value[0] = TASK_REMOTE_MKDIR;
				}
				changes.put(k, value);
			}			
		}
		return changes;
	}		

	protected Node loadRootAndSnapshot(String type, final List<Node> snapshot, File localFolder) throws SQLException{
		
		Map<String, Object> search = new HashMap<String, Object>();
		search.put("resourceType", type);
		search.put("parent_id", currentJobNodeID);
		List<Node> l = nodeDao.queryForFieldValues(search);
		final Node root;
		if(l.size() > 0){
			root = l.get(0);
			if(snapshot != null){
				CloseableIterator<Node> it = root.children.iteratorThrow();
				while(it.hasNext()){
					snapshot.add(it.next());
				}
			}
		}else{
			root = new Node(type, "", currentRepository);
			root.properties = nodeDao.getEmptyForeignCollection("properties");			
			if(localFolder != null) root.setPath(localFolder.getAbsolutePath());
			else root.setPath("");
			nodeDao.create(root);
		}

		return root;
	}
	
	protected void clearSnapshot(String type) throws SQLException{
		List<Node> l = nodeDao.queryForEq("resourceType", type);
		Node root;
		nodeDao.executeRaw("PRAGMA recursive_triggers = TRUE;");
		if(l.size() > 0){
			root = l.get(0);
			nodeDao.delete(root);
		}
	}
	
	protected void cleanDB(){
		// unlinked properties may have not been deleted
		try {
			propertyDao.executeRaw("DELETE FROM b WHERE node_id=0");
		} catch (SQLException e) {			
			e.printStackTrace();
		}
	}
	
	protected void takeLocalSnapshot(final Node rootNode, final List<Node> accumulator, final boolean save) throws Exception{
			
		if(save){
			nodeDao.executeRaw("PRAGMA recursive_triggers = TRUE;");
			nodeDao.delete(rootNode.children);
		}
		//final List<Node> list = new ArrayList<Node>();
		nodeDao.callBatchTasks(new Callable<Void>() {
			public Void call() throws Exception{
				listDirRecursive(currentLocalFolder, rootNode, accumulator, save);
				return null;
			}
		});
		if(save){
			nodeDao.update(rootNode);
		}
	}
	
	protected Map<String, Object[]> loadLocalChanges(List<Node> snapshot) throws Exception{
	
		final Node root = new Node("local_tmp", "", null);
		root.setPath(currentLocalFolder.getAbsolutePath());
		final List<Node> list = new ArrayList<Node>();
		takeLocalSnapshot(root, list, false);		
		
		Map<String, Object[]> diff = this.diffNodeLists(list, snapshot);
		//System.out.println(diff);
		return diff;
		
	}
	
	protected void listDirRecursive(File directory, Node root, List<Node> accumulator, boolean save) throws SQLException{
		
		File[] children = directory.listFiles();
		for(int i=0;i<children.length;i++){
			Node newNode = new Node(Node.NODE_TYPE_ENTRY, children[i].getName(), root);
			if(save) nodeDao.create(newNode);
			String p =children[i].getAbsolutePath().substring(root.getPath(true).length()).replace("\\", "/");
			newNode.setPath(p);
			newNode.properties = nodeDao.getEmptyForeignCollection("properties");			
			newNode.setLastModified(new Date(children[i].lastModified()));
			if(children[i].isDirectory()){
				listDirRecursive(children[i], root, accumulator, save);
			}else{				
				newNode.addProperty("bytesize", String.valueOf(children[i].length()));
				newNode.setLeaf();
			}
			if(save) nodeDao.update(newNode);
			if(accumulator != null){
				accumulator.add(newNode);
			}
		}
		
	}
	
	protected void takeRemoteSnapshot(final Node rootNode, final List<Node> accumulator, final boolean save) throws Exception{
		
		if(save){
			nodeDao.executeRaw("PRAGMA recursive_triggers = TRUE;");
			nodeDao.delete(rootNode.children);
		}
		RestRequest r = new RestRequest();
		URI uri = AjxpAPI.getInstance().getRecursiveLsDirectoryUri(rootNode);
		Document d = r.getDocumentContent(uri);
		//this.logDocument(d);
		
		final NodeList entries = d.getDocumentElement().getChildNodes();
		if(entries != null && entries.getLength() > 0){
			nodeDao.callBatchTasks(new Callable<Void>() {
				public Void call() throws Exception{
					parseNodesRecursive(entries, rootNode, accumulator, save);
					return null;
				}
			});			
		}	
		if(save){
			nodeDao.update(rootNode);
		}
	}
	
	protected Map<String, Object[]> loadRemoteChanges(List<Node> snapshot) throws URISyntaxException, Exception{				
		
		final Node root = new Node("remote_root", "", currentRepository);
		root.setPath("/");
		final ArrayList<Node> list = new ArrayList<Node>();
		takeRemoteSnapshot(root, list, false);
		
		Map<String, Object[]> diff = this.diffNodeLists(list, snapshot);
		//System.out.println(diff);
		return diff;
	}
	
	protected void parseNodesRecursive(NodeList entries, Node parentNode, List<Node> list, boolean save) throws SQLException{
		for(int i=0; i< entries.getLength(); i++){
			org.w3c.dom.Node xmlNode = entries.item(i);
			Node entry = new Node(Node.NODE_TYPE_ENTRY, "", parentNode);
			if(save) nodeDao.create(entry);
			entry.properties = nodeDao.getEmptyForeignCollection("properties");
			entry.initFromXmlNode(xmlNode);
			if(save) nodeDao.update(entry);
			if(list != null){
				list.add(entry);
			}
			if(xmlNode.getChildNodes().getLength() > 0){
				parseNodesRecursive(xmlNode.getChildNodes(), parentNode, list, save);				
			}
		}
		
	}
	
	protected Map<String, Object[]> diffNodeLists(List<Node> current, List<Node> snapshot){
		List<Node> saved = new ArrayList<Node>(snapshot);
		TreeMap<String, Object[]> diff = new TreeMap<String, Object[]>();
		Iterator<Node> cIt = current.iterator();
		while(cIt.hasNext()){
			Node c = cIt.next();
			Iterator<Node> sIt = saved.iterator();
			boolean found = false;
			while(sIt.hasNext() && !found){
				Node s = sIt.next();
				if(s.getPath(true).equals(c.getPath(true))){
					found = true;
					if(c.isLeaf()){// FILE : compare date & size
						if(c.getLastModified().after(s.getLastModified()) || !c.getPropertyValue("bytesize").equals(s.getPropertyValue("bytesize")) ){
							diff.put(c.getPath(true), makeTodoObject(NODE_CHANGE_STATUS_MODIFIED, c));
						}
					}
					saved.remove(s);
				}
			}
			if(!found){				
				diff.put(c.getPath(true), makeTodoObject((c.isLeaf()?NODE_CHANGE_STATUS_FILE_CREATED:NODE_CHANGE_STATUS_DIR_CREATED), c));
			}
		}
		if(saved.size()>0){
			Iterator<Node> sIt = saved.iterator();
			while(sIt.hasNext()){
				Node s = sIt.next();
				diff.put(s.getPath(true), makeTodoObject((s.isLeaf()?NODE_CHANGE_STATUS_FILE_DELETED:NODE_CHANGE_STATUS_DIR_DELETED), s));
			}
		}
		return diff;
	}
	
	protected Object[] makeTodoObject(Integer nodeStatus, Node node){
		Object[] val = new Object[3];
		val[0] = nodeStatus;
		val[1] = node;
		val[2] = STATUS_TODO;
		return val;
	}
	
	protected void synchronousUP(Node folderNode, File sourceFile) throws Exception{

		long totalSize = sourceFile.length();
		if(!sourceFile.exists() || totalSize == 0){
			throw new FileNotFoundException("Cannot find file !");
		}
		
    	RestRequest rest = new RestRequest();
    	// Ping to make sure the user is logged
    	rest.getStatusCodeForRequest(AjxpAPI.getInstance().getAPIUri());
    	//final long filesize = totalSize; 
    	String targetName = sourceFile.getName();
    	rest.getStringContent(AjxpAPI.getInstance().getUploadUri(folderNode.getPath(true)), null, sourceFile, targetName);
			          		
	}
	
	protected void synchronousDL(Node node, File targetFile) throws Exception{
    	RestRequest rest = new RestRequest();
    	//rest.getStatusCodeForRequest(DataProvider.dataHolder().urlHandler.getAPIUri());
    	
        int postedProgress = 0;
        int buffersize = 16384;
        int count = 0;
        
		URI uri = AjxpAPI.getInstance().getDownloadUri(node.getPath(true));
		HttpEntity entity = rest.getNotConsumedResponseEntity(uri, null);
		long fullLength = entity.getContentLength();
		
		InputStream input = entity.getContent();
		BufferedInputStream in = new BufferedInputStream(input,buffersize);
        
		FileOutputStream output = new FileOutputStream(targetFile.getPath());
		BufferedOutputStream out = new BufferedOutputStream(output);

        byte data[] = new byte[buffersize];
        int total = 0;
                    
        long startTime = System.nanoTime();
        long lastTime = startTime;
        int lastTimeTotal = 0;
        
        long secondLength = 1000000000;
        long interval = (long) 2*secondLength;	        
        
        while ( (count = in.read(data)) != -1 ) {
        	long duration = System.nanoTime()-lastTime ;
        	
        	int tmpTotal = total+count;
            // publishing the progress....
            int tmpProgress = (int)(tmpTotal*100/fullLength);
            if (tmpProgress-postedProgress > 0 || duration > secondLength) {
            	if (duration > interval) {
                	lastTime = System.nanoTime();
                	long lastTimeBytes = (long)((tmpTotal-lastTimeTotal)*secondLength/1024/1000);
                	long speed = (lastTimeBytes/(duration));
                	double bytesleft =(double)(((double)fullLength-(double)tmpTotal)/1024); 
                	@SuppressWarnings("unused")
					double ETC = bytesleft/(speed*10);
            	}
            	postedProgress=tmpProgress;
            }
            out.write(data, 0, count);
            total = tmpTotal;
        }	        
        out.flush();
    	if(out != null) out.close();
        if(in != null) in.close();
		
	}
	
	public static String computeMD5(File f){
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e1) {
			e1.printStackTrace();
			return "";
		}
		InputStream is;
		try {
			is = new FileInputStream(f);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
			return "";
		}				
		byte[] buffer = new byte[8192];
		int read = 0;
		try {
			while( (read = is.read(buffer)) > 0) {
				digest.update(buffer, 0, read);
			}		
			byte[] md5sum = digest.digest();
			BigInteger bigInt = new BigInteger(1, md5sum);
			String output = bigInt.toString(16);
			if(output.length() < 32){
				// PAD WITH 0
				while(output.length() < 32) output = "0" + output;
			}
			return output;
		}
		catch(IOException e) {
			//throw new RuntimeException("Unable to process file for MD5", e);
			return "";
		}
		finally {
			try {
				is.close();
			}
			catch(IOException e) {
				//throw new RuntimeException("Unable to close input stream for MD5 calculation", e);
				return "";
			}
		}		
	}
	
	
	protected void logMemory(){
		System.out.println("Total memory (bytes): " + 
		        Math.round(Runtime.getRuntime().totalMemory() / (1024 * 1024)) + "M");
	}
	
	protected void logDocument(Document d){
		try{
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			//initialize StreamResult with File object to save to file
			StreamResult result = new StreamResult(new StringWriter());
			DOMSource source = new DOMSource(d);
			transformer.transform(source, result);	
			String xmlString = result.getWriter().toString();
			System.out.println(xmlString);
		}catch(Exception e){
		}		
	}

}

