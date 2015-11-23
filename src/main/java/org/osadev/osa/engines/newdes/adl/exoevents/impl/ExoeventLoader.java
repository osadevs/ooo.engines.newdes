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

import java.util.Map;

import org.objectweb.fractal.adl.ADLException;
import org.objectweb.fractal.adl.AbstractLoader;
import org.objectweb.fractal.adl.Definition;
import org.objectweb.fractal.adl.Node;
import org.objectweb.fractal.adl.components.Component;
import org.objectweb.fractal.adl.components.ComponentContainer;
import org.objectweb.fractal.adl.components.ComponentDefinition;
import org.osadev.osa.engines.newdes.adl.exoevents.interfaces.Exoevent;
import org.osadev.osa.engines.newdes.adl.exoevents.interfaces.Exoevents;
import org.osadev.osa.engines.newdes.adl.exoevents.interfaces.ExoeventsContainer;

import org.osadev.osa.simapis.modeling.TimeUnit;



/**
 * ADL extension module to support exogenous event insertion. This is
 * Loader part of the module, which is responsible for the semantic
 * verifications.
 * 
 * @author odalle
 */
public class ExoeventLoader extends AbstractLoader {


  /**
   * FIXME
   * @param container
   * @param extend
   * @param context
   * @throws ADLException
   */
  private void checkExoeventsContainer(final ExoeventsContainer container,
    final boolean extend, final Map<?,?> context) throws ADLException {

    final Exoevents evts = container.getExoevents();
    if (evts == null) {

      return;
    }

    final String signature = evts.getSignature();

    if (signature == null) {
      if (!extend) {
        throw new ADLException("Signature missing", (Node) evts);
      }
      return;
    }

    /*
     * Class c; try { c =
     * getClassLoader(context).loadClass(signature); } catch
     * (ClassNotFoundException e) { throw new ADLException(
     * "Invalid signature '" + signature + "'", (Node)evts, e); }
     */

    final Exoevent[] events = evts.getExoevents();
    for (int i = 0; i < events.length; ++i) {
      final String evtTime = events[i].getTime();
      final String evtMeth = events[i].getMethod();

      if (evtTime == null) {
        throw new ADLException("Event 'time' attribute missing",
                               (Node) events[i]);
      }

      
      try {
    	  TimeUnit.parseTime(evtTime);
      } catch (Exception e) { 
        throw new ADLException("Event 'time' parse error:"+e.getLocalizedMessage(),
                               (Node) events[i]);
      }

      if (evtMeth == null) {
        throw new ADLException("Event 'method' attribute missing",
                               (Node) events[i]);
      }

    }
  }


  /**
   * FIXME
   * @param node
   * @param extend
   * @param context
   * @throws ADLException
   */
  private void checkNode(final Object node, final boolean extend,
    final Map<?,?> context) throws ADLException {
    if (node instanceof ExoeventsContainer) {
      checkExoeventsContainer((ExoeventsContainer) node, extend, context);
    }
    if (node instanceof ComponentContainer) {
      final Component[] comps = ((ComponentContainer) node).getComponents();
      for (final Component element : comps) {
        checkNode(element, extend, context);
      }
    }
  }


  /** {@inheritDoc} */
  public final Definition load(final String name, final Map<Object,Object> context)
    throws ADLException {
    final Definition d = clientLoader.load(name, context);
    boolean extend = false;
    if (d instanceof ComponentDefinition) {
      extend = ((ComponentDefinition) d).getExtends() != null;
    }
    checkNode(d, extend, context);
    return d;
  }

}
