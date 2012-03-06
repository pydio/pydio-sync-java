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
import java.util.Map;
import java.util.concurrent.Callable;

import com.cloudgarden.resource.SWTResourceManager;
import com.j256.ormlite.dao.Dao;

import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.CoolBar;
import org.eclipse.swt.widgets.CoolItem;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

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
public class JobEditor extends org.eclipse.swt.widgets.Composite {

	{
		//Register as a resource user - SWTResourceManager will
		//handle the obtaining and disposing of resources
		SWTResourceManager.registerResourceUser(this);
	}
	
	private Label hostLabel;
	private Text tfHost;
	private Label labelLogin;
	private CTabFolder cTabFolder;
	private CTabItem cTabItem2;
	private Composite composite1;
	private CTabItem cTabItem1;
	private Button buttonFileChooser;
	private Text tfTarget;
	private Label labelTarget;
	private Text tfPassword;
	private Label labelPass;
	private Button radioDirection2;
	private ToolItem toolItem1;
	private ToolBar toolBar1;
	private CoolItem coolItem1;
	private CoolBar coolBar1;
	private Button buttonLoadRepositories;
	private Button radioSyncInterval2;
	private Combo comboRepository;
	private Label labelRepository;
	private Button radioSyncInterval3;
	private Button radioSyncInterval1;
	private Label label1;
	private CLabel cLabel1;
	private Button radioDirection3;
	private Button radioDirection;
	private Button checkboxActive;
	private Composite composite2;
	private Text tfLogin;
	
	private HashMap<String, String> repoItems;
	private Node currentSynchroNode;
	

	/**
	* Auto-generated main method to display this 
	* org.eclipse.swt.widgets.Composite inside a new Shell.
	*/
		
	/**
	* Overriding checkSubclass allows this class to extend org.eclipse.swt.widgets.Composite
	*/	
	protected void checkSubclass() {
	}
	
	/**
	* Auto-generated method to display this 
	* org.eclipse.swt.widgets.Composite inside a new Shell.
	*/
	/*
	public static void showGUI() {
		Display display = Display.getDefault();
		Shell shell = new Shell(display);
		JobEditor inst = new JobEditor(shell, SWT.NULL);
		Point size = inst.getSize();
		shell.setLayout(new FillLayout());
		shell.layout();
		if(size.x == 0 && size.y == 0) {
			inst.pack();
			shell.pack();
		} else {
			Rectangle shellBounds = shell.computeTrim(0, 0, size.x, size.y);
			shell.setSize(shellBounds.width, shellBounds.height);
		}
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
	}*/

	public JobEditor(org.eclipse.swt.widgets.Composite parent, int style) {
		super(parent, style);
		initGUI();
		int baseNodeId = -1;
		try {
			Dao<Node, String> nodeDao = Manager.getInstance().getNodeDao();
			baseNodeId = nodeDao.queryForEq("resourceType", Node.NODE_TYPE_REPOSITORY).get(0).id;
			Node baseNode = Manager.getInstance().getSynchroNode(String.valueOf(baseNodeId));
			Server s = new Server(baseNode.getParent());
			HashMap<String, String> values = new HashMap<String, String>();
			values.put("HOST", s.getUrl());
			values.put("LOGIN", s.getUser());
			values.put("PASSWORD", s.getPassword());
			values.put("REPOSITORY_LABEL", baseNode.getLabel());
			values.put("REPOSITORY_ID", baseNode.getPropertyValue("repository_id"));
			values.put("TARGET", baseNode.getPropertyValue("target_folder"));
			this.loadFormData(values);
			currentSynchroNode = baseNode;
		} catch (Exception e) {
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
		
		return values;
	}
	
	public void loadFormData(Map<String, String> values){
		tfHost.setText(values.get("HOST"));
		tfLogin.setText(values.get("LOGIN"));
		tfPassword.setText(values.get("PASSWORD"));
		comboRepository.setText(values.get("REPOSITORY_LABEL"));
		comboRepository.setEnabled(true);
		repoItems = new HashMap<String, String>();
		repoItems.put(values.get("REPOSITORY_LABEL"), values.get("REPOSITORY_ID"));
		tfTarget.setText(values.get("TARGET"));
	}

	private void loadRepositories(){
		
		if(tfHost.getText().equals("") || tfLogin.getText().equals("") || tfPassword.getText().equals("") ){
			return;
		}
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
			Dao<Node, String> nodeDao = Manager.getInstance().getNodeDao();
			
			NodeList mainTag = doc.getElementsByTagName("repositories");
			if(mainTag.getLength() == 0){
				throw new Exception("No repositories found");
			}			
			final NodeList repos = mainTag.item(0).getChildNodes();
			repoItems = new HashMap<String, String>();
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
					repoItems.put(repository.getLabel(), repository.getPropertyValue("repository_id"));
					if(excluded) {
						continue;
					}
				}
			}
			
