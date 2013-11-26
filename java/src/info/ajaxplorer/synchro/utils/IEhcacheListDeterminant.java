package info.ajaxplorer.synchro.utils;

public interface IEhcacheListDeterminant<T> {

	public Object computeKey(T object);

}
