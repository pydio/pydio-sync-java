/*
 * Copyright 2012 Charles du Jeu <charles (at) ajaxplorer.info>
 * This file is part of AjaXplorer.
 *
 * AjaXplorer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AjaXplorer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AjaXplorer.  If not, see <http://www.gnu.org/licenses/>.
 *
 * The latest code can be found at <http://www.ajaxplorer.info/>.
 *
 */
package info.ajaxplorer.synchro;

import info.ajaxplorer.client.http.AjxpAPI;
import info.ajaxplorer.client.http.AjxpHttpClient;
import info.ajaxplorer.client.http.CountingMultipartRequestEntity;
import info.ajaxplorer.client.http.MessageListener;
import info.ajaxplorer.client.http.RestRequest;
import info.ajaxplorer.client.http.RestStateHolder;
import info.ajaxplorer.client.model.Node;
import info.ajaxplorer.client.model.Property;
import info.ajaxplorer.client.model.Server;
import info.ajaxplorer.client.util.RdiffProcessor;
import info.ajaxplorer.synchro.exceptions.EhcacheListException;
import info.ajaxplorer.synchro.exceptions.SynchroFileOperationException;
import info.ajaxplorer.synchro.exceptions.SynchroOperationException;
import info.ajaxplorer.synchro.gui.JobEditor;
import info.ajaxplorer.synchro.model.SyncChange;
import info.ajaxplorer.synchro.model.SyncChangeValue;
import info.ajaxplorer.synchro.model.SyncLog;
import info.ajaxplorer.synchro.model.SyncLogDetails;
import info.ajaxplorer.synchro.utils.EhcacheListFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.Callable;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathExpressionException;

import org.apache.http.HttpEntity;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Utils;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.InterruptableJob;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.j256.ormlite.dao.CloseableIterator;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.stmt.SelectArg;
import com.j256.ormlite.support.ConnectionSource;
import com.scireum.open.xml.NodeHandler;
import com.scireum.open.xml.StructuredNode;
import com.scireum.open.xml.XMLReader;

@DisallowConcurrentExecution
public class SyncJob implements InterruptableJob {

	// all ehcache files
	private static final String DIFF_NODE_LISTS_SAVED_LIST = "diffnodelistssaved";
	private static final String DIFF_NODE_LISTS_CREATED_LIST = "diffnodelistscreated";
	private static final String LOAD_REMOTE_CHANGES_LIST = "loadremotechangeslist";
	private static final String LOAD_LOCAL_CHANGES_LIST = "loadlocalchangeslist";
	private static final String INDEXATION_SNAPSHOT_LIST = "indexationsnap";
	private static final String REMOTE_SNAPSHOT_LIST = "remotesnapshot";
	private static final String LOCAL_SNAPSHOT_LIST = "localsnapshot";

	public static Integer NODE_CHANGE_STATUS_FILE_CREATED = 2;
	public static Integer NODE_CHANGE_STATUS_FILE_DELETED = 4;
	public static Integer NODE_CHANGE_STATUS_MODIFIED = 8;
	public static Integer NODE_CHANGE_STATUS_DIR_CREATED = 16;
	public static Integer NODE_CHANGE_STATUS_DIR_DELETED = 32;
	public static Integer NODE_CHANGE_STATUS_FILE_MOVED = 64;

	public static Integer TASK_DO_NOTHING = 1;
	public static Integer TASK_REMOTE_REMOVE = 2;
	public static Integer TASK_REMOTE_PUT_CONTENT = 4;
	public static Integer TASK_REMOTE_MKDIR = 8;
	public static Integer TASK_LOCAL_REMOVE = 16;
	public static Integer TASK_LOCAL_MKDIR = 32;
	public static Integer TASK_LOCAL_GET_CONTENT = 64; // FIX - does it mean
														// that we will download
														// content locally?
	public static Integer TASK_REMOTE_MOVE_FILE = 128;
	public static Integer TASK_LOCAL_MOVE_FILE = 256;

	public static Integer TASK_SOLVE_KEEP_MINE = 128;
	public static Integer TASK_SOLVE_KEEP_THEIR = 256;
	public static Integer TASK_SOLVE_KEEP_BOTH = 512;

	public static Integer STATUS_TODO = 2;
	public static Integer STATUS_DONE = 4;
	public static Integer STATUS_ERROR = 8;
	public static Integer STATUS_CONFLICT = 16;
	public static Integer STATUS_CONFLICT_SOLVED = 128;
	public static Integer STATUS_PROGRESS = 32;
	public static Integer STATUS_INTERRUPTED = 64;

	public static Integer RUNNING_STATUS_INITIALIZING = 64;
	public static Integer RUNNING_STATUS_TESTING_CONNEXION = 128;
	public static Integer RUNNING_STATUS_PREVIOUS_CHANGES = 512;
	public static Integer RUNNING_STATUS_LOCAL_CHANGES = 2;
	public static Integer RUNNING_STATUS_REMOTE_CHANGES = 4;
	public static Integer RUNNING_STATUS_COMPARING_CHANGES = 8;
	public static Integer RUNNING_STATUS_APPLY_CHANGES = 16;
	public static Integer RUNNING_STATUS_CLEANING = 32;
	public static Integer RUNNING_STATUS_INTERRUPTING = 256;

	Node currentRepository;
	Dao<Node, String> nodeDao;
	Dao<SyncChange, String> syncChangeDao;
	Dao<SyncLog, String> syncLogDao;
	Dao<Property, Integer> propertyDao;
	Dao<SyncLogDetails, String> syncLogDetailsDao;

	private String currentJobNodeID;
	private boolean clearSnapshots = false;
	private boolean localWatchOnly = false;
	private String direction;
	private File currentLocalFolder;

	boolean interruptRequired = false;

	private int countResourcesSynchronized = 0;
	private int countFilesUploaded = 0;
	private int countFilesDownloaded = 0;
	private int countResourcesInterrupted = 0;
	private int countResourcesErrors = 0;
	private int countConflictsDetected = 0;

	// FIXME - i do not think so it is good idea to handle it globally
	// should be passed by return object from sync methods...
	// also above fields
	private Map<String, String> errorMessages = new HashMap<String, String>();

	// FIXME - collection of mapDbs - we will use it for
	// closing dbs after sync
	private List<DB> mapDBs = new ArrayList<DB>();

	// denotes if user want to auto keep remote file
	// by default is FALSE
	// it is only available change to true, if direction == DOWNLOAD ONLY || BI
	private boolean autoKeepRemoteFile = true;
	// denotes if user want to auto keep local file
	// by default is FALSE
	// it is only available change to true, if direction == UPLOAD ONLY || BI
	private boolean autoKeepLocalFile = true;

	@Override
	public void execute(JobExecutionContext ctx) throws JobExecutionException {
		String keepRemoteData = ctx.getMergedJobDataMap().getString(JobEditor.AUTO_KEEP_REMOTE);
		if (keepRemoteData != null) {
			autoKeepRemoteFile = Boolean.valueOf(keepRemoteData);
		}
		String keepLocalData = ctx.getMergedJobDataMap().getString(JobEditor.AUTO_KEEP_LOCAL);
		if (keepLocalData != null) {
			autoKeepLocalFile = Boolean.valueOf(keepLocalData);
		}
		currentJobNodeID = ctx.getMergedJobDataMap().getString("node-id");
		if (ctx.getMergedJobDataMap().containsKey("clear-snapshots") && ctx.getMergedJobDataMap().getBooleanValue("clear-snapshots")) {
			clearSnapshots = true;
		} else {
			clearSnapshots = false;
		}
		if (ctx.getMergedJobDataMap().containsKey("local-monitoring") && ctx.getMergedJobDataMap().getBooleanValue("local-monitoring")) {
			localWatchOnly = true;
		} else {
			localWatchOnly = false;
		}
		long start = System.nanoTime();
		this.run();
		long elapsedTime = System.nanoTime() - start;
		Logger.getRootLogger().info("This pass took " + elapsedTime / 1000000 + " milliSeconds (Local : " + this.localWatchOnly + ")");
	}

	public void interrupt() {
		interruptRequired = true;
	}

	private MessageListener messageHandler;

	protected RestRequest getRequest() {
		RestRequest rest = new RestRequest();
		if (messageHandler == null) {

			messageHandler = new MessageListener() {
				private boolean interrupt = false;

				@Override
				public void sendMessage(int what, Object obj) {
					if (what == MessageListener.MESSAGE_WHAT_ERROR && obj instanceof String) {
						String mess;
						try {
							mess = getMessage((String) obj);
						} catch (Exception ex) {
							mess = (String) obj;
						}
						getCoreManager().notifyUser((String) obj, mess, SyncJob.this.currentJobNodeID, true);
					}
				}

				@Override
				public void requireInterrupt() {
					this.interrupt = true;
				}

				@Override
				public boolean isInterruptRequired() {
					return this.interrupt;
				}
			};

		}
		rest.setHandler(messageHandler);
		return rest;
	}

