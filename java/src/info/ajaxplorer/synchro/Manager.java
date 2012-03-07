package info.ajaxplorer.synchro;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;
import info.ajaxplorer.client.model.Node;
import info.ajaxplorer.client.model.Property;
import info.ajaxplorer.client.model.Server;
import info.ajaxplorer.synchro.gui.SysTray;
import info.ajaxplorer.synchro.model.SyncChange;

import java.io.File;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

public class Manager {

	
	public static String[] EXCLUDED_ACCESS_TYPES = {"ajxp_conf", "ajxp_shared", "mysql", "imap", "jsapi"};

	Scheduler scheduler;
	static Manager instance;
	public static Dao<Node, String> nodeDao;
	public static Dao<SyncChange, String> syncChangeDao;
	public static Dao<Property, String> propertyDao;
	private SysTray sysTray;
	private ResourceBundle messages;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		String language;
        String country;
        if (args.length != 2) {
            language = new String("en");
            country = new String("US");
        } else {
            language = new String(args[0]);
            country = new String(args[1]);
        }

        Locale currentLocale = new Locale(language, country);
		
		Display display = new Display();
		Display.setAppName("AjaXplorer Synchronizer");
		Display.setAppVersion("1.0");
		Shell shell = new Shell(display, SWT.ON_TOP | SWT.TITLE | SWT.MIN | SWT.CLOSE );
		Manager.instanciate(shell, currentLocale);
		
		Manager.getInstance().initScheduler();
		
