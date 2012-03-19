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



/**
* This code was edited or generated using CloudGarden's Jigloo
* SWT/Swing GUI Builder, which is free for non-commercial
* use. If Jigloo is being used commercially (ie, by a corporation,
* company or business for any purpose whatever) then you
* should purchase a license for each developer using Jigloo.
* Please visit www.cloudgarden.com for details.
* Use of Jigloo implies acceptance of these licensing terms.
* A COMMERCIAL LICENSE HAS NOT BEEN PURCHASED FOR
* THIS MACHINE, SO JIGLOO OR THIS CODE CANNOT BE USED
* LEGALLY FOR ANY CORPORATE OR COMMERCIAL PURPOSE.
*/
public class ConfigPanel extends Canvas {	

	{
		//Register as a resource user - SWTResourceManager will
		//handle the obtaining and disposing of resources
		SWTResourceManager.registerResourceUser(this);
	}
	
	
	private Label minimize;
	private CLabel cLabel1;
	private Label Title;
	private Combo cCombo1;
	ArrayList<Object> jobComboItems;
	private CTabItem tabItem2;
	private CTabItem tabItem1;
	private CTabFolder tabFolder1;
	private Node currentSynchroNode;
	LogViewer logViewer;
	
	private JobEditor jobEditor;
	
	public ConfigPanel(Composite shell) {
		super(shell, SWT.NONE);
		this.initShell((Shell) shell);
		this.initGUI();
        shell.pack();
        
		
		try {
			Collection<Node> nodes = Manager.getInstance().listSynchroNodes();
			if(nodes.size()>0){
				this.setCurrentNode(nodes.iterator().next());
			}
		} catch (Exception e) {
		}
		
		
	}		

