
package org.openmole.plugin.task.python

import org.openmole.core.context.{ Context, Val }
import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import org.openmole.core.fileservice.FileService
import org.openmole.core.networkservice.NetworkService
import org.openmole.core.preference.Preference
import org.openmole.core.threadprovider.ThreadProvider
import org.openmole.core.workflow.builder._
import org.openmole.core.workflow.tools.OptionalArgument
import org.openmole.core.workspace.{ NewFile, Workspace }
import org.openmole.plugin.task.container.HostFile
import org.openmole.plugin.task.external._
import org.openmole.plugin.task.udocker._
import org.openmole.plugin.tool.json._
import org.openmole.tool.outputredirection.OutputRedirection

object PythonTask {

  // could to make distinct image for python 2 and 3
  def dockerImage(major: Int) = DockerImage("python")

  def installCommands(install: Seq[String], libraries: Seq[String], major: Int): Vector[String] = {
    // need to install pip2 in case of python 2
    val effintsall = install++(if (major==2) Seq("curl https://bootstrap.pypa.io/get-pip.py -o get-pip.py","python2 get-pip.py") else Seq.empty)
    (effintsall ++ libraries.map { l ⇒ "pip"+major+" install " + l }).toVector
  }


  def apply(
    script:               RunnableScript,
    major:                Int = 3,
    libraries:            Seq[String]                        = Seq.empty,
    install:              Seq[String]                        = Seq.empty,
    forceUpdate:          Boolean                            = false,
    workDirectory:        OptionalArgument[String]           = None,
    hostFiles:            Seq[HostFile]                      = Vector.empty,
    environmentVariables: Seq[EnvironmentVariable] = Vector.empty,
    errorOnReturnValue:   Boolean                            = true,
    returnValue:          OptionalArgument[Val[Int]]                   = None,
    stdOut:               OptionalArgument[Val[String]]                = None,
    stdErr:               OptionalArgument[Val[String]]                = None)(implicit name: sourcecode.Name, definitionScope: DefinitionScope, newFile: NewFile, workspace: Workspace, preference: Preference, fileService: FileService, threadProvider: ThreadProvider, outputRedirection: OutputRedirection, networkService: NetworkService) = {

    lazy val containerPoolKey = UDockerTask.newCacheKey

    val udocker =
      UDockerTask.createUDocker(
        dockerImage(major),
        install = installCommands(install, libraries,major),
        cacheInstall = true,
        forceUpdate = forceUpdate,
        mode = "P1",
        reuseContainer = true,
        hostFiles = hostFiles,
        workDirectory = workDirectory,
        environmentVariables = environmentVariables.toVector)

    Task("PythonTask") { p ⇒
      import org.json4s.jackson.JsonMethods._
      import p._
      import Mapped.noFile

      def writeInputsJSON(file: File): Unit = {
        def values = noFile(mapped.inputs).map { m => p.context(m.v) }
        file.content = compact(render(toJSONValue(values.toArray)))
      }

      def readOutputJSON(file: File) = {
        import org.json4s._
        import org.json4s.jackson.JsonMethods._
        val outputValues = parse(file.content)
        (outputValues.asInstanceOf[JArray].arr zip noFile(mapped.outputs).map(_.v)).map { case (jvalue, v) ⇒ jValueToVariable(jvalue, v) }
      }

      def inputMapping(dicoName: String): String =
        noFile(mapped.inputs).zipWithIndex.map {
          case (m, i) ⇒ s"${m.name} = $dicoName[${i}]"
        }.mkString("\n")

      def outputMapping: String =
        s"""[${noFile(mapped.outputs).map { m ⇒ m.name }.mkString(",")}]"""

      val resultContext: Context = p.newFile.withTmpFile("script", ".py") { scriptFile ⇒
        p.newFile.withTmpFile("inputs", ".json") { jsonInputs ⇒

          def inputArrayName = "_generateddata_"
          def scriptName = "_generatescript_.py"
          def inputJSONName = "_inputs_.json"
          def outputJSONName = "_outputs_.json"

          writeInputsJSON(jsonInputs)
          scriptFile.content =
            s"""
               |import json
               |f = open('/$inputJSONName','r')
               |print(f.readlines())
               |$inputArrayName = json.load(open('/$inputJSONName'))
               |${inputMapping(inputArrayName)}
               |${RunnableScript.content(script)}
               |json.dump($outputMapping, open('/$outputJSONName','w'))
        """.stripMargin

          val outputFile = Val[File]("outputFile", Namespace("PythonTask"))

          def uDockerTask =
            UDockerTask(
              udocker,
              commands = s"python${major.toString} $scriptName",
              errorOnReturnValue = errorOnReturnValue,
              returnValue = returnValue,
              stdOut = stdOut,
              stdErr = stdErr,
              config = InputOutputConfig(),
              external = External(),
              info = InfoConfig(),
              containerPoolKey = containerPoolKey) set (
                resources += (scriptFile, scriptName, true),
                resources += (jsonInputs, inputJSONName, true),
                outputFiles += (outputJSONName, outputFile),
              Mapped.files(mapped.inputs).map { case m ⇒ inputFiles +=[UDockerTask] (m.v, m.name, true) },
              Mapped.files(mapped.outputs).map { case m ⇒ outputFiles +=[UDockerTask] (m.name, m.v) }
              )

          val resultContext = uDockerTask.process(p.executionContext).from(p.context)(p.random, p.newFile, p.fileService)
          resultContext ++ readOutputJSON(resultContext(outputFile))
        }
      }
      resultContext
    } validate { _ ⇒ Seq.empty } set (
      outputs += (Seq(returnValue.option, stdOut.option, stdErr.option).flatten: _*)
    )
  }

}

