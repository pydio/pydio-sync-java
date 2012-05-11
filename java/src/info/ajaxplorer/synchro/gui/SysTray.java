package info.ajaxplorer.synchro.gui;

import info.ajaxplorer.client.model.Node;
import info.ajaxplorer.synchro.Manager;

import java.sql.SQLException;
import java.text.DateFormat;
import java.util.Collection;
import java.util.Locale;
import java.util.ResourceBundle;

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
	private boolean showNotifications = true;
	ResourceBundle messages;
	private JobEditor jobEditor;
	private boolean schedulerStateStarted = true;
	
	public void notifyUser(String title, String message){
		if(!showNotifications || menu.isVisible()){
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
	public void setMenuTriggerRunning(String nodeId, boolean state){
		if(this.jobEditor != null){
			this.jobEditor.notifyJobStateChanged(nodeId, state);
		}
	}
	protected Image getImage(String name){
		return new Image(getDisplay(), new ImageData(this.getClass().getClassLoader().getResourceAsStream("images/"+name+".png")));
	}
	public void refreshJobsMenu(){
		refreshJobsMenu(Manager.getInstance());
	}
	public void refreshJobsMenu(Manager managerInstance){
		
		for(MenuItem item:menu.getItems()){
			item.dispose();
		}			
		
		MenuItem createM = new MenuItem (menu, SWT.PUSH);
		createM.setText ( messages.getString("cpanel_create_synchro") );
		createM.setImage(getImage("add"));
		createM.addListener (SWT.Selection, new Listener () {
			public void handleEvent (Event event) {
				openConfiguration(shell, null, "connexion");
			}
		});
		
		new MenuItem(menu, SWT.SEPARATOR);		

		
		Collection<Node> ns = managerInstance.listSynchroNodes();
		for(Node syncNode:ns){
			
			boolean running = false;
			String syncStatus = "";
			if(syncNode.getStatus() == Node.NODE_STATUS_LOADING){
				syncStatus = messages.getString("tray_menu_status_running");
				running = true;
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
						
			
			MenuItem jobTrig = new MenuItem (menu, SWT.CASCADE);
			jobTrig.setText ( managerInstance.makeJobLabel(syncNode, true));
			jobTrig.setData(syncNode);
			jobTrig.setImage(getImage("sync"));
			
			Menu jobMenu = new Menu(shell, SWT.DROP_DOWN);
			jobTrig.setMenu(jobMenu);			

			MenuItem mi = new MenuItem (jobMenu, SWT.PUSH);
			mi.setText ( messages.getString("jobeditor_stack_server") );
			mi.setImage(getImage("network_local"));
			final String nodeId = syncNode.id + "";
			mi.addListener (SWT.Selection, new Listener () {
				public void handleEvent (Event event) {
					openConfiguration(shell, nodeId, "connexion");
				}
			});
			MenuItem mij2 = new MenuItem (jobMenu, SWT.PUSH);
			mij2.setText ( messages.getString("jobeditor_stack_params") );
			mij2.setImage(getImage("history"));
			mij2.addListener (SWT.Selection, new Listener () {
				public void handleEvent (Event event) {
					openConfiguration(shell, nodeId, "parameters");
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
						
			new MenuItem(jobMenu, SWT.SEPARATOR);					
			final boolean currentActiveState = syncNode.getPropertyValue("synchro_active").equals("true");
			
			MenuItem m0 = new MenuItem(jobMenu, SWT.PUSH);			
			m0.setText(syncStatus);
			if(syncNode.getStatus() == Node.NODE_STATUS_ERROR){
				jobMenu.setDefaultItem(m0);
				m0.addListener (SWT.Selection, new Listener () {
					public void handleEvent (Event event) {
						openConfiguration(shell, nodeId, "logs");
					}
				});
			}
						
			if(currentActiveState){
				MenuItem mI = new MenuItem(jobMenu, SWT.PUSH);			
				mI.setText(messages.getString("tray_menu_trigger"));
				mI.setImage(getImage("media_playback_start"));
				mI.setData(syncNode);
				if(running) mI.setEnabled(false);
				mI.addListener (SWT.Selection, new Listener () {
					public void handleEvent (Event event) {
						try {
							Manager.getInstance().triggerJobNow((Node)event.widget.getData(), false);
						} catch (SchedulerException e) {
							e.printStackTrace();
						}
					}
				});
			}
			
			MenuItem mAct = new MenuItem(jobMenu, SWT.PUSH);			
			mAct.setText( Manager.getMessage(currentActiveState?"tray_menu_jobstatus_active":"tray_menu_jobstatus_inactive"));
			mAct.setImage(getImage(currentActiveState?"media_playback_pause":"media_playback_start"));
			mAct.setData(syncNode);
			//if(running) mAct.setEnabled(false);
			mAct.addListener (SWT.Selection, new Listener () {
				public void handleEvent (Event event) {
					Manager.getInstance().changeSynchroState((Node)event.widget.getData(), !currentActiveState);
					if(!currentActiveState){
						try {
							Manager.getInstance().triggerJobNow((Node)event.widget.getData(), false);
						} catch (SchedulerException e) {
							e.printStackTrace();
						}						
					}
				}
			});
			
			MenuItem mDel = new MenuItem(jobMenu, SWT.PUSH);			
			mDel.setText(Manager.getMessage("tray_menu_jobdelete"));
			mDel.setImage(getImage("editdelete"));
			mDel.setData(syncNode);
			//if(running) mDel.setEnabled(false);
			mDel.addListener (SWT.Selection, new Listener () {
				public void handleEvent (Event event) {
					MessageBox dialog = new MessageBox(shell, SWT.ICON_QUESTION | SWT.OK| SWT.CANCEL);
					dialog.setText(Manager.getMessage("jobeditor_diag_delete"));
					dialog.setMessage(Manager.getMessage("jobeditor_diag_deletem"));
					int returnCode = dialog.open();					
					if(returnCode == SWT.CANCEL) return;
					try {
						Manager.getInstance().deleteSynchroNode((Node)event.widget.getData());
					} catch (SchedulerException e) {
						e.printStackTrace();
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}
			});
						
			new MenuItem(jobMenu, SWT.SEPARATOR);

			MenuItem mI2 = new MenuItem(jobMenu, SWT.PUSH);
			mI2.setText(messages.getString("tray_menu_openlocal"));
			mI2.setImage(getImage("folder_home"));
			mI2.setData(syncNode);
			mI2.addListener (SWT.Selection, new Listener () {
				public void handleEvent (Event event) {
					Manager.getInstance().openLocalTarget((Node)event.widget.getData());
				}
			});
			
			MenuItem mI3 = new MenuItem(jobMenu, SWT.PUSH);
			mI3.setText(messages.getString("tray_menu_openremote"));
			mI3.setImage(getImage("network_local"));
			mI3.setData(syncNode);
			mI3.addListener (SWT.Selection, new Listener () {
				public void handleEvent (Event event) {
					Manager.getInstance().openRemoteTarget((Node)event.widget.getData());
				}
			});			
		}		
		
		new MenuItem(menu, SWT.SEPARATOR);

		final MenuItem showNotifMenu = new MenuItem (menu, SWT.PUSH);
		//showNotifMenu.setSelection(showNotifications);
		if(showNotifications){
			showNotifMenu.setImage(getImage("apply"));
		}
		showNotifMenu.setText(messages.getString("tray_menu_notif"));
		showNotifMenu.addListener (SWT.Selection, new Listener() {
			public void handleEvent (Event event) {
				showNotifications = !showNotifications;
			}
		});
		
		
		MenuItem mAct = new MenuItem(menu, SWT.PUSH);			
		mAct.setText( Manager.getMessage(schedulerStateStarted?"tray_menu_scheduler_pauseall":"tray_menu_scheduler_startall"));
		mAct.setImage(getImage(schedulerStateStarted?"media_playback_pause":"media_playback_start"));
		mAct.addListener (SWT.Selection, new Listener () {
			public void handleEvent (Event event) {
				boolean res;
				if(schedulerStateStarted){
					res = Manager.getInstance().pauseAll();
				}else{
					res = Manager.getInstance().restartAll();
				}
				if(res){
					schedulerStateStarted = !schedulerStateStarted;
				}
			}
		});
		
		
		MenuItem mi2 = new MenuItem (menu, SWT.PUSH);
		mi2.setText (messages.getString("tray_menu_quit"));
		mi2.setImage(getImage("system_log_out"));
		mi2.addListener (SWT.Selection, new Listener () {
			public void handleEvent (Event event) {
				int res = Manager.getInstance().close();
				System.exit(res);
			}
		});					
	}
	
	public SysTray(final Shell shell, ResourceBundle messages, Manager managerInstance){
		
		this.shell = shell;
		this.messages = messages;
		display = shell.getDisplay();
		
		final Tray tray = display.getSystemTray ();
		//tip = new ToolTip(shell, SWT.BALLOON | SWT.ICON_INFORMATION);
		if (tray == null) {
			System.out.println ("The system tray is not available");
			item = null;			
		} else {
			boolean isMac = SWTResourceManager.isMac();
			image = new Image(display, this.getClass().getClassLoader().getResourceAsStream("images/AjxpLogo16-"+(isMac?"BW":"Bi")+".png"));
		    //tip.setMessage("Here is a message for the user. When the message is too long it wraps. I should say something cool but nothing comes to my mind.");

			item = new TrayItem (tray, SWT.NONE);
			item.setToolTipText("AjaXplorer Synchronizer");
			//item.setToolTip(tip);
			item.addListener (SWT.Show, new Listener () {
				public void handleEvent (Event event) {
					System.out.println("show");
				}
			});
			item.addListener (SWT.Hide, new Listener () {
				public void handleEvent (Event event) {
					System.out.println("hide");
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
		shell.setBounds(0, 0, 0, 0);
		shell.open ();
		shell.setVisible(false);
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
					Node n = Manager.getInstance().getSynchroNode(nodeId);
					if(n != null){
						jobEditor.setCurrentNode(n);
					}
				}
			} catch (Exception e) {
			}				
			shell.setVisible(true);
		}else{
			try {
				if(nodeId != null){
					Node n = Manager.getInstance().getSynchroNode(nodeId);
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
	
}
