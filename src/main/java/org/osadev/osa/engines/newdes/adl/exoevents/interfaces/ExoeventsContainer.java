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
 * An AST (FIXME : Abstract Syntax Tree) node interface for nodes that
 * can contain attributes. FIXME : Need a quite more informations
 */
public interface ExoeventsContainer {

  /**
   * FIXME : Missing JavaDoc.
   * 
   * @return FIXME : Missing JavaDoc
   */
  Exoevents getExoevents();


  /**
   * FIXME : JavaDoc Missing.
   * 
   * @param events
   *        FIXME : JavaDoc Missing
   */
  void setExoevents(Exoevents events);
}
