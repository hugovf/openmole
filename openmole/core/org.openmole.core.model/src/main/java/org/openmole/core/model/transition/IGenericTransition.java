/*
 *  Copyright (C) 2010 Romain Reuillon
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the Affero GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openmole.core.model.transition;

import java.util.Collection;
import java.util.Set;

import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.model.mole.IMoleExecution;
import org.openmole.core.model.job.IContext;
import org.openmole.core.model.capsule.IGenericTaskCapsule;
import org.openmole.core.model.data.IPrototype;
import org.openmole.core.model.job.ITicket;
import org.openmole.core.model.mole.ISubMoleExecution;

/**
 *
 * Transition is made to link tasks with eachother in order to create a workflow.
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 * @param <TS> the type of capsule at the begining of this transition
 */
public interface IGenericTransition<TS extends IGenericTaskCapsule>  {

    /**
     *
     * Get the starting capsule of this transition.
     *
     * @return the starting capsule of this transition
     */
    TS getStart();


    /**
     *
     * Get the ending capsule of this transition.
     *
     * @return the ending capsule of this transition
     */
    ISlot getEnd();

    /**
     *
     * Get the condition under which this transition is performed.
     *
     * @return the condition under which this transition is performed
     */
    ICondition getCondition();


    boolean isConditionTrue(IContext global, IContext context) throws UserBadDataError, InternalProcessingError;

    /**
     *
     * Set the condition under which this transition is performed.
     *
     * @param condition the condition under which this transition is performed
     */
    void setCondition(ICondition condition);

    /**
     *
     * Set the starting point of the transition. This method doesn't plug the transition
     * to the task. User should consider using {@link IGenericTaskCapsule} pluggin facilities instead
     * of this method.
     *
     * @param task the starting task of this transition
     */
    void setStart(TS task);


    /**
     *
     * Set the ending point of the transition. This method doesn't plug the transition
     * to the task. User should consider using {@link IGenericTaskCapsule} pluggin facilities instead
     * of this method.
     *
     * @param slot the ending slot of this transition
     */
    void setEnd(ISlot slot);

    /**
     *
     * Get the names of the variables which are filtred by this transition.
     *
     * @return the names of the variables which are filtred by this transition
     */
    Collection<String> getFiltred();

    /**
     *
     * Add a filter for the variable of name <code>prototype.getName()</code>.
     *
     * @param prototype the prototype of the variable to filter.
     */
    void addFilter(IPrototype prototype);

    /**
     *
     * Add a filter for the variable of name <code>name</code>.
     *
     * @param name the name of the variable to filter.
     */
    void addFilter(String name);

    /**
     *
     * Remove the filter for variables of name <code>prototype.getName()</code>.
     *
     * @param prototype the prototype for which removing the filter
     */
    void removeFilter(IPrototype prototype);

    /**
     *
     * Remove the filter for variables of name <code>name</code>.
     *
     * @param name the name for which removing the filter
     */
    void removeFilter(String name);

    /**
     *
     * Perform the transition and submit the jobs for the following capsules in the mole.
     *
     * @param from      context generated by the previous job
     * @param ticket    ticket of the previous job
     * @param toClone   variable to clone in the transition
     * @param scheduler the scheduler for the execution
     * @param subMole   current submole
     * @throws InternalProcessingError  if something goes wrong because of a system failure
     * @throws UserBadDataError         if something goes wrong because it is missconfigured
     */
    void perform(IContext global, IContext from, ITicket ticket, Set<String> toClone, IMoleExecution scheduler, ISubMoleExecution subMole) throws InternalProcessingError, UserBadDataError;

}
