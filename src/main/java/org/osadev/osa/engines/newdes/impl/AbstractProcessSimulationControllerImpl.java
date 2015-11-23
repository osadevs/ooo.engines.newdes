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

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.objectweb.fractal.api.NoSuchInterfaceException;
import org.objectweb.fractal.api.control.NameController;
import org.objectweb.fractal.julia.Controller;
import org.objectweb.fractal.util.Fractal;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;

import org.osadev.osa.simapis.exceptions.IllegalEventMethodException;
import org.osadev.osa.simapis.exceptions.SimSchedulingException;
import org.osadev.osa.simapis.exceptions.UnknownEventMethodException;
import org.osadev.osa.simapis.modeling.ModelingTimeAPI;
import org.osadev.osa.simapis.modeling.ProcessModelingAPI;
import org.osadev.osa.simapis.modeling.TimeFactoryItf;
import org.osadev.osa.simapis.fractal.utils.InterfacesEnum;
import org.osadev.osa.simapis.simulation.AbstractEvent;
import org.osadev.osa.simapis.simulation.EventFactoryItf;
import org.osadev.osa.simapis.simulation.ProcessSimulationControllerAPI;
import org.osadev.osa.simapis.simulation.ProcessSuperSchedulerItf;
import org.osadev.osa.simapis.simulation.WaitRequest;
import org.osadev.osa.logger.newdes.SimulationLogger;

/**
 * Implements the simulation controller interface back-end.
 *
 * <p>
 * This is the simulation component side of the simulation engine.
 * 
 * @see SharedSuperScheduler the super-scheduler
 *
 * @author odalle
 */
