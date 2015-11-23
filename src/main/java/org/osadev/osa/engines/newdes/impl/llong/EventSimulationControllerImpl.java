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

import org.objectweb.fractal.fraclet.annotations.Component;
import org.objectweb.fractal.fraclet.annotations.Requires;
import org.objectweb.fractal.fraclet.extensions.Membrane;
import org.osadev.osa.engines.newdes.impl.AbstractEventSimulationControllerImpl;
import org.osadev.osa.engines.newdes.impl.Event;
import org.osadev.osa.simapis.wrappers.llong.ModelingTime;

@Component
@Membrane(controller = "mPrimitive")
public class EventSimulationControllerImpl extends
		AbstractEventSimulationControllerImpl<Long> {

	@Requires(name = "component")
	org.objectweb.fractal.api.Component component;
	
	public EventSimulationControllerImpl() {
		super(ModelingTime.getFactory(),Event.getFactory());
	}

	
	
	@Override
	protected org.objectweb.fractal.api.Component getComponent() {
		return component;
	}

}
