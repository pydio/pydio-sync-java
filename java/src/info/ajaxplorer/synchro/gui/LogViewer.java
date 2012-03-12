package info.ajaxplorer.synchro.gui;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;

import info.ajaxplorer.client.model.Node;
import info.ajaxplorer.synchro.Manager;
import info.ajaxplorer.synchro.model.SyncLog;

import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;


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
public class LogViewer extends org.eclipse.swt.widgets.Composite {
	private SashForm sashForm1;
	private Table table1;
	private TableColumn table1ColumnDate;
	private TableColumn table1ColumnResult;
	private Label label1;
	private TableColumn table1ColumnSummary;
	private Composite composite1;
	private Table table2;
	private Label label2;
	private Composite composite2;
	private TableColumn table2ColumnStatus;
	private TableColumn table2ColumnFile;

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
	
	public LogViewer(org.eclipse.swt.widgets.Composite parent, int style) {
		super(parent, style);
		//initGUI();
	}

	public void clearSynchroLog(){
		table1.removeAll();
	}
	
	public void loadSynchroLog(Node synchroNode){
		try {
			table1.removeAll();
			Collection<SyncLog> logs = Manager.getInstance().getSyncLogDao().queryForEq("synchroNode_id", synchroNode.id);
			for(SyncLog log:logs){
				TableItem it = new TableItem(table1, SWT.NONE);
				it.setText(0, new Date(log.jobDate).toString());
				it.setText(1, log.jobStatus);
				it.setText(2, log.jobSummary);
			}
			for (int i = 0; i < 2; i++) {
		        TableColumn column = table1.getColumn(i);
		        column.pack();
		    }
			table1.getColumn(2).setWidth(table1.getBounds().width - table1.getColumn(0).getWidth() - table1.getColumn(1).getWidth());
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void initGUI() {
		try {
			FormLayout thisLayout = new FormLayout();
			this.setLayout(thisLayout);
			this.setSize(581, 310);

			{
				FormData sashForm1LData = new FormData();
				sashForm1LData.left =  new FormAttachment(0, 1000, 5);
				sashForm1LData.top =  new FormAttachment(0, 1000, 5);
				sashForm1LData.width = 450;
				sashForm1LData.height = 300;
				sashForm1LData.right =  new FormAttachment(1000, 1000, -5);
				sashForm1LData.bottom =  new FormAttachment(1000, 1000, -5);
				sashForm1 = new SashForm(this, SWT.VERTICAL);
				sashForm1.SASH_WIDTH = 5;
				sashForm1.setLayoutData(sashForm1LData);
				{
					composite1 = new Composite(sashForm1, SWT.NONE);
					FormLayout composite1Layout = new FormLayout();
					composite1.setLayout(composite1Layout);
					{
						label1 = new Label(composite1, SWT.NONE);
						label1.setText("Synchronisations Log");
						FormData label1FormData = new FormData();
						label1FormData.left =  new FormAttachment(0, 1000, 5);
						label1FormData.top =  new FormAttachment(0, 1000, 10);
						label1FormData.right = new FormAttachment(1000, 1000, 0);
						label1FormData.height = 20;
						label1.setLayoutData(label1FormData);
					}					
					{
						table1 = new Table(composite1, SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION);
						table1.setLinesVisible(true);
						table1.setHeaderVisible(true);
						FormData table1FormData = new FormData();
						table1FormData.left =  new FormAttachment(0, 1000, 0);
						table1FormData.top =  new FormAttachment(0, 1000, 30);
						table1FormData.right = new FormAttachment(1000, 1000, 0);
						table1FormData.bottom = new FormAttachment(1000, 1000, 0);
						table1FormData.height = 10;
						table1.setLayoutData(table1FormData);
						{
							table1ColumnDate = new TableColumn(table1, SWT.NONE);
							table1ColumnDate.setText("Date");
							//table1ColumnDate.setWidth(120);
						}
						{
							table1ColumnResult = new TableColumn(table1, SWT.NONE);
							table1ColumnResult.setText("Result");
							//table1ColumnResult.setWidth(129);
						}
						{
							table1ColumnSummary = new TableColumn(table1, SWT.NONE);
							table1ColumnSummary.setText("Summary");
							//table1ColumnSummary.setWidth(317);
						}
					}
				}
				{
					composite2 = new Composite(sashForm1, SWT.NONE);
					FormLayout composite1Layout = new FormLayout();
					composite2.setLayout(composite1Layout);
					{
						label2 = new Label(composite2, SWT.NONE);
						label2.setText("Interrupted tasks or unresolved conflicts (right-click on each if an action is necessary)");
						FormData label1FormData = new FormData();
						label1FormData.left =  new FormAttachment(0, 1000, 5);
						label1FormData.top =  new FormAttachment(0, 1000, 10);
						label1FormData.right = new FormAttachment(1000, 1000, 0);
						label1FormData.height = 20;
						label2.setLayoutData(label1FormData);
					}					
					{
						table2 = new Table(composite2, SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION);
						table2.setLinesVisible(true);
						table2.setHeaderVisible(true);
						FormData table1FormData = new FormData();
						table1FormData.left =  new FormAttachment(0, 1000, 0);
						table1FormData.top =  new FormAttachment(0, 1000, 30);
						table1FormData.right = new FormAttachment(1000, 1000, 0);
						table1FormData.bottom = new FormAttachment(1000, 1000, 0);
						table1FormData.height = 10;
						table2.setLayoutData(table1FormData);
						{
							table2ColumnFile = new TableColumn(table2, SWT.NONE);
							table2ColumnFile.setText("File/Folder name");
							table2ColumnFile.setWidth(311);
						}
						{
							table2ColumnStatus = new TableColumn(table2, SWT.NONE);
							table2ColumnStatus.setText("Status");
							table2ColumnStatus.setWidth(248);
						}
					}
				}			}
			this.layout();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