	public SyncJob() throws URISyntaxException, Exception {

		// nodeDao = getCoreManager().getNodeDao();

		EhcacheListFactory.getInstance().initCaches(8, DIFF_NODE_LISTS_SAVED_LIST, DIFF_NODE_LISTS_CREATED_LIST, LOAD_REMOTE_CHANGES_LIST,
				LOAD_LOCAL_CHANGES_LIST, INDEXATION_SNAPSHOT_LIST, REMOTE_SNAPSHOT_LIST, LOCAL_SNAPSHOT_LIST);

	}

	private void exitWithStatusAndNotify(int status, String titleId, String messageId) throws SQLException {
		getCoreManager().notifyUser(getMessage(titleId), getMessage(messageId), this.currentJobNodeID, (status == Node.NODE_STATUS_ERROR));
		exitWithStatus(status);
	}

	private boolean exitWithStatus(int status) throws SQLException {
		currentRepository.setStatus(status);
		nodeDao.update(currentRepository);
		getCoreManager().updateSynchroState(currentRepository, false);
		getCoreManager().releaseConnection();
		DaoManager.clearCache();
		nodeDao = null;
		syncChangeDao = null;
		syncLogDao = null;
		syncLogDetailsDao = null;
		propertyDao = null;
		return true;

	}

	protected void updateRunningStatus(Integer status, boolean running) throws SQLException {
		if (currentRepository != null && propertyDao != null) {
			currentRepository.setProperty("sync_running_status", status.toString(), propertyDao);
			getCoreManager().updateSynchroState(currentRepository, true);
		}
	}

	protected void updateRunningStatus(Integer status) throws SQLException {
		updateRunningStatus(status, true);
	}

	/**
	 * Get CoreManager for all operations
	 * 
	 * @return
	 */
	protected CoreManager getCoreManager() {
		return CoreManager.getInstance();
	}

	/**
	 * Return message string
	 * 
	 * @param key
	 * @return
	 */
	protected String getMessage(String key) {
		return getCoreManager().messages.getString(key);
	}

	public void run() {

		try {
			AjxpHttpClient.clearCookiesStatic();
			// instantiate the daos
			ConnectionSource connectionSource = getCoreManager().getConnection();

			nodeDao = DaoManager.createDao(connectionSource, Node.class);
			syncChangeDao = DaoManager.createDao(connectionSource, SyncChange.class);
			syncLogDao = DaoManager.createDao(connectionSource, SyncLog.class);
			propertyDao = DaoManager.createDao(connectionSource, Property.class);

			syncLogDetailsDao = DaoManager.createDao(connectionSource, SyncLogDetails.class);

			currentRepository = getCoreManager().getSynchroNode(currentJobNodeID, nodeDao);
			if (currentRepository == null) {
				throw new Exception("The database returned an empty node.");
			}

			getCoreManager().updateSynchroState(currentRepository, (localWatchOnly ? false : true));

			currentRepository.setStatus(Node.NODE_STATUS_LOADING);
			try {
				updateRunningStatus(RUNNING_STATUS_INITIALIZING, (localWatchOnly ? false : true));
			} catch (SQLException sE) {
				Thread.sleep(100);
				updateRunningStatus(RUNNING_STATUS_INITIALIZING, (localWatchOnly ? false : true));
			}
			nodeDao.update(currentRepository);
			Server s = new Server(currentRepository.getParent());
			RestStateHolder.getInstance().setServer(s);
			RestStateHolder.getInstance().setRepository(currentRepository);
			AjxpAPI.getInstance().setServer(s);
			currentLocalFolder = new File(currentRepository.getPropertyValue("target_folder"));
			direction = currentRepository.getPropertyValue("synchro_direction");

			// if(!localWatchOnly) {
			// getCoreManager().notifyUser(getMessage("job_running"),
			// "Synchronizing " + s.getUrl());
			// }
			updateRunningStatus(RUNNING_STATUS_PREVIOUS_CHANGES, (localWatchOnly ? false : true));
			final List<SyncChange> previouslyRemaining = syncChangeDao.queryForEq("jobId", currentJobNodeID);
			// Map<String, Object[]> previousChanges = new TreeMap<String,
			// Object[]>();
			Map<String, Object[]> previousChanges = createMapDBFile("previous");
			// by default - do nothing
			int action = SyncJob.TASK_DO_NOTHING;
			// check if user want to keep remote automatically
			if (autoKeepRemoteFile) {
				action = SyncJob.TASK_SOLVE_KEEP_THEIR;
			} else if (autoKeepLocalFile) {
				action = SyncJob.TASK_SOLVE_KEEP_MINE;
			}

			boolean unsolvedConflicts = SyncChange.syncChangesToTreeMap(previouslyRemaining, previousChanges, action);
			Map<String, Object[]> again = null;
			if (!localWatchOnly && unsolvedConflicts) {
				this.exitWithStatusAndNotify(Node.NODE_STATUS_ERROR, "job_blocking_conflicts_title", "job_blocking_conflicts");
				return;
			}

			updateRunningStatus(RUNNING_STATUS_LOCAL_CHANGES, (localWatchOnly ? false : true));
			if (clearSnapshots) {
				this.clearSnapshot("local_snapshot");
				this.clearSnapshot("remote_snapshot");
			}
			List<Node> localSnapshot = EhcacheListFactory.getInstance().getList(LOCAL_SNAPSHOT_LIST);
			List<Node> remoteSnapshot = EhcacheListFactory.getInstance().getList(REMOTE_SNAPSHOT_LIST);
			Node localRootNode = loadRootAndSnapshot("local_snapshot", localSnapshot, currentLocalFolder);
			Map<String, Object[]> localDiff = loadLocalChanges(localSnapshot);

			if (unsolvedConflicts) {
				this.exitWithStatusAndNotify(Node.NODE_STATUS_ERROR, "job_blocking_conflicts_title", "job_blocking_conflicts");
				return;
			}
			if (localWatchOnly && localDiff.size() == 0 && previousChanges.size() == 0) {
				this.exitWithStatus(Node.NODE_STATUS_LOADED);
				return;
			}

			// If we are here, then we must have detected some changes
			updateRunningStatus(RUNNING_STATUS_TESTING_CONNEXION);
			if (!testConnexion()) {
				return;
			}

			updateRunningStatus(RUNNING_STATUS_REMOTE_CHANGES);
			Node remoteRootNode = loadRootAndSnapshot("remote_snapshot", remoteSnapshot, null);
			Map<String, Object[]> remoteDiff = loadRemoteChanges(remoteSnapshot);

			if (previousChanges.size() > 0) {
				updateRunningStatus(RUNNING_STATUS_PREVIOUS_CHANGES);
				Logger.getRootLogger().debug("Getting previous tasks");
				again = applyChanges(previousChanges);
				if (previouslyRemaining.size() > 999) {
					syncChangeDao.callBatchTasks(new Callable<Void>() {
						public Void call() throws Exception {
							for (int i = 0; i < previouslyRemaining.size(); i++) {
								syncChangeDao.delete(previouslyRemaining.get(i));
							}
							return null;
						}
					});

				} else {
					syncChangeDao.delete(previouslyRemaining);
				}
				this.clearSnapshot("remaining_nodes");
			}
			if (this.interruptRequired) {
				throw new InterruptedException();
			}
			updateRunningStatus(RUNNING_STATUS_COMPARING_CHANGES);
			Map<String, Object[]> changes = mergeChanges(remoteDiff, localDiff);
			updateRunningStatus(RUNNING_STATUS_APPLY_CHANGES);
			Map<String, Object[]> remainingChanges = applyChanges(changes);
			if (again != null && again.size() > 0) {
				remainingChanges.putAll(again);
			}
			if (remainingChanges.size() > 0) {
				List<SyncChange> c = SyncChange.MapToSyncChanges(remainingChanges, currentJobNodeID);
				Node remainingRoot = loadRootAndSnapshot("remaining_nodes", null, null);
				for (int i = 0; i < c.size(); i++) {
					SyncChangeValue cv = c.get(i).getChangeValue();
					Node changeNode = cv.n;
					changeNode.setParent(remainingRoot);
					if (changeNode.id == 0 || !nodeDao.idExists(changeNode.id + "")) { // Not
																						// yet
																						// created!
						nodeDao.create(changeNode);
						Map<String, String> pValues = new HashMap<String, String>();
						for (Property p : changeNode.properties) {
							pValues.put(p.getName(), p.getValue());
						}
						propertyDao.delete(changeNode.properties);
						Iterator<Map.Entry<String, String>> it = pValues.entrySet().iterator();
						while (it.hasNext()) {
							Map.Entry<String, String> ent = it.next();
							changeNode.addProperty(ent.getKey(), ent.getValue());
						}
						c.get(i).setChangeValue(cv);
					} else {
						nodeDao.update(changeNode);
					}
					syncChangeDao.create(c.get(i));
				}
			}
			updateRunningStatus(RUNNING_STATUS_CLEANING);
			takeLocalSnapshot(localRootNode, null, true, localSnapshot);
			clearSnapshot("local_tmp");

			takeRemoteSnapshot(remoteRootNode, null, true);
			clearSnapshot("remote_tmp");

			cleanDB();

			// INDICATES THAT THE JOB WAS CORRECTLY SHUTDOWN
			currentRepository.setStatus(Node.NODE_STATUS_LOADED);
			currentRepository.setLastModified(new Date());
			nodeDao.update(currentRepository);

			SyncLog sl = new SyncLog();
			String status;
			String summary = "";
			if (countConflictsDetected > 0) {
				status = SyncLog.LOG_STATUS_CONFLICTS;
				summary = getMessage("job_status_conflicts").replace("%d", countConflictsDetected + "");
			} else if (countResourcesErrors > 0) {
				status = SyncLog.LOG_STATUS_ERRORS;
				summary = getMessage("job_status_errors").replace("%d", countResourcesErrors + "");
			} else {
				if (countResourcesInterrupted > 0)
					status = SyncLog.LOG_STATUS_INTERRUPT;
				else
					status = SyncLog.LOG_STATUS_SUCCESS;
				if (countFilesDownloaded > 0) {
					summary = getMessage("job_status_downloads").replace("%d", countFilesDownloaded + "");
				}
				if (countFilesUploaded > 0) {
					summary += getMessage("job_status_uploads").replace("%d", countFilesUploaded + "");
				}
				if (countResourcesSynchronized > 0) {
					summary += getMessage("job_status_resources").replace("%d", countResourcesSynchronized + "");
				}
				if (summary.equals("")) {
					summary = getMessage("job_status_nothing");
				}
			}
			sl.jobDate = (new Date()).getTime();
			sl.jobStatus = status;
			sl.jobSummary = summary;
			sl.synchroNode = currentRepository;
			syncLogDao.create(sl);

			// if there are any errors we just save them connected with actual
			// SyncLog
			Iterator<Entry<String, String>> errorMessagesIterator = errorMessages.entrySet().iterator();
			while (errorMessagesIterator.hasNext()) {
				Entry<String, String> entry = errorMessagesIterator.next();
				SyncLogDetails details = new SyncLogDetails();
				details.setFileName(entry.getKey());
				details.setMessage(entry.getValue());
				details.setParentLog(sl);

				syncLogDetailsDao.create(details);
			}
			// clear error messages for new synchronisation
			errorMessages.clear();

			getCoreManager().updateSynchroState(currentRepository, false);
			getCoreManager().releaseConnection();
			DaoManager.clearCache();

		} catch (InterruptedException ie) {

			getCoreManager().notifyUser("Stopping", "Last synchro was interrupted on user demand", this.currentJobNodeID);
			try {
				this.exitWithStatus(Node.NODE_STATUS_FRESH);
			} catch (SQLException e) {
			}

		} catch (Exception e) {

			e.printStackTrace();
			String message = e.getMessage();
			if (message == null && e.getCause() != null)
				message = e.getCause().getMessage();
			getCoreManager().notifyUser("Error", "An error occured during synchronization: " + message, this.currentJobNodeID, true);
			if (currentRepository != null) {
				getCoreManager().updateSynchroState(currentRepository, false);
			}
			getCoreManager().releaseConnection();
			DaoManager.clearCache();
		} finally {
			// synchronisation is finished (or not) - we need to close all
			// mapDBs
			for (DB db : mapDBs) {
				db.close();
			}

		}
	}

