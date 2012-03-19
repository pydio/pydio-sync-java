package info.ajaxplorer.synchro.gui;

import info.ajaxplorer.client.model.Node;
import info.ajaxplorer.synchro.Manager;

import java.util.Collection;
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

	private boolean shellInitialized = false;
	private ToolTip tip;
	private Image image;
	private Display display;
	private Shell shell;
	private final TrayItem item; 
	private Menu menu;
	private boolean showNotifications = true;
	ResourceBundle messages;
	MenuItem mTrig ;
	Menu jobsMenu;
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
		if(mTrig == null) return;
		for(MenuItem item:jobsMenu.getItems()){
			if(item.getData() == null || !(item.getData() instanceof Node)) continue;
			if(Integer.parseInt(nodeId) == ((Node)item.getData()).id){
				String label = Manager.getInstance().makeJobLabel((Node)item.getData(), false);
				if(state){
					item.setEnabled(false);
					item.setText(label + " : " +messages.getString("tray_menu_running"));
				}else{			
					item.setEnabled(true);
					item.setText(label);
				}
			}
		}		
	}
	
	public void refreshJobsMenu(Manager managerInstance){
		
		for(MenuItem item:jobsMenu.getItems()){
			item.dispose();
		}			
		Collection<Node> ns = managerInstance.listSynchroNodes();
		for(Node syncNode:ns){
			MenuItem mI = new MenuItem(jobsMenu, SWT.PUSH);
			mI.setText(managerInstance.makeJobLabel(syncNode, false));
			mI.setData(syncNode);
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
					//System.out.println("show");
				}
			});
			item.addListener (SWT.Hide, new Listener () {
				public void handleEvent (Event event) {
					//System.out.println("hide");
				}
			});
			item.addListener (SWT.DefaultSelection, new Listener () {
				public void handleEvent (Event event) {
					openConfiguration(shell);
				}
			});
			menu = new Menu (shell, SWT.POP_UP);			
			MenuItem mi = new MenuItem (menu, SWT.PUSH);
			mi.setText ( messages.getString("tray_menu_preferences") );
			menu.setDefaultItem(mi);
			mi.addListener (SWT.Selection, new Listener () {
				public void handleEvent (Event event) {
					openConfiguration(shell);
				}
			});
			
			
			
			mTrig = new MenuItem (menu, SWT.CASCADE);
			mTrig.setText ( messages.getString("tray_menu_trigger") );
			
			jobsMenu = new Menu(shell, SWT.DROP_DOWN);
			mTrig.setMenu(jobsMenu);
			this.refreshJobsMenu(managerInstance);
			/*
			Collection<Node> ns = managerInstance.listSynchroNodes();
			for(Node syncNode:ns){
				MenuItem mI = new MenuItem(jobsMenu, SWT.PUSH);
				mI.setText(managerInstance.makeJobLabel(syncNode));
				mI.setData(syncNode);
				mI.addListener (SWT.Selection, new Listener () {
					public void handleEvent (Event event) {
						try {
							Manager.getInstance().triggerJobNow((Node)event.widget.getData(), false);
						} catch (SchedulerException e) {
							e.printStackTrace();
						}
					}
				});
			}*/
			
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
			
			if(!managerInstance.isDaemon()){
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
			
			item.addListener (SWT.Selection, new Listener () {
				public void handleEvent (Event event) {
					//menu.setVisible (true);
				}
			});
			
			item.addListener (SWT.MenuDetect, new Listener () {
				public void handleEvent (Event event) {
					menu.setVisible (true);
				}
			});
			item.setImage (image);
			//tip.setVisible(true);
		}
		shell.setBounds(0, 0, 0, 0);
		shell.open ();
		shell.setVisible(false);
	}
	
	public void disposeMe(){
		image.dispose();
	}
	
	public void openConfiguration(Shell shell){
		if(!shellInitialized){
			cPanel = new ConfigPanel(shell);
			shellInitialized = true;
		}		
		shell.setVisible(true);
		shell.forceActive();
	}
	
}
