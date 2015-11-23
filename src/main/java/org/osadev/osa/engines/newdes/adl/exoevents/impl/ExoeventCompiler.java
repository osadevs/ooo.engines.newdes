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

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.objectweb.fractal.adl.ADLException;
import org.objectweb.fractal.adl.components.ComponentContainer;
import org.objectweb.fractal.adl.components.PrimitiveCompiler;
import org.objectweb.fractal.api.NoSuchInterfaceException;
import org.objectweb.fractal.api.control.BindingController;
import org.objectweb.fractal.api.control.IllegalBindingException;
import org.objectweb.fractal.api.control.IllegalLifeCycleException;
import org.objectweb.fractal.task.core.Task;
import org.objectweb.fractal.task.core.TaskMap;
import org.objectweb.fractal.task.deployment.lib.AbstractConfigurationTask;
import org.osadev.osa.engines.newdes.adl.exoevents.interfaces.Exoevent;
import org.osadev.osa.engines.newdes.adl.exoevents.interfaces.ExoeventBuilder;
import org.osadev.osa.engines.newdes.adl.exoevents.interfaces.Exoevents;
import org.osadev.osa.engines.newdes.adl.exoevents.interfaces.ExoeventsContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * FIXME : Missing JavaDoc.
 */
public class ExoeventCompiler implements PrimitiveCompiler, BindingController {

	private static final Logger logger_ = LoggerFactory.getLogger(ExoeventCompiler.class);
  /**
   * FIXME : Missing JavaDoc.
   */
  static class ExoeventTask extends AbstractConfigurationTask {

    /**
     * FIXME : Missing JavaDoc.
     */
    private final ExoeventBuilder builder_;

    /**
     * FIXME : Missing JavaDoc.
     */
    private final String          itf_;

    /**
     * FIXME : Missing JavaDoc.
     */
    private final String          time_;
    /**
     * FIXME : Missing JavaDoc.
     */
    private final String          method_;
    /**
     * FIXME : Missing JavaDoc.
     */
    private final String          param_;


    /**
     * FIXME
     * 
     * @param builder
     * @param signature
     * @param time
     * @param method
     * @param param
     */
    public ExoeventTask(final ExoeventBuilder builder, final String signature,
                        final String time, final String method,
                        final String param) {
      super();
      // TODO Auto-generated constructor stub
      builder_ = builder;
      time_ = time;
      itf_ = signature;
      method_ = method;
      param_ = param;
    }


    /** {@inheritDoc} */
    public void execute(final Map<Object, Object> context) throws Exception {
      final Object component = getInstanceProviderTask().getInstance();
      builder_.setExoevent(component, itf_, time_, method_, param_, context);
    }


    /** {@inheritDoc} */
    public Object getResult() {
      return null;
    }


    /**
     * @param result
     *        FIXME : Missing JavaDoc.
     */
    public void setResult(final Object result) {
      // FIXME : Method non implementee
    }


    /** {@inheritDoc} */
    @Override
    public String toString() {
      return String.format("T%d[ExoventTask(%s:%s:%s)]", System.identityHashCode(this),
    		  method_,time_, param_);
    }
  }

  /**
   * Name of the mandatory interface bound to the
   * {@link ExoeventBuilder} used by this compiler.
   */
  public static final String BUILDER_BINDING = "builder";

  /**
   * FIXME : Missing JavaDoc.
   */
  private ExoeventBuilder    builder_;


  /** {@inheritDoc} */
  public final void bindFc(final String clientItfName, final Object serverItf)
    throws NoSuchInterfaceException, IllegalBindingException,
    IllegalLifeCycleException {
    if (ExoeventCompiler.BUILDER_BINDING.equals(clientItfName)) {
      builder_ = (ExoeventBuilder) serverItf;
    }
  }


  /** {@inheritDoc} */
  public final void compile(final List<ComponentContainer> arg0,
    final ComponentContainer arg1, final TaskMap arg2,
    final Map<Object, Object> arg3) throws ADLException {

	  logger_.debug("Debug: Exoevent.compile(arg0={},arg1={},arg2={},arg3={})",
			  arg0.toString(),arg1.toString(),arg2.toString(),arg3.toString());
	  
    if (arg1 instanceof ExoeventsContainer) {
      final Exoevents events = ((ExoeventsContainer) arg1).getExoevents();
      if (events != null) {
        // final InstanceProviderTask createTask =
        // (InstanceProviderTask) tasks
        // .getTask(
        // "create",
        // container);
        final TaskMap.TaskHole createTaskHole =
          arg2.getTaskHole("create", arg1);

        // final Task startTask = tasks.getTask("start", container);

        // TaskMap.TaskHole startTaskHole = tasks.getTaskHole("start",
        // container);

        final Exoevent[] evts = events.getExoevents();
        for (int i = 0; i < evts.length; ++i) {
        	
        	String taskName = String.format("event:%s:%s:%s:",
        			evts[i].getMethod(), evts[i].getTime(), evts[i].getParam());
        	logger_.debug("Processing taks:{}",taskName);
          //try {
            // the task may already exist, in case of a shared
            // component. This means we cannot have two identical events scheduled at the same time.
            //arg2.getTask(taskName, arg1);
          //} catch (final NoSuchElementException e) {
            // System.out.println("catch1");
            final ExoeventTask t =
              new ExoeventTask(builder_, events.getSignature(),
                               evts[i].getTime(), evts[i].getMethod(),
                               evts[i].getParam());
            // t.setInstanceProviderTask(createTask);
            //
            // startTask.addPreviousTask(t);
            //
            // tasks.addTask("event" + evts[i].getName(), container,
            // t);

            // TaskMap.TaskHole attributeTaskHole =
            arg2.addTask(taskName, arg1, t);

            t.setInstanceProviderTask(createTaskHole);
            t.addDependency(createTaskHole, Task.PREVIOUS_TASK_ROLE, arg1);
         // }
        }
      }
    }

  }


  /** {@inheritDoc} */
  public final String[] listFc() {
    return new String[] { ExoeventCompiler.BUILDER_BINDING };
  }


  /** {@inheritDoc} */
  public final Object lookupFc(final String clientItfName)
    throws NoSuchInterfaceException {

    if (ExoeventCompiler.BUILDER_BINDING.equals(clientItfName)) {
      return builder_;
    }
    return null;
  }


  /** {@inheritDoc} */
  public final void unbindFc(final String clientItfName)
    throws NoSuchInterfaceException, IllegalBindingException,
    IllegalLifeCycleException {
    if (ExoeventCompiler.BUILDER_BINDING.equals(clientItfName)) {
      builder_ = null;
    }
  }

}
