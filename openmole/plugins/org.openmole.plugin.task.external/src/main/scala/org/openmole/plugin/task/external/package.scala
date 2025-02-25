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

package org.openmole.plugin.task

import org.openmole.core.dsl._
import org.openmole.core.expansion.{ FromContext, ToFromContext }
import org.openmole.core.tools.service.OS

package external {

  import java.io._

  import org.openmole.core.context.Val
  import org.openmole.core.workflow.builder.InputOutputBuilder

  trait ExternalPackage {

    lazy val inputFiles = new {
      /**
       * Copy a file or directory from the dataflow to the task workspace
       */
      def +=[T: ExternalBuilder: InputOutputBuilder](p: Val[File], name: FromContext[String], link: Boolean = false): T ⇒ T =
        implicitly[ExternalBuilder[T]].inputFiles add External.InputFile(p, name, link) andThen (inputs += p)
    }

    lazy val inputFileArrays = new {
      /**
       * Copy an array of files or directory from the dataflow to the task workspace. The files
       * in the array are named prefix$nSuffix where $n i the index of the file in the array.
       */
      def +=[T: ExternalBuilder: InputOutputBuilder](p: Val[Array[File]], prefix: FromContext[String], suffix: FromContext[String] = "", link: Boolean = false): T ⇒ T =
        (implicitly[ExternalBuilder[T]].inputFileArrays add External.InputFileArray(p, prefix, suffix, link)) andThen (inputs += p)
    }

    lazy val outputFiles = new {
      /**
       * Get a file generate by the task and inject it in the dataflow
       *
       */
      def +=[T: ExternalBuilder: InputOutputBuilder](name: FromContext[String], p: Val[File]): T ⇒ T =
        (implicitly[ExternalBuilder[T]].outputFiles add External.OutputFile(name, p)) andThen (outputs += p)
    }

    lazy val resources =
      new {
        /**
         * Copy a file from your computer in the workspace of the task
         */
        def +=[T: ExternalBuilder](file: File, name: OptionalArgument[FromContext[String]] = None, link: Boolean = false, os: OS = OS()): T ⇒ T =
          implicitly[ExternalBuilder[T]].resources add External.Resource(file, name.getOrElse(file.getName), link = link, os = os)

      }

    @deprecated("Use environmentVariables", "7")
    lazy val environmentVariable = environmentVariables

    lazy val environmentVariables =
      new {
        /**
         * Add variable from openmole to the environment of the system exec task. The
         * environment variable is set using a toString of the openmole variable content.
         *
         * @param prototype the prototype of the openmole variable to inject in the environment
         * @param variable the name of the environment variable. By default the name of the environment
         *                 variable is the same as the one of the openmole protoype.
         */

        import cats.implicits._
        def +=[T: EnvironmentVariables: InputOutputBuilder](prototype: Val[_], variable: OptionalArgument[String] = None): T ⇒ T =
          this.+=(variable.getOrElse(prototype.name), FromContext.prototype(prototype).map(_.toString)) andThen
            (inputs += prototype)

        def +=[T: EnvironmentVariables: InputOutputBuilder](variable: FromContext[String], value: FromContext[String]): T ⇒ T =
          (implicitly[EnvironmentVariables[T]].environmentVariables add (variable → value))
      }
  }
}

package object external extends ExternalPackage {

  object EnvironmentVariable {
    implicit def fromTuple[N, V](tuple: (N, V))(implicit toFromContextN: ToFromContext[N, String], toFromContextV: ToFromContext[V, String]): EnvironmentVariable =
      EnvironmentVariable(toFromContextN(tuple._1), toFromContextV(tuple._2))

  }

  case class EnvironmentVariable(name: FromContext[String], value: FromContext[String])

  trait EnvironmentVariables[T] {
    def environmentVariables: monocle.Lens[T, Vector[EnvironmentVariable]]
  }

  import org.openmole.tool.file._

  def directoryContentInformation(directory: File, margin: String = "  ") = {
    def fileInformation(file: File) = {
      def permissions = {
        val w = if (file.canWrite) "w" else ""
        val r = if (file.canRead) "r" else ""
        val x = if (file.canExecute) "x" else ""
        s"$r$w$x"
      }

      def fileType =
        if (file.isDirectory) "directory"
        else if (file.isSymbolicLink) "link"
        else if (file.isFile) "file"
        else "unknown"

      s"""${directory.toPath.relativize(file.toPath)} (type=$fileType, permissions=$permissions)"""
    }

    directory.listRecursive(_ ⇒ true).filter(_ != directory).map(fileInformation).map(i ⇒ s"$margin$i").mkString("\n")
  }

}