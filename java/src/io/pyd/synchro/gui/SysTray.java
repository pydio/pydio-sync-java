/*
 * Copyright 2012 Charles du Jeu <charles (at) ajaxplorer.info>
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
package io.pyd.synchro.gui;

import info.ajaxplorer.client.model.Node;
import io.pyd.synchro.CoreManager;
import io.pyd.synchro.SyncJob;

import java.sql.SQLException;
import java.text.DateFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;
import org.apache.log4j.spi.RootLogger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolTip;
import org.eclipse.swt.widgets.Tray;
import org.eclipse.swt.widgets.TrayItem;
import org.quartz.SchedulerException;



public class SysTray {

	private ToolTip tip;
	private Image image;
	private Display display;
	private Shell shell;
	private final TrayItem item; 
	private Menu menu;
	//private MenuItem currentStateItem;
	private HashMap<String, MenuItem> currentStateItems;
	private HashMap<String, MenuItem> currentStartItems;
	private boolean showNotifications = false;
	ResourceBundle messages;
	private JobEditor jobEditor;
	private AboutPanel aboutPanel;
	private boolean schedulerStateStarted = true;
	private AnimationThread at;
	
	public void notifyUser(String title, String message, String nodeId, boolean forceDisplay){
		String jobLabel = "";
		if(nodeId != null){
			Node n = CoreManager.getInstance().getSynchroNode(nodeId);
			if(n != null) jobLabel = CoreManager.getInstance().makeJobLabel(n, true)+": ";
		}
		item.setToolTipText( jobLabel + title + " ("+message+")");
		
		if( (!showNotifications && !forceDisplay ) || menu.isVisible()){			
			return;
		}
		if(tip != null && tip.isVisible()) {
			tip.dispose();
			tip = new ToolTip(shell, SWT.BALLOON | SWT.ICON_INFORMATION);
			item.setToolTip(tip);			
		}else if(tip == null){
			tip = new ToolTip(shell, SWT.BALLOON | SWT.ICON_INFORMATION);
			item.setToolTip(tip);			
		}
		tip.setText(title);
		tip.setMessage(message);
		tip.setVisible(true);		
	}
	
	public boolean isDisposed(){
		return shell.isDisposed();
	}
	public Display getDisplay(){
		return display;
	}
	public void setMenuTriggerRunning(Node node, boolean state){
		String nodeId = String.valueOf(node.id);
		if(this.jobEditor != null){
			this.jobEditor.notifyJobStateChanged(nodeId, state);
		}
		if(this.menu.isVisible() && this.currentStateItems != null && this.currentStateItems.containsKey(nodeId)){
			this.currentStateItems.get(nodeId).setText(this.computeSyncStatus(node));
		}
		if(this.menu.isVisible() && this.currentStartItems != null && this.currentStartItems.containsKey(nodeId)){
			this.currentStartItems.get(nodeId).setEnabled(!state);
		}
		this.setIconState(state?"running":"idle");
		item.setToolTipText(CoreManager.getInstance().makeJobLabel(node, true)+": " + this.computeSyncStatus(node));
	}
	protected Image getImage(String name){
		try{
			return new Image(getDisplay(), new ImageData(this.getClass().getClassLoader().getResourceAsStream("images/"+name+".png")));			
		}catch(Exception e){
			return null;
		}
	}
	public void refreshJobsMenu(){
		refreshJobsMenu(CoreManager.getInstance());
	}
	
	protected String computeSyncStatus(Node syncNode){

		String syncStatus = "";
		if(syncNode == null) return syncStatus;
		if(syncNode.getStatus() == Node.NODE_STATUS_LOADING){
			syncStatus = messages.getString("tray_menu_status_running");
			if(syncNode.getPropertyValue("sync_running_status") != null){
				int runningStatus = new Integer(syncNode.getPropertyValue("sync_running_status"));
				String key = null;
				if(runningStatus == SyncJob.RUNNING_STATUS_INITIALIZING) key = "sync_running_init";
				else if(runningStatus == SyncJob.RUNNING_STATUS_PREVIOUS_CHANGES) key = "sync_running_previous";
				else if(runningStatus == SyncJob.RUNNING_STATUS_APPLY_CHANGES) key = "sync_running_apply";
				else if(runningStatus == SyncJob.RUNNING_STATUS_CLEANING) key = "sync_running_clean";
				else if(runningStatus == SyncJob.RUNNING_STATUS_COMPARING_CHANGES) key = "sync_running_compare";
				else if(runningStatus == SyncJob.RUNNING_STATUS_INTERRUPTING) key = "sync_running_interrupt";
				else if(runningStatus == SyncJob.RUNNING_STATUS_LOCAL_CHANGES) key = "sync_running_local";
				else if(runningStatus == SyncJob.RUNNING_STATUS_REMOTE_CHANGES) key = "sync_running_remote";
				else if(runningStatus == SyncJob.RUNNING_STATUS_TESTING_CONNEXION) key = "sync_running_connexion";
				if(key != null){
					syncStatus = messages.getString(key);
				}
			}
		}else if(syncNode.getStatus() == Node.NODE_STATUS_ERROR){
			syncStatus = messages.getString("tray_menu_status_error");
		}else{
			if(syncNode.getLastModified() != null){
				DateFormat df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, Locale.getDefault());
				syncStatus = messages.getString("tray_menu_status_last") + df.format(syncNode.getLastModified());					
			}else{
				syncStatus = messages.getString("tray_menu_status_na");
			}
		}
		//Logger.getRootLogger().info("-- Status : " + syncNode.id + " ==> " + syncNode.getPropertyValue("sync_running_status"));
		return syncStatus;
	}
	
	public void refreshJobsMenu(CoreManager managerInstance){
		
		for(MenuItem item:menu.getItems()){
			item.dispose();
		}			
		Collection<Node> ns = managerInstance.listSynchroNodes();
		boolean uniqMenu = (ns.size() < 2);
		
		this.currentStateItems = new HashMap<String, MenuItem>();
		this.currentStartItems = new HashMap<String, MenuItem>();
		
		for(Node syncNode:ns){
			
			boolean running = (syncNode.getStatus() == Node.NODE_STATUS_LOADING);


			Menu jobMenu;
			if(!uniqMenu){
				MenuItem jobTrig = new MenuItem (menu, SWT.CASCADE);
				jobTrig.setText ( managerInstance.makeJobLabel(syncNode, true));
				jobTrig.setData(syncNode);
				jobTrig.setImage(getImage("sync"));
				
				jobMenu = new Menu(shell, SWT.DROP_DOWN);
				jobTrig.setMenu(jobMenu);			
			}else{
				jobMenu = menu;
			}
			
			final String nodeId = syncNode.id + "";			
			final boolean currentActiveState = syncNode.getPropertyValue("synchro_active").equals("true");
			
			MenuItem currentStateItem = new MenuItem(jobMenu, SWT.PUSH);			
			currentStateItem.setText(this.computeSyncStatus(syncNode));
			currentStateItem.setImage(getImage("transp"));
			currentStateItem.setEnabled(false);
			if(syncNode.getStatus() == Node.NODE_STATUS_ERROR){
				jobMenu.setDefaultItem(currentStateItem);
				currentStateItem.setEnabled(true);
				currentStateItem.addListener (SWT.Selection, new Listener () {
					public void handleEvent (Event event) {
						openConfiguration(shell, nodeId, "logs");
					}
				});
			}
			this.currentStateItems.put(nodeId, currentStateItem);
						
			if(currentActiveState){
				MenuItem mI = new MenuItem(jobMenu, SWT.PUSH);			
				mI.setText(messages.getString("tray_menu_trigger"));
				mI.setImage(getImage("media_playback_start"));
				mI.setData(syncNode);
				if(running) mI.setEnabled(false);
				mI.addListener (SWT.Selection, new Listener () {
					public void handleEvent (Event event) {
						try {
							CoreManager.getInstance().triggerJobNow((Node)event.widget.getData(), false);
						} catch (SchedulerException e) {
							Logger.getRootLogger().error("Synchro", e);
						}
					}
				});
				this.currentStartItems.put(nodeId, mI);
			}
			
			MenuItem mAct = new MenuItem(jobMenu, SWT.PUSH);			
			mAct.setText( CoreManager.getMessage(currentActiveState?"tray_menu_jobstatus_active":"tray_menu_jobstatus_inactive"));
			mAct.setImage(getImage(currentActiveState?"media_playback_pause":"media_playback_start"));
			mAct.setData(syncNode);
			//if(running) mAct.setEnabled(false);
			mAct.addListener (SWT.Selection, new Listener () {
				public void handleEvent (Event event) {
					CoreManager.getInstance().changeSynchroState((Node)event.widget.getData(), !currentActiveState);
					/*
					if(!currentActiveState){
						try {
							CoreManager.getInstance().triggerJobNow((Node)event.widget.getData(), false);
						} catch (SchedulerException e) {
							Logger.getRootLogger().error("Synchro", e);
						}						
					}*/
				}
			});
			
						
			new MenuItem(jobMenu, SWT.SEPARATOR);

			
			MenuItem qaTrig = new MenuItem (jobMenu, SWT.CASCADE);
			qaTrig.setText (messages.getString("tray_menu_quick"));
			qaTrig.setImage(getImage("go_jump"));
			
			Menu qaMenu = new Menu(shell, SWT.DROP_DOWN);
			qaTrig.setMenu(qaMenu);			
			
			MenuItem mI2 = new MenuItem(qaMenu, SWT.PUSH);
			mI2.setText(messages.getString("tray_menu_openlocal"));
			mI2.setImage(getImage("folder_home"));
			mI2.setData(syncNode);
			mI2.addListener (SWT.Selection, new Listener () {
				public void handleEvent (Event event) {
					CoreManager.getInstance().openLocalTarget((Node)event.widget.getData());
				}
			});
			
			MenuItem mI3 = new MenuItem(qaMenu, SWT.PUSH);
			mI3.setText(messages.getString("tray_menu_openremote"));
			mI3.setImage(getImage("network_local"));
			mI3.setData(syncNode);
			mI3.addListener (SWT.Selection, new Listener () {
				public void handleEvent (Event event) {
					CoreManager.getInstance().openRemoteTarget((Node)event.widget.getData());
				}
			});		
			
			MenuItem configTrig = new MenuItem (jobMenu, SWT.CASCADE);
			configTrig.setText (messages.getString("tray_menu_task_params"));
			configTrig.setImage(getImage("configure"));
			
			Menu configMenu = new Menu(shell, SWT.DROP_DOWN);
			configTrig.setMenu(configMenu);	
			
			MenuItem mi = new MenuItem (configMenu, SWT.PUSH);
			mi.setText ( messages.getString("jobeditor_stack_server") );
			mi.setImage(getImage("network_local"));
			mi.addListener (SWT.Selection, new Listener () {
				public void handleEvent (Event event) {
					openConfiguration(shell, nodeId, "connexion");
				}
			});
			MenuItem mij2 = new MenuItem (configMenu, SWT.PUSH);
			mij2.setText ( messages.getString("jobeditor_stack_params") );
			mij2.setImage(getImage("history"));
			mij2.addListener (SWT.Selection, new Listener () {
				public void handleEvent (Event event) {
					openConfiguration(shell, nodeId, "parameters");
				}
			});
			
			MenuItem mDel = new MenuItem(configMenu, SWT.PUSH);			
			mDel.setText(CoreManager.getMessage("tray_menu_jobdelete"));
			mDel.setImage(getImage("editdelete"));
			mDel.setData(syncNode);
			//if(running) mDel.setEnabled(false);
			mDel.addListener (SWT.Selection, new Listener () {
				public void handleEvent (Event event) {
					MessageBox dialog = new MessageBox(shell, SWT.ICON_QUESTION | SWT.OK| SWT.CANCEL);
					dialog.setText(CoreManager.getMessage("jobeditor_diag_delete"));
					dialog.setMessage(CoreManager.getMessage("jobeditor_diag_deletem"));
					int returnCode = dialog.open();					
					if(returnCode == SWT.CANCEL) return;
					try {
						CoreManager.getInstance().deleteSynchroNode((Node)event.widget.getData());
					} catch (SchedulerException e) {
						Logger.getRootLogger().error("Synchro", e);
					} catch (SQLException e) {
						Logger.getRootLogger().error("Synchro", e);
					}
				}
			});
			
			MenuItem mij1 = new MenuItem (jobMenu, SWT.PUSH);
			mij1.setText ( messages.getString("jobeditor_stack_logs") );
			mij1.setImage(getImage("view_list_text"));
			mij1.addListener (SWT.Selection, new Listener () {
				public void handleEvent (Event event) {
					openConfiguration(shell, nodeId, "logs");
				}
			});
						
			
			
		}		
		
		new MenuItem(menu, SWT.SEPARATOR);

		MenuItem generalTrig = new MenuItem (menu, SWT.CASCADE);
		generalTrig.setText (messages.getString("tray_menu_preferences"));
		generalTrig.setImage(getImage("parameters"));
		
		Menu generalMenu = new Menu(shell, SWT.DROP_DOWN);
		generalTrig.setMenu(generalMenu);			
		
		final MenuItem showNotifMenu = new MenuItem (generalMenu, SWT.PUSH);
		//showNotifMenu.setSelection(showNotifications);
		if(showNotifications){
			showNotifMenu.setImage(getImage("apply"));
		}else{
			showNotifMenu.setImage(getImage("apply-not"));
		}
		showNotifMenu.setText(messages.getString("tray_menu_notif"));
		showNotifMenu.addListener (SWT.Selection, new Listener() {
			public void handleEvent (Event event) {
				showNotifications = !showNotifications;
			}
		});
		
		MenuItem createM = new MenuItem (generalMenu, SWT.PUSH);
		createM.setText ( messages.getString("cpanel_create_synchro") );
		createM.setImage(getImage("add"));
		createM.addListener (SWT.Selection, new Listener () {
			public void handleEvent (Event event) {
				openConfiguration(shell, null, "connexion");
			}
		});		
		
		
		MenuItem mAct = new MenuItem(generalMenu, SWT.PUSH);			
		mAct.setText( CoreManager.getMessage(schedulerStateStarted?"tray_menu_scheduler_pauseall":"tray_menu_scheduler_startall"));
		mAct.setImage(getImage(schedulerStateStarted?"media_playback_pause":"media_playback_start"));
		mAct.addListener (SWT.Selection, new Listener () {
			public void handleEvent (Event event) {
				boolean res;
				if(schedulerStateStarted){
					res = CoreManager.getInstance().pauseAll();
				}else{
					res = CoreManager.getInstance().restartAll();
				}
				if(res){
					schedulerStateStarted = !schedulerStateStarted;
				}
			}
		});
		
		MenuItem aboutM = new MenuItem (generalMenu, SWT.PUSH);
		aboutM.setText ( messages.getString("tray_menu_about") );
		aboutM.setImage(getImage("infos"));
		aboutM.addListener (SWT.Selection, new Listener () {
			public void handleEvent (Event event) {
				openAboutPane();
			}
		});
		
		MenuItem mi2 = new MenuItem (menu, SWT.PUSH);
		mi2.setText (CoreManager.getMessage("tray_menu_quit"));
		mi2.setImage(getImage("exit"));
		mi2.addListener (SWT.Selection, new Listener () {
			public void handleEvent (Event event) {
				int res = CoreManager.getInstance().close();
				System.exit(res);
			}
		});					
	}
	
	public SysTray(final Shell shell, ResourceBundle messages, CoreManager managerInstance){
		
		this.shell = shell;
		this.messages = messages;
		display = shell.getDisplay();
		
		final Tray tray = display.getSystemTray ();
		//tip = new ToolTip(shell, SWT.BALLOON | SWT.ICON_INFORMATION);
		boolean isMac = SWTResourceManager.isMac();
		if (tray == null) {
			Logger.getRootLogger().error("The system tray is not available");
			item = null;			
		} else {
			image = new Image(display, this.getClass().getClassLoader().getResourceAsStream("images/AjxpLogo16-"+(isMac?"BW":"Bi")+".png"));

			item = new TrayItem (tray, SWT.NONE);
			item.setToolTipText("Pydio Synchronizer");
			//item.setToolTip(tip);
			item.addListener (SWT.Show, new Listener () {
				public void handleEvent (Event event) {
				}
			});
			item.addListener (SWT.Hide, new Listener () {
				public void handleEvent (Event event) {
				}
			});
			/*
			item.addListener (SWT.DefaultSelection, new Listener () {
				public void handleEvent (Event event) {
					openConfiguration(shell, null);
				}
			});
			*/
			
			menu = new Menu (shell, SWT.POP_UP);
			
			if(!SWTResourceManager.isMac()){
				item.addListener (SWT.Selection, new Listener () {
					public void handleEvent (Event event) {
						menu.setVisible (true);
						refreshJobsMenu();
					}
				});
			}
			item.addListener (SWT.MenuDetect, new Listener () {
				public void handleEvent (Event event) {
					menu.setVisible (true);
					refreshJobsMenu();
				}
			});
			item.setImage (image);
		}
		shell.setBounds(-10, -10, 10, 10);
		shell.open ();
		shell.setVisible(false);
	}
		
	public void setIconState(String state){
		boolean isMac = SWTResourceManager.isMac();
		if(state.equals("running")){
			if(at!=null && at.isAlive()){
				return;
			}
			at = new AnimationThread();
			at.delayedAnimation(item, 700, "AjxpLogo16-"+(isMac?"BW":"Bi")+"-Bouncing", 8);
			at.start();					
		}else if(state.equals("idle")){
			Image restore = new Image(display, this.getClass().getClassLoader().getResourceAsStream("images/AjxpLogo16-"+(isMac?"BW":"Bi")+".png"));
			if(at!=null && at.isAlive()){
				at.requireInterrupt = true;
				at.restoreImage = restore;
			}
			item.setImage(restore);
		}
	}
	
	class AnimationThread extends Thread {
		TrayItem item;
		int delay;
		String img;
		int number;
		int currentIndex = 0;
		public boolean requireInterrupt = false;
		public Image restoreImage;
		Image[] images;

		public void delayedAnimation(TrayItem i, int delay, String imgRadical, int number) {
			this.item = i;
			this.delay = delay;
			this.img = imgRadical;
			this.number = number;
			images = new Image[10];
			for(int ind=0;ind<this.number; ind++){
				images[ind] = new Image(display, SysTray.this.getClass().getClassLoader().getResourceAsStream("images/"+this.img+"-"+ind+".png"));
			}
		}

		public Image getImage(){
			if(currentIndex < number - 1){
				currentIndex ++;
			}else{
				currentIndex = 0;
			}
			//Image image = new Image(display, SysTray.this.getClass().getClassLoader().getResourceAsStream("images/"+this.img+"-"+currentIndex+".png"));
			return images[currentIndex];
		}
		
		public void run() {
			while (true) {
				try {
					if(requireInterrupt){
						interrupt();
						if(restoreImage != null){
							display.asyncExec(new Runnable() {						
								public void run() {
									if(!SysTray.this.isDisposed()){
										item.setImage(restoreImage);										
									}
								}
							});							
						}
						return;
					}
					sleep(delay);
					final Image im = getImage();
					display.asyncExec(new Runnable() {						
						public void run() {
							if(!SysTray.this.isDisposed()){
								item.setImage(im);
							}
						}
					});
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}	
	
	public void disposeMe(){
		image.dispose();
	}
	
	public void closeConfiguration(){
		shell.setVisible(false);
		jobEditor.dispose();
		jobEditor = null;
		System.gc();
	}
	
	public void openConfiguration(Shell shell, String nodeId, String stack){
		if(!shell.isVisible()){
			jobEditor = new JobEditor(shell, this, stack);
			try {
				if(nodeId != null){
					Node n = CoreManager.getInstance().getSynchroNode(nodeId);
					if(n != null){
						jobEditor.setCurrentNode(n);
					}
				}else{
					jobEditor.setCurrentNode(null);
				}
			} catch (Exception e) {
				RootLogger.getLogger("ajxp").error(e);
			}				
			shell.setVisible(true);
		}else{
			try {
				if(nodeId != null){
					Node n = CoreManager.getInstance().getSynchroNode(nodeId);
					if(n != null){
						jobEditor.setCurrentNode(n);
					}
				}else{
					jobEditor.setCurrentNode(null);
				}
			} catch (Exception e) {
			}
			jobEditor.toggleSection(stack, true);
		}
		shell.forceActive();
	}
	
	public void openAboutPane(){
		Shell aShell = new Shell(shell.getDisplay(), SWT.NO_TRIM|SWT.ON_TOP);
		aboutPanel = new AboutPanel(aShell, this);
	}
	
	public void closeAboutPane(Shell theShell){
		theShell.setVisible(false);
		aboutPanel.dispose();
		aboutPanel = null;
		theShell.dispose();
		System.gc();
	}
	
}
