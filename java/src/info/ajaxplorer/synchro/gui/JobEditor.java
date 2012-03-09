package info.ajaxplorer.synchro.gui;
import info.ajaxplorer.client.http.AjxpAPI;
import info.ajaxplorer.client.http.RestRequest;
import info.ajaxplorer.client.http.RestStateHolder;
import info.ajaxplorer.client.model.Node;
import info.ajaxplorer.client.model.Server;
import info.ajaxplorer.synchro.Manager;
import info.ajaxplorer.synchro.model.CipheredServer;

import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.cloudgarden.resource.SWTResourceManager;
import com.j256.ormlite.dao.Dao;

public class JobEditor extends org.eclipse.swt.widgets.Canvas{

	{
		//Register as a resource user - SWTResourceManager will
		//handle the obtaining and disposing of resources
		SWTResourceManager.registerResourceUser(this);
	}
	
	private Text tfHost;
	private Button buttonFileChooser;
	private Text tfTarget;
	private Text tfPassword;
	private Button radioDirection2;
	private CLabel cLabel1;
	private Combo cCombo1;
	private Label Title;
	private Button buttonLoadRepositories;
	private Button radioSyncInterval2;
	private Combo comboRepository;
	private Button radioSyncInterval3;
	private Button radioSyncInterval1;
	private Button radioDirection3;
	private Button radioDirection;
	private Button checkboxActive;
	private Text tfLogin;
	ArrayList<Object> jobComboItems;
	
	private HashMap<String, String> repoItems;
	private Node currentSynchroNode;
	
	private Form form;
	

	/**
	* Overriding checkSubclass allows this class to extend org.eclipse.swt.widgets.Composite
	*/	
	protected void checkSubclass() {
	}

