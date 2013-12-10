package info.ajaxplorer.synchro.mocks;

import java.net.URISyntaxException;

import info.ajaxplorer.synchro.CoreManager;
import info.ajaxplorer.synchro.SyncJob;


public class SyncJobMock extends SyncJob {

	public SyncJobMock() throws URISyntaxException, Exception {
		super();
	}

	@Override
	protected CoreManager getCoreManager() {
		return CoreManagerMock.getInstance();
	}
	
}
