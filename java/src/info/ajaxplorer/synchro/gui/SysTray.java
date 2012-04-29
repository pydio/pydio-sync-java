package info.ajaxplorer.synchro.gui;

import info.ajaxplorer.client.model.Node;
import info.ajaxplorer.synchro.Manager;

import java.text.DateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.ResourceBundle;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
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
	private ConfigPanel cPanel;
	
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
		if(this.cPanel != null){
			this.cPanel.notifyJobStateChanged(nodeId, state);
		}
	}
	
	public void refreshJobsMenu(){
		refreshJobsMenu(Manager.getInstance());
	}
	public void refreshJobsMenu(Manager managerInstance){
		
		for(MenuItem item:menu.getItems()){
			item.dispose();
		}			
		
		final MenuItem showNotifMenu = new MenuItem (menu, SWT.CHECK);
		showNotifMenu.setSelection(showNotifications);
		showNotifMenu.setText (messages.getString("tray_menu_notif"));
		showNotifMenu.addSelectionListener (new SelectionListener() {				
			@Override
			public void widgetSelected(SelectionEvent event) {
				showNotifications = ((MenuItem)event.widget).getSelection();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent event) {}
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
			
			Menu jobMenu = new Menu(shell, SWT.DROP_DOWN);
			jobTrig.setMenu(jobMenu);			

			MenuItem mi = new MenuItem (jobMenu, SWT.PUSH);
			mi.setText ( messages.getString("tray_menu_preferences") );
			final String nodeId = syncNode.id + "";
			mi.addListener (SWT.Selection, new Listener () {
				public void handleEvent (Event event) {
					openConfiguration(shell, nodeId);
				}
			});
						
			MenuItem m0 = new MenuItem(jobMenu, SWT.PUSH);			
			m0.setText(syncStatus);
			
			new MenuItem(jobMenu, SWT.SEPARATOR);					
			
			MenuItem mI = new MenuItem(jobMenu, SWT.PUSH);			
			mI.setText(messages.getString("tray_menu_trigger"));
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

			MenuItem mI2 = new MenuItem(jobMenu, SWT.PUSH);
			mI2.setText(messages.getString("tray_menu_openlocal"));
			mI2.setData(syncNode);
			mI2.addListener (SWT.Selection, new Listener () {
				public void handleEvent (Event event) {
					Manager.getInstance().openLocalTarget((Node)event.widget.getData());
				}
			});
			
			MenuItem mI3 = new MenuItem(jobMenu, SWT.PUSH);
			mI3.setText(messages.getString("tray_menu_openremote"));
			mI3.setData(syncNode);
			mI3.addListener (SWT.Selection, new Listener () {
				public void handleEvent (Event event) {
					Manager.getInstance().openRemoteTarget((Node)event.widget.getData());
				}
			});			
		}		
		
		new MenuItem(menu, SWT.SEPARATOR);

		MenuItem mi2 = new MenuItem (menu, SWT.PUSH);
		mi2.setText (messages.getString("tray_menu_quit"));
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
			item.addListener (SWT.DefaultSelection, new Listener () {
				public void handleEvent (Event event) {
					openConfiguration(shell, null);
				}
			});
			
			
			menu = new Menu (shell, SWT.POP_UP);			
			item.addListener (SWT.Selection, new Listener () {
				public void handleEvent (Event event) {
					menu.setVisible (true);
					refreshJobsMenu();
				}
			});
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
		cPanel.dispose();
		cPanel = null;
		System.gc();
	}
	
	public void openConfiguration(Shell shell, String nodeId){
		if(!shell.isVisible()){
			cPanel = new ConfigPanel(shell, this, nodeId);
			shell.setVisible(true);
		}
		shell.forceActive();
	}
	
}
