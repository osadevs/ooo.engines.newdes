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
package org.osadev.osa.engines.newdes.adl.exoevents.impl;

import java.io.PrintStream;

import org.objectweb.fractal.api.Component;
import org.osadev.osa.engines.newdes.adl.exoevents.interfaces.ExoeventAttributes;
import org.osadev.osa.engines.newdes.adl.exoevents.interfaces.ExoeventBuilder;

import org.osadev.osa.simapis.modeling.EventModelingAPI;
import org.osadev.osa.simapis.modeling.TimeUnit;
import org.osadev.osa.simapis.wrappers.llong.ModelingTime;

/**
 * A Java based implementation of the {@link ExoeventBuilder}
 * interface.
 */
public class FractalExoeventBuilder implements ExoeventBuilder,
  ExoeventAttributes {

  /**
   * Print a given message on a given stream.
   * 
   * @param message
   *        The message to print.
   * @param printStream
   *        The stream.
   * @return Always <code>true</code>
   */
  public static boolean debugMsg(final String message,
    final PrintStream printStream) {
    printStream.println(message + "\n");
    return true;
  }

  /**
   * FIXME : Missing JavaDoc.
   */
  private String exoeventType_;


  /** {@inheritDoc} */
  public final String getExoeventType() {
    return exoeventType_;
  }


  /** {@inheritDoc} */
  public final void setExoevent(final Object component, final String itf,
    final String time, final String method, final String param,
    final Object context) throws Exception {
	  
    assert FractalExoeventBuilder.debugMsg("ADL compiler: setExoevent('" 
                                           + "'," + itf + "." + method + "("
                                           + param + ")," + "," + time
                                           + ")", System.err);

    // final Interface fcItf = (Interface) ((Component)
    // component).getFcInterface(itf);
    @SuppressWarnings("unchecked")
	final EventModelingAPI<Long> sc =
    		(EventModelingAPI<Long>) ((Component) component).getFcInterface("modeling-event-controller");
    String[] result;
    if (param == null) {
      result = null;
    } else {
      result = param.split(",");
    }
    sc.scheduleEventMyself(method, result, ModelingTime.getFactory().create(TimeUnit.parseTime(time)));

  }


  /** {@inheritDoc} */
  public final void setExoeventType(final String exoeventType) {
    exoeventType_ = exoeventType;
  }

}
