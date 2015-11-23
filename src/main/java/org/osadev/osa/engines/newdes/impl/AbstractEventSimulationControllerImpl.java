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
package org.osadev.osa.engines.newdes.impl;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.NoSuchElementException;

import org.objectweb.fractal.api.factory.InstantiationException;
import org.objectweb.fractal.julia.Controller;
import org.objectweb.fractal.julia.InitializationContext;
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
 * <p>
 * This is the simulation component side of the simulation engine.
 * 
 * @author odalle
 */

public abstract class AbstractEventSimulationControllerImpl<U extends Comparable<U>>
        implements EventSimulationControllerAPI<U>, EventModelingAPI<U>,
        Controller {

    /** Fractal context used to access component content. */
    protected Object                          content_;

    /**
     * Shortcut reference to the super-scheduler interface. Initialized by the
     * {@link #init} method.
     */
    protected EventSuperSchedulerItf<U>       superSchedInterface_;

    protected EventSimulationControllerAPI<U> localSchedInterface_;

    protected abstract org.objectweb.fractal.api.Component getComponent();

    protected IndexedSortedSetMultimap<U> sortedMultimap_ = new IndexedSortedSetMultimap<U>();

    protected ModelingTimeAPI<U>          nextScheduledEvent_;

    protected final TimeFactoryItf<U>     timeFactory_;
    protected final EventFactoryItf<U>    eventFactory_;

    @SuppressWarnings("rawtypes")
    static final SimulationLogger         LOGGER          = new SimulationLogger(
                                                                  AbstractEventSimulationControllerImpl.class);

    protected SimulationLogger getLogger() {
        return (SimulationLogger) LOGGER;
    }

    public AbstractEventSimulationControllerImpl(
            final TimeFactoryItf<U> timeFactory,
            final EventFactoryItf<U> eventFactory) {
        this.timeFactory_ = timeFactory;
        this.eventFactory_ = eventFactory;
        nextScheduledEvent_ = timeFactory.create(ModelingTimeSymbols.INFINITY
                .name());
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.osadev.osa.simapis.modeling.SimulationTimeAPI#getSimulationTime()
     */
    public ModelingTimeAPI<U> getSimulationTime() {
        return superSchedInterface_.getSimulationTime();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.objectweb.fractal.julia.Controller#initFcController(org.objectweb
     * .fractal.julia.InitializationContext)
     */
    public void initFcController(InitializationContext arg0)
            throws InstantiationException {
        if (arg0.content instanceof Object[]) {
            content_ = ((Object[]) arg0.content)[2];
            // content_ = ((Object[]) arg0.content)[1];
        } else {
            content_ = arg0.content;
        }

        getLogger().debug("initFcController called ({})...", this.content_);

    }

    protected long doScheduleEvent(AbstractEvent<U> event) {
        ModelingTimeAPI<U> time = event.getTime();
        getLogger().debug("doScheduleEvent 1/2: Queueing event [{}({}),{}]",
                event.getEvtMethod(), event.getEvtParam(), event.getTime());
        ModelingTimeAPI<U> prevHeadTime = nextScheduledEvent_;
        if (!sortedMultimap_.put((ModelingTimeAPI<U>) time, event)) {
            getLogger().warn("Oops! Event was not queued ({}).", event);
        }

        if (time.compareTo(prevHeadTime) < 0) {
            nextScheduledEvent_ = time;
            if (superSchedInterface_ != null)
                superSchedInterface_.waitUntil(nextScheduledEvent_, this);
        }
        getLogger().debug("doScheduleEvent 2/2: Queued event with id={}",
                event.getEventId());
        return event.getEventId();

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.osadev.osa.simapis.newdes.EventModelingAPI#scheduleEventMyself(java
     * .lang.String, java.lang.Object[],
     * org.osadev.osa.simapis.newdes.ModelingTime)
     */
    public long scheduleEventMyself(String methodName, Object[] parameters,
            ModelingTimeAPI<U> time) throws IllegalEventMethodException,
            UnknownEventMethodException {
        AbstractEvent<U> newEvent = eventFactory_.create(methodName,
                parameters, time, content_);
        return doScheduleEvent(newEvent);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osadev.osa.simapis.newdes.EventModelingAPI#cancelEvent(long)
     */
    public boolean cancelEvent(long eventId) {
        return sortedMultimap_.removeByIndex(eventId) != null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osadev.osa.simapis.newdes.EventModelingAPI#getEvent(long)
     */
    // public E getEvent(long eventId) {
    // return (E)sortedMultimap_.getByIndex((Long)eventId);
    // }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.osadev.osa.simapis.newdes.simulation.EventSimulationControllerAPI
     * #init()
     */
    @SuppressWarnings("unchecked")
    public void init() {
        if (getComponent() == null)
            throw new NullPointerException("component undefined.");
        superSchedInterface_ = (EventSuperSchedulerItf<U>) InterfacesEnum.OSA_SUPER_SCHEDULER_CLI_ITF
                .getInterface(getComponent(), "init");

        localSchedInterface_ = (EventSimulationControllerAPI<U>) InterfacesEnum.OSA_SIMULATION_CONTROLLER
                .getInterface(getComponent(), "init");

        getLogger().setTimeApi(superSchedInterface_);

        superSchedInterface_.waitUntil(nextScheduledEvent_,
                localSchedInterface_);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.osadev.osa.simapis.newdes.simulation.EventSimulationControllerAPI
     * #resumeNext(org.osadev.osa.simapis.newdes.ModelingTime)
     */
    public boolean resumeNext(ModelingTimeAPI<U> currentTime)
            throws SimSchedulingException {
        if (currentTime.compareTo(nextScheduledEvent_) != 0)
            throw new RuntimeException(
                    String.format(
                            "Trying to schedule an event at a different time %d than expected %d!",
                            currentTime.get().toString(), nextScheduledEvent_
                                    .get().toString()));

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
                nextScheduledEvent_ = (ModelingTimeAPI<U>) sortedMultimap_
                        .keys().iterator().next();
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
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(EventSimulationControllerAPI<U> o) {
        return nextScheduledEvent_.compareTo(o.getNextScheduleTime());
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.osadev.osa.simapis.newdes.simulation.EventSimulationControllerAPI
     * #getNextScheduleTime()
     */
    public ModelingTimeAPI<U> getNextScheduleTime() {
        return nextScheduledEvent_;
    }

}
