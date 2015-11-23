/**+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++--> 
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
package org.osadev.osa.engines.newdes.impl;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Semaphore;

import org.objectweb.fractal.api.Component;
import org.osadev.osa.logger.newdes.SimulationLogger;
import org.osadev.osa.simapis.exceptions.SimSchedulingException;
import org.osadev.osa.simapis.modeling.TimeFactoryItf;
import org.osadev.osa.simapis.fractal.utils.InterfacesEnum;
import org.osadev.osa.simapis.simulation.ProcessSimulationControllerAPI;
import org.osadev.osa.simapis.simulation.ProcessSuperSchedulerItf;
import org.osadev.osa.simapis.simulation.SimulationControlAPI;

/**
 * Abstract super scheduler that defines the core operations of an
 * process-based super-scheduler.
 * 
 * The OSA superscheduler is in charge of handling the time for all the
 * simulation components. An process based super-scheduler implements the
 * super-scheduler operations required for implementing the simulation of the
 * process based modeling API.
 * 
 * 
 * @author odalle
 *
 * @param <U>
 *            The generic type to used for internal time representation.
 * @see org.osadev.osa.simapis.modeling.ProcessModelingAPI
 */
public abstract class AbstractProcessSuperScheduler<U extends Comparable<U>> extends
		AbstractEventSuperScheduler<U> implements ProcessSuperSchedulerItf<U>, SimulationControlAPI {
	
	//private PriorityQueue<ProcessSimulationControllerAPI<U>> timeQueue_ = new PriorityQueue<ProcessSimulationControllerAPI<U>>();
	private Semaphore waitSema_ = new Semaphore(0);
	
	private Set<ProcessSimulationControllerAPI<U>> readySet_ = new HashSet<ProcessSimulationControllerAPI<U>>();
	
	public AbstractProcessSuperScheduler(SimulationLogger<U> logger,
			TimeFactoryItf<U> factory) {
		super(logger, factory);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.osadev.osa.engines.newdes.impl.AbstractEventSuperScheduler#startSimulation()
	 */
	public void startSimulation() {
		for (Component simC : this.boundComponents_) {
			@SuppressWarnings("unchecked")
			ProcessSimulationControllerAPI<U> esc = (ProcessSimulationControllerAPI<U>) 
					InterfacesEnum.OSA_SIMULATION_CONTROLLER.getInterface(simC, 
							"While starting simulation");
			esc.init();
		}

		while (true) {
			while (! readySet_.isEmpty()) {
				getLogger().debug("Ready set not empty...");
				ProcessSimulationControllerAPI<U> ready = readySet_.iterator().next();
				if (ready.resumeReady()) {
					parkSchedulerThread("------->> Resumed a ready thread.");
				} else {
					readySet_.remove(ready);
				}
			}
			ProcessSimulationControllerAPI<U> callBack = (ProcessSimulationControllerAPI<U>)timeQueue_.poll();
			if (callBack == null) {
				getLogger().info("No more pending events. Simulation complete.");
				break;
			}
			currentTime_ = callBack.getNextScheduleTime();
			if ((currentTime_ == null) || (currentTime_.isInfinite())) {
				getLogger().info("Simulation complete.");
				break;
			}
			getLogger().debug("Next controller: {}",callBack);
			try {
				callBack.resumeNext(currentTime_);
			} catch (SimSchedulingException e) {
				e.printStackTrace();
			}
		}
			
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.osadev.osa.simapis.simulation.ProcessSuperSchedulerItf#iterateReleaseOneOnCondition(java.lang.String, java.lang.String)
	 */
	public boolean iterateReleaseOneOnCondition(String condition, String param) {
		
		for (Component comp : this.boundComponents_) {
			@SuppressWarnings("unchecked")
			ProcessSimulationControllerAPI<U> simC = (ProcessSimulationControllerAPI<U>) 
					InterfacesEnum.OSA_SIMULATION_CONTROLLER.getInterface(comp, "iterateReleaseOne");

			if (simC.tryReleaseOneOnCondition(condition, param)) {
				// If the component has ready threads, we put it in the ready set:
				// The current thread keeps running without blocking while the released thread 
				// is moved in the ready set for later resuming.
				readySet_.add(simC);
				// We've found one, we're done.
				return true;
			} 
		}
		return false;
		
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.osadev.osa.simapis.simulation.ProcessSuperSchedulerItf#iterateReleaseAllOnCondition(java.lang.String, java.lang.String)
	 */
	public int iterateReleaseAllOnCondition(String condition, String param) {
		int retval = 0;
		for (Component comp : this.boundComponents_) {
			@SuppressWarnings("unchecked")
			ProcessSimulationControllerAPI<U> simC = (ProcessSimulationControllerAPI<U>) 
					InterfacesEnum.OSA_SIMULATION_CONTROLLER.getInterface(comp, "iterateReleaseAll");

			int count = simC.proceedReleaseAllOnCondition(condition, param);
			if (count > 0) {
				// If the component has ready threads, we put it in the ready set:
				// The current thread keeps running without blocking while the released thread 
				// is moved in the ready set for later resuming.
				readySet_.add(simC);
				// We've found one, we're done.
				retval += count;
			} 
		}
		return retval;
	}

	/*
	 * (non-Javadoc)
	 * @see org.osadev.osa.simapis.simulation.ProcessSuperSchedulerItf#parkSchedulerThread(java.lang.String)
	 */
	public void parkSchedulerThread(String trace) {
		try {
			getLogger().debug("parkScheduler: going to sleep (trace = {})...",trace);
			waitSema_.acquire();
			getLogger().debug("parkScheduler: waking up!");
		} catch (InterruptedException e) {
			throw new RuntimeException("Failed to acquire super scheduler exception:",e);
		}
		
	}

	/*
	 * (non-Javadoc)
	 * @see org.osadev.osa.simapis.simulation.ProcessSuperSchedulerItf#resumeSchedulerThread()
	 */
	public void resumeSchedulerThread() {
		getLogger().debug("resumeScheduler called.");
		waitSema_.release();
		
	}
	
	

}