	protected boolean testConnexion() throws SQLException {
		RestRequest rest = this.getRequest();
		rest.throwAuthExceptions = true;
		try {
			rest.getStringContent(AjxpAPI.getInstance().getPingUri());
		} catch (Exception e) {
			if (e.getMessage().equals(RestRequest.AUTH_ERROR_LOCKEDOUT)) {
				this.exitWithStatusAndNotify(Node.NODE_STATUS_ERROR, "auth_login_failed", "auth_locked_out_msg");
			} else if (e.getMessage().equals(RestRequest.AUTH_ERROR_LOGIN_FAILED)) {
				this.exitWithStatusAndNotify(Node.NODE_STATUS_ERROR, "auth_login_failed", "auth_login_failed_msg");
			} else {
				this.exitWithStatusAndNotify(Node.NODE_STATUS_LOADED, "no_internet_title", "no_internet_msg");
			}
			rest.release();
			return false;
		}
		rest.release();
		return true;
	}

	protected String currentLocalSnapId;
	protected String currentRemoteSnapId;

	protected Map<String, Node> findNodesInTmpSnapshot(String path) throws SQLException {

		Map<String, Node> result = new HashMap<String, Node>();
		Map<String, Object> search = new HashMap<String, Object>();
		// LOCAL
		if (currentLocalSnapId == null) {
			Node loc = loadRootAndSnapshot("local_tmp", null, null);
			currentLocalSnapId = String.valueOf(loc.id);
		}
		search.put("resourceType", new SelectArg("entry"));
		search.put("parent_id", new SelectArg(currentLocalSnapId));
		search.put("path", new SelectArg(path));

		List<Node> l;
		l = nodeDao.queryForFieldValuesArgs(search);
		if (l.size() > 0) {
			result.put("local", l.get(0));
		}
		// REMOTE
		if (currentRemoteSnapId == null) {
			Node loc = loadRootAndSnapshot("remote_tmp", null, null);
			currentRemoteSnapId = String.valueOf(loc.id);
		}
		// search.put("resourceType", "entry");
		search.put("parent_id", new SelectArg(currentRemoteSnapId));
		List<Node> l2;
		l2 = nodeDao.queryForFieldValuesArgs(search);
		if (l2.size() > 0) {
			result.put("remote", l2.get(0));
		}
		return result;

	}

