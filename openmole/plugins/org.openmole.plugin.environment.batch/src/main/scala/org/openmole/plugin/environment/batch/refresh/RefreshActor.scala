/*
 * Copyright (C) 2012 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.environment.batch.refresh

import org.openmole.core.exception.InternalProcessingError
import org.openmole.core.workflow.execution.ExecutionState._
import org.openmole.plugin.environment.batch.environment.{ BatchEnvironment, BatchJobControl }
import org.openmole.tool.logger.JavaLogger

object RefreshActor extends JavaLogger {

  def receive(refresh: Refresh)(implicit services: BatchEnvironment.Services) = {
    import services._

    val Refresh(job, bj, delay, updateErrorsInARow) = refresh
    if (job.state != KILLED) {
      try {
        val oldState = job.state
        job.state = bj.updateState()
        job.state match {
          case DONE ⇒ JobManager ! GetResult(job, bj.resultPath(), bj)
          case FAILED ⇒
            val exception = new InternalProcessingError(s"""Job status is FAILED""".stripMargin)
            val stdOutErr = BatchJobControl.tryStdOutErr(bj).toOption
            JobManager ! Error(job, exception, stdOutErr)
            JobManager ! Kill(job, Some(bj))
          case SUBMITTED | RUNNING ⇒
            val newDelay =
              if (oldState == job.state) (delay + bj.updateInterval.incrementUpdateInterval) min bj.updateInterval.maxUpdateInterval
              else bj.updateInterval.minUpdateInterval
            JobManager ! Delay(Refresh(job, bj, newDelay, 0), newDelay)
          case KILLED ⇒
          case _      ⇒ throw new InternalProcessingError(s"Job ${job} is in state ${job.state} while being refreshed")
        }
      }
      catch {
        case e: Throwable ⇒
          if (updateErrorsInARow >= preference(BatchEnvironment.MaxUpdateErrorsInARow)) {
            JobManager ! Error(job, e, BatchJobControl.tryStdOutErr(bj).toOption)
            JobManager ! Kill(job, Some(bj))
          }
          else {
            Log.logger.log(Log.FINE, s"${updateErrorsInARow + 1} errors in a row during job refresh", e)
            JobManager ! Delay(Refresh(job, bj, delay, updateErrorsInARow + 1), delay)
          }
      }

    }
  }
}

