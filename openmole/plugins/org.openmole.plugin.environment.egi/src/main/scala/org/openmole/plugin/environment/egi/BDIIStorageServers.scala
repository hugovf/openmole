/*
 * Copyright (C) 10/06/13 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.environment.egi

import java.util.concurrent.TimeUnit

import org.openmole.core.exception.InternalProcessingError
import org.openmole.tool.file._
import org.openmole.core.tools.service.{ Scaling, Random }
import org.openmole.core.batch.environment.BatchEnvironment
import fr.iscpif.gridscale.egi.{ GlobusAuthentication, BDII }
import org.openmole.core.batch.replication.ReplicaCatalog
import org.openmole.core.batch.storage.StorageService
import org.openmole.core.batch.control.AccessToken
import org.openmole.core.workspace._
import org.openmole.tool.hash.Hash
import org.openmole.tool.logger.Logger
import org.openmole.tool.thread._
import concurrent.stm._
import java.io.File
import Random._
import Scaling._
import scala.annotation.tailrec

object BDIIStorageServers extends Logger

import BDIIStorageServers.Log._

trait BDIIStorageServers extends BatchEnvironment { env ⇒
  type SS = EGIStorageService

  def bdiiServer: BDII
  def voName: String
  def proxyCreator: () ⇒ GlobusAuthentication.Proxy

  @transient lazy val storages = {
    def timeout = Workspace.preferenceAsDuration(EGIEnvironment.FetchResourcesTimeOut)
    val webdavStorages = bdiiServer.queryWebDAVLocations(voName, timeout)
    if (!webdavStorages.isEmpty) {
      logger.fine("Use webdav storages:" + webdavStorages.mkString(","))
      webdavStorages.map { s ⇒ EGIWebDAVStorageService(s, env, proxyCreator) }
    }
    else {
      val srmStorages = bdiiServer.querySRMLocations(voName, timeout)
      logger.fine("Use srm storages:" + srmStorages.mkString(","))
      srmStorages.map { s ⇒ EGISRMStorageService(s, env, proxyCreator) }
    }
  }

  def selectAStorage(usedFileHashes: Iterable[(File, Hash)]): (StorageService, AccessToken) =
    storages match {
      case Nil      ⇒ throw new InternalProcessingError("No storage service available for the environment.")
      case s :: Nil ⇒ (s, s.waitAToken)
      case _ ⇒
        val sizes = usedFileHashes.map { case (f, _) ⇒ f -> f.size }.toMap
        val totalFileSize = sizes.values.sum
        val onStorage = ReplicaCatalog.withSession(ReplicaCatalog.inCatalog(_))
        val maxTime = storages.map(_.usageControl.time).max
        val minTime = storages.map(_.usageControl.time).min

        @tailrec def select: (StorageService, AccessToken) = {
          def fitnesses =
            for {
              cur ← storages
              if cur.available > 0
            } yield {
              val sizeOnStorage = usedFileHashes.filter { case (_, h) ⇒ onStorage.getOrElse(cur.id, Set.empty).contains(h.toString) }.map { case (f, _) ⇒ sizes(f) }.sum

              val sizeFactor =
                if (totalFileSize != 0) sizeOnStorage.toDouble / totalFileSize else 0.0

              val time = cur.usageControl.time
              val timeFactor =
                if (time.isNaN || maxTime.isNaN || minTime.isNaN || maxTime == 0.0 || minTime == maxTime) 0.0
                else 1 - time.normalize(minTime, maxTime)

              import EGIEnvironment._

              val fitness = math.pow(
                Workspace.preferenceAsDouble(StorageSizeFactor) * sizeFactor +
                  Workspace.preferenceAsDouble(StorageTimeFactor) * timeFactor +
                  Workspace.preferenceAsDouble(StorageAvailabilityFactor) * cur.usageControl.availability +
                  Workspace.preferenceAsDouble(StorageSuccessRateFactor) * cur.usageControl.successRate,
                Workspace.preferenceAsDouble(StorageFitnessPower))

              (cur, fitness)
            }

          val fs = atomic { implicit txn ⇒
            @tailrec def fit: Seq[(StorageService, Double)] =
              fitnesses match {
                case Nil ⇒
                  retryFor(10000)
                  fit
                case x ⇒ x
              }
            fit
          }

          @tailrec def selected(value: Double, storages: List[(StorageService, Double)]): StorageService = {
            storages match {
              case Nil                 ⇒ throw new InternalProcessingError("The list should never be empty")
              case (storage, _) :: Nil ⇒ storage
              case (storage, fitness) :: tail ⇒
                if (value <= fitness) storage
                else selected(value - fitness, tail)
            }
          }

          val notLoaded = EGIEnvironment.normalizedFitness(fs).shuffled(Random.default)
          val fitnessSum = notLoaded.map { case (_, fitness) ⇒ fitness }.sum
          val drawn = Random.default.nextDouble * fitnessSum
          val storage = selected(drawn, notLoaded.toList)

          storage.tryGetToken match {
            case Some(token) ⇒ storage -> token
            case _           ⇒ select
          }
        }
        select
    }

  def clean = ReplicaCatalog.withSession { implicit c ⇒
    val cleaningThreadPool = fixedThreadPool(Workspace.preferenceAsInt(EGIEnvironment.EnvironmentCleaningThreads))
    storages.foreach {
      s ⇒
        background {
          s.withToken { implicit t ⇒ s.clean }
        }(cleaningThreadPool)
    }
    cleaningThreadPool.shutdown()
    cleaningThreadPool.awaitTermination(Long.MaxValue, TimeUnit.DAYS)
  }

}
