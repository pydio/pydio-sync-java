package info.ajaxplorer.synchro.gui;
import info.ajaxplorer.client.http.AjxpAPI;
import info.ajaxplorer.client.http.RestRequest;
import info.ajaxplorer.client.http.RestStateHolder;
import info.ajaxplorer.client.model.Node;
import info.ajaxplorer.client.model.Server;
import info.ajaxplorer.synchro.Manager;

import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.nebula.animation.AnimationRunner;
import org.eclipse.nebula.animation.effects.AlphaEffect;
import org.eclipse.nebula.animation.effects.SetBoundsEffect;
import org.eclipse.nebula.animation.movement.ExpoOut;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
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
	private Text tfLogin;
	
	private FormToolkit toolkit;
	private Section connexionSection;
	private Composite sectionClient;
	private Section parametersSection;
	private Section logsSection;
	private LogViewer logs;
	
	private boolean currentActiveState = true;
	
	private HashMap<String, String> repoItems;
	//private ConfigPanel configPanel;
	
	private Form form;
	
	private Map<String, HashMap<String, Object>> stackData;
	private String anchorH;
	private String anchorW;
	
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
		connData.put("LABEL", Manager.getMessage("jobeditor_stack_server"));
		connData.put("WIDTH", 280);
		connData.put("HEIGHT", 230);
		connData.put("ICON", "network_local");
		stackData.put("connexion", connData);
		
		HashMap<String, Object> connData2 = new HashMap<String, Object>();
		connData2.put("LABEL", Manager.getMessage("jobeditor_stack_params"));
		connData2.put("WIDTH", 280);
		connData2.put("HEIGHT", 180);
		connData2.put("ICON", "history");
		stackData.put("parameters", connData2);
		
		HashMap<String, Object> connData3 = new HashMap<String, Object>();
		connData3.put("LABEL", Manager.getMessage("jobeditor_stack_logs"));
		connData3.put("WIDTH", 500);
		connData3.put("HEIGHT", 400);
		connData3.put("ICON", "view_list_text");
		stackData.put("logs", connData3);
		
		this.setLayout(new FillLayout(SWT.HORIZONTAL|SWT.VERTICAL));
		populateToolkit();
		moveShellWithMouse(getMouseHandler(), getShell());
		
		shell.pack();
		
		
		toggleSection(stack, false);

		
	}	
	
	public void initShell(final Shell shell){
				
        shell.setText(Manager.getMessage("shell_title"));
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
			this.loadFormData(values);		
			if(this.form != null){
				//((FormHeading)this.form.getHead()).setText(Manager.getInstance().makeJobLabel(baseNode, true));				
				updateFormActions();
			}
			if(this.logs != null) this.logs.loadSynchroLog(baseNode);

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
		values.put("REPOSITORY_LABEL", currentRepoLabel);
		values.put("REPOSITORY_ID", currentRepoId);
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
		
	}

	public void clearFormData(){
		tfHost.setText("");
		tfLogin.setText("");
		tfPassword.setText("");
		if(comboRepository!= null) {
			comboRepository.setItems(new String[0]);
		}
		repoItems = new HashMap<String, String>();
		tfTarget.setText("");
		
		comboDirection.select(0);
		
		radioSyncInterval1.setSelection(false);
		radioSyncInterval2.setSelection(true);
		radioSyncInterval3.setSelection(false);
		
		if(this.form != null){
			updateFormActions();
		}
		if(this.logs != null) this.logs.clearSynchroLog();

	}	
	
	public Control getMouseHandler(){
		return this.form.getHead();
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
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
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


		connexionSection = configureSection(toolkit, form, Manager.getMessage("jobeditor_header_connection"),Manager.getMessage("jobeditor_legend_connection"), 1, false);
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
		Label label = toolkit.createLabel(sectionClient, Manager.getMessage("jobeditor_hostURL"));
		label.setLayoutData(getGridDataLabel());
		tfHost = toolkit.createText(sectionClient, "");
		tfHost.setLayoutData(getGridDataField(2));

		
		// LOGIN
		label = toolkit.createLabel(sectionClient, Manager.getMessage("jobeditor_login"));
		label.setLayoutData(getGridDataLabel());
		tfLogin = toolkit.createText(sectionClient, "");
		tfLogin.setLayoutData(getGridDataField(2));
		
		// PASSWORD
		label = toolkit.createLabel(sectionClient, Manager.getMessage("jobeditor_password"));
		label.setLayoutData(getGridDataLabel());
		tfPassword = toolkit.createText(sectionClient, "", SWT.PASSWORD);
		tfPassword.setLayoutData(getGridDataField(2));
		
		// REPOSITORY CHOOSER
		label = toolkit.createLabel(sectionClient, Manager.getMessage("jobeditor_repository"));
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
				if( tfRepo != null && ( tfHost.getText().equals("") || tfLogin.getText().equals("") || tfPassword.getText().equals("") )){
					MessageBox dialog = new MessageBox(getShell(), SWT.ICON_WARNING | SWT.OK );
					dialog.setText(Manager.getMessage("jobeditor_diag_noserverdata"));
					dialog.setMessage(Manager.getMessage("jobeditor_diag_noserverdata_msg"));
					dialog.open();					
					return;
				}					
				toggleRepositoryComponent();
				loadRepositories();
			}
		});

		// TARGET FOLDER CHOOSER
		label = toolkit.createLabel(sectionClient, Manager.getMessage("jobeditor_localfolder"));
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
		
		parametersSection = configureSection(toolkit, form, Manager.getMessage("jobeditor_header_execution"), Manager.getMessage("jobeditor_legend_execution"), 1, true);		
		Composite sectionClient3 = toolkit.createComposite(parametersSection);
		TableWrapLayout layout2 = new TableWrapLayout();
		sectionClient3.setLayout(layout2);
		layout2.numColumns = 1;
		parametersSection.setClient(sectionClient3);			
		stackData.get("parameters").put("SECTION", parametersSection);		
		
		Label lab = toolkit.createLabel(sectionClient3, Manager.getMessage("jobeditor_direction") + " : ");
		lab.setLayoutData(new TableWrapData(TableWrapData.LEFT, TableWrapData.MIDDLE));
		Composite rComp = toolkit.createComposite(sectionClient3);
		rComp.setLayoutData(new TableWrapData(TableWrapData.LEFT, TableWrapData.MIDDLE));
		layout2 = new TableWrapLayout();
		layout2.numColumns = 1;
		rComp.setLayout(layout2);
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
	    comboDirection.select(0);
				
		Label lab2 = toolkit.createLabel(sectionClient3, Manager.getMessage("jobeditor_frequency") + " : ");
		lab2.setLayoutData(new TableWrapData(TableWrapData.LEFT, TableWrapData.MIDDLE));
		Composite rComp2= toolkit.createComposite(sectionClient3);
		rComp2.setLayoutData(new TableWrapData(TableWrapData.LEFT, TableWrapData.MIDDLE));
		layout2 = new TableWrapLayout();
		layout2.numColumns = 3;
		rComp2.setLayout(layout2);
		radioSyncInterval1 = toolkit.createButton(rComp2, Manager.getMessage("jobeditor_min"), SWT.RADIO);
		radioSyncInterval2 = toolkit.createButton(rComp2, Manager.getMessage("jobeditor_hours"), SWT.RADIO);
		radioSyncInterval3 = toolkit.createButton(rComp2, Manager.getMessage("jobeditor_days"), SWT.RADIO);
		radioSyncInterval2.setSelection(true);
		updateFormActions();
		
		
		logsSection = configureSection(toolkit, form, Manager.getMessage("jobeditor_header_execution"), null, 1, false);		
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

		form.getToolBarManager().add(new Action("Save", new ImageDescriptor() {
			@Override
			public ImageData getImageData() {
				return new ImageData(this.getClass().getClassLoader().getResourceAsStream("images/filesave.png"));
			}
		}) {
			@Override
			public void run() {
				super.run();
				saveConfig();
			}
		});
		form.getToolBarManager().add(new Action("Close", new ImageDescriptor() {
			@Override
			public ImageData getImageData() {
				return new ImageData(this.getClass().getClassLoader().getResourceAsStream("images/fileclose.png"));
			}
		}) {
			@Override
			public void run() {
				super.run();
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
		});

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
	
	protected void saveConfig(){
		try {
			Node n = Manager.getInstance().updateSynchroNode(getFormData(), currentSynchroNode);
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
			closeConfig();
			Manager.getInstance().deleteSynchroNode(currentSynchroNode);
		} catch (SchedulerException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}	
	protected void setCurrentNode(Node n){
		if(n == null){
			currentSynchroNode = null;
			clearFormData();
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
	
	
}
