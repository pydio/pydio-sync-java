/*
 * Copyright 2012 Charles du Jeu <charles (at) pyd.io>
 * This file is part of Pydio.
 *
 * Pydio is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Pydio is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pydio.  If not, see <http://www.gnu.org/licenses/>.
 *
 * The latest code can be found at <http://pyd.io/>..
 *
 */
package io.pyd.synchro;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;
import info.ajaxplorer.client.model.Node;
import info.ajaxplorer.client.model.Property;
import info.ajaxplorer.client.model.Server;
import info.ajaxplorer.client.util.RdiffProcessor;
import io.pyd.synchro.gui.JobEditor;
import io.pyd.synchro.model.SyncChange;
import io.pyd.synchro.model.SyncLog;
import io.pyd.synchro.model.SyncLogDetails;
import io.pyd.synchro.progressmonitor.IProgressMonitor;
import io.pyd.synchro.progressmonitor.StandardProgressMonitor;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.UnableToInterruptJobException;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

public class CoreManager {
	
	private Logger log = Logger.getLogger(getClass());
	
	
	public static String[] EXCLUDED_ACCESS_TYPES = {"ajxp_conf", "ajxp_shared", "mysql", "imap", "jsapi"};
	public String[] EXCLUDED_FILES_START = {".", "Thumbs.db"};
	public String[] EXCLUDED_FILES_END = {};

	Scheduler scheduler;
	protected static CoreManager instance;	
	protected ResourceBundle messages;
	private RdiffProcessor rdiffProc;
	//private HashMap<String, WatchDir> watchers;
	public static String defaultHome;
	public boolean firstRun = false;
	private boolean schedulerInitialized = false; 	
	
	private IProgressMonitor progressMonitor;

	public IProgressMonitor getProgressMonitor() {
		if (progressMonitor == null) {
			progressMonitor = new StandardProgressMonitor();
		}
		return progressMonitor;
	}

	public RdiffProcessor getRdiffProc() {
		return rdiffProc;
	}

	public void setRdiffProc(RdiffProcessor rdiffProc) {
		this.rdiffProc = rdiffProc;
	}

	private boolean daemon;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		String language = null;
        String country = null;
        boolean daemon = false;
        RdiffProcessor proc=null ;
        int deferInit = -1;
    	for(int i = 0; i < args.length ; i++){
        	if(args[i].startsWith("rdiff=")){
        		String path = args[i].substring(new String("rdiff=").length());
        		if(i<args.length-1){
	        		for(int j=i+1;j<args.length;j++){
	        			if(args[j].contains("=")) break;
	        			path += " " + args[j];
	        		}
        		}
        		proc = new RdiffProcessor(path);
        	}else if(args[i].startsWith("default_home=")){
        		String defHome = args[i].substring(new String("default_home=").length());
        		if(i<args.length-1){
	        		for(int j=i+1;j<args.length;j++){
	        			if(args[j].contains("=")) break;
	        			defHome += " " + args[j];
	        		}
        		}
        		CoreManager.defaultHome = defHome;
        	}else if(args[i].startsWith("daemon=")){
        		daemon = args[i].substring(new String("daemon=").length()).equals("true");
        	}else if(args[i].startsWith("defer_init=")){
        		deferInit = Integer.valueOf(args[i].substring(new String("defer_init=").length()));
        	}else if(args[i].startsWith("lang=")){
        		language = args[i].substring(new String("lang=").length());
        	}else if(args[i].startsWith("country=")){
        		country = args[i].substring(new String("country=").length());
        	}else if(args[i].startsWith("winLangID=")){
        		String lId = args[i].substring(new String("winLangID=").length());
        		try{
        			ResourceBundle b = ResourceBundle.getBundle("WindowsLanguages");
        			String found = b.getString(lId);
        			if(found!=null){
        				if(found.contains("-")) {
        					String[] parts = found.split("-");
        					language = parts[0];
        					country = parts[1].toUpperCase();
        				}else{
        					language = found;
        					country = found.toUpperCase();
        				}
        			}
        		}catch(Exception e){        			
        		}
        	}
    	}
    	if(language != null && country == null) country = language.toUpperCase();
        if (language == null) {
            language = new String("en");
            country = new String("US");
        } 
        if(proc == null){
        	proc = new RdiffProcessor("/usr/local/bin/rdiff");
        }
        Locale currentLocale = new Locale(language, country);     

