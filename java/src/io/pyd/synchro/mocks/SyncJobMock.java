package io.pyd.synchro.mocks;

import java.net.URISyntaxException;

import io.pyd.synchro.CoreManager;
import io.pyd.synchro.SyncJob;


public class SyncJobMock extends SyncJob {

	public SyncJobMock() throws URISyntaxException, Exception {
		super();
	}

	@Override
	protected CoreManager getCoreManager() {
		return CoreManagerMock.getInstance();
	}
	
}
