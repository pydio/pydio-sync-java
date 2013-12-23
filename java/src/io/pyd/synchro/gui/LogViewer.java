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

import info.ajaxplorer.client.model.Node;
import io.pyd.synchro.CoreManager;
import io.pyd.synchro.SyncJob;
import io.pyd.synchro.model.SyncChange;
import io.pyd.synchro.model.SyncChangeValue;
import io.pyd.synchro.model.SyncLog;

import java.sql.SQLException;
import java.text.Collator;
import java.text.DateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;

import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.quartz.SchedulerException;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.support.ConnectionSource;


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
	
	Menu solveMenu;
	MenuItem itemSolveKeepMine;
	MenuItem itemSolveKeepTheir;
	MenuItem itemSolveKeepBoth;
	MenuItem itemSolveIgnore;
	Node currentSynchroNode;
	int currentConflictsCount;

	/**
	* Overriding checkSubclass allows this class to extend org.eclipse.swt.widgets.Composite
	*/	
	protected void checkSubclass() {
	}
	
	public LogViewer(org.eclipse.swt.widgets.Composite parent, int style) {
		super(parent, style);
		//initGUI();
	}

	public Control getMouseHandler(){
		return this;
	}
	
	public void clearSynchroLog(){
		table1.removeAll();
		table2.removeAll();
	}
	
	public void reload(){
		if( currentSynchroNode != null){
			this.loadSynchroLog(currentSynchroNode);
		}
	}
	
	public void loadSynchroLog(Node synchroNode){
		currentConflictsCount = 0;
		ConnectionSource cs;
		try {			
			cs = CoreManager.getInstance().getConnection();
			Dao<SyncLog, String> logDao = DaoManager.createDao(cs, SyncLog.class);
			Dao<SyncChange, String> changeDao = DaoManager.createDao(cs, SyncChange.class);
			this.currentSynchroNode = synchroNode;
			QueryBuilder<SyncLog, String> builder = logDao.queryBuilder();
			builder.where().eq("synchroNode_id", synchroNode.id);
			builder.orderBy("jobDate", false);
			PreparedQuery<SyncLog> preparedQuery = builder.prepare();
			table1.removeAll();
			Collection<SyncLog> logs = logDao.query(preparedQuery);
			DateFormat df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, Locale.getDefault());
			for(SyncLog log:logs){
				TableItem it = new TableItem(table1, SWT.NONE);
				it.setText(0, df.format(new Date(log.jobDate)));
				it.setText(1, log.jobStatus);
				it.setText(2, log.jobSummary);
				if(log.jobStatus.equals("conflicts") || log.jobStatus.equals("errors")){
					it.setForeground(SWTResourceManager.getColor(180, 0, 0));
				}else if(log.jobStatus.equals("running")){
					it.setForeground(SWTResourceManager.getColor(180, 180, 180));
				}
				it.setData(log);
			}
			for (int i = 0; i < 2; i++) {
		        TableColumn column = table1.getColumn(i);
		        column.pack();
		    }
			table1.getColumn(2).setWidth(table1.getBounds().width - table1.getColumn(0).getWidth() - table1.getColumn(1).getWidth()-20);
			table1.setSortColumn(table1ColumnDate);
			table1.setSortDirection(SWT.DOWN);
			
			
			table2.removeAll();
			Collection<SyncChange> changes = changeDao.queryForEq("jobId", synchroNode.id);
			for(SyncChange change:changes){
				TableItem it = new TableItem(table2, SWT.NONE);
				SyncChangeValue v = change.getChangeValue();
				it.setText(0, change.getKey());
				it.setText(1, CoreManager.getMessage("logviewer_status") + " : "+v.getStatusString());
				it.setText(2, CoreManager.getMessage("logviewer_task") + " : "+v.getTaskString());
				it.setData(change);
				if(v.status == SyncJob.STATUS_CONFLICT) currentConflictsCount ++;
			}
			
			for (int i = 0; i < 2; i++) {
		        TableColumn column = table2.getColumn(i);
		        column.pack();
		    }
			table2.getColumn(2).setWidth(table2.getBounds().width - table2.getColumn(0).getWidth() - table2.getColumn(1).getWidth()-20);
			
		} catch (SQLException e) {
			Logger.getRootLogger().error("Synchro", e);
		} finally{
			CoreManager.getInstance().releaseConnection();
		}
	}
	
	public void initGUI() {
		try {
			FillLayout compositeLayout = new FillLayout();
			compositeLayout.type = SWT.HORIZONTAL|SWT.VERTICAL;
			this.setLayout(compositeLayout);						
			
			this.setBackground(getDisplay().getSystemColor(SWT.COLOR_WHITE));
			this.setSize(581, 310);

			{
				sashForm1 = new SashForm(this, SWT.VERTICAL);
				sashForm1.SASH_WIDTH = 5;
				sashForm1.setBackground(getDisplay().getSystemColor(SWT.COLOR_WHITE));
				{
					composite1 = new Composite(sashForm1, SWT.NONE);
					composite1.setBackground(getDisplay().getSystemColor(SWT.COLOR_WHITE));
					FormLayout composite1Layout = new FormLayout();
					composite1.setLayout(composite1Layout);
					{
						label1 = new Label(composite1, SWT.NONE);
						label1.setText(CoreManager.getMessage("logviewer_synclogs"));
						FormData label1FormData = new FormData();
						label1FormData.left =  new FormAttachment(0, 1000, 5);
						label1FormData.top =  new FormAttachment(0, 1000, 10);
						label1FormData.right = new FormAttachment(1000, 1000, 0);
						label1FormData.height = 20;
						label1.setLayoutData(label1FormData);
						label1.setBackground(getDisplay().getSystemColor(SWT.COLOR_WHITE));
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
						 Listener sortListener = new Listener() {  
						        public void handleEvent(Event e) {
						        	if(table1.getSortDirection() == SWT.UP){
						        		table1.setSortDirection(SWT.DOWN);
						        	}else{
						        		table1.setSortDirection(SWT.UP);
						        	}
						        	int dir = table1.getSortDirection();
						            TableItem[] items = table1.getItems();  
						            Collator collator = Collator.getInstance(Locale.getDefault());  
						            TableColumn column = (TableColumn)e.widget;  
						            int index=0;
						            if(column.getData().equals("date")) index = 0;
						            else if(column.getData().equals("result")) index = 1;
						            else if(column.getData().equals("summary")) index = 2;
						            for (int i = 1; i < items.length; i++) {  
						                String value1 = items[i].getText(index);  
						                for (int j = 0; j < i; j++){  
						                    String value2 = items[j].getText(index);  
						                    if ((dir == SWT.UP && collator.compare(value1, value2) < 0) || (dir == SWT.DOWN && collator.compare(value1, value2) > 0)) {
						                        String[] values = {items[i].getText(0), items[i].getText(1), items[i].getText(2)};  
						                        items[i].dispose();  
						                        TableItem item = new TableItem(table1, SWT.NONE, j);
						        				if(values[1].equals("conflicts") || values[1].equals("errors")){
						        					item.setForeground(SWTResourceManager.getColor(180, 0, 0));
						        				}else if(values[1].equals("running")){
						        					item.setForeground(SWTResourceManager.getColor(180, 180, 180));
						        				}
						                        
						                        item.setText(values);  
						                        items = table1.getItems();  
						                        break;  
						                    }  
						                }  
						            }  
						            table1.setSortColumn(column);
						        }  
						    };  						
						
						table1.setLayoutData(table1FormData);
						{
							table1ColumnDate = new TableColumn(table1, SWT.NONE);
							table1ColumnDate.setText(CoreManager.getMessage("logviewer_column_date"));
							table1ColumnDate.setData("date");
							table1ColumnDate.addListener(SWT.Selection, sortListener);							
						}
						{
							table1ColumnResult = new TableColumn(table1, SWT.NONE);
							table1ColumnResult.setText(CoreManager.getMessage("logviewer_column_result"));
							table1ColumnResult.setData("result");
							table1ColumnResult.addListener(SWT.Selection, sortListener);
						}
						{
							table1ColumnSummary = new TableColumn(table1, SWT.NONE);
							table1ColumnSummary.setText(CoreManager.getMessage("logviewer_column_summary"));
							table1ColumnSummary.setData("summary");
							table1ColumnSummary.addListener(SWT.Selection, sortListener);
						}					
					}
				}
				{
					composite2 = new Composite(sashForm1, SWT.NONE);
					composite2.setBackground(getDisplay().getSystemColor(SWT.COLOR_WHITE));
					FormLayout composite1Layout = new FormLayout();
					composite2.setLayout(composite1Layout);
					{
						label2 = new Label(composite2, SWT.NONE);
						label2.setText(CoreManager.getMessage("logviewer_interrupted_tasks"));
						FormData label1FormData = new FormData();
						label1FormData.left =  new FormAttachment(0, 1000, 5);
						label1FormData.top =  new FormAttachment(0, 1000, 10);
						label1FormData.right = new FormAttachment(1000, 1000, 0);
						label1FormData.height = 20;
						label2.setLayoutData(label1FormData);
						label2.setBackground(getDisplay().getSystemColor(SWT.COLOR_WHITE));
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
							table2ColumnFile.setText(CoreManager.getMessage("logviewer_column_file"));
						}
						{
							table2ColumnStatus = new TableColumn(table2, SWT.NONE);
							table2ColumnStatus.setText(CoreManager.getMessage("logviewer_column_status"));
						}
						{
							TableColumn table2ColumnTask = new TableColumn(table2, SWT.NONE);
							table2ColumnTask.setText(CoreManager.getMessage("logviewer_column_task"));
						}
					}
				}			
			}			
			table2.addMouseListener(new MouseListener() {
				
				@Override
				public void mouseUp(MouseEvent arg0) {					
				}
				
				@Override
				public void mouseDown(MouseEvent arg0) {
					if(arg0.button != 3) return;
					Point p = new Point(arg0.x, arg0.y);
					Point p2 = new Point(arg0.x + getShell().getBounds().x, arg0.y + getShell().getBounds().y);
					if(table2.getItem(p) != null){
						p2 = table2.toDisplay(p);
						solveMenu.setLocation(p2);
						solveMenu.setVisible(true);
					}
				}
				
				@Override
				public void mouseDoubleClick(MouseEvent arg0) {
				}
			});
			 solveMenu = new Menu(this.getShell(),
                     SWT.POP_UP);
			 itemSolveKeepMine = new MenuItem(solveMenu, SWT.PUSH);
			 itemSolveKeepMine.setText(CoreManager.getMessage("logviewer_conflict_keepmine"));
			 itemSolveKeepMine.setData("mine");
			 SelectionListener listener = new SelectionListener() {				 
				 public void widgetSelected(SelectionEvent arg0) {
					 boolean changes = false;
					 ConnectionSource cs = CoreManager.getInstance().getConnection();
					 try{
					 for(TableItem currentTarget:table2.getSelection()){
						 SyncChange c = (SyncChange)currentTarget.getData();
						 String command = (String)arg0.widget.getData();
						 Dao<SyncChange, String> sCDao = DaoManager.createDao(cs, SyncChange.class);
						 SyncChangeValue v = c.getChangeValue();
						 if(command.equals("mine")) v.task = SyncJob.TASK_SOLVE_KEEP_MINE;
						 else if(command.equals("their")) v.task = SyncJob.TASK_SOLVE_KEEP_THEIR;
						 else if(command.equals("ignore")) v.task = SyncJob.TASK_DO_NOTHING;
						 else v.task = SyncJob.TASK_SOLVE_KEEP_BOTH;
						 v.status = SyncJob.STATUS_CONFLICT_SOLVED;
						 c.setChangeValue(v);
						 try {
							sCDao.update(c);
							changes = true;
						} catch (SQLException e) {
							Logger.getRootLogger().error("Synchro", e);
						}						 
					 }		
					 }catch(SQLException e){
						 Logger.getRootLogger().error("Synchro", e);
					 }finally{
						 CoreManager.getInstance().releaseConnection();
					 }
					 if(changes) reload();
					 solveMenu.setVisible(false);
					 if(currentConflictsCount == 0){
						MessageBox messageBox = new MessageBox(LogViewer.this.getShell(), SWT.ICON_QUESTION|SWT.YES|SWT.NO);
					    messageBox.setMessage(CoreManager.getMessage("logviewer_conflict_solved"));
					    if(messageBox.open() == SWT.YES){
					    	try {
								CoreManager.getInstance().triggerJobNow(currentSynchroNode, false);
							} catch (SchedulerException e) {
								Logger.getRootLogger().error("Synchro", e);
							}
					    }
					 }
				 }				 
				 public void widgetDefaultSelected(SelectionEvent arg0) {}
			 };			 
			 itemSolveKeepMine.addSelectionListener(listener);
			 itemSolveKeepTheir = new MenuItem(solveMenu, SWT.PUSH);
			 itemSolveKeepTheir.setText(CoreManager.getMessage("logviewer_conflict_keeptheir"));
			 itemSolveKeepTheir.setData("their");
			 itemSolveKeepTheir.addSelectionListener(listener);
			 itemSolveKeepBoth = new MenuItem(solveMenu, SWT.PUSH);
			 itemSolveKeepBoth.setText(CoreManager.getMessage("logviewer_conflict_keepboth"));
			 itemSolveKeepBoth.setData("both");
			 itemSolveKeepBoth.addSelectionListener(listener);
             itemSolveIgnore = new MenuItem(solveMenu, SWT.PUSH);
             itemSolveIgnore.setText(CoreManager.getMessage("logviewer_conflict_ignore"));
             itemSolveIgnore.setData("ignore");
             itemSolveIgnore.addSelectionListener(listener);
			this.layout();
		} catch (Exception e) {
			Logger.getRootLogger().error("Synchro", e);
		}
	}

}
