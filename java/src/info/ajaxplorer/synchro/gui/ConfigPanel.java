package info.ajaxplorer.synchro.gui;

import info.ajaxplorer.client.model.Node;
import info.ajaxplorer.synchro.Manager;

import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.quartz.SchedulerException;

public class ConfigPanel extends Canvas {	

	{
		SWTResourceManager.registerResourceUser(this);
	}
	
	
	ArrayList<Object> jobComboItems;
	private Node currentSynchroNode;
	//LogViewer logViewer;
	private SysTray sTray;
	private Listener moveListener;
	
	private JobEditor jobEditor;
	
	public ConfigPanel(Composite shell, SysTray sysTray, String nodeId) {
		super(shell, SWT.NONE);
		this.initShell((Shell) shell);
		this.initGUI();
        shell.pack();
        this.sTray = sysTray;
		
		try {
			if(nodeId != null){
				Node n = Manager.getInstance().getSynchroNode(nodeId);
				if(n != null){
					this.setCurrentNode(n);
				}
			}
		} catch (Exception e) {
		}
		
		
	}		

	public void initShell(final Shell shell){
		
		if(shell.getData("listeners_attached") == null){
			shell.addListener(SWT.Close, new Listener() {
				public void handleEvent(Event event) {
					//shell.setVisible(false);				
					event.doit = false;
					sTray.closeConfiguration();
				}
			});		
			
			shell.addListener(SWT.MIN, new Listener() {
				public void handleEvent(Event event) {
					shell.setVisible(false);
					event.doit = false;
				}
		    });
		}
        shell.setData("listeners_attached", "true");
		
        shell.setText(Manager.getMessage("shell_title"));
        shell.setImage(new Image(shell.getDisplay(), this.getClass().getClassLoader().getResourceAsStream("images/AjxpLogo16-Bi.png")));
        
        Point p = shell.getSize();
        shell.setBounds(500, 500, p.x, p.y);                
        this.moveShellWithMouse(this, shell);
	    
	}		
	
	private void moveShellWithMouse(Control cont, final Shell shell){
		
		// add ability to move shell around
	    moveListener = new Listener() {
	      Point origin;

	      public void handleEvent(Event e) {
	        switch (e.type) {
	        case SWT.MouseDown:
	          origin = new Point(e.x, e.y);
	          break;
	        case SWT.MouseUp:
	          origin = null;
	          break;
	        case SWT.MouseMove:
	          if (origin != null) {
	            Point p = shell.getDisplay().map(shell, null, e.x, e.y);
	            shell.setLocation(p.x - origin.x, p.y - origin.y);
	          }
	          break;
	        }
	      }
	    };
	    cont.addListener(SWT.MouseDown, moveListener);
	    cont.addListener(SWT.MouseUp, moveListener);
	    cont.addListener(SWT.MouseMove, moveListener);
		
	}
	
	public void notifyJobStateChanged(String nodeId, boolean running){
		//this.logViewer.reload();
	}
	
	private void initGUI() {
		try {
			FillLayout thisLayout = new FillLayout();
			this.setLayout(thisLayout);
			this.setSize(280,230);
			jobEditor = new JobEditor(this, this);				
			FillLayout compositeLayout = new FillLayout();
			compositeLayout.type = SWT.HORIZONTAL|SWT.VERTICAL;
			jobEditor.setLayout(compositeLayout);
			jobEditor.populateToolkit();
			moveShellWithMouse(jobEditor.getMouseHandler(), getShell());
			this.layout();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
		
	protected void closeConfig(){
		sTray.closeConfiguration();
	}
	
	protected void saveConfig(){
		try {
			Node n = Manager.getInstance().updateSynchroNode(jobEditor.getFormData(), currentSynchroNode);
			this.setCurrentNode(n);
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
	
	protected void deleteConfig(){
		try {
			Manager.getInstance().deleteSynchroNode(currentSynchroNode);
			currentSynchroNode = null;
			try {
				Collection<Node> nodes = Manager.getInstance().listSynchroNodes();
				if(nodes.size()>0){
					this.setCurrentNode(nodes.iterator().next());
				}
			} catch (Exception e) {
			}						
		} catch (SchedulerException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	
	protected void setCurrentNode(Node n){
		if(n == null){
			currentSynchroNode = null;
			jobEditor.clearFormData();
			//logViewer.clearSynchroLog();
		}else{
			currentSynchroNode = n;
			jobEditor.loadFormFromNode(n);
			//logViewer.loadSynchroLog(n);
			
		}
	}
	
	private static Image oldImage = null;

	protected void applyGradientBG(Composite composite) {
		Rectangle rect = composite.getClientArea();
		Image newImage = new Image(composite.getDisplay(), 1, Math.max(1,
				rect.height));
		GC gc = new GC(newImage);
		gc.setForeground(composite.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		gc.setBackground(SWTResourceManager.getColor(213, 221, 227));
		gc.fillGradientRectangle(0, 0, 1, 150, true);
		gc.setBackground(SWTResourceManager.getColor(181, 196, 210));
		gc.fillRectangle(0, 1, 1, 29);
		gc.dispose();
		composite.setBackgroundImage(newImage);

		if (oldImage != null)
			oldImage.dispose();
		oldImage = newImage;
	}	
	
	
}
