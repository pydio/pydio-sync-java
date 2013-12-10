package info.ajaxplorer.synchro;

import java.util.Iterator;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.DiskStoreConfiguration;
import net.sf.ehcache.config.MemoryUnit;
import net.sf.ehcache.config.PersistenceConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration.Strategy;

import org.apache.commons.lang.RandomStringUtils;

public class EhCacheTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Configuration cacheManagerConfig = new Configuration().diskStore(new DiskStoreConfiguration().path("/path/to/store/data"));
		CacheConfiguration cacheConfig = new CacheConfiguration().name("my-cache").maxBytesLocalHeap(8, MemoryUnit.MEGABYTES)
		// .maxBytesLocalOffHeap(256, MemoryUnit.MEGABYTES)
				.persistence(new PersistenceConfiguration().strategy(Strategy.LOCALTEMPSWAP));

		cacheManagerConfig.addCache(cacheConfig);

		CacheManager cacheManager = new CacheManager(cacheManagerConfig);
		Ehcache myCache = cacheManager.getEhcache("my-cache");

		for (long i = 0; i < Long.MAX_VALUE; i++) {
			System.out.println(i);
			myCache.put(new Element(i, RandomStringUtils.random(50000)));
		}


		Iterator iterator = myCache.getKeys().iterator();
		while (iterator.hasNext()) {
			Object key = iterator.next();
			Object value = myCache.getQuiet(key);
			if (value instanceof Element) {
				value = ((Element) value).getObjectValue();
			}
			System.out.println("Key: " + key + " val: " + value);
		}

		cacheManager.removeAllCaches();
	}

}
