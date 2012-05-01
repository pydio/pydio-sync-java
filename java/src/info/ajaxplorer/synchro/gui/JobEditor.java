package info.ajaxplorer.synchro.gui;
import info.ajaxplorer.client.http.AjxpAPI;
import info.ajaxplorer.client.http.RestRequest;
import info.ajaxplorer.client.http.RestStateHolder;
import info.ajaxplorer.client.model.Node;
import info.ajaxplorer.client.model.Server;
import info.ajaxplorer.synchro.Manager;

import java.awt.Color;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;
import org.eclipse.ui.internal.forms.widgets.FormHeading;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;


public class JobEditor extends Composite{

	{
		//Register as a resource user - SWTResourceManager will
		//handle the obtaining and disposing of resources
		SWTResourceManager.registerResourceUser(this);
	}
	
	private String currentRepoId;
	private String currentRepoLabel;
	private String currentLocalFolder;
	
	private Text tfHost;
	private Hyperlink buttonFileChooser;
	private Text tfTarget;
	private Text tfPassword;
	private Hyperlink linkLoadRepositories;
	private Button radioSyncInterval2;
	private Combo comboRepository;
	private Button radioSyncInterval3;
	private Button radioSyncInterval1;
	//private Button radioDirection3;
	//private Button radioDirection2;
	//private Button radioDirection;
	private Combo comboDirection;
	private Button checkboxActive;
	private Text tfLogin;
	
	private boolean currentActiveState;
	
	private HashMap<String, String> repoItems;
	private ConfigPanel configPanel;
	
	private Form form;
	

	/**
	* Overriding checkSubclass allows this class to extend org.eclipse.swt.widgets.Composite
	*/	
	protected void checkSubclass() {
	}
	
	public JobEditor(Composite parent, ConfigPanel configPanel) {
		super(parent, SWT.WRAP);
		this.configPanel = configPanel;
		//populateToolkit(parent);		
	}	
	