			comboRepository.setEnabled(true);
			comboRepository.setItems(repoItems.keySet().toArray(new String[0]));
			
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
	private void initGUI() {
		try {
			FormLayout thisLayout = new FormLayout();
			this.setLayout(thisLayout);
			this.setSize(394, 260);
			{
				FormData coolBar1LData = new FormData();
				coolBar1LData.left =  new FormAttachment(0, 1000, 0);
				coolBar1LData.top =  new FormAttachment(0, 1000, 0);
				coolBar1LData.width = 394;
				coolBar1LData.height = 22;
				coolBar1 = new CoolBar(this, SWT.NONE);
				coolBar1.setLayoutData(coolBar1LData);
				{
					coolItem1 = new CoolItem(coolBar1, SWT.NONE);
					coolItem1.setMinimumSize(new org.eclipse.swt.graphics.Point(24, 22));
					coolItem1.setPreferredSize(new org.eclipse.swt.graphics.Point(24, 22));
					{
						toolBar1 = new ToolBar(coolBar1, SWT.NONE);
						coolItem1.setControl(toolBar1);
						{
							toolItem1 = new ToolItem(toolBar1, SWT.NONE);
							toolItem1.setText("Save");
							toolItem1.addListener(SWT.Selection, new Listener() {								
								@Override
								public void handleEvent(Event arg0) {
									try {
										Manager.getInstance().updateSynchroNode(getFormData(), currentSynchroNode);
									} catch (SQLException e) {
										e.printStackTrace();
									} catch (URISyntaxException e) {
										e.printStackTrace();
									} catch (Exception e) {
										e.printStackTrace();
									}
								}
							});
						}
					}
				}
			}
			{
				cTabFolder = new CTabFolder(this, SWT.NONE);
				{
					cTabItem1 = new CTabItem(cTabFolder, SWT.NONE);
					cTabItem1.setText("Connexion data");
					{
						composite1 = new Composite(cTabFolder, SWT.NONE);
						FormLayout composite1Layout = new FormLayout();
						composite1.setLayout(composite1Layout);
						cTabItem1.setControl(composite1);
						composite1.setBackground(SWTResourceManager.getColor(255, 255, 255));
						{
							buttonLoadRepositories = new Button(composite1, SWT.PUSH | SWT.CENTER);
							FormData buttonLoadRepositoriesLData = new FormData();
							buttonLoadRepositoriesLData.left =  new FormAttachment(0, 1000, 294);
							buttonLoadRepositoriesLData.top =  new FormAttachment(0, 1000, 124);
							buttonLoadRepositoriesLData.width = 64;
							buttonLoadRepositoriesLData.height = 23;
							buttonLoadRepositories.setLayoutData(buttonLoadRepositoriesLData);
							buttonLoadRepositories.setText("Load");
							buttonLoadRepositories.addListener(SWT.Selection, new Listener() {
								@Override
								public void handleEvent(Event arg0) {
									loadRepositories();
								}
							});
						}
						{
							FormData comboRepositoryLData = new FormData();
							comboRepositoryLData.left =  new FormAttachment(0, 1000, 111);
							comboRepositoryLData.top =  new FormAttachment(0, 1000, 126);
							comboRepositoryLData.width = 143;
							comboRepositoryLData.height = 21;
							comboRepository = new Combo(composite1, SWT.NONE);
							comboRepository.setLayoutData(comboRepositoryLData);
							comboRepository.setEnabled(false);
						}
						{
							labelRepository = new Label(composite1, SWT.NONE);
							FormData labelRepositoryLData = new FormData();
							labelRepositoryLData.left =  new FormAttachment(0, 1000, 30);
							labelRepositoryLData.top =  new FormAttachment(0, 1000, 128);
							labelRepositoryLData.width = 69;
							labelRepositoryLData.height = 13;
							labelRepository.setLayoutData(labelRepositoryLData);
							labelRepository.setText("Repository");
							labelRepository.setAlignment(SWT.RIGHT);
							labelRepository.setBackground(SWTResourceManager.getColor(255, 255, 255));
						}
						{
							hostLabel = new Label(composite1, SWT.NONE);
							FormData hostLabelLData = new FormData();
							hostLabelLData.left =  new FormAttachment(0, 1000, 30);
							hostLabelLData.top =  new FormAttachment(0, 1000, 20);
							hostLabelLData.width = 69;
							hostLabelLData.height = 13;
							hostLabel.setLayoutData(hostLabelLData);
							hostLabel.setText("Host URL");
							hostLabel.setAlignment(SWT.RIGHT);
							hostLabel.setBackground(SWTResourceManager.getColor(255, 255, 255));
						}
						{
							FormData hostTextLData = new FormData();
							hostTextLData.left =  new FormAttachment(0, 1000, 111);
							hostTextLData.top =  new FormAttachment(0, 1000, 17);
							hostTextLData.width = 235;
							hostTextLData.height = 14;
							tfHost = new Text(composite1, SWT.BORDER);
							tfHost.setLayoutData(hostTextLData);
							tfHost.setOrientation(SWT.HORIZONTAL);
						}
						{
							labelLogin = new Label(composite1, SWT.NONE);
							FormData labelLoginLData = new FormData();
							labelLoginLData.left =  new FormAttachment(0, 1000, 30);
							labelLoginLData.top =  new FormAttachment(0, 1000, 56);
							labelLoginLData.width = 69;
							labelLoginLData.height = 13;
							labelLogin.setLayoutData(labelLoginLData);
							labelLogin.setText("Login");
							labelLogin.setAlignment(SWT.RIGHT);
							labelLogin.setBackground(SWTResourceManager.getColor(255, 255, 255));
						}
						{
							FormData tfLoginLData = new FormData();
							tfLoginLData.left =  new FormAttachment(0, 1000, 111);
							tfLoginLData.top =  new FormAttachment(0, 1000, 53);
							tfLoginLData.width = 235;
							tfLoginLData.height = 14;
							tfLogin = new Text(composite1, SWT.BORDER);
							tfLogin.setLayoutData(tfLoginLData);
						}
						{
							labelPass = new Label(composite1, SWT.NONE);
							FormData labelPassLData = new FormData();
							labelPassLData.left =  new FormAttachment(0, 1000, 30);
							labelPassLData.top =  new FormAttachment(0, 1000, 92);
							labelPassLData.width = 69;
							labelPassLData.height = 13;
							labelPass.setLayoutData(labelPassLData);
							labelPass.setText("Password");
							labelPass.setAlignment(SWT.RIGHT);
							labelPass.setBackground(SWTResourceManager.getColor(255, 255, 255));
						}
						{
							FormData tfPasswordLData = new FormData();
							tfPasswordLData.left =  new FormAttachment(0, 1000, 111);
							tfPasswordLData.top =  new FormAttachment(0, 1000, 89);
							tfPasswordLData.width = 235;
							tfPasswordLData.height = 14;
							tfPassword = new Text(composite1, SWT.BORDER|SWT.PASSWORD);
							tfPassword.setLayoutData(tfPasswordLData);
						}
						{
							labelTarget = new Label(composite1, SWT.NONE);
							FormData labelTargetLData = new FormData();
							labelTargetLData.left =  new FormAttachment(0, 1000, 30);
							labelTargetLData.top =  new FormAttachment(0, 1000, 165);
							labelTargetLData.width = 69;
							labelTargetLData.height = 13;
							labelTarget.setLayoutData(labelTargetLData);
							labelTarget.setText("Target Folder");
							labelTarget.setAlignment(SWT.RIGHT);
							labelTarget.setBackground(SWTResourceManager.getColor(255, 255, 255));
						}
						{
							FormData tfTargetLData = new FormData();
							tfTargetLData.left =  new FormAttachment(0, 1000, 111);
							tfTargetLData.top =  new FormAttachment(0, 1000, 163);
							tfTargetLData.width = 159;
							tfTargetLData.height = 15;
							tfTarget = new Text(composite1, SWT.BORDER);
							tfTarget.setLayoutData(tfTargetLData);
							tfTarget.setEditable(false);
						}
						{
							buttonFileChooser = new Button(composite1, SWT.PUSH | SWT.CENTER);
							FormData buttonFileChooserLData = new FormData();
							buttonFileChooserLData.left =  new FormAttachment(0, 1000, 294);
							buttonFileChooserLData.top =  new FormAttachment(0, 1000, 162);
							buttonFileChooserLData.width = 64;
							buttonFileChooserLData.height = 23;
							buttonFileChooser.setLayoutData(buttonFileChooserLData);
							buttonFileChooser.setText("Browse");
							buttonFileChooser.addListener(SWT.Selection, new Listener() {
								
								@Override
								public void handleEvent(Event arg0) {									
									DirectoryDialog dialog = new DirectoryDialog(JobEditor.this.getShell());
									tfTarget.setText(dialog.open());
								}
							});
						}
					}

				}
				{
					cTabItem2 = new CTabItem(cTabFolder, SWT.NONE);
					cTabItem2.setText("Job Execution");
					{
						composite2 = new Composite(cTabFolder, SWT.NONE);
						FormLayout composite2Layout = new FormLayout();
						composite2.setLayout(composite2Layout);
						cTabItem2.setControl(composite2);
						composite2.setBackground(SWTResourceManager.getColor(255, 255, 255));
						{
							radioSyncInterval3 = new Button(composite2, SWT.RADIO | SWT.LEFT);
							FormData radioSyncInterval3LData = new FormData();
							radioSyncInterval3LData.left =  new FormAttachment(0, 1000, 265);
							radioSyncInterval3LData.top =  new FormAttachment(0, 1000, 134);
							radioSyncInterval3LData.width = 113;
							radioSyncInterval3LData.height = 16;
							radioSyncInterval3.setLayoutData(radioSyncInterval3LData);
							radioSyncInterval3.setText("Day");
							radioSyncInterval3.setBackground(SWTResourceManager.getColor(255, 255, 255));
						}
						{
							radioSyncInterval2 = new Button(composite2, SWT.RADIO | SWT.LEFT);
							FormData radioSyncInterval2LData = new FormData();
							radioSyncInterval2LData.left =  new FormAttachment(0, 1000, 135);
							radioSyncInterval2LData.top =  new FormAttachment(0, 1000, 134);
							radioSyncInterval2LData.width = 118;
							radioSyncInterval2LData.height = 16;
							radioSyncInterval2.setLayoutData(radioSyncInterval2LData);
							radioSyncInterval2.setText("Hour");
							radioSyncInterval2.setBackground(SWTResourceManager.getColor(255, 255, 255));
						}
						{
							radioSyncInterval1 = new Button(composite2, SWT.RADIO | SWT.LEFT);
							FormData radioSyncInterval1LData = new FormData();
							radioSyncInterval1LData.left =  new FormAttachment(0, 1000, 18);
							radioSyncInterval1LData.top =  new FormAttachment(0, 1000, 134);
							radioSyncInterval1LData.width = 105;
							radioSyncInterval1LData.height = 16;
							radioSyncInterval1.setLayoutData(radioSyncInterval1LData);
							radioSyncInterval1.setText("Minute");
							radioSyncInterval1.setBackground(SWTResourceManager.getColor(255, 255, 255));
						}
						{
							label1 = new Label(composite2, SWT.NONE);
							FormData label1LData = new FormData();
							label1LData.left =  new FormAttachment(0, 1000, 12);
							label1LData.top =  new FormAttachment(0, 1000, 106);
							label1LData.width = 366;
							label1LData.height = 23;
							label1LData.right =  new FormAttachment(1000, 1000, -12);
							label1.setLayoutData(label1LData);
							label1.setText("Run synchronization every ...");
							label1.setFont(SWTResourceManager.getFont("Tahoma", 10, 1, false, false));
							label1.setBackground(SWTResourceManager.getColor(255, 255, 255));
						}
						{
							cLabel1 = new CLabel(composite2, SWT.NONE);
							FormData cLabel1LData = new FormData();
							cLabel1LData.left =  new FormAttachment(0, 1000, 8);
							cLabel1LData.top =  new FormAttachment(0, 1000, 43);
							cLabel1LData.width = 293;
							cLabel1LData.height = 19;
							cLabel1LData.right =  new FormAttachment(1000, 1000, -12);
							cLabel1.setLayoutData(cLabel1LData);
							cLabel1.setText("Synchronisation direction");
							cLabel1.setFont(SWTResourceManager.getFont("Tahoma", 10, 1, false, false));
							cLabel1.setBounds(8, 43, 293, 19);
							cLabel1.setBackground(SWTResourceManager.getColor(255, 255, 255));
						}
						{
							radioDirection3 = new Button(composite2, SWT.RADIO | SWT.LEFT);
							FormData radioDirection3LData = new FormData();
							radioDirection3LData.left =  new FormAttachment(0, 1000, 265);
							radioDirection3LData.top =  new FormAttachment(0, 1000, 67);
							radioDirection3LData.width = 113;
							radioDirection3LData.height = 16;
							radioDirection3.setLayoutData(radioDirection3LData);
							radioDirection3.setText("Upload Only");
							radioDirection3.setBackground(SWTResourceManager.getColor(255, 255, 255));
						}
						{
							radioDirection2 = new Button(composite2, SWT.RADIO | SWT.LEFT);
							FormData radioDirection2LData = new FormData();
							radioDirection2LData.left =  new FormAttachment(0, 1000, 135);
							radioDirection2LData.top =  new FormAttachment(0, 1000, 67);
							radioDirection2LData.width = 118;
							radioDirection2LData.height = 16;
							radioDirection2.setLayoutData(radioDirection2LData);
							radioDirection2.setText("Download only");
							radioDirection2.setBackground(SWTResourceManager.getColor(255, 255, 255));
						}
						{
							radioDirection = new Button(composite2, SWT.RADIO | SWT.LEFT);
							FormData radioDirectionLData = new FormData();
							radioDirectionLData.left =  new FormAttachment(0, 1000, 18);
							radioDirectionLData.top =  new FormAttachment(0, 1000, 67);
							radioDirectionLData.width = 105;
							radioDirectionLData.height = 16;
							radioDirection.setLayoutData(radioDirectionLData);
							radioDirection.setText("Bidirectionnal");
							radioDirection.setBackground(SWTResourceManager.getColor(255, 255, 255));
						}
						{
							checkboxActive = new Button(composite2, SWT.CHECK | SWT.LEFT);
							FormData checkboxActiveLData = new FormData();
							checkboxActiveLData.left =  new FormAttachment(0, 1000, 20);
							checkboxActiveLData.top =  new FormAttachment(0, 1000, 12);
							checkboxActiveLData.width = 273;
							checkboxActiveLData.height = 16;
							checkboxActive.setLayoutData(checkboxActiveLData);
							checkboxActive.setText("This job is active");
							checkboxActive.setBackground(SWTResourceManager.getColor(255, 255, 255));
						}
					}
				}
				FormData cTabFolderLData = new FormData();
				cTabFolderLData.left =  new FormAttachment(0, 1000, 0);
				cTabFolderLData.top =  new FormAttachment(0, 1000, 23);
				cTabFolderLData.width = 313;
				cTabFolderLData.height = 185;
				cTabFolderLData.right =  new FormAttachment(1000, 1000, 0);
				cTabFolderLData.bottom =  new FormAttachment(1000, 1000, 0);
				cTabFolder.setLayoutData(cTabFolderLData);
				cTabFolder.setSelection(0);
				cTabFolder.setBackground(SWTResourceManager.getColor(255, 255, 255));
			}
			this.layout();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
