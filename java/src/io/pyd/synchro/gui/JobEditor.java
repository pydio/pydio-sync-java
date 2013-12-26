/*
 * Copyright 2012 Charles du Jeu <charles (at) ajaxplorer.info>
 * This file is part of Pydio.
 *
 * Pydio is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Pydio is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pydio.  If not, see <http://www.gnu.org/licenses/>.
 *
 * The latest code can be found at <http://pyd.io/>..
 *
 */
package io.pyd.synchro.gui;
import info.ajaxplorer.client.http.AjxpAPI;
import info.ajaxplorer.client.http.RestRequest;
import info.ajaxplorer.client.http.RestStateHolder;
import info.ajaxplorer.client.model.Node;
import info.ajaxplorer.client.model.Server;
import io.pyd.synchro.CoreManager;

import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.nebula.animation.AnimationRunner;
import org.eclipse.nebula.animation.effects.AlphaEffect;
import org.eclipse.nebula.animation.effects.SetBoundsEffect;
import org.eclipse.nebula.animation.movement.ExpoOut;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;
import org.quartz.SchedulerException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;


public class JobEditor extends Composite{

	public static final String AUTO_KEEP_REMOTE_DATA = "AUTOKEEPREMOTE";
	public static final String AUTO_KEEP_REMOTE = "auto_keep_remote";
	public static final String AUTO_KEEP_LOCAL_DATA = "AUTOKEEPLOCAL";
	public static final String AUTO_KEEP_LOCAL = "auto_keep_local";

	{
		//Register as a resource user - SWTResourceManager will
		//handle the obtaining and disposing of resources
		SWTResourceManager.registerResourceUser(this);
	}
	
	private String currentRepoId;
	private String currentRepoLabel;
	
	private Text tfHost;
	private ImageHyperlink buttonFileChooser;
	private Text tfRepo;
	private Text tfTarget;
	private Text tfPassword;
	private ImageHyperlink linkLoadRepositories;
	private Button radioSyncInterval2;
	private Combo comboRepository;
	private Button radioSyncInterval3;
	private Button radioSyncInterval1;
	private Combo comboDirection;

	private Button keepAutoRemote;
	private Button keepAutoLocal;
	private Text tfLogin;
	
	private Action saveAction;
	private Action closeAction;
	
	private FormToolkit toolkit;
	private Section connexionSection;
	private Composite sectionClient;
	private Section parametersSection;
	private Section logsSection;
	private LogViewer logs;
	
	// FIXME later - refactor to ensure MVC pattern.
	// all data should be stored in model object for manipulate

	private boolean currentActiveState = true;
	private boolean autoKeepRemoteState = false;
	private boolean autoKeepLocalState = false;
	
	private HashMap<String, String> repoItems;
	//private ConfigPanel configPanel;
	
	private Form form;
	
	private Map<String, HashMap<String, Object>> stackData;
	private String anchorH;
	private String anchorW;
	int heightHint = 0;
	
	SysTray sTray;
	Node currentSynchroNode;

	/**
	* Overriding checkSubclass allows this class to extend org.eclipse.swt.widgets.Composite
	*/	
	protected void checkSubclass() {
	}
	
