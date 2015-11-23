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
package org.osadev.osa.engines.newdes.impl.llong;

import org.osadev.osa.engines.newdes.impl.AbstractEventSuperScheduler;
import org.osadev.osa.logger.newdes.SimulationLogger;
import org.osadev.osa.simapis.simulation.EventSuperSchedulerItf;
import org.osadev.osa.simapis.simulation.SuperSchedulerControlItf;
import org.osadev.osa.simapis.wrappers.llong.ModelingTime;

@org.objectweb.fractal.fraclet.annotations.Component
public class EventSuperScheduler extends
		AbstractEventSuperScheduler<Long>
	implements SuperSchedulerControlItf,EventSuperSchedulerItf<Long> {

	public EventSuperScheduler() {
		super(new SimulationLogger<Long>(EventSuperScheduler.class), ModelingTime.getFactory());
	}

}
