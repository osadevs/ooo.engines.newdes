package org.osadev.osa.engines.newdes.impl;
/** ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++-->
<!--                Open Simulation Architecture (OSA)                  -->
<!--                                                                    -->
<!--      This software is distributed under the terms of the           -->
<!--           CECILL-C FREE SOFTWARE LICENSE AGREEMENT                 -->
<!--  (see http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html) -->
<!--                                                                    -->
<!--  Copyright © 2006-2015 Université Nice Sophia Antipolis            -->
<!--  Contact author: Olivier Dalle (olivier.dalle@unice.fr)            -->
<!--                                                                    -->
<!--  Parts of this software development were supported and hosted by   -->
<!--  INRIA from 2006 to 2015, in the context of the common research    -->
<!--  teams of INRIA and I3S, UMR CNRS 7172 (MASCOTTE, COATI, OASIS and -->
<!--  SCALE).                                                           -->
<!--++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++**/

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.objectweb.fractal.api.factory.InstantiationException;
import org.objectweb.fractal.julia.Controller;
import org.objectweb.fractal.julia.InitializationContext;

import com.google.common.base.Supplier;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Multiset;

import org.osadev.osa.logger.newdes.SimulationLogger;
import org.osadev.osa.simapis.exceptions.IllegalEventMethodException;
import org.osadev.osa.simapis.exceptions.SimSchedulingException;
import org.osadev.osa.simapis.exceptions.UnknownEventMethodException;
import org.osadev.osa.simapis.modeling.EventModelingAPI;
import org.osadev.osa.simapis.modeling.ModelingTimeAPI;
import org.osadev.osa.simapis.modeling.ModelingTimeSymbols;
import org.osadev.osa.simapis.modeling.TimeFactoryItf;
import org.osadev.osa.simapis.fractal.utils.InterfacesEnum;
import org.osadev.osa.simapis.simulation.AbstractEvent;
import org.osadev.osa.simapis.simulation.EventFactoryItf;
import org.osadev.osa.simapis.simulation.EventSimulationControllerAPI;
import org.osadev.osa.simapis.simulation.EventSuperSchedulerItf;


/**
 * Implements the basic (event-driven) simulation controller interface back-end.
 *
 * <p> This is the simulation component side of the simulation engine.
 * @see SharedSuperScheduler the super-scheduler
 *
 * @author odalle
 */