	protected void saveConfig(){
		try {
			currentSynchroNode = Manager.getInstance().updateSynchroNode(getFormData(), currentSynchroNode);
			loadJobComboValues();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
	
	public JobEditor(final org.eclipse.swt.widgets.Composite parent, int style) {
		super(parent, SWT.NONE);
		initGUI();
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
	            Point p = JobEditor.this.getDisplay().map(JobEditor.this, null, e.x, e.y);
	            JobEditor.this.getShell().setLocation(p.x - origin.x, p.y - origin.y);
	          }
	          break;
	        }
	      }
	    };
	    this.addListener(SWT.MouseDown, l);
	    this.addListener(SWT.MouseUp, l);
	    this.addListener(SWT.MouseMove, l);
	    
	    
		try {
			Collection<Node> nodes = Manager.getInstance().listSynchroNodes();
			if(nodes.size()>0){
				currentSynchroNode = nodes.iterator().next();
				loadFormFromNode(currentSynchroNode);
			}
		} catch (Exception e) {
		}

	}
	
	protected void loadFormFromNode(Node baseNode){
		Server s;
		try {
			s = new CipheredServer(baseNode.getParent());
			HashMap<String, String> values = new HashMap<String, String>();
			values.put("HOST", s.getUrl());
			values.put("LOGIN", s.getUser());
			values.put("PASSWORD", s.getPassword());
			values.put("REPOSITORY_LABEL", baseNode.getLabel());
			values.put("REPOSITORY_ID", baseNode.getPropertyValue("repository_id"));
			values.put("TARGET", baseNode.getPropertyValue("target_folder"));
			values.put("ACTIVE", baseNode.getPropertyValue("synchro_active"));
			values.put("DIRECTION", baseNode.getPropertyValue("synchro_direction"));
			values.put("INTERVAL", baseNode.getPropertyValue("synchro_interval"));
			this.loadFormData(values);		
			if(this.form != null){
				this.form.setText(Manager.getInstance().makeJobLabel(baseNode));
			}
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}
	
	public Map<String, String> getFormData() throws Exception{
		HashMap<String, String> values = new HashMap<String, String>();
		if(repoItems == null) throw new Exception("Please select a repository first!");
		
		values.put("HOST", tfHost.getText());		
		values.put("LOGIN", tfLogin.getText());		
		values.put("PASSWORD", tfPassword.getText());		
		values.put("REPOSITORY_LABEL", comboRepository.getText());
		values.put("REPOSITORY_ID", repoItems.get(comboRepository.getText()));
		values.put("TARGET", tfTarget.getText());
		values.put("ACTIVE", (checkboxActive.getSelection()?"true":"false"));
		String dir = "bi";
		if(radioDirection2.getSelection()) dir = "down";
		else if(radioDirection3.getSelection()) dir = "up";
		values.put("DIRECTION", dir);
		String freq = "minute";
		if(radioSyncInterval2.getSelection()) freq = "hour";
		else if(radioSyncInterval3.getSelection()) freq = "day";
		values.put("INTERVAL", freq);
		return values;
	}
	
	public void loadFormData(Map<String, String> values){
		tfHost.setText(values.get("HOST"));
		tfLogin.setText(values.get("LOGIN"));
		tfPassword.setText(values.get("PASSWORD"));
		comboRepository.setText(values.get("REPOSITORY_LABEL"));
		repoItems = new HashMap<String, String>();
		repoItems.put(values.get("REPOSITORY_LABEL"), values.get("REPOSITORY_ID"));
		tfTarget.setText(values.get("TARGET"));
		checkboxActive.setSelection(values.get("ACTIVE").equals("true"));
		
		radioDirection.setSelection(values.get("DIRECTION").equals("bi"));
		radioDirection2.setSelection(values.get("DIRECTION").equals("down"));
		radioDirection3.setSelection(values.get("DIRECTION").equals("up"));
		
		radioSyncInterval1.setSelection(values.get("INTERVAL").equals("minute"));
		radioSyncInterval2.setSelection(values.get("INTERVAL").equals("hour"));
		radioSyncInterval3.setSelection(values.get("INTERVAL").equals("day"));
	}

	public void clearFormData(){
		tfHost.setText("");
		tfLogin.setText("");
		tfPassword.setText("");
		comboRepository.setText("");
		comboRepository.setItems(new String[0]);
		repoItems = new HashMap<String, String>();
		tfTarget.setText("");
		checkboxActive.setSelection(true);
		
		radioDirection.setSelection(true);
		radioDirection2.setSelection(false);
		radioDirection3.setSelection(false);
		
		radioSyncInterval1.setSelection(false);
		radioSyncInterval2.setSelection(true);
		radioSyncInterval3.setSelection(false);
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
						loadFormFromNode(currentSynchroNode);
						break;
					}
				}
				if(!found){
					currentSynchroNode = null;
					clearFormData();					
				}
			};
		});		
	}
	
	private void loadRepositories(){
		
		System.out.println("Loading repositories");
		if(tfHost.getText().equals("") || tfLogin.getText().equals("") || tfPassword.getText().equals("") ){
			return;
		}
		System.out.println("Update comborepo");

		comboRepository.setText("Loading ...");
		String host = tfHost.getText();
		String login = tfLogin.getText();
		String pass = tfPassword.getText();
		
		Server s;
		try {
			s = new CipheredServer("Test", host, login, pass, true, false);
			RestStateHolder.getInstance().setServer(s);
			AjxpAPI.getInstance().setServer(s);
			RestRequest rest = new RestRequest();
			System.out.println("Will send request");
			Document doc = rest.getDocumentContent(AjxpAPI.getInstance().getGetXmlRegistryUri());
			final Dao<Node, String> nodeDao = Manager.getInstance().getNodeDao();
			
			NodeList mainTag = doc.getElementsByTagName("repositories");
			if(mainTag.getLength() == 0){
				throw new Exception("No repositories found");
			}			
			final NodeList repos = mainTag.item(0).getChildNodes();
			repoItems = new HashMap<String, String>();
			
			nodeDao.callBatchTasks(new Callable<Void>() {
				public Void call() throws Exception{			
					if (repos!=null && repos.getLength() > 0){
						for (int i = 0; i < repos.getLength(); i++) {
							org.w3c.dom.Node xmlNode = repos.item(i);
							Node repository = new Node(Node.NODE_TYPE_REPOSITORY, "", null);
							repository.properties = nodeDao.getEmptyForeignCollection("properties");
							repository.initFromXmlNode(xmlNode);
							boolean excluded = false;
							for(int p =0;p<Manager.EXCLUDED_ACCESS_TYPES.length;p++){
								if(repository.getPropertyValue("access_type").equalsIgnoreCase(Manager.EXCLUDED_ACCESS_TYPES[p])){
									excluded = true; break;
								}
							}
							if(excluded) {
								continue;
							}
							repoItems.put(repository.getLabel(), repository.getPropertyValue("repository_id"));
						}
					}
					return null;
				}
			});
			System.out.println("Parsed response!");

			comboRepository.setText("");
			comboRepository.setItems(repoItems.keySet().toArray(new String[0]));
			
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
	}
	
	private void initGUI() {
		try {
			FormLayout thisLayout = new FormLayout();
			this.setLayout(thisLayout);
			this.setSize(600, 600);
			this.setBackground(SWTResourceManager.getColor(94, 124, 144));		
			this.setBackgroundMode(1);
			{
				cLabel1 = new CLabel(this, SWT.NONE);
				FormData cLabel1LData = new FormData();
				cLabel1LData.left =  new FormAttachment(0, 1000, 11);
				cLabel1LData.top =  new FormAttachment(0, 1000, 32);
				cLabel1LData.width = 360;
				cLabel1LData.height = 19;
				cLabel1.setLayoutData(cLabel1LData);
				cLabel1.setText("Select a synchronisation job to edit");
				cLabel1.setForeground(SWTResourceManager.getColor(255, 255, 255));
			}
			{
				cCombo1 = new Combo(this, SWT.BORDER);
				FormData cCombo1LData = new FormData();
				cCombo1LData.left =  new FormAttachment(0, 1000, 12);
				cCombo1LData.top =  new FormAttachment(0, 1000, 56);
				cCombo1LData.width = 360;
				cCombo1.setLayoutData(cCombo1LData);
				loadJobComboValues();
				initJobComboListener();
			}
			{
				Title = new Label(this, SWT.NONE);
				FormData TitleLData = new FormData();
				TitleLData.left =  new FormAttachment(0, 1000, 13);
				TitleLData.top =  new FormAttachment(0, 1000, 9);
				TitleLData.width = 360;
				TitleLData.height = 26;
				TitleLData.right =  new FormAttachment(1000, 1000, -106);
				Title.setLayoutData(TitleLData);
				Title.setText("AjaXplorer Synchronizer");
				Title.setForeground(SWTResourceManager.getColor(255, 255, 255));
				Title.setBackground(SWTResourceManager.getColor(94, 124, 144));
				Title.setFont(SWTResourceManager.getFont("Arial", 18, 0, false, false));
			}
			{
				Composite formComposite = new Composite(this, SWT.NONE);
				FillLayout compositeLayout = new FillLayout(SWT.FILL);
				formComposite.setLayout(compositeLayout);
				FormData cTabFolderLData = new FormData();				
				cTabFolderLData.left =  new FormAttachment(0, 1000, 0);
				cTabFolderLData.top =  new FormAttachment(0, 1000, 85);
				cTabFolderLData.width = 407;
				cTabFolderLData.height = 330;
				cTabFolderLData.right =  new FormAttachment(1000, 1000, 0);
				cTabFolderLData.bottom =  new FormAttachment(1000, 1000, 0);
				formComposite.setLayoutData(cTabFolderLData);
				populateToolkit(formComposite);
				
			}
			this.layout();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void populateToolkit(Composite parent) {
		FormToolkit toolkit = new FormToolkit(parent.getDisplay());
		final Form form = toolkit.createForm(parent);
		form.setText("New job ... ");
		this.form = form;
		toolkit.decorateFormHeading(form);
		TableWrapLayout layout = new TableWrapLayout();
		layout.verticalSpacing = 20;
		layout.horizontalSpacing = 10;
		//layout.makeColumnsEqualWidth = true;
		layout.numColumns = 2;
		form.getBody().setLayout(layout);		


		Section section = configureSection(toolkit, form, "Server Connection", "Set up the remote server connection. The URL will be the exact same adress as you would use to access AjaXplorer via a web browser.", 1);
		Composite sectionClient = toolkit.createComposite(section);		
		layout = new TableWrapLayout();
		sectionClient.setLayout(layout);
		layout.numColumns = 2;
		layout.horizontalSpacing = 8;
		layout.verticalSpacing = 10;
		
		section.setClient(sectionClient);	
		
		// HOST
		toolkit.createLabel(sectionClient, "Host URL : ");
		tfHost = toolkit.createText(sectionClient, "");
		tfHost.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		
		// LOGIN
		toolkit.createLabel(sectionClient, "Login : ");
		tfLogin = toolkit.createText(sectionClient, "");
		tfLogin.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		
		// PASSWORD
		toolkit.createLabel(sectionClient, "Password : ");
		tfPassword = toolkit.createText(sectionClient, "", SWT.PASSWORD);
		tfPassword.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		
		Section section2 = configureSection(toolkit, form, "Synchronization Targets", "Once the remote connection is set up, load the accessible repositories and choose one, and browse the local folder to synchronize with.", 1);
		((TableWrapData)section2.getLayoutData()).maxWidth = 430;
		
		Composite sectionClient2 = toolkit.createComposite(section2);
		layout = new TableWrapLayout();
		sectionClient2.setLayout(layout);
		layout.numColumns = 3;		
		section2.setClient(sectionClient2);	
		
		// REPOSITORY CHOOSER
		toolkit.createLabel(sectionClient2, "Repository : ");
		comboRepository = new Combo(sectionClient2, SWT.BORDER);
		comboRepository.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		toolkit.adapt(comboRepository, true, true);
		
		buttonLoadRepositories = toolkit.createButton(sectionClient2, "Load", SWT.PUSH);
		buttonLoadRepositories.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				loadRepositories();
			}
		});
		buttonLoadRepositories.setLayoutData(new TableWrapData());

		// TARGET FOLDER CHOOSER
		toolkit.createLabel(sectionClient2, "Local Folder : ");
		tfTarget = toolkit.createText(sectionClient2, "");
		TableWrapData td = new TableWrapData(TableWrapData.FILL_GRAB);
		td.valign = TableWrapData.MIDDLE;
		tfTarget.setLayoutData(td);
		
		buttonFileChooser = toolkit.createButton(sectionClient2, "Browse", SWT.PUSH);
		td = new TableWrapData(TableWrapData.FILL_GRAB);
		td.valign = TableWrapData.MIDDLE;
		buttonFileChooser.setLayoutData(td);
		buttonFileChooser.addListener(SWT.Selection, new Listener() {
			
			@Override
			public void handleEvent(Event arg0) {									
				DirectoryDialog dialog = new DirectoryDialog(JobEditor.this.getShell());
				String res = dialog.open();
				if(res != null){
					tfTarget.setText(res);
				}
			}
		});
		
		Section section3 = configureSection(toolkit, form, "Job Execution Parameters", "Set how this synchronization job must be executed and when", 2);		
		Composite sectionClient3 = toolkit.createComposite(section3);
		layout = new TableWrapLayout();
		sectionClient3.setLayout(layout);
		layout.numColumns = 2;
		section3.setClient(sectionClient3);			
		
		Label lab = toolkit.createLabel(sectionClient3, "Synchronisation direction : ");
		lab.setLayoutData(new TableWrapData(TableWrapData.LEFT, TableWrapData.MIDDLE));
		Composite rComp = toolkit.createComposite(sectionClient3);
		rComp.setLayoutData(new TableWrapData(TableWrapData.LEFT, TableWrapData.MIDDLE));
		layout = new TableWrapLayout();
		layout.numColumns = 3;
		rComp.setLayout(layout);
		radioDirection = toolkit.createButton(rComp, "Bidirectionnal", SWT.RADIO);
		radioDirection2 = toolkit.createButton(rComp, "Download only", SWT.RADIO);
		radioDirection3 = toolkit.createButton(rComp, "Upload only", SWT.RADIO);
				
		Label lab2 = toolkit.createLabel(sectionClient3, "Execute synchro every : ");
		lab2.setLayoutData(new TableWrapData(TableWrapData.LEFT, TableWrapData.MIDDLE));
		Composite rComp2= toolkit.createComposite(sectionClient3);
		rComp2.setLayoutData(new TableWrapData(TableWrapData.LEFT, TableWrapData.MIDDLE));
		layout = new TableWrapLayout();
		layout.numColumns = 3;
		rComp2.setLayout(layout);
		radioSyncInterval1 = toolkit.createButton(rComp2, "Minutes", SWT.RADIO);
		radioSyncInterval2 = toolkit.createButton(rComp2, "Hours", SWT.RADIO);
		radioSyncInterval3 = toolkit.createButton(rComp2, "Days", SWT.RADIO);
		
		checkboxActive = toolkit.createButton(sectionClient3, "This job is active", SWT.CHECK | SWT.SELECTED);
		td = new TableWrapData(TableWrapData.FILL_GRAB);
		td.colspan = 2;
		checkboxActive.setLayoutData(td);

		/*
		{
			saveButton = new Button(composite1, SWT.PUSH | SWT.LEFT);
			FormData saveButtonLData = new FormData();
			saveButtonLData.width = 92;
			saveButtonLData.height = 28;
			saveButtonLData.right =  new FormAttachment(1000, 1000, -109);
			saveButtonLData.top =  new FormAttachment(0, 1000, 287);
			saveButton.setLayoutData(saveButtonLData);
			saveButton.setImage(SWTResourceManager.getImage("images/editdelete.png"));
			//saveButton.setBackground(SWTResourceManager.getColor(255,255,255));
			saveButton.setText("Delete");
			saveButton.setEnabled(false);
		}
		{
			button1 = new Button(composite1, SWT.PUSH | SWT.LEFT);
			button1.setText("Save");
			FormData button1LData = new FormData();
			button1LData.width = 91;
			button1LData.height = 28;
			button1LData.right =  new FormAttachment(1000, 1000, -13);
			button1LData.top =  new FormAttachment(0, 1000, 287);
			button1.setLayoutData(button1LData);
			button1.setImage(SWTResourceManager.getImage("images/filesave.png"));
			//button1.setBackground(SWTResourceManager.getColor(255,255,255));
			button1.addListener(SWT.Selection, new Listener() {
				
				@Override
				public void handleEvent(Event arg0) {
					saveConfig();						
				}
			});
		}
		*/
		
		toolkit.paintBordersFor(sectionClient);
		toolkit.paintBordersFor(sectionClient2);
		toolkit.paintBordersFor(sectionClient3);
	}	
	
	protected Section configureSection(FormToolkit toolkit, final Form form, String title, String description, int colspan){
		Section section = toolkit.createSection(form.getBody(), 
				Section.DESCRIPTION|Section.TITLE_BAR|
				Section.TWISTIE|Section.EXPANDED);
		section.descriptionVerticalSpacing = 5;
		section.clientVerticalSpacing = 10;
		
		TableWrapData td = new TableWrapData(TableWrapData.FILL_GRAB);
		td.colspan = colspan;
		section.setLayoutData(td);
		section.addExpansionListener(new ExpansionAdapter() {
			public void expansionStateChanged(ExpansionEvent e) {
				//form.reflow(true);
			}
		});
		section.setText(title);
		section.setDescription(description);
		return section;
	}

}
