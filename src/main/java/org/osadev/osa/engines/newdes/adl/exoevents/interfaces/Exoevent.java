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
package org.osadev.osa.engines.newdes.adl.exoevents.interfaces;

/**
 * ADL extension module to support exogeneous event insertion.
 * 
 * This is the AST interface definition for the new ExoEvent node with three
 * attributes : the currentTime_ of the event, the method name and the interface
 * to which the method belong and an optional parameter.
 * 
 * @author odalle
 */
public interface Exoevent {
    /**
     * Give the name of the method associated with this event.
     * 
     * @return The name of the method associated with this event.
     */
    String getMethod();

    /**
     * Give the parameter of the method associated with this event.
     * 
     * @return The parameter of the method associated with this event.
     */
    String getParam();

    /**
     * Return the currentTime_ of this event.
     * 
     * @return The currentTime_ of this event.
     */
    String getTime();

    /**
     * Set the method name associated with this event.
     * 
     * @param method
     *            The method name. FIXME : <code>null</code> is a valid value ?
     */
    void setMethod(String method);

    /**
     * Set the parameter of the method associated with this event.
     * 
     * @param parameter
     *            The parameter of the method associated with this event. FIXME
     *            : <code>null</code> is a valid value ?
     */
    void setParam(String parameter);

    /**
     * Set the currentTime_ of this event.
     * 
     * @param time
     *            The currentTime_ of this event.
     */
    void setTime(String time);
}
