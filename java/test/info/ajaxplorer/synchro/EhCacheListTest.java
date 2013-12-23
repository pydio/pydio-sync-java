package info.ajaxplorer.synchro;

import io.pyd.synchro.exceptions.EhcacheListException;
import io.pyd.synchro.utils.EhcacheList;
import io.pyd.synchro.utils.EhcacheListFactory;
import io.pyd.synchro.utils.IEhcacheListDeterminant;

import java.rmi.UnexpectedException;
import java.util.Iterator;
import java.util.Random;

import junit.framework.Assert;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;

public class EhCacheListTest {

	private EhcacheList<String> list;

	@Before
	public void setUp() {
		IEhcacheListDeterminant determinant = new IEhcacheListDeterminant<String>() {

			@Override
			public Object computeKey(String object) {
				return object;
			}

		};
		try {
			EhcacheListFactory.getInstance().initCaches(8, determinant, "teststringlist");
			list = EhcacheListFactory.getInstance().getList("teststringlist");
		} catch (EhcacheListException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnexpectedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		list.clear();
		list.add("aaa");
		list.add("bbb");
	}

	@Test
	public void testSize() {
		Assert.assertEquals(2, list.size());
	}

	@Test
	public void testIsEmpty() {
		Assert.assertFalse(list.isEmpty());
		Assert.assertEquals(2, list.size());
	}

	@Test
	public void testIsEmptyTrue() {
		list.clear();
		Assert.assertTrue(list.isEmpty());
		Assert.assertEquals(0, list.size());
	}

	@Test
	public void testContains() {
		Assert.assertTrue(list.contains("aaa"));
	}

	@Test
	public void testRemove() {
		Assert.assertTrue(list.remove("aaa"));
		Assert.assertEquals(1, list.size());
	}

	@Test
	public void testGet() {
		Assert.assertEquals("aaa", list.get(0));
	}

	@Test
	public void testNotContains() {
		Assert.assertTrue(list.remove("aaa"));
		Assert.assertFalse(list.contains("aaa"));
	}

	@Test
	public void testAddDifferent() {
		list.add("ccc");
		Assert.assertEquals(3, list.size());
	}

	@Test
	public void testAddTheSame() {
		try {
			list.add("aaa");
			Assert.fail("We shouldnt can add the same item");
		} catch (Exception e) {
		}
	}

	@Test
	public void testRemoveTheSameTwice() {
		list.remove("aaa");
		Assert.assertEquals(1, list.size());
		list.remove("aaa");
		Assert.assertEquals(1, list.size());
	}

	@Test
	public void testIterator() {
		Iterator<String> iterator = list.iterator();
		int i = 0;
		boolean rightOrder = true;
		while (iterator.hasNext()) {
			String next = iterator.next();
			if (i == 0 && !"aaa".equals(next)) {
				rightOrder = false;
			}
			if (i == 1 && !"bbb".equals(next)) {
				rightOrder = false;
			}
			i++;
		}
		Assert.assertTrue(rightOrder);

	}

	@Test
	public void testStoreBigAmount() {
		int size = 30000;
		Random r = new Random();
		list.clear();
		long time = System.currentTimeMillis();
		for (int i = 0; i < size; i++) {
			list.add(RandomStringUtils.random(3));
		}
		Assert.assertEquals(size, list.size());

		System.out.println("Time: " + (System.currentTimeMillis() - time));
		time = System.currentTimeMillis();
		int j = 0;
		Iterator<String> sIt = list.iterator();
		while (sIt.hasNext()) {
			sIt.next();
			j++;
		}
		Assert.assertEquals(size, j);
		System.out.println("Time: " + (System.currentTimeMillis() - time));

	}

}
