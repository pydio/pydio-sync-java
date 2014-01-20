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

import info.ajaxplorer.client.model.Node;
import info.ajaxplorer.client.util.RdiffProcessor;
import io.pyd.synchro.gui.SysTray;

import java.io.File;
import java.sql.SQLException;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;

public class Manager extends CoreManager {
	
	static Manager instance;	
	static Shell instanceShell;	
	private SysTray sysTray;
	
	// Runnables instances for reuse, so we are not recreating them all the time
	// anyway, we will end up with OOME after some time of running app
	private UpdateSynchroStateRunnable updateSynchroStateRunnable;
	private UpdateSysTrayJobsMenuRunnable updateSysTrayJobsMenuRunnable;
	private NotifyUserRunnable notifyUserRunnable;
	
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
        		Manager.defaultHome = defHome;
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
		Display.setAppName(ResourceBundle.getBundle("strings/MessagesBundle", currentLocale).getString("shell_title"));
		Display.setAppVersion("1.0");
		final Display display = new Display();
		final Shell shell = new Shell(display, SWT.ALPHA|SWT.NONE);
		shell.setActive();

		Manager.instanceShell = shell;
		
		Logger.getRootLogger().info("Rdiff Processor active? " + (proc.rdiffEnabled()?"Yes" :"No"));
		Manager.instanciate(currentLocale, daemon);
		CoreManager.getInstance().setRdiffProc(proc);
		if(deferInit > 0 ){
			Timer t = new Timer();
			t.schedule(new ManagerTimerTask(), 1000 * deferInit);
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
		

		if(Manager.instance.firstRun){
			Manager.instance.sysTray.openConfiguration(shell, null, "connexion");
		}
    	//Manager.defaultHome = null;
		
		
		while (!shell.isDisposed ()) {
			if (!display.readAndDispatch ()) display.sleep ();
		}
		display.dispose ();		
		
	}
		
	public static void instanciate(Locale locale, boolean daemon){
		CoreManager.instance = new Manager(locale, daemon);
		Manager.instance = (Manager)CoreManager.instance;
	}
	
	public void notifyUser(final String title, final String message, final String nodeId, final boolean forceDisplay){
		if(this.sysTray == null) {
			Logger.getRootLogger().info("No systray - message was " + message);
			return;
		}
		if (notifyUserRunnable == null) {
			notifyUserRunnable = new NotifyUserRunnable(forceDisplay, nodeId, title, message);
		} else {
			notifyUserRunnable.setForceDisplay(forceDisplay);
			notifyUserRunnable.setMessage(message);
			notifyUserRunnable.setNodeId(nodeId);
			notifyUserRunnable.setTitle(title);
		}

		this.sysTray.getDisplay().asyncExec(notifyUserRunnable);
	}
	
	public void notifyUser(final String title, final String message, final String nodeId){
		notifyUser(title, message, nodeId, false);
	}
	
	public void updateSynchroState(final Node node, final boolean running){
		updateSynchroState(node, running, true);
	}

	public void updateSynchroState(final Node node, final boolean running, boolean updateIconState) {
		if(running){
			//this.stopWatcher(nodeId);
		}else{
			//this.startWatcher(nodeId);
		}
		if(this.sysTray == null) {
			return;
		}
		if (updateSynchroStateRunnable == null) {
			updateSynchroStateRunnable = new UpdateSynchroStateRunnable(node, running);
		} else {
			updateSynchroStateRunnable.setNode(node);
			updateSynchroStateRunnable.setRunning(running);
		}
		updateSynchroStateRunnable.setUpdateIconState(updateIconState);
		this.sysTray.getDisplay().asyncExec(updateSynchroStateRunnable);
	}
	
	public void updateSysTrayJobsMenu(){
		if (updateSysTrayJobsMenuRunnable == null) {
			updateSysTrayJobsMenuRunnable = new UpdateSysTrayJobsMenuRunnable();
		}
		this.sysTray.getDisplay().asyncExec(updateSysTrayJobsMenuRunnable);
	}
	
	public Manager(Locale locale, boolean daemon){
		super();
		messages = ResourceBundle.getBundle("strings/MessagesBundle", locale);
		boolean alreadyExists = false;
		try {
			alreadyExists = initializeDAO();
		}catch(SQLException e){
			Logger.getRootLogger().error("Synchro", e);
		}
		sysTray = new SysTray(Manager.instanceShell, messages, this);
	    try {			
            scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();
        } catch (SchedulerException se) {
            Logger.getRootLogger().error("Synchro", se);
        }
	    if(!alreadyExists) this.firstRun = true;
	}
	
		
	public boolean openLocalTarget(Node synchroNode){
		File f = new File(synchroNode.getPropertyValue("target_folder"));
		if(f.exists()){
			Program.launch(synchroNode.getPropertyValue("target_folder"));
		}
		return true;
	}
	
	public boolean openRemoteTarget(Node synchroNode){
		String server = synchroNode.getParent().getLabel();
		if(!server.endsWith("/")) server += "/";
		Program.launch(server + "?repository_id=" + synchroNode.getPropertyValue("repository_id") + "&folder=%2F");
		return true;
	}

	private final class UpdateSysTrayJobsMenuRunnable implements Runnable {
		public void run() {
			if (!sysTray.isDisposed()) {
				sysTray.refreshJobsMenu(Manager.this);
			}
		}
	}

	private final class UpdateSynchroStateRunnable implements Runnable {
		private Node node;
		private boolean running;
		private boolean updateIconState = true;

		private UpdateSynchroStateRunnable(Node node, boolean running) {
			this.node = node;
			this.running = running;
		}

		public void setNode(Node node) {
			this.node = node;
		}

		public void setRunning(boolean running) {
			this.running = running;
		}

		public void setUpdateIconState(boolean updateIconState) {
			this.updateIconState = updateIconState;
		}

		public void run() {
			if (!sysTray.isDisposed()) {
				sysTray.setMenuTriggerRunning(node, running, updateIconState);
			}
		}
	}

	private final class NotifyUserRunnable implements Runnable {
		private boolean forceDisplay;
		private String nodeId;
		private String title;
		private String message;

		private NotifyUserRunnable(boolean forceDisplay, String nodeId, String title, String message) {
			this.forceDisplay = forceDisplay;
			this.nodeId = nodeId;
			this.title = title;
			this.message = message;
		}

		public void setForceDisplay(boolean forceDisplay) {
			this.forceDisplay = forceDisplay;
		}

		public void setMessage(String message) {
			this.message = message;
		}

		public void setNodeId(String nodeId) {
			this.nodeId = nodeId;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public void run() {
			if (!sysTray.isDisposed()) {
				sysTray.notifyUser(title, message, nodeId, forceDisplay);
			} else {
				Logger.getRootLogger().info("No systray - message was " + message);
			}
		}
	}

	private static final class ManagerTimerTask extends TimerTask {
		@Override
		public void run() {
			CoreManager.getInstance().initScheduler();
		}
	}

}

