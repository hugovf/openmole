/*
 *  Copyright (C) 2015 Jonathan Passerat-Palmbach
 *  Copyright (C) 2016 Romain Reuillon
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.task.care

import monocle.macros.Lenses
import org.openmole.core.context.{ Context, Val, Variable }
import org.openmole.core.exception.{ InternalProcessingError, UserBadDataError }
import org.openmole.core.workflow.builder._
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.validation._
import org.openmole.plugin.task.external.{ External, ExternalBuilder }
import org.openmole.plugin.task.systemexec._
import org.openmole.core.expansion._
import org.openmole.tool.logger.JavaLogger
import org.openmole.tool.random._
import org.openmole.plugin.task.container
import cats.implicits._
import org.openmole.core.preference.ConfigurationLocation
import org.openmole.plugin.task.external._
import org.openmole.plugin.task.container._

object CARETask extends JavaLogger {

  val disableSeccomp = ConfigurationLocation("CARETask", "DisableSeccomp", Some(false))

  implicit def isTask: InputOutputBuilder[CARETask] = InputOutputBuilder(CARETask._config)
  implicit def isExternal: ExternalBuilder[CARETask] = ExternalBuilder(CARETask.external)
  implicit def isInfo = InfoBuilder(info)

  implicit def isBuilder = new ReturnValue[CARETask] with ErrorOnReturnValue[CARETask] with StdOutErr[CARETask] with EnvironmentVariables[CARETask] with WorkDirectory[CARETask] with HostFiles[CARETask] {
    override def hostFiles = CARETask.hostFiles
    override def environmentVariables = CARETask.environmentVariables
    override def workDirectory = CARETask.workDirectory
    override def returnValue = CARETask.returnValue
    override def errorOnReturnValue = CARETask.errorOnReturnValue
    override def stdOut = CARETask.stdOut
    override def stdErr = CARETask.stdErr
  }

  def apply(
    archive:              File,
    command:              String,
    returnValue:          OptionalArgument[Val[Int]]    = None,
    stdOut:               OptionalArgument[Val[String]] = None,
    stdErr:               OptionalArgument[Val[String]] = None,
    errorOnReturnValue:   Boolean                       = true,
    environmentVariables: Vector[EnvironmentVariable]   = Vector.empty,
    hostFiles:            Vector[HostFile]              = Vector.empty,
    workDirectory:        OptionalArgument[String]      = None)(implicit sourceCodeName: sourcecode.Name, definitionScope: DefinitionScope): CARETask =
    new CARETask(
      archive = archive,
      command = command,
      hostFiles = hostFiles,
      workDirectory = workDirectory,
      errorOnReturnValue = errorOnReturnValue,
      returnValue = returnValue,
      stdOut = stdOut,
      stdErr = stdErr,
      environmentVariables = environmentVariables,
      _config = InputOutputConfig(),
      external = External(),
      info = InfoConfig()
    )

}

@Lenses case class CARETask(
  archive:              File,
  hostFiles:            Vector[HostFile],
  command:              FromContext[String],
  workDirectory:        Option[String],
  errorOnReturnValue:   Boolean,
  returnValue:          Option[Val[Int]],
  stdOut:               Option[Val[String]],
  stdErr:               Option[Val[String]],
  environmentVariables: Vector[EnvironmentVariable],
  _config:              InputOutputConfig,
  external:             External,
  info:                 InfoConfig
) extends Task with ValidateTask {

  def config = InputOutputConfig.outputs.modify(_ ++ Seq(stdOut, stdErr, returnValue).flatten)(_config)

  def validateArchive(archive: File) =
    if (!archive.exists) container.ArchiveNotFound(archive)
    else if (!archive.canExecute) Seq(new UserBadDataError(s"Archive $archive must be executable. Make sure you upload it with x permissions"))
    else container.ArchiveOK

  override def validate = Validate { p ⇒
    import p._
    validateArchive(archive) ++
      container.validateContainer(Vector(command), environmentVariables, external, this.inputs).apply
  }

  override protected def process(executionContext: TaskExecutionContext) = FromContext[Context] { parameters ⇒
    import executionContext._
    parameters.newFile.withTmpDir { taskWorkDirectory ⇒
      import parameters._

      val context = parameters.context + (External.PWD → taskWorkDirectory.getAbsolutePath)

      def rootfs = "rootfs"

      // unarchiving in task's work directory
      // no need to retrieve error => will throw exception if failing
      execute(Array(archive.getAbsolutePath), taskWorkDirectory, Vector.empty, captureOutput = true, captureError = true)

      val extractedArchive = taskWorkDirectory.listFilesSafe.headOption.getOrElse(
        throw new InternalProcessingError("Work directory should contain extracted archive, but is empty")
      )

      val reExecute = extractedArchive / "re-execute.sh"

      val packagingDirectory: String = workDirectoryLine(reExecute.lines).getOrElse(
        throw new InternalProcessingError(s"Could not find packaging path in $archive")
      )

      def userWorkDirectory = workDirectory.getOrElse(packagingDirectory)

      val inputDirectory = taskWorkDirectory / "inputs"

      def inputPathResolver(path: String) = container.inputPathResolver(inputDirectory, userWorkDirectory)(path)

      val (preparedContext, preparedFilesInfo) = External.deployAndListInputFiles(external, context, inputPathResolver)

      // Replace new proot with a version with user bindings
      val proot = extractedArchive / "proot"
      proot move (extractedArchive / "proot.origin")

      def preparedFileBindings =
        preparedFilesInfo.map {
          case (f, d) ⇒
            val absoluteBindingPath: String = if (File(f.expandedUserPath).isAbsolute) f.expandedUserPath else (File(userWorkDirectory) / f.expandedUserPath).getCanonicalPath
            d.getAbsolutePath → absoluteBindingPath
        }

      def hostFileBindings = hostFiles.map { h ⇒ h.path → h.destination }
      def bindings = preparedFileBindings ++ hostFileBindings

      def createDestination(binding: (String, String)) = {
        import org.openmole.tool.file.{ File ⇒ OMFile }
        val (f, b) = binding

        if (OMFile(f).isDirectory) (taskWorkDirectory / rootfs / b).mkdirs()
        else {
          val dest = taskWorkDirectory / rootfs / b
          dest.getParentFileSafe.mkdirs()
          dest.createNewFile()
        }
      }

      for (binding ← bindings) createDestination(binding)

      // replace original proot executable with a script that will first bind all the inputs in the guest rootfs before
      // calling the original proot
      proot.content =
        s"""
          |#!/bin/bash
          |TRUEPROOT="$${PROOT-$$(dirname $$0)/proot.origin}"
          |$${TRUEPROOT} \\
          | ${bindings.map { case (f, d) ⇒ s"""-b "$f:$d"""" }.mkString(" \\\n")} \\
          | "$${@}"
        """.stripMargin

      proot.setExecutable(true)

      reExecute.content = reExecute.lines.map {
        case line if line.trim.startsWith("-w") ⇒ s"-w '$userWorkDirectory' \\"
        case line                               ⇒ line
      }.mkString("\n")

      reExecute.setExecutable(true)

      val cl = commandLine(command.map(s"./${reExecute.getName} " + _).from(preparedContext))

      def prootNoSeccomp = if (preference(CARETask.disableSeccomp)) Vector(("PROOT_NO_SECCOMP", "1")) else Vector()

      val allEnvironmentVariables = environmentVariables.map { v ⇒ v.name.from(context) -> v.value.from(context) } ++ prootNoSeccomp
      val executionResult = execute(cl, extractedArchive, allEnvironmentVariables, stdOut.isDefined, stdErr.isDefined)

      if (errorOnReturnValue && returnValue.isEmpty && executionResult.returnCode != 0) throw error(cl.toVector, executionResult)

      def rootDirectory = extractedArchive / rootfs

      def outputPathResolver = container.outputPathResolver(preparedFileBindings, hostFileBindings, inputDirectory, userWorkDirectory, rootDirectory) _

      val retContext = External.fetchOutputFiles(external, outputs, preparedContext, outputPathResolver, taskWorkDirectory)

      retContext ++
        List(
          stdOut.map { o ⇒ Variable(o, executionResult.output.get) },
          stdErr.map { e ⇒ Variable(e, executionResult.errorOutput.get) },
          returnValue.map { r ⇒ Variable(r, executionResult.returnCode) }
        ).flatten
    }
  }

}
