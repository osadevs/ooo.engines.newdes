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

import org.objectweb.fractal.api.Component;
import org.objectweb.fractal.api.NoSuchInterfaceException;
import org.objectweb.fractal.api.control.BindingController;
import org.objectweb.fractal.api.control.IllegalBindingException;
import org.objectweb.fractal.api.control.IllegalContentException;
import org.objectweb.fractal.api.control.IllegalLifeCycleException;
import org.objectweb.fractal.util.Fractal;

import org.osadev.osa.logger.newdes.SimulationLogger;
import org.osadev.osa.simapis.exceptions.OsaRuntimeException;
import org.osadev.osa.simapis.modeling.ModelingTimeAPI;
import org.osadev.osa.simapis.fractal.utils.InterfacesEnum;
import org.osadev.osa.simapis.simulation.EventSimulationControllerAPI;
import org.osadev.osa.simapis.simulation.EventSuperSchedulerItf;
import org.osadev.osa.simapis.simulation.SharedSuperSchedulerItf;

/**
 * Abstract super scheduler that defines the core operations of a shared
 * super-scheduler.
 * 
 * The OSA superscheduler is in charge of handling the time for all the
 * simulation components. A shared super-scheduler is a singleton component that
 * is bound to all the simulation components. This class defines the operations
 * needed to establish this one-to-all binding.
 * 
 * @author odalle
 *
 * @param <U>
 *            The generic type to used for internal time representation.
 */
public abstract class AbstractSharedSuperScheduler<U extends Comparable<U>>
        implements SharedSuperSchedulerItf {

    @SuppressWarnings("rawtypes")
    private static SimulationLogger logger_;

    /**
     * External reference to this component so we can self introspect. Caveat: a
     * strict implementation of the component model may prohibit passing the
     * external reference of this object.
     */
    private Component               self_            = null;

    /**
     * Set of components to which this super-scheduler is attached
     */
    protected final Set<Component>  boundComponents_ = new HashSet<Component>();

    /**
     * Logger instance usd by this super-scheduler
     * 
     * @param logger
     */
    protected AbstractSharedSuperScheduler(SimulationLogger<U> logger) {
        logger_ = logger;
    }

    /**
     * Logger instance accessor.
     * 
     * @return Logger instance.
     */
    @SuppressWarnings("unchecked")
    protected SimulationLogger<U> getLogger() {
        return (SimulationLogger<U>) logger_;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.osadev.osa.simapis.simulation.SharedSuperSchedulerItf#deployAndBind
     * (org.objectweb.fractal.api.Component,
     * org.objectweb.fractal.api.Component)
     */
    @SuppressWarnings("unchecked")
    public void deployAndBind(Component superSched, Component rootComponent)
            throws OsaRuntimeException {
        logger_.debug("Deploy and Bind: supersched={}, rootComp={}",
                superSched, rootComponent);

        if ((self_ == null) && (superSched == null))
            throw new OsaRuntimeException(
                    "Superscheduler deploy: missing super scheduler component");

        if ((superSched != null) && (self_ == null)) {
            // We need a super sched reference and we have a candidate

            InterfacesEnum.OSA_SUPER_SCHEDULER_CONTROL.getInterface(superSched,
                    "Superscheduler deploy: "
                            + "superSched parameter is not a super scheduler ("
                            + superSched + ").");
            logger_.debug(
                    "Deploy and bind: setting SS ref to this component: {}",
                    superSched.getFcType());
            self_ = superSched;
        }

        if (rootComponent == null)
            throw new OsaRuntimeException(
                    "Superscheduler deploy: missing root component");

        // Lookup all components in root
        Component[] subComponents = null;
        try {
            subComponents = Fractal.getContentController(rootComponent)
                    .getFcSubComponents();
        } catch (NoSuchInterfaceException e) {
            // No content controller means this root is a primitive: end of
            // recursion
            logger_.debug(
                    "This component (%s) has no sub component, recursion done.",
                    rootComponent, e);
            return;
        }

        // bind to all simulation components if not yet in the list of bound
        // components
        for (Component comp : subComponents) {
            EventSimulationControllerAPI<ModelingTimeAPI<Long>> simC = null;
            logger_.debug("SubComponent: {}", comp.getFcType());

            try {
                simC = (EventSimulationControllerAPI<ModelingTimeAPI<Long>>) InterfacesEnum.OSA_SIMULATION_CONTROLLER
                        .getInterface(comp);

            } catch (NoSuchInterfaceException e) {
                logger_.debug(
                        "No simulation controller, ignoring this component: {}",
                        comp.getFcType());
                // A component with no simulation API. Probably a composite but
                // this is a weak assumption, best to ignore for now.
            }

            // We found a simulation component. Ignore if it is already bound.
            if (boundComponents_.contains(comp))
                continue;

            logger_.debug("This simulation component is not bound yet: {}",
                    simC);

            // Bind to this component
            if (simC != null) {
                BindingController bc;
                EventSuperSchedulerItf<ModelingTimeAPI<Long>> superSchedItf;

                bc = (BindingController) InterfacesEnum.FC_BINDING_CONTROLLER
                        .getInterface(comp,
                                "Superscheduler deploy: Can't find BC on component "
                                        + comp);

                superSchedItf = (EventSuperSchedulerItf<ModelingTimeAPI<Long>>) InterfacesEnum.OSA_SUPER_SCHEDULER_SRV_ITF
                        .getInterface(self_,
                                "Superscheduler deploy: Can't find superschedulersvc"
                                        + "interface on superscheduler ");

                try {
                    bc.bindFc("superscheduler", superSchedItf);
                    boundComponents_.add(comp);
                    logger_.debug("Bound SS to this component: {}", simC);
                } catch (NoSuchInterfaceException e) {
                    throw new OsaRuntimeException(
                            "Superscheduler deploy: Can't bind superscheduler client itf to superschedulersvc interface of super scheduler: Comp ="
                                    + comp, e);
                } catch (IllegalBindingException e) {
                    throw new OsaRuntimeException(
                            "Superscheduler deploy: Weird, can't bind to this sim component!",
                            e);
                } catch (IllegalLifeCycleException e) {
                    throw new OsaRuntimeException(
                            "Superscheduler deploy: binding to this sim component blocked.",
                            e);
                }
            }

            Component[] subSubComponents;
            // Check if this is a composite and if so, if it already contains a
            // SS
            try {
                subSubComponents = Fractal.getContentController(comp)
                        .getFcSubComponents();
            } catch (NoSuchInterfaceException e) {
                // Not a composite, ignore.
                continue;
            }

            // Is one of the sub-sub-components a SS?
            boolean found = false;
            for (Component subComp : subSubComponents) {
                if (subComp.equals(self_)) {
                    found = true;
                    break;
                }
            }

            // already contains a SS (happens with shared component)
            if (found)
                continue;

            logger_.debug("Recursion: adding SS to this component: {}", comp);

            try {
                Fractal.getContentController(comp).addFcSubComponent(self_);
            } catch (IllegalContentException e) {
                throw new OsaRuntimeException(
                        "Superscheduler deploy: failed to get sub-component content",
                        e);
            } catch (IllegalLifeCycleException e) {
                throw new OsaRuntimeException(
                        "Superscheduler deploy: failed to insert SS recusively",
                        e);
            } catch (NoSuchInterfaceException e) {
                throw new OsaRuntimeException(
                        "Superscheduler deploy: failed to get CC interface", e);
            }

            // Last but not least: iterate recursively
            deployAndBind(null, comp);
        }

    }

}
