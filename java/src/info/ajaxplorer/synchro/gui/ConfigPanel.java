package info.ajaxplorer.synchro.gui;

import info.ajaxplorer.client.model.Node;
import info.ajaxplorer.synchro.Manager;

import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
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

import com.cloudgarden.resource.SWTResourceManager;

public class ConfigPanel extends Canvas {	
	
	private Label minimize;
	private CLabel cLabel1;
	private Label Title;
	private Combo cCombo1;
	ArrayList<Object> jobComboItems;
	private Node currentSynchroNode;

	
	private JobEditor jobEditor;
	
	public ConfigPanel(Composite shell) {
		super(shell, SWT.NONE);
		this.initShell((Shell) shell);
		this.initGUI();
        shell.pack();
        
		
		try {
			Collection<Node> nodes = Manager.getInstance().listSynchroNodes();
			if(nodes.size()>0){
				Node currentSynchroNode = nodes.iterator().next();
				jobEditor.loadFormFromNode(currentSynchroNode);
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
	
	private void initGUI() {
		try {
			FormLayout thisLayout = new FormLayout();
			this.setLayout(thisLayout);
			if(System.getProperty("os.name").toLowerCase().indexOf("mac") >= 0){
				this.setSize(750, 550);
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
				cCombo1LData.height = 21;
				cCombo1.setLayoutData(cCombo1LData);
				loadJobComboValues();
				initJobComboListener();
			}
			{
				jobEditor = new JobEditor(this);				
				FillLayout compositeLayout = new FillLayout();
				compositeLayout.type = SWT.HORIZONTAL;
				jobEditor.setLayout(compositeLayout);
				FormData cTabFolderLData = new FormData();				
				cTabFolderLData.left =  new FormAttachment(0, 1000, 0);
				cTabFolderLData.top =  new FormAttachment(0, 1000, 105);
				cTabFolderLData.right =  new FormAttachment(1000, 1000, 0);
				cTabFolderLData.bottom =  new FormAttachment(1000, 1000, 0);
				jobEditor.setLayoutData(cTabFolderLData);
				jobEditor.populateToolkit();
			}
			this.layout();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	protected void saveConfig(){
		try {
			currentSynchroNode = Manager.getInstance().updateSynchroNode(jobEditor.getFormData(), currentSynchroNode);
			loadJobComboValues();
			//this.form.setText(Manager.getInstance().makeJobLabel(currentSynchroNode));
			jobEditor.loadFormFromNode(currentSynchroNode);
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
					currentSynchroNode = nodes.iterator().next();
					jobEditor.loadFormFromNode(currentSynchroNode);
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
			String label = Manager.getInstance().makeJobLabel(syncNode);
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
					if(Manager.getInstance().makeJobLabel(n).equals(labelSelected)){
						found = true;
						currentSynchroNode = n;
						jobEditor.loadFormFromNode(currentSynchroNode);
						break;
					}
				}
				if(!found){
					currentSynchroNode = null;
					jobEditor.clearFormData();					
				}
			};
		});		
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