	protected void loadFormFromNode(Node baseNode){
		Server s;
		try {
			s = new Server(baseNode.getParent());
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
				//((FormHeading)this.form.getHead()).setText(Manager.getInstance().makeJobLabel(baseNode, true));				
				updateFormActions(true, true, true);
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
		//values.put("REPOSITORY_LABEL", comboRepository.getText());
		//values.put("REPOSITORY_ID", repoItems.get(comboRepository.getText()));
		values.put("TARGET", tfTarget.getText());
		values.put("ACTIVE", (currentActiveState?"true":"false"));
		String dir = "bi";
		if(comboDirection.getSelectionIndex() == 1) dir = "up";
		else if(comboDirection.getSelectionIndex() == 2) dir = "down";
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
		//comboRepository.setText(values.get("REPOSITORY_LABEL"));
		currentRepoLabel = values.get("REPOSITORY_LABEL");
		currentRepoId = values.get("REPOSITORY_ID");
		repoItems = new HashMap<String, String>();
		repoItems.put(values.get("REPOSITORY_LABEL"), values.get("REPOSITORY_ID"));
		//tfTarget.setText(values.get("TARGET"));
		currentLocalFolder = values.get("TARGET");
		//checkboxActive.setSelection(values.get("ACTIVE").equals("true"));
		currentActiveState = values.get("ACTIVE").equals("true");
		
		String dir = values.get("DIRECTION");
		int selectionIndex = 0;
		if(dir.equals("bi")) selectionIndex = 0;
		else if(dir.equals("up")) selectionIndex = 1;
		else if(dir.equals("down")) selectionIndex = 2;
		comboDirection.select(selectionIndex);
		
		radioSyncInterval1.setSelection(values.get("INTERVAL").equals("minute"));
		radioSyncInterval2.setSelection(values.get("INTERVAL").equals("hour"));
		radioSyncInterval3.setSelection(values.get("INTERVAL").equals("day"));
		
		refreshHyperlinkChoosers();
	}

	public void clearFormData(){
		tfHost.setText("");
		tfLogin.setText("");
		tfPassword.setText("");
		comboRepository.setText("");
		comboRepository.setItems(new String[0]);
		repoItems = new HashMap<String, String>();
		tfTarget.setText("");
		//checkboxActive.setSelection(true);
		
		comboDirection.select(0);
		
		radioSyncInterval1.setSelection(false);
		radioSyncInterval2.setSelection(true);
		radioSyncInterval3.setSelection(false);
		
		if(this.form != null){
			//this.form.setText(Manager.getMessage("cpanel_create_synchro"));
			updateFormActions(true, false, true);
		}

	}	
	
	private void loadRepositories(){
		
		Logger.getRootLogger().debug("Loading repositories");		
		if(tfHost.getText().equals("") || tfLogin.getText().equals("") || tfPassword.getText().equals("") ){
			return;
		}
		Logger.getRootLogger().debug("Updating combo repo");		

		linkLoadRepositories.setText(Manager.getMessage("jobeditor_loading"));
		String host = tfHost.getText();
		String login = tfLogin.getText();
		String pass = tfPassword.getText();
		
		Server s;
		try {
			s = new Server("Test", host, login, pass, true, false);
			RestStateHolder.getInstance().setServer(s);
			AjxpAPI.getInstance().setServer(s);
			RestRequest rest = new RestRequest();
			Document doc = rest.getDocumentContent(AjxpAPI.getInstance().getGetXmlRegistryUri());
			
			NodeList mainTag = doc.getElementsByTagName("repositories");
			if(mainTag.getLength() == 0){
				throw new Exception("No repositories found");
			}			
			final NodeList repos = mainTag.item(0).getChildNodes();
			repoItems = new HashMap<String, String>();
			
			if (repos!=null && repos.getLength() > 0){
				for (int i = 0; i < repos.getLength(); i++) {
					org.w3c.dom.Node xmlNode = repos.item(i);
					NamedNodeMap attributes = xmlNode.getAttributes();
					NodeList children = xmlNode.getChildNodes();
					String label= "";
					for(int k=0;k<children.getLength();k++){
						if(children.item(k).getNodeName().equals("label")){
							label = children.item(k).getTextContent(); 
						}
					}
					String accessType = attributes.getNamedItem("access_type").getNodeValue();
					String repositoryId = attributes.getNamedItem("id").getNodeValue();
					boolean excluded = false;
					for(int p =0;p<Manager.EXCLUDED_ACCESS_TYPES.length;p++){
						if(accessType.equalsIgnoreCase(Manager.EXCLUDED_ACCESS_TYPES[p])){
							excluded = true; break;
						}
					}
					if(excluded) {
						continue;
					}
					repoItems.put(label, repositoryId);
				}
			}
			comboRepository.setText("");
			comboRepository.setItems(repoItems.keySet().toArray(new String[0]));
			comboRepository.setListVisible(true);
			
			linkLoadRepositories.setText(Manager.getMessage("jobeditor_load"));
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
	}	
	
	public void populateToolkit(/*Composite parent*/) {
		
		FormToolkit toolkit = new FormToolkit(this.getDisplay());
		final Form form = toolkit.createForm(this);
		this.form = form;
		toolkit.decorateFormHeading(form);
		TableWrapLayout layout = new TableWrapLayout();
		layout.verticalSpacing = 20;
		layout.horizontalSpacing = 10;
		layout.makeColumnsEqualWidth = true;
		layout.numColumns = 1;
		form.getBody().setLayout(layout);		


		Section section = configureSection(toolkit, form, Manager.getMessage("jobeditor_header_connection"),Manager.getMessage("jobeditor_legend_connection"), 1, false);
		Composite sectionClient = toolkit.createComposite(section);		
		layout = new TableWrapLayout();
		sectionClient.setLayout(layout);
		layout.numColumns = 2;
		layout.rightMargin = 5;
		layout.leftMargin = 5;
		layout.horizontalSpacing = 5;
		layout.verticalSpacing = 5;
		
		section.setClient(sectionClient);	
		
		// HOST
		toolkit.createLabel(sectionClient, Manager.getMessage("jobeditor_hostURL"));
		tfHost = toolkit.createText(sectionClient, "");
		tfHost.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		
		// LOGIN
		toolkit.createLabel(sectionClient, Manager.getMessage("jobeditor_login"));
		tfLogin = toolkit.createText(sectionClient, "");
		tfLogin.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		
		// PASSWORD
		toolkit.createLabel(sectionClient, Manager.getMessage("jobeditor_password"));
		tfPassword = toolkit.createText(sectionClient, "", SWT.PASSWORD);
		tfPassword.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		
		/*
		Section section2 = configureSection(toolkit, form, Manager.getMessage("jobeditor_header_targets"), Manager.getMessage("jobeditor_legend_targets"), 1);
		Composite sectionClient2 = toolkit.createComposite(section2);
		layout = new TableWrapLayout();
		sectionClient2.setLayout(layout);
		layout.numColumns = 2;		
		section2.setClient(sectionClient2);	
		*/
		// REPOSITORY CHOOSER
		Label l = toolkit.createLabel(sectionClient, Manager.getMessage("jobeditor_repository") + " : ");
		l.setLayoutData(getTWDataFillMiddle(1));

			
		String t = (currentRepoLabel!=null?currentRepoLabel+ " (" + Manager.getMessage("jobeditor_load") + ")":"Choose remote repository");
		linkLoadRepositories = toolkit.createHyperlink(sectionClient, t, SWT.NULL);
		linkLoadRepositories.setLayoutData(getTWDataFillMiddle(1, false));
		linkLoadRepositories.setUnderlined(false);
		linkLoadRepositories.addHyperlinkListener(new IHyperlinkListener() {			
			@Override
			public void linkExited(org.eclipse.ui.forms.events.HyperlinkEvent arg0) {}
			
			@Override
			public void linkEntered(org.eclipse.ui.forms.events.HyperlinkEvent arg0) {}
			
			@Override
			public void linkActivated(org.eclipse.ui.forms.events.HyperlinkEvent arg0) {
				//loadRepositories();
				openRepositoryChooser();
			}
		});
		
		/*
		comboRepository = new Combo(sectionClient2, SWT.BORDER|SWT.READ_ONLY);
		comboRepository.setLayoutData(getTWDataFillMiddle(2));
		toolkit.adapt(comboRepository, true, true);
		*/

		// TARGET FOLDER CHOOSER
		l = toolkit.createLabel(sectionClient, Manager.getMessage("jobeditor_localfolder") + " : ");
		l.setLayoutData(getTWDataFillMiddle(1));
		//tfTarget = toolkit.createText(sectionClient2, "");
		//tfTarget.setLayoutData(getTWDataFillMiddle(1));
		
		buttonFileChooser = toolkit.createHyperlink(sectionClient, currentLocalFolder + " (" + Manager.getMessage("jobeditor_browse") + ")", SWT.PUSH);
		buttonFileChooser.setLayoutData(getTWDataFillMiddle(1, false));
		buttonFileChooser.setUnderlined(false);
		buttonFileChooser.addHyperlinkListener(new IHyperlinkListener() {			
			@Override
			public void linkExited(org.eclipse.ui.forms.events.HyperlinkEvent arg0) {}
			
			@Override
			public void linkEntered(org.eclipse.ui.forms.events.HyperlinkEvent arg0) {}
			
			@Override
			public void linkActivated(org.eclipse.ui.forms.events.HyperlinkEvent arg0) {
				DirectoryDialog dialog = new DirectoryDialog(JobEditor.this.getShell());
				String res = dialog.open();
				if(res != null){
					currentLocalFolder = res;
				}
			}
		});
		
		Section section3 = configureSection(toolkit, form, Manager.getMessage("jobeditor_header_execution"), Manager.getMessage("jobeditor_legend_execution"), 1, true);		
		Composite sectionClient3 = toolkit.createComposite(section3);
		layout = new TableWrapLayout();
		sectionClient3.setLayout(layout);
		layout.numColumns = 1;
		section3.setClient(sectionClient3);			
		
		/*
		checkboxActive = toolkit.createButton(sectionClient3, Manager.getMessage("jobeditor_jobactive"), SWT.CHECK | SWT.SELECTED);
		TableWrapData td = new TableWrapData(TableWrapData.FILL_GRAB);
		td.colspan = 1;
		checkboxActive.setLayoutData(td);
		*/
		
		Label lab = toolkit.createLabel(sectionClient3, Manager.getMessage("jobeditor_direction") + " : ");
		lab.setLayoutData(new TableWrapData(TableWrapData.LEFT, TableWrapData.MIDDLE));
		Composite rComp = toolkit.createComposite(sectionClient3);
		rComp.setLayoutData(new TableWrapData(TableWrapData.LEFT, TableWrapData.MIDDLE));
		layout = new TableWrapLayout();
		layout.numColumns = 1;
		rComp.setLayout(layout);
		/*
		radioDirection = toolkit.createButton(rComp, Manager.getMessage("jobeditor_bi"), SWT.RADIO);
		radioDirection2 = toolkit.createButton(rComp, Manager.getMessage("jobeditor_down"), SWT.RADIO);
		radioDirection3 = toolkit.createButton(rComp, Manager.getMessage("jobeditor_up"), SWT.RADIO);
			*/
		comboDirection = new Combo(sectionClient3, SWT.READ_ONLY|SWT.BORDER);
	    comboDirection.setItems(new String[] { 
	    		Manager.getMessage("jobeditor_bi"), 
	    		Manager.getMessage("jobeditor_up"), 
	    		Manager.getMessage("jobeditor_down") 
	    		});
	    //toolkit.adapt(comboDirection);
	    TableWrapData test = new TableWrapData(TableWrapData.FILL_GRAB);
	    test.maxWidth = 180;
	    comboDirection.setLayoutData(test);
				
		Label lab2 = toolkit.createLabel(sectionClient3, Manager.getMessage("jobeditor_frequency") + " : ");
		lab2.setLayoutData(new TableWrapData(TableWrapData.LEFT, TableWrapData.MIDDLE));
		Composite rComp2= toolkit.createComposite(sectionClient3);
		rComp2.setLayoutData(new TableWrapData(TableWrapData.LEFT, TableWrapData.MIDDLE));
		layout = new TableWrapLayout();
		layout.numColumns = 3;
		rComp2.setLayout(layout);
		radioSyncInterval1 = toolkit.createButton(rComp2, Manager.getMessage("jobeditor_min"), SWT.RADIO);
		radioSyncInterval2 = toolkit.createButton(rComp2, Manager.getMessage("jobeditor_hours"), SWT.RADIO);
		radioSyncInterval3 = toolkit.createButton(rComp2, Manager.getMessage("jobeditor_days"), SWT.RADIO);
		updateFormActions(true, true, true);
		
		toolkit.paintBordersFor(sectionClient);
		//toolkit.paintBordersFor(sectionClient2);
		toolkit.paintBordersFor(sectionClient3);
		
		this.form.setText("Connexion");		
		this.layout();
	}	
	
	protected void openRepositoryChooser(){
		Shell dialog = new Shell (getShell(), SWT.DIALOG_TRIM|SWT.APPLICATION_MODAL);
		//Label label = new Label (dialog, SWT.NONE);
		List list = new List(dialog, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
		list.add("Repo 1");
		list.add("Repo 2");
		list.add("Repo 3");
		Button okButton = new Button (dialog, SWT.PUSH);
		okButton.setText ("&OK");
		Button cancelButton = new Button (dialog, SWT.PUSH);
		cancelButton.setText ("&Cancel");
		/*
		FormData listAtt = new FormData();
		listAtt.left = new FormAttachment(0, 100);
		listAtt.right = new FormAttachment(0, 100);
		list.setLayoutData(listAtt);
		*/
		FormLayout form = new FormLayout ();
		form.marginWidth = form.marginHeight = 8;
		dialog.setLayout (form);
		FormData okData = new FormData ();
		okData.top = new FormAttachment (list, 8);
		okButton.setLayoutData (okData);
		FormData cancelData = new FormData ();
		cancelData.left = new FormAttachment (okButton, 8);
		cancelData.top = new FormAttachment (okButton, 0, SWT.TOP);
		cancelButton.setLayoutData (cancelData);
		
		dialog.setDefaultButton (okButton);
		dialog.pack ();
		dialog.open ();	}
	
	protected void refreshHyperlinkChoosers(){
		buttonFileChooser.setText(currentLocalFolder + " (" + Manager.getMessage("jobeditor_browse") + ")");
		String t = (currentRepoLabel!=null?currentRepoLabel+ " (" + Manager.getMessage("jobeditor_load") + ")":"Choose remote repository");		
		linkLoadRepositories.setText(t);
	}
	
	protected TableWrapData getTWDataFillMiddle(int colspan){
		return  getTWDataFillMiddle(colspan, false);
	}
	protected TableWrapData getTWDataFillMiddle(int colspan, boolean wrap){
		TableWrapData td = new TableWrapData((wrap?TableWrapData.LEFT:TableWrapData.FILL_GRAB));
		td.valign = TableWrapData.MIDDLE;
		td.colspan = colspan;
		td.maxWidth = 200;
		return td;
	}
	protected void updateFormActions(boolean save, boolean delete, boolean activeState){
		if(form == null) return;
		form.getToolBarManager().removeAll();
		if(save){
			form.getToolBarManager().add(new Action("Save", new ImageDescriptor() {
				@Override
				public ImageData getImageData() {
					return new ImageData(this.getClass().getClassLoader().getResourceAsStream("images/filesave.png"));
				}
			}) {
				@Override
				public void run() {
					super.run();
					configPanel.saveConfig();
				}
			});
		}
		if(delete){
			form.getToolBarManager().add(new Action("Delete", new ImageDescriptor() {
				@Override
				public ImageData getImageData() {
					return new ImageData(this.getClass().getClassLoader().getResourceAsStream("images/editdelete.png"));
				}
			}) {
				@Override
				public void run() {
					super.run();
					MessageBox dialog = new MessageBox(getShell(), SWT.ICON_QUESTION | SWT.OK| SWT.CANCEL);
					dialog.setText(Manager.getMessage("jobeditor_diag_delete"));
					dialog.setMessage(Manager.getMessage("jobeditor_diag_deletem"));
					int returnCode = dialog.open();					
					if(returnCode == SWT.OK) configPanel.deleteConfig();
				}
			});
		}
		if(activeState){
			
			form.getToolBarManager().add(new Action("Toggle active", new ImageDescriptor() {
				@Override
				public ImageData getImageData() {
					return new ImageData(this.getClass().getClassLoader().getResourceAsStream(currentActiveState?"images/media_playback_stop.png":"images/media_playback_start.png"));
				}
			}) {
				@Override
				public void run() {
					super.run();
					currentActiveState = !currentActiveState;
					updateFormActions(true, true, true);
				}
			});
		}
		form.getToolBarManager().update(true);			
		
	}
	
	protected Section configureSection(FormToolkit toolkit, final Form form, String title, String description, int colspan, boolean expandable){
		int style = Section.EXPANDED|Section.DESCRIPTION;
		if(expandable){
			style = Section.TREE_NODE|Section.DESCRIPTION|Section.COMPACT;
		}
		Section section = toolkit.createSection(form.getBody(),style);
		section.descriptionVerticalSpacing = 0;
		section.clientVerticalSpacing = 0;
		
		TableWrapData td = new TableWrapData(TableWrapData.FILL_GRAB);
		td.colspan = colspan;
		section.setLayoutData(td);
		section.addExpansionListener(new ExpansionAdapter() {
			public void expansionStateChanged(ExpansionEvent e) {
				//form.reflow(true);
			}
		});
		section.setText(title);
		toolkit.createCompositeSeparator(section);
		section.setDescription(description);
		
		return section;
	}
	
}