	protected Map<String, Object[]> applyChanges(Map<String, Object[]> changes) {
		Iterator<Map.Entry<String, Object[]>> it = changes.entrySet().iterator();
		// Map<String, Object[]> notApplied = new TreeMap<String, Object[]>();
		// // Make sure to apply those one at the end
		// Map<String, Object[]> moves = new TreeMap<String, Object[]>();
		// Map<String, Object[]> deletes = new TreeMap<String, Object[]>();
		Map<String, Object[]> notApplied = createMapDBFile("notApplied");
		// Make sure to apply those one at the end
		Map<String, Object[]> moves = createMapDBFile("moves");
		Map<String, Object[]> deletes = createMapDBFile("deletes");
		RestRequest rest = this.getRequest();
		while (it.hasNext()) {
			Map.Entry<String, Object[]> entry = it.next();
			String k = entry.getKey();
			Object[] value = entry.getValue().clone();
			Integer v = (Integer) value[0];
			Node n = (Node) value[1];
			if (n == null)
				continue;
			if (this.interruptRequired) {
				value[2] = STATUS_INTERRUPTED;
				notApplied.put(k, value);
				continue;
			}
			// Thread.sleep(2000);
			try {
				Map<String, Node> tmpNodes = findNodesInTmpSnapshot(k);
				if (n.isLeaf() && value[2].equals(STATUS_CONFLICT_SOLVED)) {
					if (v.equals(TASK_SOLVE_KEEP_MINE)) {
						v = TASK_REMOTE_PUT_CONTENT;
					} else if (v.equals(TASK_SOLVE_KEEP_THEIR)) {
						v = TASK_LOCAL_GET_CONTENT;
					} else if (v.equals(TASK_SOLVE_KEEP_BOTH)) {
						// COPY LOCAL FILE AND GET REMOTE COPY
						File origFile = new File(currentLocalFolder, k);
						File targetFile = new File(currentLocalFolder, k + ".mine");
						InputStream in = new FileInputStream(origFile);
						OutputStream out = new FileOutputStream(targetFile);
						byte[] buf = new byte[1024];
						int len;
						while ((len = in.read(buf)) > 0) {
							out.write(buf, 0, len);
						}
						in.close();
						out.close();
						v = TASK_LOCAL_GET_CONTENT;
					}
				}

				if (v == TASK_LOCAL_GET_CONTENT) {

					if (direction.equals("up"))
						continue;
					if (tmpNodes.get("local") != null && tmpNodes.get("remote") != null) {
						if (tmpNodes.get("local").getPropertyValue("md5") != null
								&& tmpNodes.get("local").getPropertyValue("md5").equals(tmpNodes.get("remote").getPropertyValue("md5"))) {
							continue;
						}
					}
					Node node = new Node(Node.NODE_TYPE_ENTRY, "", null);
					node.setPath(k);
					File targetFile = new File(currentLocalFolder, k);
					this.logChange(getMessage("job_log_downloading"), k);
					try {
						this.updateNode(node, targetFile, n);
					} catch (IllegalStateException e) {
						if (this.statRemoteFile(node, "file", rest) == null)
							continue;
						else
							throw e;
					}
					if (!targetFile.exists() || targetFile.length() != Integer.parseInt(n.getPropertyValue("bytesize"))) {
						JSONObject obj = this.statRemoteFile(node, "file", rest);
						if (obj == null || obj.get("size").equals(0))
							continue;
						else
							throw new Exception("Error while downloading file from server");
					}
					if (n != null) {
						targetFile.setLastModified(n.getLastModified().getTime());
					}
					countFilesDownloaded++;

				} else if (v == TASK_LOCAL_MKDIR) {

					if (direction.equals("up"))
						continue;
					File f = new File(currentLocalFolder, k);
					if (!f.exists()) {
						this.logChange(getMessage("job_log_mkdir"), k);
						boolean res = f.mkdirs();
						if (!res) {
							throw new Exception("Error while creating local folder");
						}
						countResourcesSynchronized++;
					}

				} else if (v == TASK_LOCAL_REMOVE) {

					if (direction.equals("up"))
						continue;
					deletes.put(k, value);

				} else if (v == TASK_REMOTE_REMOVE) {

					if (direction.equals("down"))
						continue;
					deletes.put(k, value);

				} else if (v == TASK_REMOTE_MKDIR) {

					if (direction.equals("down"))
						continue;
					this.logChange(getMessage("job_log_mkdir_remote"), k);
					Node currentDirectory = new Node(Node.NODE_TYPE_ENTRY, "", null);
					int lastSlash = k.lastIndexOf("/");
					currentDirectory.setPath(k.substring(0, lastSlash));
					RestStateHolder.getInstance().setDirectory(currentDirectory);
					rest.getStatusCodeForRequest(AjxpAPI.getInstance().getMkdirUri(k.substring(lastSlash + 1)));
					JSONObject object = rest.getJSonContent(AjxpAPI.getInstance().getStatUri(k));
					if (!object.has("mtime")) {
						throw new Exception("Could not create remote folder");
					}
					countResourcesSynchronized++;

				} else if (v == TASK_REMOTE_PUT_CONTENT) {

					if (direction.equals("down"))
						continue;
					if (tmpNodes.get("local") != null && tmpNodes.get("remote") != null) {
						if (tmpNodes.get("local").getPropertyValue("md5") != null
								&& tmpNodes.get("local").getPropertyValue("md5").equals(tmpNodes.get("remote").getPropertyValue("md5"))) {
							continue;
						}
					}
					this.logChange(getMessage("job_log_uploading"), k);
					Node currentDirectory = new Node(Node.NODE_TYPE_ENTRY, "", null);
					int lastSlash = k.lastIndexOf("/");
					currentDirectory.setPath(k.substring(0, lastSlash));
					RestStateHolder.getInstance().setDirectory(currentDirectory);
					File sourceFile = new File(currentLocalFolder, k);
					if (!sourceFile.exists()) {
						// Silently ignore, or it will continously try to
						// reupload it.
						continue;
					}
					boolean checked = false;
					if (sourceFile.length() == 0) {
						rest.getStringContent(AjxpAPI.getInstance().getMkfileUri(sourceFile.getName()));
					} else {
						checked = this.synchronousUP(currentDirectory, sourceFile, n);
					}
					if (!checked) {
						JSONObject object = rest.getJSonContent(AjxpAPI.getInstance().getStatUri(n.getPath(true)));
						if (!object.has("size") || object.getInt("size") != (int) sourceFile.length()) {
							throw new Exception("Could not upload file to the server");
						}
					}
					countFilesUploaded++;

				} else if (v == TASK_DO_NOTHING && value[2] == STATUS_CONFLICT) {

					// Recheck that it's a real conflict?

					this.logChange(getMessage("job_log_conflict"), k);
					notApplied.put(k, value);
					countConflictsDetected++;

				} else if (v == TASK_LOCAL_MOVE_FILE || v == TASK_REMOTE_MOVE_FILE) {
					if (v == TASK_LOCAL_MOVE_FILE && direction.equals("up"))
						continue;
					if (v == TASK_REMOTE_MOVE_FILE && direction.equals("down"))
						continue;
					moves.put(k, value);
				}
			} catch (FileNotFoundException ex) {
				addSyncDetailMessage(k, ex);
				ex.printStackTrace();
				countResourcesErrors++;
				// Do not put in the notApplied again, otherwise it will
				// indefinitely happen.
			} catch (Exception e) {
				addSyncDetailMessage(k, e);
				Logger.getRootLogger().error("Synchro", e);
				countResourcesErrors++;
				value[2] = STATUS_ERROR;
				notApplied.put(k, value);
			}
		}

		// APPLY MOVES
		Iterator<Map.Entry<String, Object[]>> mIt = moves.entrySet().iterator();
		while (mIt.hasNext()) {
			Map.Entry<String, Object[]> entry = mIt.next();
			String k = entry.getKey();
			Object[] value = entry.getValue().clone();
			Integer v = (Integer) value[0];
			Node n = (Node) value[1];
			if (this.interruptRequired) {
				value[2] = STATUS_INTERRUPTED;
				notApplied.put(k, value);
				continue;
			}
			try {
				if (v == TASK_LOCAL_MOVE_FILE && value.length == 4) {

					this.logChange("Moving resource locally", k);
					Node dest = (Node) value[3];
					File origFile = new File(currentLocalFolder, n.getPath());
					if (!origFile.exists()) {
						// Cannot move a non-existing file! Download instead!
						value[0] = TASK_LOCAL_GET_CONTENT;
						value[1] = dest;
						value[2] = STATUS_TODO;
						notApplied.put(dest.getPath(true), value);
						continue;
					}
					File destFile = new File(currentLocalFolder, dest.getPath());
					origFile.renameTo(destFile);
					if (!destFile.exists()) {
						throw new Exception("Error while creating " + dest.getPath());
					}
					countResourcesSynchronized++;

				} else if (v == TASK_REMOTE_MOVE_FILE && value.length == 4) {

					this.logChange("Moving resource remotely", k);
					Node dest = (Node) value[3];
					JSONObject object = rest.getJSonContent(AjxpAPI.getInstance().getStatUri(n.getPath()));
					if (!object.has("size")) {
						value[0] = TASK_REMOTE_PUT_CONTENT;
						value[1] = dest;
						value[2] = STATUS_TODO;
						notApplied.put(dest.getPath(true), value);
						continue;
					}
					rest.getStatusCodeForRequest(AjxpAPI.getInstance().getRenameUri(n, dest));
					object = rest.getJSonContent(AjxpAPI.getInstance().getStatUri(dest.getPath()));
					if (!object.has("size")) {
						throw new Exception("Could not move remote file to " + dest.getPath());
					}
					countResourcesSynchronized++;

				}
			} catch (FileNotFoundException ex) {
				addSyncDetailMessage(k, ex);
				ex.printStackTrace();
				countResourcesErrors++;
				// Do not put in the notApplied again, otherwise it will
				// indefinitely happen.
			} catch (Exception e) {
				addSyncDetailMessage(k, e);
				Logger.getRootLogger().error("Synchro", e);
				countResourcesErrors++;
				value[2] = STATUS_ERROR;
				notApplied.put(k, value);
			}
		}

		// APPLY DELETES
		Iterator<Map.Entry<String, Object[]>> dIt = deletes.entrySet().iterator();
		while (dIt.hasNext()) {
			Map.Entry<String, Object[]> entry = dIt.next();
			String k = entry.getKey();
			Object[] value = entry.getValue().clone();
			Integer v = (Integer) value[0];
			// Node n = (Node)value[1];
			if (this.interruptRequired) {
				value[2] = STATUS_INTERRUPTED;
				notApplied.put(k, value);
				continue;
			}
			try {

				if (v == TASK_LOCAL_REMOVE) {

					this.logChange(getMessage("job_log_rmlocal"), k);
					File f = new File(currentLocalFolder, k);
					if (f.exists()) {
						boolean res = f.delete();
						if (!res) {
							throw new Exception("Error while removing local resource");
						}
						countResourcesSynchronized++;
					}

				} else if (v == TASK_REMOTE_REMOVE) {

					this.logChange(getMessage("job_log_rmremote"), k);
					Node currentDirectory = new Node(Node.NODE_TYPE_ENTRY, "", null);
					int lastSlash = k.lastIndexOf("/");
					currentDirectory.setPath(k.substring(0, lastSlash));
					RestStateHolder.getInstance().setDirectory(currentDirectory);
					rest.getStatusCodeForRequest(AjxpAPI.getInstance().getDeleteUri(k));
					JSONObject object = rest.getJSonContent(AjxpAPI.getInstance().getStatUri(k));
					if (object.has("mtime")) { // Still exists, should be empty!
						throw new Exception("Could not remove the resource from the server");
					}
					countResourcesSynchronized++;
				}

			} catch (FileNotFoundException ex) {
				addSyncDetailMessage(k, ex);
				ex.printStackTrace();
				countResourcesErrors++;
				// Do not put in the notApplied again, otherwise it will
				// indefinitely happen.
			} catch (Exception e) {
				addSyncDetailMessage(k, e);
				Logger.getRootLogger().error("Synchro", e);
				countResourcesErrors++;
				value[2] = STATUS_ERROR;
				notApplied.put(k, value);
			}
		}
		rest.release();
		return notApplied;
	}

