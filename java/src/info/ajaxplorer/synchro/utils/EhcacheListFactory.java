package info.ajaxplorer.synchro.utils;

import info.ajaxplorer.synchro.exceptions.EhcacheListException;

import java.rmi.UnexpectedException;
import java.util.HashMap;
import java.util.Map;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.DiskStoreConfiguration;
import net.sf.ehcache.config.MemoryUnit;
import net.sf.ehcache.config.PersistenceConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration.Strategy;
import net.sf.ehcache.config.PinningConfiguration;

import org.apache.log4j.Logger;

public class EhcacheListFactory {

	private static EhcacheListFactory instance;

	private CacheManager cacheManager;

	private Map<String, EhcacheList> listsMap = new HashMap<String, EhcacheList>();

	private IEhcacheListDeterminant determinant;

	public static EhcacheListFactory getInstance() {
		if (instance == null) {
			instance = new EhcacheListFactory();
		}
		return instance;
	}

	public void initCaches(int totalMem, IEhcacheListDeterminant tdeterminant, String... cacheNames) throws UnexpectedException {
		if (tdeterminant == null) {
			throw new UnexpectedException("EhcacheListFactory should have IEhcacheListDeterminant provided for object key computation");
		}
		this.determinant = tdeterminant;

		Configuration cacheManagerConfig = new Configuration().diskStore(new DiskStoreConfiguration().path(System.getProperty("user.home")
				+ System.getProperty("file.separator") + ".ajaxplorer" + System.getProperty("file.separator") + "ehcache"));

		int oneFileMem = totalMem / cacheNames.length;

		for (String name : cacheNames) {
			PinningConfiguration pinningC = new PinningConfiguration();
			pinningC.setStore(PinningConfiguration.Store.INCACHE.name());
			CacheConfiguration cacheConfig = new CacheConfiguration().name(name).pinning(pinningC)
					.maxBytesLocalHeap(oneFileMem, MemoryUnit.MEGABYTES).maxBytesLocalDisk(200, MemoryUnit.MEGABYTES)
					.persistence(new PersistenceConfiguration().strategy(Strategy.LOCALTEMPSWAP));
			cacheConfig.setEternal(true);

			cacheManagerConfig.addCache(cacheConfig);
		}
		cacheManager = CacheManager.create(cacheManagerConfig);
	}

	public <E> EhcacheList<E> getList(String name) throws EhcacheListException {
		if (cacheManager == null) {
			throw new EhcacheListException("Error - no cacheManager available - call initCaches() first!");
		}

		// check if we have allready this list?
		EhcacheList ehcacheList = listsMap.get(name);
		if (ehcacheList == null) {
			// create new one
			Ehcache cache = cacheManager.getEhcache(name);
			if (cache == null) {
				throw new EhcacheListException("Error - no cache for: " + name + " - the name has to be denoted when initCaches() called!");
			}
			ehcacheList = new EhcacheList<E>(cache, determinant);
			listsMap.put(name, ehcacheList);
		}
		// ensure that we have empty list
		ehcacheList.clear();

		Logger.getRootLogger().info("Return " + name + " list, size: " + ehcacheList.size());

		return ehcacheList;
	}
}