	public void initShell(final Shell shell){
		
		shell.addListener(SWT.Close, new Listener() {
			public void handleEvent(Event event) {
				shell.setVisible(false);
				event.doit = false;
			}
		});		
		
		shell.addListener(SWT.MIN, new Listener() {
			public void handleEvent(Event event) {
				shell.setVisible(false);
				event.doit = false;
			}
	    });		
		
        shell.setText("AjaXplorer Synchronizer");
        shell.setSize(450, 400);
        shell.setImage(new Image(shell.getDisplay(), this.getClass().getClassLoader().getResourceAsStream("images/AjxpLogo16-Bi.png")));
        
        Point p = shell.getSize();

        shell.setBounds(150, 150, p.x, p.y);
                
		// add ability to move shell around
	    Listener l = new Listener() {
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
	    this.addListener(SWT.MouseDown, l);
	    this.addListener(SWT.MouseUp, l);
	    this.addListener(SWT.MouseMove, l);
	    
	    
	}		
	
	public void notifyJobStateChanged(String nodeId, boolean running){
		this.logViewer.reload();
	}
	
	private void initGUI() {
		try {
			FormLayout thisLayout = new FormLayout();
			this.setLayout(thisLayout);
			if(System.getProperty("os.name").toLowerCase().indexOf("mac") >= 0){
				this.setSize(680, 520);
			}else{
				this.setSize(600, 500);
			}
			//this.setBackground(SWTResourceManager.getColor(94, 124, 144));
			this.applyGradientBG(this);
			this.setBackgroundMode(1);
			{
				minimize = new Label(this, SWT.NONE);
				FormData minimizeLData = new FormData();
				minimizeLData.width = 16;
				minimizeLData.height = 16;
				minimizeLData.top =  new FormAttachment(0, 1000, 4);
				minimizeLData.right =  new FormAttachment(1000, 1000, -4);
				minimize.setLayoutData(minimizeLData);
				minimize.setImage(SWTResourceManager.getImage("images/minimize.png"));
				minimize.setToolTipText("Close this window");
				minimize.addMouseListener(new MouseListener() {
					
					@Override
					public void mouseUp(MouseEvent arg0) {
						// TODO Auto-generated method stub
						
					}
					
					@Override
					public void mouseDown(MouseEvent arg0) {
						// TODO Auto-generated method stub
						getShell().setVisible(false);
					}
					
					@Override
					public void mouseDoubleClick(MouseEvent arg0) {
						// TODO Auto-generated method stub
						
					}
				});
			}
			{
				Title = new Label(this, SWT.NONE);
				FormData TitleLData = new FormData();
				TitleLData.left =  new FormAttachment(0, 1000, 13);
				TitleLData.top =  new FormAttachment(0, 1000, 10);
				TitleLData.width = 463;
				TitleLData.height = 26;
				TitleLData.right =  new FormAttachment(1000, 1000, -124);
				Title.setLayoutData(TitleLData);
				Title.setText("AjaXplorer Synchronizer");
				Title.setForeground(SWTResourceManager.getColor(255, 255, 255));
				//Title.setForeground(SWTResourceManager.getColor(94, 124, 144));
				Title.setFont(SWTResourceManager.getFont("Arial", 18, 0, false, false));
			}
			{
				cLabel1 = new CLabel(this, SWT.NONE);
				FormData cLabel1LData = new FormData();
				cLabel1LData.left =  new FormAttachment(0, 1000, 12);
				cLabel1LData.top =  new FormAttachment(0, 1000, 52);
				cLabel1LData.width = 317;
				cLabel1LData.height = 19;
				cLabel1.setLayoutData(cLabel1LData);
				cLabel1.setText("Select a synchronisation job to edit");
				cLabel1.setForeground(SWTResourceManager.getColor(255, 255, 255));
			}
			{
				cCombo1 = new Combo(this, SWT.BORDER);
				FormData cCombo1LData = new FormData();
				cCombo1LData.left =  new FormAttachment(0, 1000, 12);
				cCombo1LData.top =  new FormAttachment(0, 1000, 73);
				cCombo1LData.width = 289;
				//cCombo1LData.height = 21;
				cCombo1.setLayoutData(cCombo1LData);
				loadJobComboValues();
				initJobComboListener();
			}
			{
				tabFolder1 = new CTabFolder(this, SWT.TOP|SWT.FLAT);
				FormData tabFolder1LData = new FormData();
				tabFolder1LData.left =  new FormAttachment(0, 1000, 0);
				tabFolder1LData.right =  new FormAttachment(1000, 1000, 0);
				tabFolder1LData.top =  new FormAttachment(0, 1000, 105);
				tabFolder1LData.bottom =  new FormAttachment(1000, 1000, 0);
				tabFolder1LData.width = 592;
				tabFolder1LData.height = 369;
				tabFolder1.setLayoutData(tabFolder1LData);
				{
					tabItem1 = new CTabItem(tabFolder1, SWT.NONE);
					tabItem1.setText(" Job Data ");
					{				
						jobEditor = new JobEditor(tabFolder1, this);				
						tabItem1.setControl(jobEditor);
						FillLayout compositeLayout = new FillLayout();
						compositeLayout.type = SWT.HORIZONTAL|SWT.VERTICAL;
						jobEditor.setLayout(compositeLayout);
						jobEditor.populateToolkit();
					}
				}
				{
					tabItem2 = new CTabItem(tabFolder1, SWT.NONE);
					tabItem2.setText(" Synchronization Logs ");
					{
						logViewer = new LogViewer(tabFolder1, SWT.NONE);
						tabItem2.setControl(logViewer);
						FillLayout compositeLayout = new FillLayout();
						compositeLayout.type = SWT.HORIZONTAL|SWT.VERTICAL;
						logViewer.setLayout(compositeLayout);						
						logViewer.initGUI();
					}
				}
				tabFolder1.setSelection(0);
				tabFolder1.addPaintListener(new PaintListener() {
					
					@Override
					public void paintControl(PaintEvent e) {
						Color c = SWTResourceManager.getColor(100, 100, 100);
		                e.gc.setForeground(c);
		                if(tabFolder1.getSelection() == tabItem1){
		                	Rectangle r2 = tabItem2.getBounds();
		                	e.gc.drawLine(r2.x-2, 0, r2.x + r2.width-1, 0);
		                }else{
		                	Rectangle r = tabItem1.getBounds();
		                	e.gc.drawLine(0, 0, r.x + r.width+1, 0);		                	
		                }
					}
				});			
				tabFolder1.setTabHeight(20);
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
			loadJobComboValues();
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
			loadJobComboValues();
		} catch (SchedulerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	private void loadJobComboValues(){
		Collection<Node> nodes = Manager.getInstance().listSynchroNodes();
		jobComboItems = new ArrayList<Object>();
		ArrayList<String> keys = new ArrayList<String>();
		int currentSel = 0;
		int i=0;
		for(Node syncNode:nodes){
			String label = Manager.getInstance().makeJobLabel(syncNode, false);
			jobComboItems.add(syncNode);
			keys.add(label);
			if(currentSynchroNode != null && syncNode.id == currentSynchroNode.id) {
				currentSel = i;
			}
			i++;
		}
		keys.add("Create a new synchronization job...");		
		cCombo1.setItems(keys.toArray(new String[0]));
		cCombo1.select(currentSel);
	}
	
	private void initJobComboListener(){
		
		cCombo1.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				Collection<Node> nodes = Manager.getInstance().listSynchroNodes();
				String labelSelected = cCombo1.getText();
				boolean found = false;
				for(Node n:nodes){
					if(Manager.getInstance().makeJobLabel(n, false).equals(labelSelected)){
						found = true;
						setCurrentNode(n);
						break;
					}
				}
				if(!found){
					setCurrentNode(null);
				}
			};
		});		
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
		gc.setBackground(composite.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		gc.setForeground(SWTResourceManager.getColor(94, 124, 144));
		gc.fillGradientRectangle(0, 0, 1, 135, true);
		gc.dispose();
		composite.setBackgroundImage(newImage);

		if (oldImage != null)
			oldImage.dispose();
		oldImage = newImage;
	}	
	
	
}