public abstract class AbstractEventSimulationControllerImpl<U extends Comparable<U>> implements 
	EventSimulationControllerAPI<U>, EventModelingAPI<U>, Controller {
	
	/** Fractal context used to access component content. */
	protected Object content_;
	
	/**
	 * Shortcut reference to the super-scheduler interface.
	 * Initialized by the {@link #init} method.
	 */
	protected EventSuperSchedulerItf<U> superSchedInterface_;
	
	protected EventSimulationControllerAPI<U> localSchedInterface_;

	private static class IndexedSortedSetMultimap<U extends Comparable<U>> implements ListMultimap<ModelingTimeAPI<U>,AbstractEvent<U>> {
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
			LOGGER.debug("put(K={},V={})",key,value);
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
	
	protected abstract  org.objectweb.fractal.api.Component getComponent();
	
	protected IndexedSortedSetMultimap<U> sortedMultimap_ = new IndexedSortedSetMultimap<U>();
	
	protected ModelingTimeAPI<U> nextScheduledEvent_;
	
	protected final TimeFactoryItf<U> timeFactory_;
	protected final EventFactoryItf<U> eventFactory_;

	@SuppressWarnings("rawtypes")
	private static final SimulationLogger LOGGER = new SimulationLogger(AbstractEventSimulationControllerImpl.class);

	protected SimulationLogger getLogger() {
		return (SimulationLogger)LOGGER;
	}
	
	public AbstractEventSimulationControllerImpl(final TimeFactoryItf<U> timeFactory, final EventFactoryItf<U> eventFactory) {
		this.timeFactory_ = timeFactory;
		this.eventFactory_ = eventFactory;
		nextScheduledEvent_ = timeFactory.create(ModelingTimeSymbols.INFINITY.name());
	}

	/*
	 * (non-Javadoc)
	 * @see org.osadev.osa.simapis.newdes.SimulationTimeAPI#getSimulationTime()
	 */
	public ModelingTimeAPI<U> getSimulationTime() {
		return superSchedInterface_.getSimulationTime();
	}

	/*
	 * (non-Javadoc)
	 * @see org.objectweb.fractal.julia.Controller#initFcController(org.objectweb.fractal.julia.InitializationContext)
	 */
	public void initFcController(InitializationContext arg0)
			throws InstantiationException {
		if (arg0.content instanceof Object[]) {
			content_ = ((Object[]) arg0.content)[2];
			//content_ = ((Object[]) arg0.content)[1];
		} else {
			content_ = arg0.content;
		}

		getLogger().debug("initFcController called ({})...",this.content_);
		
	}
	
	
	protected long doScheduleEvent(AbstractEvent<U> event)
	{
		ModelingTimeAPI<U> time = event.getTime();
		getLogger().debug("doScheduleEvent 1/2: Queueing event [{}({}),{}]",event.getEvtMethod(),event.getEvtParam(),event.getTime());
		ModelingTimeAPI<U> prevHeadTime = nextScheduledEvent_;
		if (! sortedMultimap_.put((ModelingTimeAPI<U>)time, event)) {
			getLogger().warn("Oops! Event was not queued ({}).",event);
		}
		
		if (time.compareTo(prevHeadTime) < 0) {
			nextScheduledEvent_ = time;
			if (superSchedInterface_ != null)
				superSchedInterface_.waitUntil(nextScheduledEvent_, this);
		}
		getLogger().debug("doScheduleEvent 2/2: Queued event with id={}",event.getEventId());
		return event.getEventId();
		
	}

	/*
	 * (non-Javadoc)
	 * @see org.osadev.osa.simapis.newdes.EventModelingAPI#scheduleEventMyself(java.lang.String, java.lang.Object[], org.osadev.osa.simapis.newdes.ModelingTime)
	 */
	public long scheduleEventMyself(String methodName, Object[] parameters, ModelingTimeAPI<U> time) 
	throws IllegalEventMethodException,	UnknownEventMethodException {
		AbstractEvent<U> newEvent = eventFactory_.create(methodName, parameters, time, content_);
		return doScheduleEvent(newEvent);
	}

	
	
	/*
	 * (non-Javadoc)
	 * @see org.osadev.osa.simapis.newdes.EventModelingAPI#cancelEvent(long)
	 */
	public boolean cancelEvent(long eventId) {
		return sortedMultimap_.removeByIndex(eventId) != null;
	}

	
	/*
	 * (non-Javadoc)
	 * @see org.osadev.osa.simapis.newdes.EventModelingAPI#getEvent(long)
	 */
	//public E getEvent(long eventId) {
	//	return (E)sortedMultimap_.getByIndex((Long)eventId);
	//}

	/*
	 * (non-Javadoc)
	 * @see org.osadev.osa.simapis.newdes.simulation.EventSimulationControllerAPI#init()
	 */
	@SuppressWarnings("unchecked")
	public void init() {
		if (getComponent() == null)
			throw new NullPointerException("component undefined.");
		superSchedInterface_ = (EventSuperSchedulerItf<U>) 
				InterfacesEnum.OSA_SUPER_SCHEDULER_CLI_ITF
				.getInterface(getComponent(), "init");

		localSchedInterface_ = (EventSimulationControllerAPI<U>) 
				InterfacesEnum.OSA_SIMULATION_CONTROLLER
				.getInterface(getComponent(), "init");

		getLogger().setTimeApi(superSchedInterface_);

		superSchedInterface_.waitUntil(nextScheduledEvent_, localSchedInterface_);
	}

	/*
	 * (non-Javadoc)
	 * @see org.osadev.osa.simapis.newdes.simulation.EventSimulationControllerAPI#resumeNext(org.osadev.osa.simapis.newdes.ModelingTime)
	 */
	public boolean resumeNext(ModelingTimeAPI<U> currentTime)
			throws SimSchedulingException {
		if (currentTime.compareTo(nextScheduledEvent_) != 0)
			throw new RuntimeException(String.format(
					"Trying to schedule an event at a different time %d than expected %d!",
					currentTime.get().toString(), nextScheduledEvent_.get().toString()));
		
		while (currentTime.compareTo(nextScheduledEvent_) == 0) {
		
			List<AbstractEvent<U>> events = sortedMultimap_
					.removeAll(currentTime);

			for (AbstractEvent<U> e : events) {
				try {
					e.invoke();
				} catch (IllegalAccessException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (IllegalArgumentException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (NoSuchMethodException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (SecurityException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (InvocationTargetException e1) {
					LOGGER.error("Error invoking event {}", e);
					e1.printStackTrace();
				}
			}
			try {
				nextScheduledEvent_ = (ModelingTimeAPI<U>) sortedMultimap_.keys()
						.iterator().next();
				superSchedInterface_.waitUntil(nextScheduledEvent_,
						localSchedInterface_);
			} catch (NoSuchElementException e) {
				// Empty
				nextScheduledEvent_ = timeFactory_
						.create(ModelingTimeSymbols.INFINITY.name());
			}
		}
		return false;
	}
	
	public void quit() {
		// Not much cleaning to do
	}

	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(EventSimulationControllerAPI<U> o) {
		return nextScheduledEvent_.compareTo(o.getNextScheduleTime());
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.osadev.osa.simapis.newdes.simulation.EventSimulationControllerAPI#getNextScheduleTime()
	 */
	public ModelingTimeAPI<U> getNextScheduleTime() {
		return nextScheduledEvent_;
	}
	

}