	/**
	 * Add detailed message to error messages collection executed during any
	 * error occurred which increases error count will handle details for
	 * SyncLog
	 * 
	 * @param fileName
	 * @param ex
	 */
	private void addSyncDetailMessage(String fileName, Throwable ex) {
		if (!errorMessages.containsKey(fileName)) {
			errorMessages.put(fileName, ex.getMessage());
		}
	}

	protected JSONObject statRemoteFile(Node n, String type, RestRequest rest) {
		try {
			JSONObject object = rest.getJSonContent(AjxpAPI.getInstance().getStatUri(n.getPath()));
			if (type == "file" && object.has("size")) {
				return object;
			}
			if (type == "dir" && object.has("mtime")) {
				return object;
			}
		} catch (URISyntaxException e) {
			Logger.getRootLogger().error("Synchro", e);
		} catch (Exception e) {
			Logger.getRootLogger().error("Synchro", e);
		}
		return null;
	}

	protected void logChange(String action, String path) {
		getCoreManager().notifyUser(getMessage("job_log_balloontitle"), action + " : " + path, this.currentJobNodeID);
	}

	protected boolean ignoreTreeConflict(Integer remoteChange, Integer localChange, Node remoteNode, Node localNode) {
		if (remoteChange != localChange) {
			return false;
		}
		if (localChange == NODE_CHANGE_STATUS_DIR_CREATED || localChange == NODE_CHANGE_STATUS_DIR_DELETED
				|| localChange == NODE_CHANGE_STATUS_FILE_DELETED) {
			return true;
		} else if (remoteNode.getPropertyValue("md5") != null) {
			// Get local node md5
			this.updateLocalMD5(localNode);
			String localMd5 = localNode.getPropertyValue("md5");
			// File f = new File(currentLocalFolder, localNode.getPath(true));
			// String localMd5 = SyncJob.computeMD5(f);
			if (remoteNode.getPropertyValue("md5").equals(localMd5)) {
				return true;
			}
			Logger.getRootLogger().debug("MD5 differ " + remoteNode.getPropertyValue("md5") + " - " + localMd5);
		}
		return false;
	}

