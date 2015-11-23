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

import org.osadev.osa.simapis.modeling.ModelingTimeAPI;
import org.osadev.osa.simapis.simulation.AbstractEvent;
import org.osadev.osa.simapis.simulation.EventFactoryItf;
import org.osadev.osa.simapis.simulation.IndexableAndComparable;
import org.osadev.osa.simapis.simulation.SimulationEventItf;

/**
 * Basic Simulation events. 
 * 
 * Reuses the basic implementation provided by the 
 * {@link org.osadev.osa.simapis.basic.AbstractEvent} class.
 * 
 */
public class Event extends AbstractEvent<Long> 
implements SimulationEventItf<Long>, IndexableAndComparable<Long,AbstractEvent<Long>> {
	
	private Event(String method, Object[] params, ModelingTimeAPI<Long> time, Object instance) {
		super(method, params, time, instance);
	}
	
	public static AbstractEvent<Long> createEvent(String method, Object[] params, 
			ModelingTimeAPI<Long> time, Object instance) {
		return new Event(method,params,time, instance);
	}
	
	
	public static class EventFactory implements EventFactoryItf<Long> {

		public AbstractEvent<Long> create(String method, Object[] params, ModelingTimeAPI<Long> time, Object instance) {
			return new Event(method, params, time, instance);
		}
		
	}
	
	
	public static EventFactory getFactory() {
		return new EventFactory();
	}


	
}
