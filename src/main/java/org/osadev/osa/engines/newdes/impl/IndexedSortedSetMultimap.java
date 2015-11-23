package org.osadev.osa.engines.newdes.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.osadev.osa.simapis.modeling.ModelingTimeAPI;
import org.osadev.osa.simapis.simulation.AbstractEvent;

import com.google.common.base.Supplier;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Multiset;

class IndexedSortedSetMultimap<U extends Comparable<U>> implements ListMultimap<ModelingTimeAPI<U>,AbstractEvent<U>> {
	//protected SortedSetMultimap<ModelingTime<U>,AbstractEvent<U>> sortedMultimap_ = TreeMultimap.<ModelingTime<U>,AbstractEvent<U>>create();
	
	// A tree multimap does not allow multiple insertions of the same (key,value) pair
	// and existing prdefined implementation does provide the sorted key feature so
	// we build our custom one, using a treemap for keys and List for values.
	protected ListMultimap<ModelingTimeAPI<U>,AbstractEvent<U>> sortedMultimap_ = 
			Multimaps.newListMultimap(
					new TreeMap< ModelingTimeAPI<U>, Collection<AbstractEvent<U> > >(), 
					new Supplier<List<AbstractEvent<U>>>() { 
						public List<AbstractEvent<U>> get() {
							return new LinkedList<AbstractEvent<U>>();
						} 
					});
	
	// This second map is used to associate each event with a supposedly unique Id 
	// (this is not a multimap, only on value per key)
	protected SortedMap<Long, AbstractEvent<U>> indexMap_ = new TreeMap<Long,AbstractEvent<U>>();
	
	
	public AbstractEvent<U> removeByIndex(Long index) {
		AbstractEvent<U> v = indexMap_.remove(index);
		sortedMultimap_.values().remove(v);
		return v;
	}
	
	public AbstractEvent<U> getByIndex(Long index) {
		return indexMap_.get(index);
	}

	public List<AbstractEvent<U>> get(ModelingTimeAPI<U> key) {
		return sortedMultimap_.get(key);
	}

	public List<AbstractEvent<U>> removeAll(Object key) {
		List<AbstractEvent<U>> vals = sortedMultimap_.removeAll(key);
		indexMap_.values().removeAll(vals);
		return vals;
	}

	public Collection<Entry<ModelingTimeAPI<U>, AbstractEvent<U>>> entries() {
		return sortedMultimap_.entries();
	}

	public int size() {
		return sortedMultimap_.size();
	}

	

	public boolean isEmpty() {
		return sortedMultimap_.isEmpty();
	}

	public boolean containsKey(Object key) {
		return sortedMultimap_.containsKey(key);
	}

	public boolean equals(Object obj) {
		return sortedMultimap_.equals(obj);
	}

	public boolean containsValue(Object value) {
		return sortedMultimap_.containsValue(value);
	}

	public Map<ModelingTimeAPI<U>, Collection<AbstractEvent<U>>> asMap() {
		// This would require to build a new view ...
		throw new RuntimeException("IndexedSortedMultimap: asMap() is not implemented.");
		//return sortedMultimap_.asMap();
	}

	public boolean containsEntry(Object key, Object value) {
		return sortedMultimap_.containsEntry(key, value);
	}

	public boolean put(ModelingTimeAPI<U> key, AbstractEvent<U> value) {
		AbstractEventSimulationControllerImpl.LOGGER.debug("put(K={},V={})",key,value);
		indexMap_.put(value.getIndex(), value);
		return sortedMultimap_.put(key, value);
	}

	public boolean remove(Object key, Object value) {
		boolean result = sortedMultimap_.remove(key, value);
		if (result) indexMap_.values().remove(value);
		return result;
	}


	public boolean putAll(Multimap<? extends ModelingTimeAPI<U>,? extends  AbstractEvent<U>> multimap) {
		boolean result = sortedMultimap_.putAll(multimap);
		if (result) {
			@SuppressWarnings("unchecked")
			AbstractEvent<U> values = (AbstractEvent<U>) multimap.values();
			for (AbstractEvent<U> val: Arrays.asList(values)) {
				indexMap_.put(val.getIndex(), val);
			}
		}
		return result;
	}

	public void clear() {
		sortedMultimap_.clear();
		indexMap_.clear();
	}

	
	/**
	 * WARNING -- partial implementation: Modifying the returned collection will 
	 * leave this data structure in an inconsistent state.
	 * 
	 * {@inheritDoc}
	 * 
	 */
	public Set<ModelingTimeAPI<U>> keySet() {
		return sortedMultimap_.keySet();
	}

	/**
	 * WARNING -- partial implementation: Modifying the returned collection will 
	 * leave this data structure in an inconsistent state.
	 * 
	 * {@inheritDoc}
	 * 
	 */
	public Multiset<ModelingTimeAPI<U>> keys() {
		return sortedMultimap_.keys();
	}

	/**
	 * WARNING -- partial implementation: Modifying the returned collection will 
	 * leave this data structure in an inconsistent state.
	 * 
	 * {@inheritDoc}
	 * 
	 */
	public Collection<AbstractEvent<U>> values() {
		return sortedMultimap_.values();
	}

	public int hashCode() {
		return sortedMultimap_.hashCode();
	}

	public boolean putAll(ModelingTimeAPI<U> key,
			Iterable<? extends AbstractEvent<U>> values) {
		// TODO Auto-generated method stub
		return false;
	}

	
	public List<AbstractEvent<U>> replaceValues(ModelingTimeAPI<U> key, Iterable<? extends AbstractEvent<U>> values) {
		List<AbstractEvent<U>> oldVals = sortedMultimap_.replaceValues(key, values);
		indexMap_.values().removeAll(oldVals);
		for (AbstractEvent<U> val: values) {
			indexMap_.put(val.getIndex(), val);
		}
		return oldVals;
	}

}