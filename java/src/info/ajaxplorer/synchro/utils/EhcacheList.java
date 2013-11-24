package info.ajaxplorer.synchro.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

public class EhcacheList<E> implements List<E> {

	private Ehcache cache;

	/* package */EhcacheList(Ehcache tcache) {
		cache = tcache;
	}

	@Override
	public int size() {
		return cache.getSize();
	}

	@Override
	public boolean isEmpty() {
		return cache.getKeys().isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		if (o == null)
			return false; // FIXME shouldnt we denote also null values?
		return cache.get(o.hashCode()) != null;
	}

	@Override
	public Iterator<E> iterator() {
		return new Iterator<E>() {

			private Iterator keyIterator = cache.getKeys().iterator();

			@Override
			public boolean hasNext() {
				return keyIterator.hasNext();
			}

			@Override
			public E next() {
				E o = null;

				Object key = keyIterator.next();
				if (key != null) {
					Object val = cache.get(key);
					if (val instanceof Element) {
						o = (E) ((Element) val).getObjectValue();
					}
				}
				return o;
			}

			@Override
			public void remove() {
				// FIXME - is it enough?
				keyIterator.remove();
			}
		};
	}

	@Override
	public Object[] toArray() {
		throw new UnsupportedOperationException("Cannot convert cache list to array - to big object!");
	}

	@Override
	public <T> T[] toArray(T[] a) {
		throw new UnsupportedOperationException("Cannot convert cache list to array - to big object!");
	}

	@Override
	public boolean add(E e) {
		if (e == null) {
			return false; // FIXME - add nulls also?
		}
		if (cache.isKeyInCache(e.hashCode())) {
			throw new UnsupportedOperationException("List cannot containt the same element twice");
		}
		cache.put(new Element(e.hashCode(), e));
		return true;
	}

	@Override
	public boolean remove(Object o) {
		if (o == null)
			return false;
		return cache.remove(o.hashCode());
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		// FIXME - optimize? or throw unsupportedException?
		boolean containsAll = true;
		for (Object o : c) {
			if (o == null) {
				continue;
			}
			containsAll = cache.isKeyInCache(o.hashCode());
			if (!containsAll) {
				break;
			}
		}
		return containsAll;
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		List<Element> newCollection = new ArrayList<Element>();
		for (E e : c) {
			newCollection.add(new Element(e.hashCode(), e));
		}
		boolean result = c.size() == newCollection.size();
		if (result) {
			cache.putAll(newCollection);
		}
		return result;
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		throw new UnsupportedOperationException("Cannot convert cache list to array - to big object!");
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException("Cannot convert cache list to array - to big object!");
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException("Cannot convert cache list to array - to big object!");
	}

	@Override
	public void clear() {
		cache.removeAll();
	}

	@Override
	public E get(int index) {
		E result = null;
		Object key = cache.getKeys().get(index);
		if (key != null) {
			Object intResult = cache.get(key);
			if (intResult instanceof Element) {
				result = (E) ((Element) intResult).getObjectValue();
			}
		}
		return result;
	}

	@Override
	public E set(int index, E element) {
		throw new UnsupportedOperationException("Cannot convert cache list to array - to big object!");
	}

	@Override
	public void add(int index, E element) {
		throw new UnsupportedOperationException("Cannot convert cache list to array - to big object!");
	}

	@Override
	public E remove(int index) {
		E removed = null;
		Object key = cache.getKeys().remove(index);
		if (key != null) {
			Object rem = cache.get(key);
			if (rem instanceof Element) {
				removed = (E) ((Element) rem).getObjectValue();
			}
		}
		return removed;
	}

	@Override
	public int indexOf(Object o) {
		if (o == null)
			return -1;
		return cache.getKeys().indexOf(o.hashCode());
	}

	@Override
	public int lastIndexOf(Object o) {
		throw new UnsupportedOperationException("Cannot convert cache list to array - to big object!");
	}

	@Override
	public ListIterator<E> listIterator() {
		throw new UnsupportedOperationException("Cannot convert cache list to array - to big object!");
	}

	@Override
	public ListIterator<E> listIterator(int index) {
		throw new UnsupportedOperationException("Cannot convert cache list to array - to big object!");
	}

	@Override
	public List<E> subList(int fromIndex, int toIndex) {
		throw new UnsupportedOperationException("Cannot convert cache list to array - to big object!");
	}

}
