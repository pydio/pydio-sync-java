package info.ajaxplorer.synchro.gui;

import info.ajaxplorer.client.model.Node;
import info.ajaxplorer.synchro.Manager;

import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.quartz.SchedulerException;

public class ConfigPanel extends Canvas {	

	{
		SWTResourceManager.registerResourceUser(this);
	}
	
	
	private Label minimize;
	private CLabel cLabel1;
	private Combo cCombo1;
	ArrayList<Object> jobComboItems;
	private CTabItem tabItem2;
	private Label titleImage;
	private CTabItem tabItem1;
	private CTabFolder tabFolder1;
	private Node currentSynchroNode;
	LogViewer logViewer;
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
			/*
			if(this.currentSynchroNode == null){
				Collection<Node> nodes = Manager.getInstance().listSynchroNodes();
				if(nodes.size()>0){
					this.setCurrentNode(nodes.iterator().next());
				}
			}
			*/
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
        shell.setSize(450, 400);
        //shell.setImage(new Image(shell.getDisplay(), this.getClass().getClassLoader().getResourceAsStream("images/AjxpLogo16-Bi.png")));
        
        Point p = shell.getSize();

        shell.setBounds(150, 150, p.x, p.y);                
        this.moveShellWithMouse(shell);
	    
	}		
	
	private void moveShellWithMouse(final Shell shell){
		
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
	    this.addListener(SWT.MouseDown, moveListener);
	    this.addListener(SWT.MouseUp, moveListener);
	    this.addListener(SWT.MouseMove, moveListener);
		
	}
	
	public void notifyJobStateChanged(String nodeId, boolean running){
		this.logViewer.reload();
	}
	
	private void initGUI() {
		try {
			FormLayout thisLayout = new FormLayout();
			this.setLayout(thisLayout);
			if(System.getProperty("os.name").toLowerCase().indexOf("windows xp") >= 0){
				this.setSize(240, 540);
			}else{
				this.setSize(280, 580);
			}
			{
				final Composite contentPanel = new Composite (this, SWT.NULL);
				FormData tabFolder1LData = new FormData();
				tabFolder1LData.left =  new FormAttachment(0, 1000, 0);
				tabFolder1LData.right =  new FormAttachment(1000, 1000, 0);
				tabFolder1LData.top =  new FormAttachment(0, 1000, 0);
				tabFolder1LData.bottom =  new FormAttachment(1000, 1000, 0);
				contentPanel.setLayoutData(tabFolder1LData);
				final StackLayout layout = new StackLayout ();
				contentPanel.setLayout(layout);
				{				
					jobEditor = new JobEditor(contentPanel, this);				
					FillLayout compositeLayout = new FillLayout();
					compositeLayout.type = SWT.HORIZONTAL|SWT.VERTICAL;
					jobEditor.setLayout(compositeLayout);
					jobEditor.populateToolkit();
				}
				{
					logViewer = new LogViewer(contentPanel, SWT.NONE);
					FillLayout compositeLayout = new FillLayout();
					compositeLayout.type = SWT.HORIZONTAL|SWT.VERTICAL;
					logViewer.setLayout(compositeLayout);						
					logViewer.initGUI();
				}				
				layout.topControl = jobEditor;
			}
			
			this.layout();
		} catch (Exception e) {
			e.printStackTrace();
		}
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
			logViewer.clearSynchroLog();
		}else{
			currentSynchroNode = n;
			jobEditor.loadFormFromNode(n);
			logViewer.loadSynchroLog(n);
			
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