		while (!shell.isDisposed ()) {
			if (!display.readAndDispatch ()) display.sleep ();
		}
		display.dispose ();
		
		
	}
	
	public static String getMessage(String s){
		return Manager.instance.messages.getString(s);
	}
	
	public static void instanciate(Shell shell, Locale locale){
		Manager.instance = new Manager(shell, locale);
	}
	public static Manager getInstance(){
		return Manager.instance;
	}
	public Dao<Node, String> getNodeDao(){
		return nodeDao;
	}
	public Dao<SyncChange, String> getSyncChangeDao(){
		return syncChangeDao;
	}
	public Dao<Property, String> getPropertyDao(){
		return propertyDao;
	}
	
	public void notifyUser(final String title, final String message){
		if(this.sysTray == null) {
			System.out.println("No systray - message was " + message);
			return;
		}
		this.sysTray.getDisplay().asyncExec(new Runnable() {
			
			public void run() {
				if(!sysTray.isDisposed()){
					sysTray.notifyUser(title, message);
				}else{
					System.out.println("No systray - message was " + message);
				}
			}
		});
	}
	
	public void updateSynchroState(final boolean running){
		if(this.sysTray == null) {
			return;
		}
		this.sysTray.getDisplay().asyncExec(new Runnable() {			
			public void run() {
				if(!sysTray.isDisposed()){
					sysTray.setMenuTriggerRunning(running);
				}
			}
		});
	}
	
	public Manager(Shell shell, Locale locale){
		messages = ResourceBundle.getBundle("strings/MessagesBundle", locale);
		sysTray = new SysTray(shell, messages);
		try {
			initializeDAO();
		}catch(SQLException e){
			e.printStackTrace();
		}
		
	    try {			
            scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();
            //scheduler.shutdown();
        } catch (SchedulerException se) {
            se.printStackTrace();
        }		
	    
	}
	
	private void initializeDAO() throws SQLException{
		
		boolean dbAlreadyCreated = (new File("ajxpsync.db")).exists();
		 // this uses h2 by default but change to match your database
       String databaseUrl = "jdbc:sqlite:ajxpsync.db";
       ConnectionSource connectionSource = new JdbcConnectionSource(databaseUrl);
       // instantiate the dao
       nodeDao = DaoManager.createDao(connectionSource, Node.class);
       syncChangeDao = DaoManager.createDao(connectionSource, SyncChange.class);
       propertyDao = DaoManager.createDao(connectionSource, Property.class);
       if(!dbAlreadyCreated){
           TableUtils.createTable(connectionSource, Node.class);
           TableUtils.createTable(connectionSource, Property.class);
           TableUtils.createTable(connectionSource, SyncChange.class);
           
           nodeDao.executeRaw("CREATE TRIGGER on_delete_cascade AFTER DELETE ON a BEGIN\n" + 
					"  DELETE FROM b WHERE node_id=old.id;\n" +
					"  DELETE FROM a WHERE parent_id=old.id;\n" +
					"END;");           
       }        		
		
	}
	
	public Node updateSynchroNode(Map<String, String> data, Node node) throws SQLException, URISyntaxException{
		Server s;		
		if(node == null){
			s = new Server(data.get("HOST"), data.get("HOST"), data.get("LOGIN"), data.get("PASSWORD"), false, false);			
			Node serverNode = s.createDbNode(nodeDao);
			node = new Node(Node.NODE_TYPE_REPOSITORY, data.get("REPOSITORY_LABEL"), serverNode);
			nodeDao.create(node);
			node.setParent(serverNode);
			node.properties = nodeDao.getEmptyForeignCollection("properties");
			node.addProperty("repository_id", data.get("REPOSITORY_ID"));
			node.addProperty("target_folder", data.get("TARGET"));
			node.addProperty("synchro_active", data.get("ACTIVE"));
			node.addProperty("synchro_direction", data.get("DIRECTION"));
			node.addProperty("synchro_interval", data.get("INTERVAL"));
			nodeDao.update(node);
			try {
				this.scheduleJob(node);
			} catch (SchedulerException e) {
				e.printStackTrace();
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
			for(Property p:props){
				if(p.getName().equals("repository_id")
						&& !p.getValue().equals(data.get("REPOSITORY_ID"))) {
					p.setValue(data.get("REPOSITORY_ID"));
					serverChanges = true;
					toSave.add(p);
				}
				else if(p.getName().equals("target_folder")
						&& !p.getValue().equals(data.get("TARGET"))) {
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
			}
			try {
				if(serverChanges){

					// STOP CURRENT
					this.unscheduleJob(node);
					
					// UPDATE DB
					s.updateDbNode(nodeDao, propertyDao);
					if(toSave.size() > 0){
						for(Property ps:toSave) propertyDao.update(ps);
					}
					nodeDao.update(node);	
					
					// RESCHEDULE AND CLEAN SNAPSHOTS
					this.scheduleJob(node, true);
					
				}else if(intervalChanges){
					
					// UPDATE DB
					for(Property ps:toSave) propertyDao.update(ps);
					nodeDao.refresh(node);
					
					// CHANGE INTERVAL
					this.changeJobInterval(node);
					
				}
			} catch (SchedulerException e) {
				e.printStackTrace();
			}
		}
		
		
		return node;
	}
	
	public Node getSynchroNode(String nodeId) throws SQLException{
		Node n = this.getNodeDao().queryForId(nodeId);
		n.setParent(this.getNodeDao().queryForId(String.valueOf(n.getParent().id)));
		return n;
	}
	
	public Collection<Node> listSynchroNodes(){
		Collection<Node> n = new ArrayList<Node>();
		try {
			n =  nodeDao.queryForEq("resourceType", Node.NODE_TYPE_REPOSITORY);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return n;
	}
	
	protected void initScheduler(){
		try {
			Collection<Node> l = listSynchroNodes();
			for(Node n:l){
				scheduleJob(n);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}

	public void scheduleJob(Node n) throws SchedulerException{				
		this.scheduleJob(n, false);
	}
	
	public void scheduleJob(Node n, boolean firstTriggerClear) throws SchedulerException{				
		
        JobDetail job = newJob(SyncJob.class)
        		.withIdentity(String.valueOf(n.id), "sync")
        		.usingJobData("node-id", String.valueOf(n.id))
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
            		.startNow()        		
            		.withSchedule(getSSBFromString(n.getPropertyValue("synchro_interval")))
            		.build();

            scheduler.scheduleJob(job, trigger);		   
        	
        }


	}
	
	public void unscheduleJob(Node n) throws SchedulerException{
		
		scheduler.deleteJob(new JobKey(String.valueOf(n.id), "sync"));
		
	}
	
	protected SimpleScheduleBuilder getSSBFromString(String s){
		SimpleScheduleBuilder ssB = null;
		if(s.equals("hour")){
			ssB = simpleSchedule().withIntervalInHours(1).repeatForever();
		}else if(s.equals("minute")){
			ssB = simpleSchedule().withIntervalInMinutes(1).repeatForever();
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
	        Trigger trigger = newTrigger()
	        		.withIdentity("onetime-"+String.valueOf(n.id), "ajxp")
	        		.forJob(jK)
	        		.usingJobData("clear-snapshots", renewSnapshots)
	        		.startNow().build();
	        scheduler.scheduleJob(trigger);
		}
		
	}

}
