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

import java.util.PriorityQueue;

import org.objectweb.fractal.api.Component;
import org.osadev.osa.logger.newdes.SimulationLogger;
import org.osadev.osa.simapis.exceptions.SimSchedulingException;
import org.osadev.osa.simapis.modeling.ModelingTimeAPI;
import org.osadev.osa.simapis.modeling.ModelingTimeSymbols;
import org.osadev.osa.simapis.modeling.TimeFactoryItf;
import org.osadev.osa.simapis.fractal.utils.InterfacesEnum;
import org.osadev.osa.simapis.simulation.EventSimulationControllerAPI;
import org.osadev.osa.simapis.simulation.EventSuperSchedulerItf;
import org.osadev.osa.simapis.simulation.SimulationControlAPI;

/**
 * Abstract super scheduler that defines the core operations of an
 * event-oriented super-scheduler.
 * 
 * The OSA superscheduler is in charge of handling the time for all the
 * simulation components. An event-oriented super-scheduler implements the
 * super-scheduler operations required for implementing the simulation of the
 * event-orientd modeling API.
 * 
 * 
 * @author odalle
 *
 * @param <U>
 *            The generic type to used for internal time representation.
 * @see org.osadev.osa.simapis.modeling.EventModelingAPI
 */
public abstract class AbstractEventSuperScheduler<U extends Comparable<U>>
        extends AbstractSharedSuperScheduler<U> implements
        EventSuperSchedulerItf<U>, SimulationControlAPI {

    protected ModelingTimeAPI<U>                             currentTime_;

    protected PriorityQueue<EventSimulationControllerAPI<U>> timeQueue_ = new PriorityQueue<EventSimulationControllerAPI<U>>();

    public AbstractEventSuperScheduler(final SimulationLogger<U> logger,
            final TimeFactoryItf<U> factory) {
        super(logger);
        getLogger().setTimeApi(this);
        currentTime_ = factory.create(ModelingTimeSymbols.INFINITY.name());
    }

    public ModelingTimeAPI<U> getSimulationTime() {
        return currentTime_;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.osadev.osa.simapis.newdes.simulation.EventSuperSchedulerItf#waitUntil
     * (org.osadev.osa.simapis.newdes.ModelingTime,
     * org.osadev.osa.simapis.newdes.simulation.EventSimulationControllerAPI)
     */
    public void waitUntil(ModelingTimeAPI<U> time,
            EventSimulationControllerAPI<U> callBack) {
        // Time value is useless because simulation controllers are supposed to
        // be comparable
        // based on their next time of scheduling. However, assuming the sorting
        // is to occur
        // at the time of insertion (which is not always true, but safest to
        // consider)
        // an element must be removed before being reinserted with a new value.
        timeQueue_.remove(callBack);
        timeQueue_.add(callBack);
        return;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.osadev.osa.simapis.newdes.simulation.SimulationControlAPI#startSimulation
     * ()
     */
    public void startSimulation() {
        for (Component simC : this.boundComponents_) {

            @SuppressWarnings("unchecked")
            EventSimulationControllerAPI<U> esc = (EventSimulationControllerAPI<U>) InterfacesEnum.OSA_SIMULATION_CONTROLLER
                    .getInterface(simC, "startSimulation");
            esc.init();

        }

        while (true) {
            EventSimulationControllerAPI<U> callBack = timeQueue_.poll();
            if (callBack == null) {
                getLogger()
                        .info("No more pending events. Simulation complete.");
                break;
            }
            currentTime_ = callBack.getNextScheduleTime();
            if ((currentTime_ == null) || (currentTime_.isInfinite())) {
                getLogger().info("Simulation complete.");
                break;
            }
            getLogger().debug("Next controller: {}", callBack);
            try {
                callBack.resumeNext(currentTime_);
            } catch (SimSchedulingException e) {
                e.printStackTrace();
            }
        }

    }

}
