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
 * An AST (FIXME : Abstract Syntax Tree ?) node interface to define a
 * set of exogenous events.
 */
public interface Exoevents {

  /**
   * FIXME : Missing JavaDoc.
   * 
   * @param exoEvent
   *        FIXME : Missing JavaDoc
   */
  void addExoevent(Exoevent exoEvent);


  /**
   * FIXME : Missing JavaDoc.
   * 
   * @return FIXME : Missing JavaDoc
   */
  Exoevent[] getExoevents();


  /**
   * FIXME : Missing JavaDoc.
   * 
   * @return FIXME : Missing JavaDoc
   */
  String getSignature();


  /**
   * FIXME : Missing JavaDoc.
   * 
   * @param exoEvent
   *        FIXME : Missing JavaDoc
   */
  void removeExoevent(Exoevent exoEvent);


  /**
   * FIXME : Missing JavaDoc.
   * 
   * @param signature
   *        FIXME : Missing JavaDoc
   */
  void setSignature(String signature);
}
