package io.pyd.synchro.utils;

import io.pyd.synchro.CoreManager;
import io.pyd.synchro.exceptions.EhcacheListException;

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

/**
 * Factory for all Ehcache list objects.
 * Ehcache cache objects have to be created before use, so this factory is
 * intend to
 * create initialized lists collection and deliver proper list by names.
 * All lists are cleared when returned from factory!
 * 
 * @author WojT
 * 
 */
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

	/**
	 * Initializes a list collection
	 * Ehcache lists are configured as pinned In-cache - all objects are stored
	 * forever
	 * The strategy is LOCAL so we have file backed representation with some
	 * additional memory support
	 * 
	 * When configuring cache lists we assign memory size equal to totalMem
	 * split between all items
	 * 
	 * @param totalMem
	 *            - how many memory we want to share for ALL ehcache lists
	 * @param tdeterminant
	 * @param cacheNames
	 * @throws UnexpectedException
	 */
	public void initCaches(int totalMem, IEhcacheListDeterminant tdeterminant, String... cacheNames) throws UnexpectedException {
		if (tdeterminant == null) {
			throw new UnexpectedException("EhcacheListFactory should have IEhcacheListDeterminant provided for object key computation");
		}
		this.determinant = tdeterminant;

		Configuration cacheManagerConfig = new Configuration().diskStore(new DiskStoreConfiguration().path(CoreManager.getDBHomeDir() + System.getProperty("file.separator") + "ehcache"));

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

	/**
	 * Provide a cache list by name
	 * 
	 * @param <E>
	 * @param name
	 * @return
	 * @throws EhcacheListException
	 */
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


		return ehcacheList;
	}
}
