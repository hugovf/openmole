/*
 * Copyright (C) 2015 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
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
package org.openmole.core.communication

import java.io.File

import org.openmole.core.context.Context
import org.openmole.core.exception.InternalProcessingError
import org.openmole.core.fileservice.{ FileDeleter, FileService }
import org.openmole.core.serializer.SerializerService
import org.openmole.core.tools.service._
import org.openmole.core.workflow.execution.Environment.RuntimeLog
import org.openmole.core.workflow.job.MoleJob._
import org.openmole.core.workflow.job._
import org.openmole.core.workflow.task.Task
import org.openmole.core.workspace.{ NewFile, Workspace }

import util.Try
import org.openmole.tool.file._
import org.openmole.tool.hash._
import org.openmole.tool.tar._

package object message {

  object FileMessage {
    implicit def replicatedFile2FileMessage(r: ReplicatedFile) = FileMessage(r)
    def apply(replicatedFile: ReplicatedFile): FileMessage = apply(replicatedFile.path, replicatedFile.hash)
  }

  object RunnableTask {
    def apply(moleJob: MoleJob) = new RunnableTask(moleJob.task, moleJob.context, moleJob.id)
  }

  class RunnableTask(val task: Task, val context: Context, val id: MoleJobId)

  case class FileMessage(path: String, hash: String)

  object ReplicatedFile {
    def download(replicatedFile: ReplicatedFile)(download: (String, File) ⇒ Unit, verifyHash: Boolean = false)(implicit newFile: NewFile, fileService: FileService) = {
      val localDirectory = newFile.makeNewDir("replica")
      try {
        def verify(cache: File) =
          if (verifyHash) {
            val cacheHash = cache.hash().toString
            if (cacheHash != replicatedFile.hash) throw new InternalProcessingError("Hash is incorrect for file " + replicatedFile.originalPath + " replicated at " + replicatedFile.path)
          }

        val dl =
          if (replicatedFile.directory) {
            val cache = localDirectory.newFile("archive", ".tgz")
            download(replicatedFile.path, cache)
            verify(cache)

            val local = localDirectory / replicatedFile.name
            cache.extract(local)
            cache.delete
            local.mode = replicatedFile.mode
            local
          }
          else {
            val cache = localDirectory / replicatedFile.name
            download(replicatedFile.path, cache)
            verify(cache)

            cache.mode = replicatedFile.mode
            cache
          }

        dl
      }
      catch {
        case t: Throwable ⇒
          localDirectory.recursiveDelete
          throw t
      }
      finally fileService.deleteWhenEmpty(localDirectory)
    }

    def upload(file: File, upload: File ⇒ String)(implicit newFile: NewFile) = {
      val isDir = file.isDirectory

      val toReplicate =
        if (isDir) {
          val ret = newFile.newFile("archive", ".tar")
          file.archive(ret)
          ret
        }
        else file

      val mode = file.mode
      val hash = toReplicate.hash().toString
      val uploaded = upload(toReplicate)
      ReplicatedFile(file.getPath, file.getName, isDir, hash, uploaded, mode)
    }

  }

  case class ReplicatedFile(originalPath: String, name: String, directory: Boolean, hash: String, path: String, mode: Int)
  case class RuntimeSettings(archiveResult: Boolean)

  object ExecutionMessage {
    def load(file: File)(implicit serialiserService: SerializerService, fileService: FileService, newFile: NewFile) = {
      serialiserService.deserializeAndExtractFiles[ExecutionMessage](file)
    }
  }

  case class ExecutionMessage(plugins: Iterable[ReplicatedFile], files: Iterable[ReplicatedFile], jobs: File, runtimeSettings: RuntimeSettings)

  object RuntimeResult {
    def load(file: File)(implicit serialiserService: SerializerService, fileService: FileService, newFile: NewFile) =
      serialiserService.deserializeAndExtractFiles[RuntimeResult](file)
  }

  case class RuntimeResult(stdOut: Option[File], result: Try[(SerializedContextResults, RuntimeLog)], info: RuntimeInfo)
  sealed trait SerializedContextResults
  case class ArchiveContextResults(contextResults: File) extends SerializedContextResults
  case class IndividualFilesContextResults(contextResults: File, files: Iterable[ReplicatedFile]) extends SerializedContextResults
  case class ContextResults(results: PartialFunction[MoleJobId, Try[Context]])

}
