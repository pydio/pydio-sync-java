package info.ajaxplorer.synchro;

import io.pyd.synchro.mocks.CoreManagerMock;
import io.pyd.synchro.mocks.JobExecutionContextMock;

import java.util.Locale;

import org.junit.Before;
import org.junit.Test;
import org.quartz.JobDataMap;

public class SyncJobTest {

	private JobExecutionContextMock ctx;

	@Before
	public void setUp() {
		System.out.println("SomeTest.setUp()");
		CoreManagerMock.instanciate(Locale.getDefault(), false);

		
		
		ctx = new JobExecutionContextMock();
		JobDataMap dataMap = new JobDataMap();
		dataMap.put("node-id", "JUnitTestNode");
		ctx.setDataMap(dataMap);

	}
	
	@Test
	public void dummyTest() {
		
	}

//	@Test
//	public void listSynchroNodes() {
//		CoreManagerMock coreManager = CoreManagerMock.getInstance();
//		Node n = new Node(Node.NODE_TYPE_ENTRY, "JUnitTestNode", null);
//		n.properties = new ArrayList();
//		n.setProperty("synchro_active", "false");
//		n.id = 1;
//		try {
//			coreManager.scheduleJob(n );
//			Collection<Node> nodes = coreManager.listSynchroNodes();
//			System.out.println(nodes.size());
//		} catch (SchedulerException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
	
//	@Test
//	public void someTest() {
//		try {
//			new SyncJobMock().execute(ctx);
//		} catch (URISyntaxException e) {
//			Assert.fail("Cannot run sync: " + e.getMessage());
//		} catch (NullPointerException e) {
//			e.printStackTrace();
//			Assert.fail("Cannot run sync: " + e.getMessage());
//		} catch (Exception e) {
//			Assert.fail("Cannot run sync: " + e.getMessage());
//		}
//	}

}
