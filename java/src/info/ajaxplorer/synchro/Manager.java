package info.ajaxplorer.synchro;

import java.io.File;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import info.ajaxplorer.client.model.Node;
import info.ajaxplorer.client.model.Property;
import info.ajaxplorer.client.model.Server;
import info.ajaxplorer.synchro.gui.SysTray;
import info.ajaxplorer.synchro.model.SyncChange;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.JobListener;
import org.quartz.Matcher;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.EverythingMatcher;
import org.quartz.impl.matchers.KeyMatcher;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import static org.quartz.JobBuilder.*;
import static org.quartz.SimpleScheduleBuilder.*;
import static org.quartz.TriggerBuilder.*;

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
		
		try {
			Manager.getInstance().scheduleJob();
		} catch (SchedulerException e) {
			e.printStackTrace();
		}		
		
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
				this.scheduleJob();
			} catch (SchedulerException e) {
				e.printStackTrace();
			}
		}else{
			s = new Server(node.getParent());
			s.setUrl(data.get("HOST"));
			s.setUser(data.get("LOGIN"));
			s.setPassword(data.get("PASSWORD"));
			s.updateDbNode(nodeDao);
			nodeDao.update(node);		
			Collection<Property> props = node.properties;
			Iterator<Property> it = props.iterator();
			while(it.hasNext()){
				Property p = it.next();
				if(p.getName().equals("repository_id")) {
					p.setValue(data.get("REPOSITORY_ID"));
					propertyDao.update(p);
				}
				else if(p.getName().equals("target_folder")) {
					p.setValue(data.get("TARGET"));
					propertyDao.update(p);
				}
				else if(p.getName().equals("synchro_active")) {
					p.setValue(data.get("ACTIVE"));
					propertyDao.update(p);
				}
				else if(p.getName().equals("synchro_direction")) {
					p.setValue(data.get("DIRECTION"));
					propertyDao.update(p);
				}
				else if(p.getName().equals("synchro_interval")) {
					p.setValue(data.get("INTERVAL"));
					propertyDao.update(p);
				}
			}
			nodeDao.refresh(node);
			
			try {
				this.triggerJobNow(true);
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
	
	public void scheduleJob() throws SchedulerException{
		int baseNodeId = -1;
		try {
			baseNodeId = nodeDao.queryForEq("resourceType", Node.NODE_TYPE_REPOSITORY).get(0).id;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
		}
		if(baseNodeId == -1){
			return;
		}
		
        JobDetail job = newJob(SyncJob.class)
        		.withIdentity("syncJob", "ajxp")
        		.usingJobData("node-id", String.valueOf(baseNodeId))
        		.build();
        

        Trigger trigger = newTrigger()
        		.withIdentity("syncTrigger", "ajxp")
        		.startNow()        		
        		.withSchedule(simpleSchedule()
    				.withIntervalInSeconds(60)
    				.repeatForever())
        		.build();

        scheduler.scheduleJob(job, trigger);		   

        //scheduler.unscheduleJob(new TriggerKey("syncTrigger", "ajxp"));
        
	}
	
	public void triggerJobNow(boolean renewSnapshots) throws SchedulerException{
		
		JobKey jK = new JobKey("syncJob", "ajxp");
		JobDetail job = scheduler.getJobDetail(jK);
		if(job != null){
	        Trigger trigger = newTrigger()
	        		.withIdentity("oneTimeTrigger", "ajxp")
	        		.forJob(jK)
	        		.usingJobData("clear-snapshots", renewSnapshots)
	        		.startNow().build();
	        scheduler.scheduleJob(trigger);
		}
		
	}

}