	public JobEditor(Shell shell, SysTray systemTray, String stack) {
		
		super(shell, SWT.WRAP);
		this.initShell(shell);
		this.sTray = systemTray;
		this.anchorH = "bottom";
		this.anchorW = "right";
		//populateToolkit(parent);	
		stackData = new LinkedHashMap<String, HashMap<String, Object>>();
		HashMap<String, Object> connData = new HashMap<String, Object>();
		connData.put("LABEL", CoreManager.getMessage("jobeditor_stack_server"));
		connData.put("WIDTH", 280);
		connData.put("HEIGHT", 240);
		connData.put("FONT_HEIGHT", 18);
		connData.put("FONT_WIDTH", 22);
		connData.put("ICON", "network_local");
		stackData.put("connexion", connData);
		
		HashMap<String, Object> connData2 = new HashMap<String, Object>();
		connData2.put("LABEL", CoreManager.getMessage("jobeditor_stack_params"));
		connData2.put("WIDTH", 280);
		connData2.put("HEIGHT", 210);
		connData2.put("FONT_HEIGHT", 17);
		connData2.put("FONT_WIDTH", 22);
		connData2.put("ICON", "history");
		stackData.put("parameters", connData2);
		
		HashMap<String, Object> connData3 = new HashMap<String, Object>();
		connData3.put("LABEL", CoreManager.getMessage("jobeditor_stack_logs"));
		connData3.put("WIDTH", 520);
		connData3.put("HEIGHT", 420);
		connData3.put("FONT_HEIGHT", 33);
		connData3.put("FONT_WIDTH", 40);		
		connData3.put("ICON", "view_list_text");
		stackData.put("logs", connData3);
		
		this.setLayout(new FillLayout(SWT.HORIZONTAL|SWT.VERTICAL));
		populateToolkit();
		moveShellWithMouse(getMouseHandler(), getShell());

		GC gc = new GC(shell);
		this.heightHint = gc.getFontMetrics().getHeight();
		gc.dispose();
		
		shell.pack();
		toggleSection(stack, false);
		
	}	
	
