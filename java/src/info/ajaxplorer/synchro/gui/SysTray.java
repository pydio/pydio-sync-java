package info.ajaxplorer.synchro.gui;

import java.util.ResourceBundle;
import org.eclipse.swt.*;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;


public class SysTray {

	private boolean shellInitialized = false;
	private ToolTip tip;
	private Image image;
	private Display display;
	private Shell shell;
	private final TrayItem item; 
	private boolean showNotifications = true;
	ResourceBundle messages;
	
	public void notifyUser(String title, String message){
		if(!showNotifications){
			return;
		}
		if(tip.isVisible()) {
			tip.dispose();
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
	
	public SysTray(final Shell shell, ResourceBundle messages){
		
		this.shell = shell;
		this.messages = messages;
		display = shell.getDisplay();
		
		final Tray tray = display.getSystemTray ();
		tip = new ToolTip(shell, SWT.BALLOON | SWT.ICON_INFORMATION);
		if (tray == null) {
			System.out.println ("The system tray is not available");
			item = null;			
		} else {			
			image = new Image(display, this.getClass().getClassLoader().getResourceAsStream("info/ajaxplorer/synchro/resources/AjxpLogo16-Bi.png"));
		    tip.setMessage("Here is a message for the user. When the message is too long it wraps. I should say something cool but nothing comes to my mind.");

			item = new TrayItem (tray, SWT.NONE);
			item.setToolTipText("AjaXplorer Synchronizer");
			item.setToolTip(tip);
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
			item.addListener (SWT.Selection, new Listener () {
				public void handleEvent (Event event) {
					//System.out.println("selection");
				}
			});
			item.addListener (SWT.DefaultSelection, new Listener () {
				public void handleEvent (Event event) {
					openConfiguration(shell);
				}
			});
			final Menu menu = new Menu (shell, SWT.POP_UP);
			MenuItem mi = new MenuItem (menu, SWT.PUSH);
			mi.setText ( messages.getString("tray_menu_preferences") );
			menu.setDefaultItem(mi);
			mi.addListener (SWT.Selection, new Listener () {
				public void handleEvent (Event event) {
					openConfiguration(shell);
				}
			});
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
			
			MenuItem mi2 = new MenuItem (menu, SWT.PUSH);
			mi2.setText (messages.getString("tray_menu_quit"));
			mi2.addListener (SWT.Selection, new Listener () {
				public void handleEvent (Event event) {
					System.exit(0);
				}
			});
			
			item.addListener (SWT.MenuDetect, new Listener () {
				public void handleEvent (Event event) {
					menu.setVisible (true);
				}
			});
			item.setImage (image);
			tip.setVisible(true);
		}
		shell.setBounds(0, 0, 0, 0);
		shell.open ();
		shell.setVisible(false);
	}
	
	public void disposeMe(){
		image.dispose();
	}
	
	protected void openConfiguration(Shell shell){
		if(!shellInitialized){
			ConfigEditor c = new ConfigEditor();
			c.initShell(shell);
			shellInitialized = true;
		}
		shell.setVisible(true);
		shell.setMinimized(true);
		shell.setMinimized(false);		
	}
	
}
