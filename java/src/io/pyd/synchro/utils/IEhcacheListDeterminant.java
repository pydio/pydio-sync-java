package io.pyd.synchro.utils;

/**
 * Interface which helps accessing data in EhcacheList
 * Used by <code>EhcacheList</code> to determine object key
 * 
 * @author WojT
 * 
 * @param <T>
 */
public interface IEhcacheListDeterminant<T> {

	/**
	 * Compute object key for list accessing (put/get)
	 * 
	 * @param object
	 * @return
	 */
	public Object computeKey(T object);

}