	public void initShell(final Shell shell){
				
        shell.setText(CoreManager.getMessage("shell_title"));
        shell.setImage(new Image(shell.getDisplay(), this.getClass().getClassLoader().getResourceAsStream("images/AjxpLogo16-Bi.png")));        
        this.moveShellWithMouse(this, shell);
		shell.setLayout(new FillLayout(SWT.HORIZONTAL|SWT.VERTICAL));		        
        
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

			values.put(AUTO_KEEP_REMOTE_DATA,
					baseNode.getPropertyValue(AUTO_KEEP_REMOTE));
			values.put(AUTO_KEEP_LOCAL_DATA,
					baseNode.getPropertyValue(AUTO_KEEP_LOCAL));

			this.loadFormData(values);		
			if(this.form != null){
				//((FormHeading)this.form.getHead()).setText(CoreManager.getInstance().makeJobLabel(baseNode, true));				
				updateFormActions();
			}
			if(this.logs != null) this.logs.loadSynchroLog(baseNode);

		} catch (URISyntaxException e) {
			Logger.getRootLogger().error("Synchro", e);
		}
	}
	
	public Map<String, String> getFormData() throws Exception{
		HashMap<String, String> values = new HashMap<String, String>();
		if(repoItems == null) throw new Exception("Please select a repository first!");
		
		values.put("HOST", tfHost.getText());		
		values.put("LOGIN", tfLogin.getText());		
		values.put("PASSWORD", tfPassword.getText());		
		values.put("REPOSITORY_LABEL", currentRepoLabel);
		values.put("REPOSITORY_ID", currentRepoId);
		values.put("TARGET", tfTarget.getText());
		values.put("ACTIVE", (currentActiveState?"true":"false"));

		values.put(AUTO_KEEP_REMOTE_DATA, (autoKeepRemoteState ? "true"
				: "false"));
		values.put(AUTO_KEEP_LOCAL_DATA,
				(autoKeepLocalState ? "true" : "false"));

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
		currentRepoLabel = values.get("REPOSITORY_LABEL");
		currentRepoId = values.get("REPOSITORY_ID");
		tfRepo.setText(currentRepoLabel);
		repoItems = new HashMap<String, String>();
		repoItems.put(values.get("REPOSITORY_LABEL"), values.get("REPOSITORY_ID"));
		tfTarget.setText(values.get("TARGET"));
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
		
		autoKeepRemoteState = "true".equals(values.get(AUTO_KEEP_REMOTE_DATA));
		autoKeepLocalState = "true".equals(values.get(AUTO_KEEP_LOCAL_DATA));

		updateAutoKeeping(false);

		saveAction.setEnabled(false);
		
	}

	public void clearFormData(){
		tfHost.setText("");
		tfLogin.setText("");
		tfPassword.setText("");
		if(comboRepository!= null) {
			comboRepository.setItems(new String[0]);
		}
		if(tfRepo != null) {
			tfRepo.setText("");			
		}
		repoItems = new HashMap<String, String>();
		tfTarget.setText("");
		comboDirection.select(0);
		

		updateAutoKeeping(true);

		radioSyncInterval1.setSelection(false);
		radioSyncInterval2.setSelection(true);
		radioSyncInterval3.setSelection(false);
		
		if(this.form != null){
			updateFormActions();
			saveAction.setEnabled(false);
		}

		if(this.logs != null) this.logs.clearSynchroLog();

	}	
	


	public Control getMouseHandler(){
		return this.form.getHead();
	}
	
	/**
	 * Checks whetever server configuration is ok
	 * 
	 * @return
	 */
	private boolean checkConfiguration() {
		final String host = tfHost.getText();
		final String login = tfLogin.getText();
		final String pass = tfPassword.getText();

		boolean serverOk = false;

		RestRequest rest = new RestRequest();
		try {
			Server s = new Server("Test", host, login, pass, true, false);
			RestStateHolder.getInstance().setServer(s);
			AjxpAPI.getInstance().setServer(s);
			rest.throwAuthExceptions = true;
			rest.getHttpClient().clearCookies();
			rest.getDocumentContent(AjxpAPI.getInstance().getGetXmlRegistryUri());
			serverOk = true;
		} catch (URISyntaxException e) {
		} catch (Exception e) {
		} finally{
			rest.release();
		}

		// check if repo is selected and local folder as well
		if (serverOk) {
			if (currentRepoId != null && !currentRepoId.trim().isEmpty() && tfTarget != null && !tfTarget.isDisposed()
					&& tfTarget.getText() != null && !tfTarget.getText().trim().isEmpty()) {
				serverOk = true;
			} else {
				serverOk = false;
			}
		}

		return serverOk;
	}

	private void loadRepositories(){
		
		Logger.getRootLogger().debug("Updating combo repo");		
		
		final String host = tfHost.getText();
		final String login = tfLogin.getText();
		final String pass = tfPassword.getText();
		
		this.getDisplay().asyncExec(new Runnable() {
			
			@Override
			public void run() {
				
				Server s;
				try {
					s = new Server("Test", host, login, pass, true, false);
					RestStateHolder.getInstance().setServer(s);
					AjxpAPI.getInstance().setServer(s);					
					RestRequest rest = new RestRequest();
					rest.getHttpClient().clearCookies();
					Document doc = rest.getDocumentContent(AjxpAPI.getInstance().getGetXmlRegistryUri());
					rest.release();
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
							for(int p =0;p<CoreManager.EXCLUDED_ACCESS_TYPES.length;p++){
								if(accessType.equalsIgnoreCase(CoreManager.EXCLUDED_ACCESS_TYPES[p])){
									excluded = true; break;
								}
							}
							if(excluded) {
								continue;
							}
							repoItems.put(label, repositoryId);
						}
					}
					comboRepository.setItems(repoItems.keySet().toArray(new String[0]));
					comboRepository.setListVisible(true);
					comboRepository.addModifyListener(new ModifyListener() {							
						@Override
						public void modifyText(ModifyEvent arg0) {
							currentRepoLabel = comboRepository.getText();
							currentRepoId = repoItems.get(currentRepoLabel);
							getDisplay().asyncExec(new Runnable() {
								@Override
								public void run() {
									toggleRepositoryComponent();
									tfRepo.setText(currentRepoLabel);							
								}
							});
						}
					});
					
				} catch (URISyntaxException e) {
					Logger.getRootLogger().error("Synchro", e);
					saveAction.setEnabled(false);
					MessageDialog.openError(getShell(), CoreManager.getMessage("jobeditor_diag_baddata_title"),
							CoreManager.getMessage("jobeditor_diag_baddata"));
					return;
				} catch (Exception e) {
					Logger.getRootLogger().error("Synchro", e);
					saveAction.setEnabled(false);
					MessageDialog.openError(getShell(), CoreManager.getMessage("jobeditor_diag_baddata_title"),
							CoreManager.getMessage("jobeditor_diag_baddata"));
					return;
				}			
			}
		});
	}	
	
	public void populateToolkit() {
		
		toolkit = new FormToolkit(this.getDisplay());
		final Form form = toolkit.createForm(this);
		this.form = form;
		toolkit.decorateFormHeading(form);
		StackLayout sLayout = new StackLayout();
		form.getBody().setLayout(sLayout);				


		connexionSection = configureSection(toolkit, form, CoreManager.getMessage("jobeditor_header_connection"),CoreManager.getMessage("jobeditor_legend_connection"), 1, false);
		sectionClient = toolkit.createComposite(connexionSection);		
		GridLayout layout = new GridLayout();
		sectionClient.setLayout(layout);
		layout.numColumns = 3;
		layout.horizontalSpacing = 10;
		layout.verticalSpacing = 10;
		sLayout.topControl = connexionSection;
		connexionSection.setClient(sectionClient);
		stackData.get("connexion").put("SECTION", connexionSection);
		
		
		// HOST
		Label label = toolkit.createLabel(sectionClient, CoreManager.getMessage("jobeditor_hostURL"));
		label.setLayoutData(getGridDataLabel());
		tfHost = toolkit.createText(sectionClient, "");
		tfHost.setLayoutData(getGridDataField(2));

		
		// LOGIN
		label = toolkit.createLabel(sectionClient, CoreManager.getMessage("jobeditor_login"));
		label.setLayoutData(getGridDataLabel());
		tfLogin = toolkit.createText(sectionClient, "");
		tfLogin.setLayoutData(getGridDataField(2));
		
		// PASSWORD
		label = toolkit.createLabel(sectionClient, CoreManager.getMessage("jobeditor_password"));
		label.setLayoutData(getGridDataLabel());
		tfPassword = toolkit.createText(sectionClient, "", SWT.PASSWORD);
		tfPassword.setLayoutData(getGridDataField(2));
		
		// REPOSITORY CHOOSER
		label = toolkit.createLabel(sectionClient, CoreManager.getMessage("jobeditor_repository"));
		label.setLayoutData(getGridDataLabel());
		
		
		tfRepo = toolkit.createText(sectionClient, "click to load the repositories...", SWT.READ_ONLY);
		tfRepo.setLayoutData(getGridDataField(1));
		
		linkLoadRepositories = toolkit.createImageHyperlink(sectionClient, SWT.NULL);
		linkLoadRepositories.setImage(new Image(getDisplay(), new ImageData(this.getClass().getClassLoader().getResourceAsStream("images/reload.png"))));
		linkLoadRepositories.setLayoutData(new GridData(GridData.FILL));
		linkLoadRepositories.setUnderlined(false);
		linkLoadRepositories.addHyperlinkListener(new IHyperlinkListener() {			
			@Override
			public void linkExited(org.eclipse.ui.forms.events.HyperlinkEvent arg0) {}
			
			@Override
			public void linkEntered(org.eclipse.ui.forms.events.HyperlinkEvent arg0) {}
			
			@Override
			public void linkActivated(org.eclipse.ui.forms.events.HyperlinkEvent arg0) {
				if (tfRepo != null
						&& (tfHost.getText().trim().equals("") || tfLogin.getText().trim().equals("") || tfPassword.getText().trim()
								.equals(""))) {
					MessageBox dialog = new MessageBox(getShell(), SWT.ICON_WARNING | SWT.OK );
					dialog.setText(CoreManager.getMessage("jobeditor_diag_noserverdata"));
					dialog.setMessage(CoreManager.getMessage("jobeditor_diag_noserverdata_msg"));
					dialog.open();					
					return;
				}					
				toggleRepositoryComponent();
				if(comboRepository != null){
					loadRepositories();
				}
			}
		});

		// TARGET FOLDER CHOOSER
		label = toolkit.createLabel(sectionClient, CoreManager.getMessage("jobeditor_localfolder"));
		label.setLayoutData(getGridDataLabel());
		tfTarget = toolkit.createText(sectionClient, "", SWT.READ_ONLY);		
		tfTarget.setLayoutData(getGridDataField(1));
		
		buttonFileChooser = toolkit.createImageHyperlink(sectionClient, SWT.NULL);
		buttonFileChooser.setImage(new Image(getDisplay(), new ImageData(this.getClass().getClassLoader().getResourceAsStream("images/folder_home.png"))));		
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
					tfTarget.setText(res);
				}
			}
		});
		
		parametersSection = configureSection(toolkit, form, CoreManager.getMessage("jobeditor_header_execution"), CoreManager.getMessage("jobeditor_legend_execution"), 1, true);		
		Composite sectionClient3 = toolkit.createComposite(parametersSection);
		TableWrapLayout layout2 = new TableWrapLayout();
		sectionClient3.setLayout(layout2);
		layout2.numColumns = 1;
		parametersSection.setClient(sectionClient3);			
		stackData.get("parameters").put("SECTION", parametersSection);		
		
		Label lab = toolkit.createLabel(sectionClient3, CoreManager.getMessage("jobeditor_direction") + " : ");
		lab.setLayoutData(new TableWrapData(TableWrapData.LEFT, TableWrapData.MIDDLE));
		Composite rComp = toolkit.createComposite(sectionClient3);
		rComp.setLayoutData(new TableWrapData(TableWrapData.LEFT, TableWrapData.MIDDLE));
		layout2 = new TableWrapLayout();
		layout2.numColumns = 1;
		rComp.setLayout(layout2);
		comboDirection = new Combo(sectionClient3, SWT.READ_ONLY|SWT.BORDER);
	    comboDirection.setItems(new String[] { 
	    		CoreManager.getMessage("jobeditor_bi"), 
	    		CoreManager.getMessage("jobeditor_up"), 
	    		CoreManager.getMessage("jobeditor_down") 
	    		});
	    //toolkit.adapt(comboDirection);
	    TableWrapData test = new TableWrapData(TableWrapData.FILL_GRAB);
	    test.maxWidth = 180;
	    comboDirection.setLayoutData(test);
	    comboDirection.select(0);
		comboDirection.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent event) {
				updateAutoKeeping(true);
			}
		});

		// checkbox for setting auto keeping remote
		keepAutoRemote = toolkit.createButton(sectionClient3,
				CoreManager.getMessage("jobeditor_autokeepremote"), SWT.CHECK);
		keepAutoRemote.setLayoutData(new TableWrapData(TableWrapData.LEFT,
				TableWrapData.MIDDLE));
		// checkbox for setting auto keeping local
		keepAutoLocal = toolkit.createButton(sectionClient3,
				CoreManager.getMessage("jobeditor_autokeeplocal"), SWT.CHECK);
		keepAutoLocal.setLayoutData(new TableWrapData(TableWrapData.LEFT,
				TableWrapData.MIDDLE));

		Label lab2 = toolkit.createLabel(sectionClient3, CoreManager.getMessage("jobeditor_frequency") + " : ");
		lab2.setLayoutData(new TableWrapData(TableWrapData.LEFT, TableWrapData.MIDDLE));
		Composite rComp2= toolkit.createComposite(sectionClient3);
		rComp2.setLayoutData(new TableWrapData(TableWrapData.LEFT, TableWrapData.MIDDLE));
		layout2 = new TableWrapLayout();
		layout2.numColumns = 3;
		rComp2.setLayout(layout2);
		radioSyncInterval1 = toolkit.createButton(rComp2, CoreManager.getMessage("jobeditor_min"), SWT.RADIO);
		radioSyncInterval2 = toolkit.createButton(rComp2, CoreManager.getMessage("jobeditor_hours"), SWT.RADIO);
		radioSyncInterval3 = toolkit.createButton(rComp2, CoreManager.getMessage("jobeditor_days"), SWT.RADIO);
		radioSyncInterval2.setSelection(true);
		
		saveAction = new Action("Save", new ImageDescriptor() {
			@Override
			public ImageData getImageData() {
				return new ImageData(this.getClass().getClassLoader().getResourceAsStream("images/filesave.png"));
			}
		}) {
			@Override
			public void run() {
				super.run();
				if(saveConfig()){
					saveAction.setEnabled(false);
				}
			}
		};
		ModifyListener tfMod = new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent arg0) {
				saveAction.setEnabled(true);				
			}
		};
		SelectionListener rSel = new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				saveAction.setEnabled(true);
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
		};
		tfHost.addModifyListener(tfMod);
		tfLogin.addModifyListener(tfMod);
		tfPassword.addModifyListener(tfMod);
		tfTarget.addModifyListener(tfMod);
		comboDirection.addModifyListener(tfMod);
		keepAutoRemote.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				super.widgetSelected(e);
				autoKeepRemoteState = keepAutoRemote.getSelection();
				autoKeepLocalState = false;
				updateAutoKeeping(false);
				saveAction.setEnabled(true);
			}
		});
		keepAutoLocal.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				super.widgetSelected(e);
				autoKeepRemoteState = false;
				autoKeepLocalState = keepAutoLocal.getSelection();
				updateAutoKeeping(false);
				saveAction.setEnabled(true);
			}
		});
		radioSyncInterval1.addSelectionListener(rSel);
		
		closeAction = new Action("Close", new ImageDescriptor() {
			@Override
			public ImageData getImageData() {
				return new ImageData(this.getClass().getClassLoader().getResourceAsStream("images/fileclose.png"));
			}
		}) {
			@Override
			public void run() {
				super.run();
				if(saveAction.isEnabled()){
					MessageBox dialog = new MessageBox(getShell(), SWT.ICON_QUESTION | SWT.OK| SWT.CANCEL);
					dialog.setText(CoreManager.getMessage("jobeditor_diag_savenotchanges"));
					dialog.setMessage(CoreManager.getMessage("jobeditor_diag_savenotchanges_msg"));
					int returnCode = dialog.open();					
					if(returnCode == SWT.CANCEL) return;
				}
				
				new AnimationRunner().runEffect(
					new AlphaEffect(
							getShell(), 
							255 /*initial value*/, 
							0 /*final value*/, 
							1500 /*duration*/, 
							new ExpoOut() /*movement*/, 
							new Runnable() {
								@Override
								public void run() {								
									closeConfig();
								}
							},
							null /*run on cancel*/
							)		
				);
			}
		};
		
		updateFormActions();
		
		
		logsSection = configureSection(toolkit, form, CoreManager.getMessage("jobeditor_header_execution"), null, 1, false);		
		logs = new LogViewer(logsSection, SWT.NONE);
		logs.initGUI();
		logsSection.setClient(logs);				
		stackData.get("logs").put("SECTION", logsSection);		
		
		
		toolkit.paintBordersFor(sectionClient);
		toolkit.paintBordersFor(logs);
		toolkit.paintBordersFor(sectionClient3);
	}	
	
	protected void toggleSection(String name, boolean animateSize){
		
		if(stackData.get(name) == null) return;
		
		StackLayout sL = ((StackLayout)form.getBody().getLayout());
		
		int[] size = new int[2];		
		sL.topControl = (Control)stackData.get(name).get("SECTION");
		size[0] = (Integer)stackData.get(name).get("WIDTH");
		size[1] = (Integer)stackData.get(name).get("HEIGHT");
		if(stackData.get(name).containsKey("FONT_HEIGHT")){
			size[1] = (Integer)stackData.get(name).get("FONT_HEIGHT") * this.heightHint;			
		}
		if(stackData.get(name).containsKey("FONT_WIDTH")){
			size[0] = (Integer)stackData.get(name).get("FONT_WIDTH") * this.heightHint;			
		}
		String os = System.getProperty("os.name").toLowerCase();
		if(os.indexOf("windows xp") == -1){
			size[0] += 50;
			size[1] += 30;
		}		
			
        
		Rectangle screen = getShell().getDisplay().getPrimaryMonitor().getClientArea();
		int top;
		int left;
		int margin = 10;
		if(anchorH.equals("bottom")){
			top = screen.height - size[1] - margin;
		}else{
			top = margin;
		}
		if(anchorW.equals("right")){
			left = screen.width - size[0] - margin;
		}else{
			left = margin;
		}
		
		Rectangle orig = getShell().getBounds();
		Rectangle r = new Rectangle(left, top, size[0], size[1]);
		if(animateSize){
			new AnimationRunner().runEffect(
					new SetBoundsEffect(getShell(), orig, r, 1000, new ExpoOut(), null, null)
			);
		}else{
			getShell().setAlpha(0);
			getShell().setBounds(r);
			new AnimationRunner().runEffect(
					new AlphaEffect(
							getShell(), 
							0 /*initial value*/, 
							255 /*final value*/, 
							2000 /*duration*/, 
							new ExpoOut() /*movement*/, 
							null,
							null /*run on cancel*/
							));			
		}
		
		updateFormActions();
		this.form.setText((String)stackData.get(name).get("LABEL"));
		this.layout();		
	}
	
	
	protected void toggleRepositoryComponent(){
		if(comboRepository == null){
			comboRepository = new Combo(sectionClient, SWT.BORDER|SWT.DROP_DOWN);
			comboRepository.setLayoutData(getGridDataField(1));
			comboRepository.setItems(new String[]{"Loading repositories..."});
			comboRepository.select(0);
			toolkit.adapt(comboRepository, true, true);
			comboRepository.moveAbove(tfRepo);
			tfRepo.dispose();
			sectionClient.layout(new Control[] {comboRepository});
			tfRepo = null;			
		}else if(tfRepo == null){
			tfRepo = toolkit.createText(sectionClient, "", SWT.READ_ONLY);
			tfRepo.setLayoutData(getGridDataField(1));
			tfRepo.moveAbove(comboRepository);
			comboRepository.dispose();
			sectionClient.layout(new Control[] {tfRepo});
			comboRepository = null;
			tfRepo.addModifyListener(new ModifyListener() {
				
				@Override
				public void modifyText(ModifyEvent arg0) {
					saveAction.setEnabled(true);
				}
			});
		}
		sectionClient.redraw();
	}
		
	protected GridData getGridDataLabel(){
		GridData gd = new GridData(GridData.FILL);
		gd.horizontalAlignment = GridData.END;
		return gd;
	}
	
	protected GridData getGridDataField(int colspan){
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = colspan;
		return gd;
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
	protected void updateFormActions(){
		if(form == null) return;
		form.getToolBarManager().removeAll();
		form.getMenuManager().removeAll();

		Iterator<Entry<String, HashMap<String, Object>>> it = stackData.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry<String, HashMap<String, Object>> e = it.next();
			final String key = e.getKey();
			final String label = (String)e.getValue().get("LABEL");
			final String icon = (String)e.getValue().get("ICON");
			Section sec = (Section)e.getValue().get("SECTION");

			Action a = new Action(label, new ImageDescriptor() {
				@Override
				public ImageData getImageData() {
					return new ImageData(this.getClass().getClassLoader().getResourceAsStream("images/"+icon+".png"));
				}
			}) {
				@Override
				public void run() {
					super.run();
					toggleSection(key, true);
				}
			};
			if(((StackLayout)form.getBody().getLayout()).topControl == sec){
				a.setEnabled(false);
			}
			form.getMenuManager().add(a);
		}

		form.getToolBarManager().add(saveAction);
		form.getToolBarManager().add(closeAction);

		form.getToolBarManager().update(true);			

	}
	
	protected Section configureSection(FormToolkit toolkit, final Form form, String title, String description, int colspan, boolean expandable){
		int style = Section.EXPANDED|Section.DESCRIPTION|Section.NO_TITLE;
		if(expandable){
			//style = Section.TREE_NODE|Section.DESCRIPTION|Section.COMPACT;
		}
		Section section = toolkit.createSection(form.getBody(),style);
		section.descriptionVerticalSpacing = 6;
		section.clientVerticalSpacing = 8;
		section.marginWidth = 8;
		section.marginHeight = 6;
		
		TableWrapData td = new TableWrapData(TableWrapData.FILL_GRAB);
		td.colspan = colspan;
		section.setLayoutData(td);
		section.addExpansionListener(new ExpansionAdapter() {
			public void expansionStateChanged(ExpansionEvent e) {
				//form.reflow(true);
			}
		});
		//section.setText(title);
		//toolkit.createCompositeSeparator(section);
		if(description != null) section.setDescription(description);
		
		return section;
	}
	
	private void moveShellWithMouse(Control cont, final Shell shell){
		
		// add ability to move shell around
	    Listener moveListener = new Listener() {
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
	
	protected void closeConfig(){
		sTray.closeConfiguration();
	}
	
	protected boolean saveConfig(){

		boolean serverOk = checkConfiguration();
		if (!serverOk) {
			MessageDialog.openError(getShell(), CoreManager.getMessage("jobeditor_diag_baddata_title"),
					CoreManager.getMessage("jobeditor_diag_baddata"));
			return false;
		}
		try {
			Node n = CoreManager.getInstance().updateSynchroNode(getFormData(), currentSynchroNode);
			this.setCurrentNode(n);
			return true;
		} catch (SQLException e) {
			Logger.getRootLogger().error("Synchro", e);
		} catch (URISyntaxException e) {
			Logger.getRootLogger().error("Synchro", e);
		} catch (Exception e) {
			Logger.getRootLogger().error("Synchro", e);
		}		
		return false;
	}	
	
	protected void deleteConfig(){
		try {
			closeConfig();
			CoreManager.getInstance().deleteSynchroNode(currentSynchroNode);
		} catch (SchedulerException e) {
			Logger.getRootLogger().error("Synchro", e);
		} catch (SQLException e) {
			Logger.getRootLogger().error("Synchro", e);
		}
	}	
	protected void setCurrentNode(Node n){
		if(n == null){
			currentSynchroNode = null;
			clearFormData();
			if(CoreManager.defaultHome != null){
				tfTarget.setText(CoreManager.defaultHome);
			}
			logs.clearSynchroLog();
		}else{
			currentSynchroNode = n;
			loadFormFromNode(n);
			logs.loadSynchroLog(n);
			
		}
	}	
	
	public void notifyJobStateChanged(String nodeId, boolean running){
		logs.reload();
	}
	
	/**
	 * Updates autoKeeping checkbox states if reset - means, that user changed
	 * direction option, and we need to set default states also
	 * 
	 * @param reset
	 */
	private void updateAutoKeeping(boolean reset) {
		if (reset) {
			int index = comboDirection.getSelectionIndex();
			// BIDIRECTIONAL - both enabled and by default unselected - user
			// decides
			autoKeepLocalState = false;
			autoKeepRemoteState = false;
			keepAutoLocal.setEnabled(true);
			keepAutoRemote.setEnabled(true);
			if (index == 1) {
				// UPLOAD - local enabled and by default selected
				autoKeepLocalState = true;
				keepAutoLocal.setEnabled(true);
				keepAutoRemote.setEnabled(false);
			} else if (index == 2) {
				// DOWNLOAD - remote enabled and by default selected
				autoKeepRemoteState = true;
				keepAutoLocal.setEnabled(false);
				keepAutoRemote.setEnabled(true);
			}

		}
		keepAutoLocal.setSelection(autoKeepLocalState);
		keepAutoRemote.setSelection(autoKeepRemoteState);
	}
}