	protected Map<String, Object[]> mergeChanges(Map<String, Object[]> remoteDiff, Map<String, Object[]> localDiff) {
		// Map<String, Object[]> changes = new TreeMap<String, Object[]>();
		Map<String, Object[]> changes = createMapDBFile("merge");
		Iterator<Map.Entry<String, Object[]>> it = remoteDiff.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, Object[]> entry = it.next();
			String k = entry.getKey();
			Object[] value = entry.getValue();
			Integer v = (Integer) value[0];

			if (localDiff.containsKey(k)) {
				Object[] localValue = localDiff.get(k);
				Integer localChange = (Integer) localValue[0];
				if (ignoreTreeConflict(v, localChange, (Node) value[1], (Node) localValue[1])) {
					localDiff.remove(k);
					continue;
				} else {
					// check if user want to have auto keep remote
					if (autoKeepRemoteFile) {
						value[0] = TASK_SOLVE_KEEP_THEIR;
						value[2] = STATUS_CONFLICT_SOLVED;
					} else if (autoKeepLocalFile) {
						value[0] = TASK_SOLVE_KEEP_MINE;
						value[2] = STATUS_CONFLICT_SOLVED;
					} else {
						value[0] = TASK_DO_NOTHING;
						value[2] = STATUS_CONFLICT;
					}
					localDiff.remove(k);
					changes.put(k, value);
				}
				continue;
			}
			if (v == NODE_CHANGE_STATUS_FILE_CREATED || v == NODE_CHANGE_STATUS_MODIFIED) {
				if (direction.equals("up")) {
					if (v == NODE_CHANGE_STATUS_MODIFIED)
						localDiff.put(k, value);
				} else {
					value[0] = TASK_LOCAL_GET_CONTENT;
					changes.put(k, value);
				}
			} else if (v == NODE_CHANGE_STATUS_FILE_DELETED || v == NODE_CHANGE_STATUS_DIR_DELETED) {
				if (direction.equals("up")) {
					if (v == NODE_CHANGE_STATUS_FILE_DELETED)
						value[0] = NODE_CHANGE_STATUS_MODIFIED;
					else
						value[0] = NODE_CHANGE_STATUS_DIR_CREATED;
					localDiff.put(k, value);
				} else {
					value[0] = TASK_LOCAL_REMOVE;
					changes.put(k, value);
				}
			} else if (v == NODE_CHANGE_STATUS_DIR_CREATED && !direction.equals("up")) {
				value[0] = TASK_LOCAL_MKDIR;
				changes.put(k, value);
			} else if (v == NODE_CHANGE_STATUS_FILE_MOVED && !direction.equals("up")) {
				value[0] = TASK_LOCAL_MOVE_FILE;
				changes.put(k, value);
			}
		}
		it = localDiff.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, Object[]> entry = it.next();
			String k = entry.getKey();
			Object[] value = entry.getValue();
			Integer v = (Integer) value[0];
			if (v == NODE_CHANGE_STATUS_FILE_CREATED || v == NODE_CHANGE_STATUS_MODIFIED) {
				if (direction.equals("down")) {
					if (v == NODE_CHANGE_STATUS_FILE_CREATED)
						value[0] = TASK_LOCAL_REMOVE;
					else
						value[0] = TASK_LOCAL_GET_CONTENT;
				} else {
					value[0] = TASK_REMOTE_PUT_CONTENT;
				}
				changes.put(k, value);
			} else if (v == NODE_CHANGE_STATUS_FILE_DELETED || v == NODE_CHANGE_STATUS_DIR_DELETED) {
				if (direction.equals("down")) {
					if (v == NODE_CHANGE_STATUS_FILE_DELETED)
						value[0] = TASK_LOCAL_GET_CONTENT;
					else
						value[0] = TASK_LOCAL_MKDIR;
				} else {
					value[0] = TASK_REMOTE_REMOVE;
				}
				changes.put(k, value);
			} else if (v == NODE_CHANGE_STATUS_DIR_CREATED) {
				if (direction.equals("down")) {
					value[0] = TASK_LOCAL_REMOVE;
				} else {
					value[0] = TASK_REMOTE_MKDIR;
				}
				changes.put(k, value);
			} else if (v == NODE_CHANGE_STATUS_FILE_MOVED && !direction.equals("down")) {
				value[0] = TASK_REMOTE_MOVE_FILE;
				changes.put(k, value);
			}

		}
		return changes;
	}

	protected Node loadRootAndSnapshot(String type, final List<Node> snapshot, File localFolder) throws SQLException {

		Map<String, Object> search = new HashMap<String, Object>();
		search.put("resourceType", type);
		search.put("parent_id", currentJobNodeID);
		List<Node> l = nodeDao.queryForFieldValues(search);
		final Node root;
		if (l.size() > 0) {
			root = l.get(0);
			if (snapshot != null) {
				CloseableIterator<Node> it = root.children.iteratorThrow();
				while (it.hasNext()) {
					snapshot.add(it.next());
				}
			}
		} else {
			root = new Node(type, "", currentRepository);
			root.properties = nodeDao.getEmptyForeignCollection("properties");
			if (localFolder != null)
				root.setPath(localFolder.getAbsolutePath());
			else
				root.setPath("");
			nodeDao.create(root);
		}

		return root;
	}

	protected void clearSnapshot(String type) throws SQLException {
		Map<String, Object> search = new HashMap<String, Object>();
		search.put("resourceType", type);
		search.put("parent_id", currentJobNodeID);
		List<Node> l = nodeDao.queryForFieldValues(search);
		// List<Node> l = nodeDao.queryForEq("resourceType", type);
		Node root;
		nodeDao.executeRaw("PRAGMA recursive_triggers = TRUE;");
		if (l.size() > 0) {
			root = l.get(0);
			nodeDao.delete(root);
		}
		cleanDB();
	}

	protected void cleanDB() throws SQLException {
		// unlinked properties may have not been deleted
		propertyDao.executeRaw("DELETE FROM b WHERE node_id=0;");
		propertyDao.executeRaw("VACUUM;");
	}

	protected void emptyNodeChildren(final Node rootNode, boolean insideBatchTask) throws Exception {
		if (rootNode.children == null || rootNode.children.size() == 0)
			return;
		final int SQLITE_LIMIT = 999;
		if (insideBatchTask) {
			nodeDao.executeRaw("PRAGMA recursive_triggers = TRUE;");
			int test = rootNode.children.size();
			if (test > SQLITE_LIMIT) {
				Iterator<Node> i = rootNode.children.iterator();
				while (i.hasNext()) {
					nodeDao.delete(i.next());
				}
			} else {
				nodeDao.delete(rootNode.children);
			}
		} else {
			nodeDao.callBatchTasks(new Callable<Void>() {
				public Void call() throws Exception {
					int test = rootNode.children.size();
					if (test > SQLITE_LIMIT) {
						Iterator<Node> i = rootNode.children.iterator();
						while (i.hasNext()) {
							nodeDao.delete(i.next());
						}
					} else {
						nodeDao.delete(rootNode.children);
					}
					return null;
				}
			});
		}

	}

	protected void takeLocalSnapshot(final Node rootNode, final List<Node> accumulator, final boolean save,
			final List<Node> previousSnapshot) throws Exception {

		nodeDao.callBatchTasks(new Callable<Void>() {
			public Void call() throws Exception {
				if (save) {
					emptyNodeChildren(rootNode, true);
				}
				listDirRecursive(currentLocalFolder, rootNode, accumulator, save, previousSnapshot);
				return null;
			}
		});
		if (save) {
			rootNode.setStatus(Node.NODE_STATUS_LOADED);
			nodeDao.update(rootNode);
		}
	}

	protected Map<String, Object[]> loadLocalChanges(List<Node> snapshot) throws Exception {

		List<Node> previousSnapshot;
		List<Node> indexationSnap = EhcacheListFactory.getInstance().getList(INDEXATION_SNAPSHOT_LIST);
		Node index = loadRootAndSnapshot("local_tmp", indexationSnap, currentLocalFolder);
		final Node root;
		final List<Node> list = EhcacheListFactory.getInstance().getList(LOAD_LOCAL_CHANGES_LIST);
		if (indexationSnap.size() > 0) {
			index.setResourceType("previous_indexation");
			nodeDao.update(index);
			// FIXME - can we copy Ehcache list that way?
			previousSnapshot = indexationSnap;
			root = loadRootAndSnapshot("local_tmp", null, currentLocalFolder);
		} else {
			// FIXME - can we copy Ehcache list that way?
			previousSnapshot = snapshot;
			root = index;
		}

		takeLocalSnapshot(root, list, true, previousSnapshot);

		Map<String, Object[]> diff = this.diffNodeLists(list, snapshot, "local");

		clearSnapshot("previous_indexation");

		return diff;

	}

	protected String normalizeUnicode(String str) {
		Normalizer.Form form = Normalizer.Form.NFD;
		if (!Normalizer.isNormalized(str, form)) {
			return Normalizer.normalize(str, form);
		}
		return str;
	}

	protected void listDirRecursive(File directory, Node root, List<Node> accumulator, boolean save, List<Node> previousSnapshot)
			throws InterruptedException, SQLException {

		if (this.interruptRequired) {
			throw new InterruptedException("Interrupt required");
		}
		// Logger.getRootLogger().info("Searching "+directory.getAbsolutePath());
		File[] children = directory.listFiles();
		String[] start = getCoreManager().EXCLUDED_FILES_START;
		String[] end = getCoreManager().EXCLUDED_FILES_END;
		if (children != null) {
			for (int i = 0; i < children.length; i++) {
				boolean ignore = false;
				for (int j = 0; j < start.length; j++) {
					if (children[i].getName().startsWith(start[j])) {
						ignore = true;
						break;
					}
				}
				if (ignore)
					continue;
				for (int j = 0; j < end.length; j++) {
					if (children[i].getName().endsWith(end[j])) {
						ignore = true;
						break;
					}
				}
				if (ignore)
					continue;
				Node newNode = new Node(Node.NODE_TYPE_ENTRY, children[i].getName(), root);
				if (save)
					nodeDao.create(newNode);
				String p = children[i].getAbsolutePath().substring(root.getPath(true).length()).replace("\\", "/");
				newNode.setPath(p);
				newNode.properties = nodeDao.getEmptyForeignCollection("properties");
				newNode.setLastModified(new Date(children[i].lastModified()));
				if (children[i].isDirectory()) {
					listDirRecursive(children[i], root, accumulator, save, previousSnapshot);
				} else {
					newNode.addProperty("bytesize", String.valueOf(children[i].length()));
					String md5 = null;

					if (previousSnapshot != null) {
						// Logger.getRootLogger().info("Searching node in previous snapshot for "
						// + p);
						Iterator<Node> it = previousSnapshot.iterator();
						while (it.hasNext()) {
							Node previous = it.next();
							if (previous.getPath(true).equals(p)) {
								if (previous.getLastModified().equals(newNode.getLastModified())
										&& previous.getPropertyValue("bytesize").equals(newNode.getPropertyValue("bytesize"))) {
									md5 = previous.getPropertyValue("md5");
									// Logger.getRootLogger().info("-- Getting md5 from previous snapshot");
								}
								break;
							}
						}
					}
					if (md5 == null) {
						// Logger.getRootLogger().info("-- Computing new md5");
						getCoreManager().notifyUser("Indexation", "Indexing " + p, currentJobNodeID);
						md5 = computeMD5(children[i]);
					}
					newNode.addProperty("md5", md5);
					newNode.setLeaf();
				}
				if (save) {
					nodeDao.update(newNode);
				}
				if (accumulator != null) {
					accumulator.add(newNode);
				}
				long totalMemory = Runtime.getRuntime().totalMemory();
				long currentMemory = Runtime.getRuntime().freeMemory();
				long percent = (currentMemory * 100 / totalMemory);
				// Logger.getRootLogger().info( percent + "%");
				if (percent <= 5) {
					// System.gc();
				}
			}
		}
	}

	/**
	 * Creates snapshot - node tree structure - using saved XML file - file
	 * saved by HTTP response input stream
	 * 
	 * @param rootNode
	 * @param accumulator
	 * @param save
	 * @throws Exception
	 */
	protected void takeRemoteSnapshot(final Node rootNode, final List<Node> accumulator, final boolean save) throws Exception {

		if (save) {
			emptyNodeChildren(rootNode, false);
		}

		URI uri = AjxpAPI.getInstance().getRecursiveLsDirectoryUri(rootNode);

		// create temp file
		final File remoteTreeFile = File.createTempFile("pydio", "xmlremote");
		// save xml with remote tree content to file using input stream
		uriContentToFile(uri, remoteTreeFile, null);

		nodeDao.callBatchTasks(new Callable<Void>() {
			public Void call() throws Exception {
				// parse file structure to node tree
				parseNodesFromFile(remoteTreeFile, rootNode, accumulator, save);
				return null;
			}
		});
		if (save) {
			nodeDao.update(rootNode);
		}
	}

	protected Map<String, Object[]> loadRemoteChanges(List<Node> snapshot) throws URISyntaxException, Exception {

		final Node root = loadRootAndSnapshot("remote_tmp", null, null);
		final List<Node> list = EhcacheListFactory.getInstance().getList(LOAD_REMOTE_CHANGES_LIST);
		takeRemoteSnapshot(root, list, true);

		Map<String, Object[]> diff = this.diffNodeLists(list, snapshot, "remote");
		// Logger.getRootLogger().info(diff);
		return diff;
	}

	/**
	 * Parses remote node structure directly from XML file using modified
	 * SAXparser.
	 * 
	 * FIXME - consider passing stream instead of file - direct stream with XML
	 * structure from server - ommit saving to file!
	 * 
	 * @param remoteTreeFile
	 * @param parentNode
	 * @param list
	 * @param save
	 * @throws SynchroOperationException
	 */
	protected void parseNodesFromFile(File remoteTreeFile, final Node parentNode, final List<Node> list, final boolean save)
			throws SynchroOperationException {

		try {
			InputStream is = new FileInputStream(remoteTreeFile);

			XMLReader reader = new XMLReader();
			reader.addHandler("tree", new NodeHandler() {

				@Override
				public void process(StructuredNode node) {

					try {
						org.w3c.dom.Node xmlNode = node.queryXMLNode("/tree");
						Node entry = new Node(Node.NODE_TYPE_ENTRY, "", parentNode);
						if (save) {
							nodeDao.create(entry);
						}
						entry.properties = nodeDao.getEmptyForeignCollection("properties");
						entry.initFromXmlNode(xmlNode);
						if (save) {
							nodeDao.update(entry);
						}
						if (list != null) {
							list.add(entry);
						}
					} catch (XPathExpressionException e) {
						// FIXME - how to manage this errors here?
					} catch (SQLException e) {
						// FIXME - how to manage this errors here?
					}
				}
			});
			reader.parse(is);
			// close the stream
			is.close();
			// delete file - we do not need it anymore
			boolean deleted = remoteTreeFile.delete();
			if (list != null) {
			Logger.getRootLogger().info("Parsed " + list.size() + " nodes from stream, xml file deleted: " + deleted);
			} else {
				Logger.getRootLogger().info("No items to parse");
			}
		} catch (ParserConfigurationException e) {
			throw new SynchroOperationException("Error during parsing remote node tree structure: " + e.getMessage(), e);
		} catch (SAXException e) {
			throw new SynchroOperationException("Error during parsing remote node tree structure: " + e.getMessage(), e);
		} catch (IOException e) {
			throw new SynchroOperationException("Error during parsing remote node tree structure: " + e.getMessage(), e);
		}
	}

	/**
	 * Creates a new mapDB collection for file accessible map
	 * 
	 * @param mapName
	 * @return
	 */
	private Map<String, Object[]> createMapDBFile(String mapName) {
		/**
		 * Open database in temporary directory
		 */
		File dbFile = null;
		try {
			dbFile = File.createTempFile("pydio", "mapdb");
			dbFile.deleteOnExit();
		} catch (IOException e) {
			dbFile = Utils.tempDbFile();
		}
		DB db = DBMaker.newFileDB(dbFile).deleteFilesAfterClose().transactionDisable().make();
		// add db to collection for closing after syncro finishes
		mapDBs.add(db);

		return db.createTreeMap(mapName).make();
	}

	protected Map<String, Object[]> diffNodeLists(List<Node> current, List<Node> snapshot, String type) throws EhcacheListException {
		List<Node> saved = EhcacheListFactory.getInstance().getList(DIFF_NODE_LISTS_SAVED_LIST);
		saved.addAll(snapshot);
		// TreeMap<String, Object[]> diff = new TreeMap<String, Object[]>();

		Map<String, Object[]> diff = createMapDBFile(type);
		Iterator<Node> cIt = current.iterator();
		List<Node> created = EhcacheListFactory.getInstance().getList(DIFF_NODE_LISTS_CREATED_LIST);
		while (cIt.hasNext()) {
			Node c = cIt.next();
			Iterator<Node> sIt = saved.iterator();
			boolean found = false;
			while (sIt.hasNext() && !found) {
				Node s = sIt.next();
				if (s.getPath(true).equals(c.getPath(true))) {
					found = true;
					if (c.isLeaf()) {// FILE : compare date & size
						if ((c.getLastModified().after(s.getLastModified()) || !c.getPropertyValue("bytesize").equals(
								s.getPropertyValue("bytesize")))
								&& (c.getPropertyValue("md5") == null || s.getPropertyValue("md5") == null || !s.getPropertyValue("md5")
										.equals(c.getPropertyValue("md5")))) {
							diff.put(c.getPath(true), makeTodoObject(NODE_CHANGE_STATUS_MODIFIED, c));
						}
					}
					saved.remove(s);
				}
			}
			if (!found) {
				created.add(c);
				// diff.put(c.getPath(true),
				// makeTodoObject((c.isLeaf()?NODE_CHANGE_STATUS_FILE_CREATED:NODE_CHANGE_STATUS_DIR_CREATED),
				// c));
			}
		}
		if (saved.size() > 0) {
			Iterator<Node> sIt = saved.iterator();
			while (sIt.hasNext()) {
				Node s = sIt.next();
				if (s.isLeaf()) {
					Iterator<Node> creaIt = created.iterator();
					boolean isMoved = false;
					Node destinationNode = null;
					while (creaIt.hasNext()) {
						Node createdNode = creaIt.next();
						// if(type.equals("local"))
						// this.updateLocalMD5(createdNode);
						if (createdNode.isLeaf() && createdNode.getPropertyValue("bytesize").equals(s.getPropertyValue("bytesize"))) {
							isMoved = (createdNode.getPropertyValue("md5") != null && s.getPropertyValue("md5") != null && createdNode
									.getPropertyValue("md5").equals(s.getPropertyValue("md5")));
							if (isMoved) {
								destinationNode = createdNode;
								break;
							}
						}
					}
					if (isMoved) {
						// DETECTED, DO SOMETHING.
						created.remove(destinationNode);
						// Logger.getRootLogger().info("This item was moved, it's not necessary to reup/download it again!");
						diff.put(s.getPath(true), makeTodoObjectWithData(NODE_CHANGE_STATUS_FILE_MOVED, s, destinationNode));
					} else {
						diff.put(s.getPath(true),
								makeTodoObject((s.isLeaf() ? NODE_CHANGE_STATUS_FILE_DELETED : NODE_CHANGE_STATUS_DIR_DELETED), s));
					}
				} else {
					diff.put(s.getPath(true),
							makeTodoObject((s.isLeaf() ? NODE_CHANGE_STATUS_FILE_DELETED : NODE_CHANGE_STATUS_DIR_DELETED), s));
				}
			}
		}
		// NOW ADD CREATED ITEMS
		if (created.size() > 0) {
			Iterator<Node> it = created.iterator();
			while (it.hasNext()) {
				Node c = it.next();
				diff.put(c.getPath(true),
						makeTodoObject((c.isLeaf() ? NODE_CHANGE_STATUS_FILE_CREATED : NODE_CHANGE_STATUS_DIR_CREATED), c));
			}
		}
		return diff;
	}

	protected void updateLocalMD5(Node node) {
		if (node.getPropertyValue("md5") != null)
			return;
		Logger.getRootLogger().info("Computing md5 for node " + node.getPath());
		String md5 = SyncJob.computeMD5(new File(currentLocalFolder, node.getPath()));
		node.addProperty("md5", md5);
	}

	protected Object[] makeTodoObject(Integer nodeStatus, Node node) {
		Object[] val = new Object[3];
		val[0] = nodeStatus;
		val[1] = node;
		val[2] = STATUS_TODO;
		return val;
	}

	protected Object[] makeTodoObjectWithData(Integer nodeStatus, Node node, Object data) {
		Object[] val = new Object[4];
		val[0] = nodeStatus;
		val[1] = node;
		val[2] = STATUS_TODO;
		val[3] = data;
		return val;
	}

	protected boolean synchronousUP(Node folderNode, final File sourceFile, Node remoteNode) throws Exception {

		if (getCoreManager().getRdiffProc() != null && getCoreManager().getRdiffProc().rdiffEnabled()) {
			// RDIFF !
			File signatureFile = tmpFileName(sourceFile, "sig");
			boolean remoteHasSignature = false;
			try {
				this.uriContentToFile(AjxpAPI.getInstance().getFilehashSignatureUri(remoteNode), signatureFile, null);
				remoteHasSignature = true;

			} catch (IllegalStateException e) {
			}
			if (remoteHasSignature && signatureFile.exists() && signatureFile.length() > 0) {
				// Compute delta
				File deltaFile = tmpFileName(sourceFile, "delta");
				RdiffProcessor proc = getCoreManager().getRdiffProc();
				proc.delta(signatureFile, sourceFile, deltaFile);
				signatureFile.delete();
				if (deltaFile.exists()) {
					// Send back to server
					RestRequest rest = this.getRequest();
					logChange(getMessage("job_log_updelta"), sourceFile.getName());
					String patchedFileMd5 = rest.getStringContent(AjxpAPI.getInstance().getFilehashPatchUri(remoteNode), null, deltaFile,
							null);
					rest.release();
					deltaFile.delete();
					// String localMD5 = (folderNode)
					if (patchedFileMd5.trim().equals(SyncJob.computeMD5(sourceFile))) {
						// OK !
						return true;
					}
				}
			}
		}

		final long totalSize = sourceFile.length();
		if (!sourceFile.exists() || totalSize == 0) {
			throw new FileNotFoundException("Cannot find file :" + sourceFile.getAbsolutePath());
		}
		Logger.getRootLogger().info("Uploading " + totalSize + " bytes");
		RestRequest rest = this.getRequest();
		// Ping to make sure the user is logged
		rest.getStatusCodeForRequest(AjxpAPI.getInstance().getAPIUri());
		// final long filesize = totalSize;
		rest.setUploadProgressListener(new CountingMultipartRequestEntity.ProgressListener() {
			private int previousPercent = 0;
			private int currentPart = 0;
			private int currentTotal = 1;

			@Override
			public void transferred(long num) throws IOException {
				if (SyncJob.this.interruptRequired) {
					throw new IOException("Upload interrupted on demand");
				}
				int currentPercent = (int) (num * 100 / totalSize);
				if (this.currentTotal > 1) {
					long partsSize = totalSize / this.currentTotal;
					currentPercent = (int) (((partsSize * this.currentPart) + num) * 100 / totalSize);
				}
				currentPercent = Math.min(Math.max(currentPercent, 0), 100);
				if (currentPercent > previousPercent) {
					logChange(getMessage("job_log_uploading"), sourceFile.getName() + " - " + currentPercent + "%");
				}
				previousPercent = currentPercent;
			}

			@Override
			public void partTransferred(int part, int total) throws IOException {
				this.currentPart = part;
				this.currentTotal = total;
				if (SyncJob.this.interruptRequired) {
					throw new IOException("Upload interrupted on demand");
				}
				Logger.getRootLogger().info("PARTS " + " [" + (part + 1) + "/" + total + "]");
				logChange(getMessage("job_log_uploading"), sourceFile.getName() + " [" + (part + 1) + "/" + total + "]");
			}
		});
		String targetName = sourceFile.getName();
		try {
			rest.getStringContent(AjxpAPI.getInstance().getUploadUri(folderNode.getPath(true)), null, sourceFile, targetName);
		} catch (IOException ex) {
			if (this.interruptRequired) {
				rest.release();
				throw new InterruptedException();
			}
		}
		rest.release();
		return false;

	}

	/**
	 * Update node during synchro
	 * 
	 * @param node
	 * @param targetFile
	 * @param remoteNode
	 * @throws SynchroOperationException
	 * @throws SynchroFileOperationException
	 * @throws InterruptedException
	 */
	protected void updateNode(Node node, File targetFile, Node remoteNode) throws SynchroOperationException, SynchroFileOperationException,
			InterruptedException {

		if (targetFile.exists() && getCoreManager().getRdiffProc() != null && getCoreManager().getRdiffProc().rdiffEnabled()) {

			// Compute signature
			File sigFile = tmpFileName(targetFile, "sig");
			File delta = tmpFileName(targetFile, "delta");
			RdiffProcessor proc = getCoreManager().getRdiffProc();
			proc.signature(targetFile, sigFile);
			// Post it to the server to retrieve delta,
			logChange(getMessage("job_log_downdelta"), targetFile.getName());
			URI uri = null;
			try {
				uri = AjxpAPI.getInstance().getFilehashDeltaUri(node);
			} catch (URISyntaxException e) {
				throw new SynchroOperationException("URI Syntax error: " + e.getMessage(), e);
			}
			this.uriContentToFile(uri, delta, sigFile);
			sigFile.delete();
			// apply patch to a tmp version
			File patched = new File(targetFile.getParent(), targetFile.getName() + ".patched");
			if (delta.length() > 0) {
				proc.patch(targetFile, delta, patched);
			}
			if (!patched.exists()) {
				this.synchronousDL(node, targetFile);
				return;
			}
			delta.delete();
			// check md5
			if (remoteNode != null && remoteNode.getPropertyValue("md5") != null
					&& remoteNode.getPropertyValue("md5").equals(SyncJob.computeMD5(patched))) {
				targetFile.delete();
				patched.renameTo(targetFile);
			} else {
				// There is a doubt, re-download whole file!
				patched.delete();
				this.synchronousDL(node, targetFile);
			}

		} else {

			this.synchronousDL(node, targetFile);

		}

	}

	protected File tmpFileName(File source, String ext) {
		File dir = new File(System.getProperty("java.io.tmpdir"));
		String name = String.format("ajxp_%s.%s", UUID.randomUUID(), ext);
		return new File(dir, name);
	}

	/**
	 * Download remote file content for node
	 * 
	 * @param node
	 * @param targetFile
	 * @throws SynchroOperationException
	 * @throws SynchroFileOperationException
	 * @throws InterruptedException
	 */
	protected void synchronousDL(Node node, File targetFile) throws SynchroOperationException, SynchroFileOperationException,
			InterruptedException {

		URI uri = null;
		try {
			uri = AjxpAPI.getInstance().getDownloadUri(node.getPath(true));
		} catch (URISyntaxException e) {
			throw new SynchroOperationException("Wrong URI Syntax: " + e.getMessage(), e);
		}
		this.uriContentToFile(uri, targetFile, null);

	}

	/**
	 * Rewrites remote file content to local file Many things can go wrong so
	 * there are different exception types thrown When succeded -
	 * <code>targetFile</code> contain <code>uploadFile</code> content
	 * 
	 * @param uri
	 * @param targetFile
	 * @param uploadFile
	 * @throws SynchroOperationException
	 * @throws SynchroFileOperationException
	 * @throws InterruptedException
	 */
	protected void uriContentToFile(URI uri, File targetFile, File uploadFile) throws SynchroOperationException,
			SynchroFileOperationException, InterruptedException {

		RestRequest rest = this.getRequest();
		int postedProgress = 0;
		int buffersize = 16384;
		int count = 0;
		HttpEntity entity;
		try {
			entity = rest.getNotConsumedResponseEntity(uri, null, uploadFile, true);
		} catch (Exception e) {
			throw new SynchroOperationException("Error during response entity: " + e.getMessage(), e);
		}
		long fullLength = entity.getContentLength();
		if (fullLength <= 0) {
			rest.release();
			return;
		}
		Logger.getRootLogger().info("Downloading " + fullLength + " bytes");

		InputStream input = null;
		try {
			input = entity.getContent();
		} catch (IllegalStateException e) {
			throw new SynchroOperationException("Error during getting entity content: " + e.getMessage(), e);
		} catch (IOException e) {
			throw new SynchroOperationException("Error during getting entity content: " + e.getMessage(), e);
		}
		BufferedInputStream in = new BufferedInputStream(input, buffersize);

		FileOutputStream output;
		try {
			output = new FileOutputStream(targetFile.getPath());
		} catch (FileNotFoundException e) {
			throw new SynchroFileOperationException("Error during file accessing: " + e.getMessage(), e);
		}
		BufferedOutputStream out = new BufferedOutputStream(output);

		byte data[] = new byte[buffersize];
		int total = 0;

		long startTime = System.nanoTime();
		long lastTime = startTime;
		int lastTimeTotal = 0;

		long secondLength = 1000000000;
		long interval = (long) 2 * secondLength;

		try {
			while ((count = in.read(data)) != -1) {
				long duration = System.nanoTime() - lastTime;

				int tmpTotal = total + count;
				// publishing the progress....
				int tmpProgress = (int) (tmpTotal * 100 / fullLength);
				if (tmpProgress - postedProgress > 0 || duration > secondLength) {
					if (duration > interval) {
						lastTime = System.nanoTime();
						long lastTimeBytes = (long) ((tmpTotal - lastTimeTotal) * secondLength / 1024 / 1000);
						long speed = (lastTimeBytes / (duration));
						double bytesleft = (double) (((double) fullLength - (double) tmpTotal) / 1024);
						@SuppressWarnings("unused")
						double ETC = bytesleft / (speed * 10);
					}
					if (tmpProgress != postedProgress) {
						logChange(getMessage("job_log_downloading"), targetFile.getName() + " - " + tmpProgress + "%");
					}
					postedProgress = tmpProgress;
				}
				out.write(data, 0, count);
				total = tmpTotal;
				if (this.interruptRequired) {
					break;
				}
			}
			out.flush();
			if (out != null) {
				out.close();
			}
			if (in != null) {
				in.close();
			}
		} catch (IOException e) {
			throw new SynchroFileOperationException("Error during file content rewriting: " + e.getMessage(), e);
		}
		if (this.interruptRequired) {
			rest.release();
			throw new InterruptedException();
		}
		rest.release();
	}

	public static String computeMD5(File f) {
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e1) {
			e1.printStackTrace();
			return "";
		}
		InputStream is;
		try {
			is = new FileInputStream(f);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
			return "";
		}
		byte[] buffer = new byte[8192];
		int read = 0;
		try {
			while ((read = is.read(buffer)) > 0) {
				digest.update(buffer, 0, read);
			}
			byte[] md5sum = digest.digest();
			BigInteger bigInt = new BigInteger(1, md5sum);
			String output = bigInt.toString(16);
			if (output.length() < 32) {
				// PAD WITH 0
				while (output.length() < 32)
					output = "0" + output;
			}
			return output;
		} catch (IOException e) {
			// throw new RuntimeException("Unable to process file for MD5", e);
			return "";
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				// throw new
				// RuntimeException("Unable to close input stream for MD5 calculation",
				// e);
				return "";
			}
		}
	}

	protected void logMemory() {
		Logger.getRootLogger().info("Total memory (bytes): " + Math.round(Runtime.getRuntime().totalMemory() / (1024 * 1024)) + "M");
	}

	protected void logDocument(Document d) {
		try {
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			// initialize StreamResult with File object to save to file
			StreamResult result = new StreamResult(new StringWriter());
			DOMSource source = new DOMSource(d);
			transformer.transform(source, result);
			String xmlString = result.getWriter().toString();
			Logger.getRootLogger().info(xmlString);
		} catch (Exception e) {
		}
	}

	protected void writeDocument(Document d) {
		try {
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			// initialize StreamResult with File object to save to file
			StreamResult result = new StreamResult(new StringWriter());
			DOMSource source = new DOMSource(d);
			transformer.transform(source, result);
			String xmlString = result.getWriter().toString();
			BufferedWriter bw = new BufferedWriter(new FileWriter("C:\\temp\\ajaxplorer.xml"));
			bw.write(xmlString);
			bw.close();
		} catch (Exception e) {
		}
	}
}