public abstract class AbstractProcessSimulationControllerImpl<U extends Comparable<U>>
        extends AbstractEventSimulationControllerImpl<U> implements
        ProcessSimulationControllerAPI<U>, ProcessModelingAPI<U>, Controller {

    @SuppressWarnings("rawtypes")
    private static final SimulationLogger    LOGGER     = new SimulationLogger(
                                                                AbstractProcessSimulationControllerImpl.class);

    /**
     * Shortcut reference to the super-scheduler interface. Initialized by the
     * {@link #init} method.
     */
    protected ProcessSuperSchedulerItf<U>    psuperSchedInterface_;

    private Multimap<String, WaitRequest<U>> condMap_   = LinkedListMultimap
                                                                .<String, WaitRequest<U>> create();

    private List<WaitRequest<U>>             readyList_ = new LinkedList<WaitRequest<U>>();

    protected AbstractProcessSimulationControllerImpl(
            TimeFactoryItf<U> timeFactory, EventFactoryItf<U> eventFactory) {
        super(timeFactory, eventFactory);
    }

   /**
    * Puts a model execution thread to sleep and wakes up super-scheduler.
    * 
    * @param wr
    *           The wait request object used to handle the thread scheduling.
    */
    private void goToSleep(WaitRequest<U> wr) {
        this.psuperSchedInterface_.resumeSchedulerThread();
        try {
            wr.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException("Wait on semaphore interrupted: ", e);
        }
    }

    /**
     * Wakes up a model execution thread that was blocked on a condition.
     * 
     * @param wr
     *           The wait request object used to handle the thread scheduling.
     */
    public void wakeUpThread(WaitRequest<U> wr) {
        getLogger().debug("wakeUpThread called with wr={}", wr);
        if (wr.getCondition() != null) {
            getLogger().debug("re)moving wr from condMap_: {}", wr);
            condMap_.remove(wr.getCondition(), wr);
        }
        wr.release();
        this.psuperSchedInterface_.parkSchedulerThread("wakeUpThread complete");
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.osadev.osa.simapis.newdes.ProcessModelingAPI#waitForDelay(org.osadev
     * .osa.simapis.newdes.ModelingTime)
     */
    public void waitForDelay(ModelingTimeAPI<U> delay) {
        WaitRequest<U> wr = new WaitRequest<U>();
        wr.setTimedOut();
        wr.setCondition(null);
        AbstractEvent<U> event = eventFactory_.create("wakeUpThread",
                new Object[] { wr },
                this.getSimulationTime().getDelayed(delay.get()), this);
        getLogger().debug("Created new event={}", event);
        this.doScheduleEvent(event);
        goToSleep(wr);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.osadev.osa.simapis.newdes.ProcessModelingAPI#waitOnConditionForDelay
     * (java.lang.String, org.osadev.osa.simapis.newdes.ModelingTime)
     */
    public String waitOnConditionForDelay(String condition,
            ModelingTimeAPI<U> delay) throws InterruptedException {

        WaitRequest<U> wr = new WaitRequest<U>();
        AbstractEvent<U> event = eventFactory_.create("wakeUpThread",
                new Object[] { wr },
                this.getSimulationTime().getDelayed(delay.get()), this);
        wr.setEvent(event);
        wr.setCondition(condition);
        getLogger().debug("Created new event={}", event);
        condMap_.put(condition, wr);
        this.doScheduleEvent(event);
        goToSleep(wr);
        return wr.getResult();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.osadev.osa.simapis.modeling.ProcessModelingAPI#releaseOneOnCondition
     * (java.lang.String, java.lang.String)
     */
    public boolean releaseOneOnCondition(String condition, String param) {
        // Simply forward request to super-sched
        return this.psuperSchedInterface_.iterateReleaseOneOnCondition(
                condition, param);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osadev.osa.simapis.simulation.ProcessSimulationControllerAPI#
     * tryReleaseOneOnCondition(java.lang.String, java.lang.String)
     */
    public boolean tryReleaseOneOnCondition(String condition, String param) {
        Collection<WaitRequest<U>> coll = condMap_.get(condition);
        Iterator<WaitRequest<U>> iter = coll.iterator();

        if (iter.hasNext()) {
            WaitRequest<U> wr = iter.next();
            iter.remove();
            wr.setResult(param);
            this.cancelEvent(wr.getEvent().getEventId());
            readyList_.add(wr);
            return true;
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osadev.osa.simapis.simulation.ProcessSimulationControllerAPI#
     * proceedReleaseAllOnCondition(java.lang.String, java.lang.String)
     */
    public int proceedReleaseAllOnCondition(final String condition,
            final String param) {
        Collection<WaitRequest<U>> coll = condMap_.removeAll(condition);
        int retval = coll.size();

        for (WaitRequest<U> wr : coll) {
            wr.setResult(param);
            this.cancelEvent(wr.getEvent().getEventId());
            readyList_.add(wr);
        }
        return retval;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.osadev.osa.simapis.modeling.ProcessModelingAPI#releaseAllOnCondition
     * (java.lang.String, java.lang.String)
     */
    public int releaseAllOnCondition(final String condition, final String param) {
        return this.psuperSchedInterface_.iterateReleaseAllOnCondition(
                condition, param);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.osadev.osa.simapis.newdes.EventModelingAPI#scheduleEventMyself(java
     * .lang.String, java.lang.Object[],
     * org.osadev.osa.simapis.newdes.ModelingTime)
     */
    @Override
    public long scheduleEventMyself(String methodName, Object[] parameters,
            ModelingTimeAPI<U> time) throws IllegalEventMethodException,
            UnknownEventMethodException {
        // In case the event scheduling method is called we implement
        // the scheduling of a new process to execute that event
        getLogger()
                .debug("ProcessController::schedEventMyself(meth={},params={},time={})",
                        methodName, parameters, time);
        return scheduleProcessMyself(methodName, parameters, time);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.osadev.osa.simapis.simulation.ProcessSimulationControllerAPI#startProcess
     * (java.lang.Object)
     */
    public void startProcess(final Object obj) {
        // We cannot restrict the type of parameter because it makes
        // the lookup of a matching method fail
        final AbstractEvent<U> event = (AbstractEvent<U>) obj;
        getLogger().debug("startProcess(event={})", event);
        Thread t = new Thread() {
            public void run() {
                try {
                    event.invoke();
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } finally {
                    psuperSchedInterface_.resumeSchedulerThread();
                }
            }
        };
        t.start();
        this.psuperSchedInterface_
                .parkSchedulerThread("startProcess complete.");
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.osadev.osa.simapis.newdes.ProcessModelingAPI#scheduleProcessMyself
     * (java.lang.String, java.lang.Object[],
     * org.osadev.osa.simapis.newdes.ModelingTime)
     */
    public long scheduleProcessMyself(String methodName, Object[] parameters,
            ModelingTimeAPI<U> time) throws IllegalEventMethodException,
            UnknownEventMethodException {
        getLogger().debug("startProcessMyself(meth={},params={},time={})",
                methodName, parameters, time);
        // Scheduling a process consists in scheduling a control event that will
        // create the process
        // which will in turn execute the business event.
        AbstractEvent<U> busEvent = eventFactory_.create(methodName,
                parameters, time, content_);
        AbstractEvent<U> ctlEvent = eventFactory_.create("startProcess",
                new Object[] { (Object) busEvent }, time, this);
        return this.doScheduleEvent(ctlEvent);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.osadev.osa.simapis.newdes.simulation.ProcessSimulationControllerAPI
     * #resumeReady()
     */
    public boolean resumeReady() {
        getLogger().debug("resumeReady: list size={}", readyList_.size());
        if (readyList_.size() > 0) {
            WaitRequest<U> wr = readyList_.remove(0);
            getLogger().debug("Found this request in list: {} (permits={}).",
                    wr, wr.availablePermits());
            wr.release();
            return true;
        }
        return false;
    }

    @Override
    /*
     * (non-Javadoc)
     * 
     * @see
     * org.osadev.osa.engines.newdes.impl.AbstractEventSimulationControllerImpl
     * #resumeNext(org.osadev.osa.simapis.modeling.ModelingTimeAPI)
     */
    public boolean resumeNext(ModelingTimeAPI<U> currentTime)
            throws SimSchedulingException {
        getLogger().debug("resumeNext(time={})", currentTime);

        if (super.resumeNext(currentTime)) {
            this.psuperSchedInterface_
                    .parkSchedulerThread("resumeNext() woke succeeded to wake up a thread");
            return true;
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.osadev.osa.engines.newdes.impl.AbstractEventSimulationControllerImpl
     * #getComponent()
     */
    protected abstract org.objectweb.fractal.api.Component getComponent();

    @SuppressWarnings("unchecked")
    @Override
    /*
     * (non-Javadoc)
     * 
     * @see
     * org.osadev.osa.engines.newdes.impl.AbstractEventSimulationControllerImpl
     * #init()
     */
    public void init() {
        if (getComponent() == null)
            throw new NullPointerException("component undefined.");

        superSchedInterface_ = psuperSchedInterface_ = (ProcessSuperSchedulerItf<U>) InterfacesEnum.OSA_SUPER_SCHEDULER_CLI_ITF
                .getInterface(getComponent(), "init");

        localSchedInterface_ = (ProcessSimulationControllerAPI<U>) InterfacesEnum.OSA_SIMULATION_CONTROLLER
                .getInterface(getComponent(), "init");

        getLogger().setTimeApi(psuperSchedInterface_);
        super.getLogger().setTimeApi(psuperSchedInterface_);

        psuperSchedInterface_.waitUntil(nextScheduledEvent_,
                localSchedInterface_);
    }

    @Override
    /*
     * (non-Javadoc)
     * 
     * @see
     * org.osadev.osa.engines.newdes.impl.AbstractEventSimulationControllerImpl
     * #getLogger()
     */
    protected SimulationLogger<U> getLogger() {
        return LOGGER;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    public String toString() {
        NameController nc;
        try {
            nc = Fractal
                    .getNameController((org.objectweb.fractal.api.Component) this);
        } catch (NoSuchInterfaceException e) {
            return "<No name>";
        }

        return nc.getFcName();
    }

}