		Logger.getRootLogger().info("Rdiff Processor active? " + (proc.rdiffEnabled()?"Yes" :"No"));
		CoreManager.instanciate(currentLocale, daemon);
		CoreManager.getInstance().setRdiffProc(proc);
		if(deferInit > 0 ){
			Timer t = new Timer();
			t.schedule(new TimerTask() {
				
				@Override
				public void run() {
					CoreManager.getInstance().initScheduler();
				}
			}, 1000*deferInit);
		}else{
			CoreManager.getInstance().initScheduler();
			/*
			Map<String, String> headers = new HashMap<String, String>();
			headers.put("Ajxp-User", "admin");
			headers.put("Ajxp-Password", "admin");
			AjxpWebSocket awS = new AjxpWebSocket(URI.create("ws://192.168.0.18:8090/ajaxplorer"), headers);
			Thread t = new Thread(awS);
			t.start();
			try {
				t.join();
			} catch ( InterruptedException e1 ) {
				e1.printStackTrace();
			} finally {
				awS.close();
			}			
			*/
		}
		

    	//Manager.defaultHome = null;
	}
	
	public static void stop(String[] args){
		int res = CoreManager.getInstance().close();
		System.exit(res);
	}
	
	public static String getMessage(String s){
		return CoreManager.instance.messages.getString(s);
	}
	
	public static void instanciate(Locale locale, boolean daemon){
		CoreManager.instance = new CoreManager(locale, daemon);
	}
	public static CoreManager getInstance(){
		return CoreManager.instance;
	}
	
	public void notifyUser(final String title, final String message, final String nodeId, final boolean forceDisplay){
		Logger.getRootLogger().info("Notify User - " + message);
	}
	
	public void notifyUser(final String title, final String message, final String nodeId){
		notifyUser(title, message, nodeId, false);
	}
	
	public void updateSynchroState(final Node node, final boolean running){
	}
	
	public CoreManager(){
	}
	
	public CoreManager(Locale locale, boolean daemon){
		messages = ResourceBundle.getBundle("strings/MessagesBundle", locale);
		this.daemon = daemon;
		boolean alreadyExists = false;
		try {
			alreadyExists = initializeDAO();
		}catch(SQLException e){
			Logger.getRootLogger().error("Synchro", e);
		}
	    try {			
            scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();
        } catch (SchedulerException se) {
            Logger.getRootLogger().error("Synchro", se);
        }
	    if(!alreadyExists) this.firstRun = true;

	}
	
	public boolean isDaemon(){
		return this.daemon;
	}
	
	public int close(){
		try {
			this.notifyUser(getMessage("notif_shuttingdown_title"), getMessage("notif_shuttingdown"), null);
			Set<JobKey> keys = scheduler.getJobKeys(GroupMatcher.jobGroupEquals("sync"));
			Iterator<JobKey> it = keys.iterator();
			while(it.hasNext()){
				scheduler.interrupt(it.next());
			}
			scheduler.shutdown(true);
			return 0;
		} catch (SchedulerException e) {
			Logger.getLogger("main").error("Closing scheduler", e);
			return 1;
		}
	}
	
	public boolean pauseAll(){
		try {
			scheduler.standby();
			return true;
		} catch (SchedulerException e) {
			Logger.getRootLogger().error("Synchro", e);
			return false;
		}
	}
	
	public boolean restartAll(){
		try {
			scheduler.start();
			return true;
		} catch (SchedulerException e) {
			Logger.getRootLogger().error("Synchro", e);
			return false;
		}
	}
	
	private int connectionRefs = 0;
	private String databaseUrl;
	private ConnectionSource connectionSource;
	
	public synchronized ConnectionSource getConnection(){
		//Logger.getRootLogger().info("Incrementing refs");
		if(connectionRefs > 0 && connectionSource != null){
			connectionRefs ++;
			return connectionSource;
		}else{
			try {
				connectionSource = new JdbcConnectionSource(databaseUrl);
			} catch (SQLException e) {
				Logger.getRootLogger().error("Synchro", e);
				return null;
			}
			connectionRefs ++;
			return connectionSource;
		}
	}
	
	public synchronized void releaseConnection(){
		//Logger.getRootLogger().info("Decrementing refs");
		connectionRefs --;
		if(connectionRefs == 0){
			try {
				connectionSource.close();
			} catch (SQLException e) {
				Logger.getRootLogger().error("Synchro", e);
			}
			connectionSource = null;
			System.gc();
			//Logger.getRootLogger().info("Releasing connection");
		}
	}
	
	/**
	 * Return home directory for DB
	 * @return
	 */
	public String getDBHomeDir() {
		return System.getProperty("user.home")+System.getProperty("file.separator") + ".pydio";
	}
	
	protected boolean initializeDAO() throws SQLException{

		String dbHomeDir = getDBHomeDir();
		
		log.info("CoreManager " + getClass() + " initializeDao: " + dbHomeDir);
		
		File work = new File(dbHomeDir);
		if(!work.exists()) work.mkdirs();
		File dbFile = new File(work, "ajxpsync.db");
		boolean dbAlreadyCreated = dbFile.exists();
		databaseUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();		


		if(!dbAlreadyCreated){
			ConnectionSource cs = this.getConnection();
			TableUtils.createTable(cs, Node.class);
			TableUtils.createTable(cs, Property.class);
			TableUtils.createTable(cs, SyncChange.class);
			TableUtils.createTable(cs, SyncLog.class);
			TableUtils.createTable(cs, SyncLogDetails.class);

			DaoManager.createDao(cs, Node.class).executeRaw("CREATE TRIGGER on_delete_cascade AFTER DELETE ON a BEGIN\n" + 
					"  DELETE FROM b WHERE node_id=old.id;\n" +
					"  DELETE FROM a WHERE parent_id=old.id;\n" +
					"END;");
			this.releaseConnection();
		} else {
			// we have added some new tables?
			// backwar compatibility
			ConnectionSource cs = this.getConnection();
			TableUtils.createTableIfNotExists(cs, SyncLogDetails.class);
			this.releaseConnection();
		}
		return dbAlreadyCreated;
	}
	
	public void deleteSynchroNode(Node node) throws SchedulerException, SQLException{
		this.unscheduleJob(node);		
		ConnectionSource cSource = this.getConnection();
		Dao<Node, String>nodeDao = DaoManager.createDao(cSource, Node.class);
		nodeDao.executeRaw("PRAGMA recursive_triggers = TRUE;");
		nodeDao.delete(node);
		nodeDao.executeRaw("DELETE FROM b WHERE node_id=0;");
		this.releaseConnection();
	}
	
	public Node updateSynchroNode(Map<String, String> data, Node node) throws SQLException, URISyntaxException{
		Server s;		
		ConnectionSource cSource = this.getConnection();
		Dao<Node, String> nDao = DaoManager.createDao(cSource, Node.class);
		Dao<Property, String> pDao = DaoManager.createDao(cSource, Property.class);
		if(node == null){
			s = new Server(data.get("HOST"), data.get("HOST"), data.get("LOGIN"), data.get("PASSWORD"), data.get("TRUST_SSL").equals("true"), false);			
			Node serverNode = s.createDbNode(nDao);
			node = new Node(Node.NODE_TYPE_REPOSITORY, data.get("REPOSITORY_LABEL"), serverNode);
			nDao.create(node);
			node.setParent(serverNode);
			node.properties = nDao.getEmptyForeignCollection("properties");
			node.addProperty("repository_id", data.get("REPOSITORY_ID"));
			node.addProperty("target_folder", data.get("TARGET"));
			node.addProperty("trust_ssl", data.get("TRUST_SSL"));
			node.addProperty("synchro_active", data.get("ACTIVE"));
			node.addProperty("synchro_direction", data.get("DIRECTION"));
			node.addProperty("synchro_interval", data.get("INTERVAL"));
			node.addProperty(JobEditor.AUTO_KEEP_REMOTE,
					data.get(JobEditor.AUTO_KEEP_REMOTE_DATA));
			node.addProperty(JobEditor.AUTO_KEEP_LOCAL,
					data.get(JobEditor.AUTO_KEEP_LOCAL_DATA));

			node.addProperty("sync_running_status", "-1");
			nDao.update(node);
			try {
				this.scheduleJob(node);
			} catch (SchedulerException e) {
				Logger.getRootLogger().error("Synchro", e);
			}
		}else{
			boolean serverChanges = false;
			boolean intervalChanges = false;
			
			// UPDATE SERVER NODE
			s = new Server(node.getParent());
			String crtHost = s.getUrl();
			if(!crtHost.equals(data.get("HOST"))){
				s.setUrl(data.get("HOST"));
				s.setLabel(data.get("HOST"));
				serverChanges = true;
			}
			if(!s.getUser().equals(data.get("LOGIN"))){
				s.setUser(data.get("LOGIN"));
				serverChanges = true;
			}
			if(!s.getPassword().equals(data.get("PASSWORD"))){
				s.setPassword(data.get("PASSWORD"));
				serverChanges = true;
			}			
			
			// UPDATE REPOSITORY NODE
			node.setLabel(data.get("REPOSITORY_LABEL"));
			node.setPath("/");
			Collection<Property> props = node.properties;
			Collection<Property> toSave = new ArrayList<Property>();

			s.setTrustSSL(data.get("TRUST_SSL").equals("true"));
			// check if we already have property auto keep remote & auto keep
			// local
			if (node.getPropertyValue(JobEditor.AUTO_KEEP_REMOTE) == null) {
				node.addProperty(JobEditor.AUTO_KEEP_REMOTE,
						data.get(JobEditor.AUTO_KEEP_REMOTE_DATA));
			}
			if (node.getPropertyValue(JobEditor.AUTO_KEEP_LOCAL) == null) {
				node.addProperty(JobEditor.AUTO_KEEP_LOCAL,
						data.get(JobEditor.AUTO_KEEP_LOCAL_DATA));
			}
			for(Property p:props){
				if(p.getName().equals("repository_id")
						&& (p.getValue() == null || !p.getValue().equals(data.get("REPOSITORY_ID")))) {
					p.setValue(data.get("REPOSITORY_ID"));
					serverChanges = true;
					toSave.add(p);
				}
				else if(p.getName().equals("target_folder")
						&& (p.getValue() == null || !p.getValue().equals(data.get("TARGET")))) {
					p.setValue(data.get("TARGET"));
					serverChanges = true;
					toSave.add(p);
				}
				else if(p.getName().equals("synchro_active")
						&& !p.getValue().equals(data.get("ACTIVE"))) {
					p.setValue(data.get("ACTIVE"));
					serverChanges = true;
					toSave.add(p);
				}
				else if(p.getName().equals("synchro_direction")
						&& !p.getValue().equals(data.get("DIRECTION"))) {
					p.setValue(data.get("DIRECTION"));
					serverChanges = true;
					toSave.add(p);
				}
				else if(p.getName().equals("synchro_interval") 
						&& !p.getValue().equals(data.get("INTERVAL"))) {
					p.setValue(data.get("INTERVAL"));
					intervalChanges = true;
					toSave.add(p);
				}
				else if(p.getName().equals("trust_ssl") 
						&& !p.getValue().equals(data.get("TRUST_SSL"))) {
					p.setValue(data.get("TRUST_SSL"));
					intervalChanges = true;
					toSave.add(p);
				}
				else if (p.getName().equals(JobEditor.AUTO_KEEP_REMOTE)
						&& !p.getValue().equals(
								data.get(JobEditor.AUTO_KEEP_REMOTE_DATA))) {
					p.setValue(data.get(JobEditor.AUTO_KEEP_REMOTE_DATA));
					serverChanges = true;
					toSave.add(p);
				} else if (p.getName().equals(JobEditor.AUTO_KEEP_LOCAL)
						&& !p.getValue().equals(
								data.get(JobEditor.AUTO_KEEP_LOCAL_DATA))) {
					p.setValue(data.get(JobEditor.AUTO_KEEP_LOCAL_DATA));
					serverChanges = true;
					toSave.add(p);
				}
			}

			try {
				if(serverChanges){

					// STOP CURRENT
					this.unscheduleJob(node);
					
					// UPDATE DB
					s.updateDbNode(nDao, pDao);
					if(toSave.size() > 0){
						for(Property ps:toSave) pDao.update(ps);
					}
					nDao.update(node);	
					
					// RESCHEDULE AND CLEAN SNAPSHOTS
					this.scheduleJob(node, true);
					
				}else if(intervalChanges){
					
					// UPDATE DB
					for(Property ps:toSave) pDao.update(ps);
					nDao.refresh(node);
					
					// CHANGE INTERVAL
					if(node.getPropertyValue("synchro_active").equals("true")){
						this.changeJobInterval(node);						
					}
					
				}
			} catch (SchedulerException e) {
				Logger.getRootLogger().error("Synchro", e);
			}
		}
		this.releaseConnection();
		
		return node;
	}
	
	public boolean openLocalTarget(Node synchroNode){
		File f = new File(synchroNode.getPropertyValue("target_folder"));
		if(f.exists()){
			//Program.launch(synchroNode.getPropertyValue("target_folder"));
		}
		return true;
	}
	
	public boolean openRemoteTarget(Node synchroNode){
		String server = synchroNode.getParent().getLabel();
		if(!server.endsWith("/")) server += "/";
		//Program.launch(server + "?repository_id=" + synchroNode.getPropertyValue("repository_id") + "&folder=%2F");
		return true;
	}
	
	public boolean changeSynchroState(Node synchroNode, boolean state){
		
		if(!state){
			// MAKE SURE TO INTERRUPT JOB
			try {
				scheduler.interrupt(new JobKey(String.valueOf(synchroNode.id), "sync"));
				this.unscheduleJob(synchroNode);
			} catch (UnableToInterruptJobException e) {
				Logger.getRootLogger().error("Synchro", e);
			} catch (SchedulerException e) {
				Logger.getRootLogger().error("Synchro", e);
			}
		}
		ConnectionSource cSource = this.getConnection();
		Dao<Property, Integer> pDao;
		try {
			pDao = DaoManager.createDao(cSource, Property.class);
			synchroNode.setProperty("synchro_active", (state?"true":"false"), pDao);
			this.releaseConnection();
		} catch (SQLException e) {
			Logger.getRootLogger().error("Synchro", e);
			this.releaseConnection();
			return false;
		}
		if(state){
			try {
				this.scheduleJob(synchroNode, true);
			} catch (SchedulerException e) {
				Logger.getRootLogger().error("Synchro", e);
			}
		}
		return true;
	}
		
	public String makeJobLabel(Node node, boolean shortFormat){
		if(node == null) return "null node!";
		String s = getMessage("joblabel_format");
		s = s.replace("REPO", node.getLabel());
		URI uri = URI.create(node.getParent().getLabel());
		if(uri != null){
			s = s.replace("HOST", uri.getHost());
		}
		if(!shortFormat){
			s = s.replace("LOCAL", node.getPropertyValue("target_folder"));
		}else{
			File f = new File(node.getPropertyValue("target_folder"));
			s = s.replace("LOCAL", f.getName());			
		}
		if(node.getPropertyValue("synchro_active").equals("false")){
			s = s + " - " + getMessage("sync_short_status_inactive");
		}else if(node.getStatus() == Node.NODE_STATUS_LOADING){
			s = s + " - " + getMessage("sync_short_status_running");
		}else if(node.getStatus() == Node.NODE_STATUS_ERROR){
			s = s + " - " + getMessage("sync_short_status_error");
		}
		return s.toString();
	}
	
	public Node getSynchroNode(String nodeId, Dao<Node, String> nDao){
		try {
			Node n = nDao.queryForId(nodeId);
			n.setParent(nDao.queryForId(String.valueOf(n.getParent().id)));
			return n;
		} catch (SQLException e) {
			Logger.getRootLogger().error("Synchro", e);
			return null;
		}		
	}
	
	public Node getSynchroNode(String nodeId){
		ConnectionSource cSource = this.getConnection();
		Dao<Node, String> nDao;
		try {
			nDao = DaoManager.createDao(cSource, Node.class);
			Node n = nDao.queryForId(nodeId);
			n.setParent(nDao.queryForId(String.valueOf(n.getParent().id)));
			this.releaseConnection();
			return n;
		} catch (SQLException e) {
			Logger.getRootLogger().error("Synchro", e);
			this.releaseConnection();
			return null;
		}
	}
	
	public Collection<Node> listSynchroNodes(){
		if(!schedulerInitialized){
			this.initScheduler();
		}
		ConnectionSource cSource = this.getConnection();
		Collection<Node> n = new ArrayList<Node>();
		try {
			Dao<Node, String> nDao = DaoManager.createDao(cSource, Node.class);
			Collection<Node> servers  =  nDao.queryForEq("resourceType", Node.NODE_TYPE_SERVER);
			for(Node s:servers){
				for(Node c:s.children){
					if(c.getResourceType().equals(Node.NODE_TYPE_REPOSITORY)){
						n.add(c);
					}
				}
			}
		} catch (SQLException e) {
			Logger.getRootLogger().error("Synchro", e);
		} finally {
			this.releaseConnection();
		}
		return n;
	}
		
	protected void initScheduler(){
		if(schedulerInitialized) return;
		schedulerInitialized = true;
		try {
			Collection<Node> l = listSynchroNodes();
			ConnectionSource cSource = this.getConnection();
			Dao<Node, String> nDao = DaoManager.createDao(cSource, Node.class);

			for(Node n:l){
				boolean notCorrectlyShutdown = false;
				if(n.getStatus() == Node.NODE_STATUS_LOADING){
					notCorrectlyShutdown = true;	
					n.setStatus(Node.NODE_STATUS_ERROR);
					nDao.update(n);
				}
				scheduleJob(n, notCorrectlyShutdown);
			}
		} catch (Exception e) {
			Logger.getRootLogger().error("Synchro", e);
		} finally {
			this.releaseConnection();
		}
	}

	public void scheduleJob(Node n) throws SchedulerException{				
		this.scheduleJob(n, false);
	}
	
	public void scheduleJob(Node n, boolean firstTriggerClear) throws SchedulerException{				
		
		if(n.getPropertyValue("synchro_active").equals("false")) return;
		
        JobDetail job = newJob(SyncJob.class)
        		.withIdentity(String.valueOf(n.id), "sync")
        		.usingJobData("node-id", String.valueOf(n.id))
				.usingJobData(JobEditor.AUTO_KEEP_REMOTE,
						n.getPropertyValue(JobEditor.AUTO_KEEP_REMOTE))
				.usingJobData(JobEditor.AUTO_KEEP_LOCAL,
						n.getPropertyValue(JobEditor.AUTO_KEEP_LOCAL))
        		.build();

        if(firstTriggerClear){
        	
            Trigger trigger1 = newTrigger()
            		.withIdentity("onetime-"+String.valueOf(n.id), "ajxp")
            		.usingJobData("clear-snapshots", true)
            		.startNow()
            		.build();

            Trigger trigger2 = newTrigger()
            		.withIdentity("periodic-"+String.valueOf(n.id), "ajxp")            		        	
            		.withSchedule(getSSBFromString(n.getPropertyValue("synchro_interval")))
            		.forJob(job)
            		.build();
            
            scheduler.scheduleJob(job, trigger1);
            scheduler.scheduleJob(trigger2);  	
        	
        }else{
        	
            Trigger trigger = newTrigger()
            		.withIdentity("periodic-"+String.valueOf(n.id), "ajxp")
            		//.startNow()        		
            		.withSchedule(getSSBFromString(n.getPropertyValue("synchro_interval")))
            		.build();

            scheduler.scheduleJob(job, trigger);	
            
            //this.startWatcher(n);
        	
        }

        Trigger localTrigger = newTrigger()
        		.withIdentity("local-periodic-"+String.valueOf(n.id), "ajxp")            		        	
        		.withSchedule(getSSBFromString("local_monitor"))
        		.forJob(job)
        		.usingJobData("local-monitoring", true)
        		.build();
        
        scheduler.scheduleJob(localTrigger);

	}
	/*
	public void stopWatcher(String nodeId){
		if(this.watchers != null && this.watchers.containsKey(nodeId)){
			this.watchers.get(nodeId).stopProcessing();
		}
	}
	
	public void startWatcher(String nodeId){		
		this.startWatcher(getSynchroNode(nodeId));
	}
		
	public void startWatcher(Node n){
		if(this.watchers == null){
			this.watchers = new HashMap<String, WatchDir>();
		}
		try {
			WatchDir w = new WatchDir(n, this); 
			w.start();
			this.watchers.put(String.valueOf(n.id), w);
		} catch (IOException e) {
			Logger.getRootLogger().error("Synchro", e);
		}

	}
	*/
	
	public void unscheduleJob(Node n) throws SchedulerException{
		
		scheduler.deleteJob(new JobKey(String.valueOf(n.id), "sync"));
		
	}
	
	protected SimpleScheduleBuilder getSSBFromString(String s){
		SimpleScheduleBuilder ssB = null;
		if(s.equals("hour")){
			ssB = simpleSchedule().withIntervalInHours(2).repeatForever();
		}else if(s.equals("minute")){
			ssB = simpleSchedule().withIntervalInMinutes(10).repeatForever();
		}else if(s.equals("local_monitor")){
			ssB = simpleSchedule().withIntervalInSeconds(55).repeatForever();
		}else if(s.equals("day")){
			ssB = simpleSchedule().withIntervalInHours(24).repeatForever();
		}
		return ssB;
	}
	
	public void changeJobInterval(Node n) throws SchedulerException{
		
		SimpleScheduleBuilder ssB = getSSBFromString(n.getPropertyValue("synchro_interval"));
		if(ssB != null){
	        Trigger trigger = newTrigger()
	        		.withIdentity("periodic-"+String.valueOf(n.id), "ajxp")
	        		.startNow()        		
	        		.withSchedule(ssB)
	        		.build();
	        scheduler.rescheduleJob(new TriggerKey("periodic-"+String.valueOf(n.id), "ajxp"), trigger);
		}
	}
	
	public void triggerJobNow(Node n, boolean renewSnapshots) throws SchedulerException{
		
		JobKey jK = new JobKey(String.valueOf(n.id), "sync");
		JobDetail job = scheduler.getJobDetail(jK);
		if(job != null){
			Trigger existing = scheduler.getTrigger(new TriggerKey("onetime-"+String.valueOf(n.id), "ajxp"));
			List<JobExecutionContext> jobs = scheduler.getCurrentlyExecutingJobs();
			if(!jobs.contains(job) && existing == null){
				Logger.getRootLogger().info("Triggerring job now");
		        Trigger trigger = newTrigger()
		        		.withIdentity("onetime-"+String.valueOf(n.id), "ajxp")
		        		.forJob(jK)
		        		.usingJobData("clear-snapshots", renewSnapshots)
		        		.startNow().build();
		        scheduler.scheduleJob(trigger);
			}else{
				Logger.getRootLogger().info("Trigger now : already running, ignore");
			}
		}else{
			Logger.getRootLogger().info("Triggerring job now");
			this.scheduleJob(n, true);
		}
		
	}

}
