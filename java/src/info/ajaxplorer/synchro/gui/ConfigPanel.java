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
	
	public ConfigPanel(Composite shell, SysTray sysTray) {
		super(shell, SWT.NONE);
		this.initShell((Shell) shell);
		this.initGUI();
        shell.pack();
        this.sTray = sysTray;
		
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
		
        shell.setText(Manager.getMessage("shell_title"));
        shell.setSize(450, 400);
        //shell.setImage(new Image(shell.getDisplay(), this.getClass().getClassLoader().getResourceAsStream("images/AjxpLogo16-Bi.png")));
        
        Point p = shell.getSize();

        shell.setBounds(150, 150, p.x, p.y);
                
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
			if(System.getProperty("os.name").toLowerCase().indexOf("mac") >= 0){
				this.setSize(680, 520);
			}else{
				this.setSize(600, 500);
			}
			//this.setBackground(SWTResourceManager.getColor(94, 124, 144));
			this.applyGradientBG(this);
			this.setBackgroundMode(1);
			{
				FormData titleImageLData = new FormData();
				titleImageLData.left =  new FormAttachment(0, 1000, 0);
				titleImageLData.top =  new FormAttachment(0, 1000, 1);
				titleImageLData.width = 261;
				titleImageLData.height = 29;
				titleImage = new Label(this, SWT.NONE);
				titleImage.setImage(SWTResourceManager.getImage("images/SynchroLayout_03.png"));
				titleImage.setLayoutData(titleImageLData);
				titleImage.addListener(SWT.MouseDown, moveListener);
				titleImage.addListener(SWT.MouseUp, moveListener);
				titleImage.addListener(SWT.MouseMove, moveListener);				
			}
			{
				minimize = new Label(this, SWT.NONE);
				FormData minimizeLData = new FormData();
				minimizeLData.width = 32;
				minimizeLData.height = 29;
				minimizeLData.top =  new FormAttachment(0, 1000, 1);
				minimizeLData.right =  new FormAttachment(1000, 1000, 0);
				minimize.setLayoutData(minimizeLData);
				minimize.setImage(SWTResourceManager.getImage("images/SynchroLayout_05.png"));
				minimize.setToolTipText(Manager.getMessage("cpanel_tooltip_close"));
				minimize.addMouseListener(new MouseListener() {
					
					@Override
					public void mouseUp(MouseEvent arg0) {						
					}
					
					@Override
					public void mouseDown(MouseEvent arg0) {
						sTray.closeConfiguration();
					}
					
					@Override
					public void mouseDoubleClick(MouseEvent arg0) {
					}
				});
			}
			{
				cLabel1 = new CLabel(this, SWT.NONE);
				FormData cLabel1LData = new FormData();
				cLabel1LData.left =  new FormAttachment(0, 1000, 11);
				cLabel1LData.top =  new FormAttachment(0, 1000, 47);
				cLabel1LData.width = 317;
				cLabel1LData.height = 19;
				cLabel1.setLayoutData(cLabel1LData);
				cLabel1.setText(Manager.getMessage("cpanel_selectjob"));
				cLabel1.setForeground(SWTResourceManager.getColor(0, 0, 0));
			}
			{
				cCombo1 = new Combo(this, SWT.BORDER);
				FormData cCombo1LData = new FormData();
				cCombo1LData.left =  new FormAttachment(0, 1000, 12);
				cCombo1LData.top =  new FormAttachment(0, 1000, 67);
				cCombo1LData.width = 289;
				cCombo1LData.height = 21;
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
					tabItem1.setText(" " + Manager.getMessage("cpanel_tab_jobdata") + " ");
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
					tabItem2.setText(" " + Manager.getMessage("cpanel_tab_logs") + " ");
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
			e.printStackTrace();
		} catch (SQLException e) {
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
		keys.add(Manager.getMessage("cpanel_create_synchro"));		
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
