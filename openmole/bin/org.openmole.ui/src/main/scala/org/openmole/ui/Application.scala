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

package org.openmole.ui

import java.awt.Desktop
import java.io.{ File, FileOutputStream, IOException }
import java.util.logging.Level
import java.net.URI

import org.openmole.console.Console.ExitCodes
import org.openmole.core.project._
import org.openmole.core.console.ScalaREPL
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.logconfig.LoggerConfig
import org.openmole.core.pluginmanager.PluginManager
import org.openmole.core.workspace.Workspace
import org.openmole.rest.server.RESTServer
import org.openmole.tool.logger.JavaLogger

import annotation.tailrec
import org.openmole.gui.server.core._
import org.openmole.console._
import org.openmole.tool.file._
import org.openmole.tool.hash._
import org.openmole.core.{ location, module }
import org.openmole.core.outputmanager.OutputManager
import org.openmole.core.preference._
import org.openmole.core.services._
import org.openmole.core.networkservice._
import org.openmole.tool.outputredirection.OutputRedirection

object Application extends JavaLogger {

  import Log._

  lazy val consoleSplash =
    """
      |  ___                   __  __  ___  _     _____    ___
      | / _ \ _ __   ___ _ __ |  \/  |/ _ \| |   | ____|  ( _ )
      || | | | '_ \ / _ \ '_ \| |\/| | | | | |   |  _|    / _ \
      || |_| | |_) |  __/ | | | |  | | |_| | |___| |___  | (_) |
      | \___/| .__/ \___|_| |_|_|  |_|\___/|_____|_____|  \___/
      |      |_|
      |""".stripMargin

  lazy val consoleUsage = "(Type :q to quit)"

  def run(args: Array[String]): Int = {

    sealed trait LaunchMode
    object ConsoleMode extends LaunchMode
    object GUIMode extends LaunchMode
    object HelpMode extends LaunchMode
    object RESTMode extends LaunchMode
    case class Reset(initialisePassword: Boolean) extends LaunchMode
    case class TestCompile(files: List[File]) extends LaunchMode

    case class Config(
      userPlugins:          List[String]    = Nil,
      loadHomePlugins:      Option[Boolean] = None,
      scriptFile:           Option[String]  = None,
      consoleWorkDirectory: Option[File]    = None,
      password:             Option[String]  = None,
      passwordFile:         Option[File]    = None,
      workspace:            Option[File]    = None,
      hostName:             Option[String]  = None,
      launchMode:           LaunchMode      = GUIMode,
      ignored:              List[String]    = Nil,
      port:                 Option[Int]     = None,
      loggerLevel:          Option[String]  = None,
      unoptimizedJS:        Boolean         = false,
      remote:               Boolean         = false,
      http:                 Boolean         = false,
      browse:               Boolean         = true,
      proxyURI:             Option[String]  = None,
      httpSubDirectory:     Option[String]  = None,
      args:                 List[String]    = Nil,
      extraHeader:          Option[File]    = None
    )

    def takeArg(args: List[String]) =
      args match {
        case h :: t ⇒ h
        case Nil    ⇒ ""
      }

    def dropArg(args: List[String]) =
      args match {
        case h :: t ⇒ t
        case Nil    ⇒ Nil
      }

    def takeArgs(args: List[String]) = args.takeWhile(!_.startsWith("-"))
    def dropArgs(args: List[String]) = args.dropWhile(!_.startsWith("-"))

    def usage =
      """OpenMOLE application options:
      |[-p | --plugin list of arg] plugins list of jar or category containing jars to be loaded
      |[-c | --console] console mode
      |[--port port] specify the port for the GUI or REST API
      |[--script path] a path of an OpenMOLE script to execute
      |[--password password] openmole password
      |[--password-file file containing a password] read the OpenMOLE password (--password option) in a file
      |[--workspace directory] run openmole with an alternative workspace location
      |[--rest] run the REST server
      |[--remote] enable remote connection to the web interface
      |[--http] force http connection instead of https in remote mode for the web interface
      |[--no-browser] don't automatically launch the browser in GUI mode
      |[--unoptimizedJS] do not optimize JS (do not use Google Closure Compiler)
      |[--extra-header path] specify a file containing a piece of html code to be inserted in the GUI html header file
      |[--load-workspace-plugins] load the plugins of the OpenMOLE workspace (these plugins are always loaded in GUI mode)
      |[--console-work-directory] specify the workDirectory variable in console mode (it is set to the current directory by default)
      |[--reset] reset all preferences and authentications
      |[--reset-password] reset all preferences and ask for the a password
      |[--mem memory] allocate more memory to the JVM (not supported on windows yes), for instance --mem 2G
      |[--logger-level level] set the level of logging (OFF, SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST, ALL)
      |[--proxy hostname] set the proxy to use to install containers or R packages, in the form http://myproxy.org:3128
      |[--http-sub-directory] set the subdirectory for openmole app (for non-root path). No '/' is required (Example: "user1")
      |[--] end of options the remaining arguments are provided to the console in the args array
      |[-h | --help] print help""".stripMargin

    def parse(args: List[String], c: Config = Config()): Config = {
      def plugins(tail: List[String]) = parse(dropArgs(tail), c.copy(userPlugins = takeArgs(tail)))
      def help(tail: List[String]) = parse(tail, c.copy(launchMode = HelpMode))
      def script(tail: List[String]) = parse(dropArg(tail), c.copy(scriptFile = Some(takeArg(tail)), launchMode = ConsoleMode))
      def console(tail: List[String]) = parse(tail, c.copy(launchMode = ConsoleMode))
      args match {
        case "-p" :: tail                       ⇒ plugins(tail)
        case "--plugins" :: tail                ⇒ plugins(tail)
        case "-c" :: tail                       ⇒ console(tail)
        case "--console" :: tail                ⇒ console(tail)
        case "-s" :: tail                       ⇒ script(tail)
        case "--script" :: tail                 ⇒ script(tail)
        case "--port" :: tail                   ⇒ parse(tail.tail, c.copy(port = Some(tail.head.toInt)))
        case "--password" :: tail               ⇒ parse(dropArg(tail), c.copy(password = Some(takeArg(tail))))
        case "--password-file" :: tail          ⇒ parse(dropArg(tail), c.copy(passwordFile = Some(new File(takeArg(tail)))))
        case "--workspace" :: tail              ⇒ parse(dropArg(tail), c.copy(workspace = Some(new File(takeArg(tail)))))
        case "--rest" :: tail                   ⇒ parse(tail, c.copy(launchMode = RESTMode))
        case "--load-workspace-plugins" :: tail ⇒ parse(tail, c.copy(loadHomePlugins = Some(true)))
        case "--console-work-directory" :: tail ⇒ parse(dropArg(tail), c.copy(consoleWorkDirectory = Some(new File(takeArg(tail)))))
        case "--logger-level" :: tail           ⇒ parse(tail.tail, c.copy(loggerLevel = Some(tail.head)))
        case "--remote" :: tail                 ⇒ parse(tail, c.copy(remote = true))
        case "--http" :: tail                   ⇒ parse(tail, c.copy(http = true))
        case "--no-browser" :: tail             ⇒ parse(tail, c.copy(browse = false))
        case "--unoptimizedJS" :: tail          ⇒ parse(tail, c.copy(unoptimizedJS = true))
        case "--extra-header" :: tail           ⇒ parse(dropArg(tail), c.copy(extraHeader = Some(new File(takeArg(tail)))))
        case "--reset" :: tail                  ⇒ parse(tail, c.copy(launchMode = Reset(initialisePassword = false)))
        case "--host-name" :: tail              ⇒ parse(tail.tail, c.copy(hostName = Some(tail.head)))
        case "--reset-password" :: tail         ⇒ parse(tail, c.copy(launchMode = Reset(initialisePassword = true)))
        case "--proxy" :: tail                  ⇒ parse(tail.tail, c.copy(proxyURI = Some(tail.head)))
        case "--http-sub-directory" :: tail     ⇒ parse(tail.tail, c.copy(httpSubDirectory = Some(tail.head)))
        case "--" :: tail                       ⇒ parse(Nil, c.copy(args = tail))
        case "-h" :: tail                       ⇒ help(tail)
        case "--help" :: tail                   ⇒ help(tail)
        case "--test-compile" :: tail           ⇒ parse(dropArgs(tail), c.copy(launchMode = TestCompile(takeArgs(tail).map(p ⇒ new File(p)))))
        case s :: tail                          ⇒ parse(tail, c.copy(ignored = s :: c.ignored))
        case Nil                                ⇒ c
      }
    }

    PluginManager.startAll.foreach { case (b, e) ⇒ logger.log(WARNING, s"Error staring bundle $b", e) }

    val config = parse(args.map(_.trim).toList)

    val logLevel = config.loggerLevel.map(l ⇒ Level.parse(l.toUpperCase))
    logLevel.foreach(LoggerConfig.level)

    val workspaceDirectory = config.workspace.getOrElse(org.openmole.core.workspace.defaultOpenMOLEDirectory)
    implicit val workspace = Workspace(workspaceDirectory)
    import org.openmole.tool.thread._
    Runtime.getRuntime.addShutdownHook(thread(Workspace.clean(workspace)))

    def loadPlugins = {
      val (existingUserPlugins, notExistingUserPlugins) = config.userPlugins.span(new File(_).exists)

      if (!notExistingUserPlugins.isEmpty) logger.warning(s"""Some plugins or plugin folders don't exist: ${notExistingUserPlugins.mkString(",")}""")

      val userPlugins =
        existingUserPlugins.flatMap { p ⇒ PluginManager.listBundles(new File(p)) } ++ module.allModules

      logger.fine(s"Loading user plugins " + userPlugins)

      PluginManager.tryLoad(userPlugins)
    }

    def displayErrors(load: ⇒ Iterable[(File, Throwable)]) =
      load.foreach { case (f, e) ⇒ logger.log(WARNING, s"Error loading bundle $f", e) }

    def password = config.password orElse config.passwordFile.map(_.lines.head)

    if (!config.ignored.isEmpty) logger.warning("Ignored options: " + config.ignored.reverse.mkString(" "))

    config.launchMode match {
      case HelpMode ⇒
        println(usage)
        Console.ExitCodes.ok
      case Reset(initialisePassword) ⇒
        implicit val preference = Services.preference(workspace)
        implicit val authenticationStore = Services.authenticationStore(workspace)
        Services.resetPassword
        if (initialisePassword) Console.initPassword
        Console.ExitCodes.ok
      case RESTMode ⇒
        implicit val preference = Services.preference(workspace)
        displayErrors(loadPlugins)

        val passwordString = password match {
          case Some(p) ⇒ p
          case None    ⇒ Console.initPassword
        }

        if (!Console.testPassword(passwordString)) {
          println("Password is incorrect")
          Console.ExitCodes.incorrectPassword
        }
        else {
          Services.withServices(workspaceDirectory, passwordString, config.proxyURI, logLevel) { services ⇒
            val server = new RESTServer(config.port, config.hostName, services, config.httpSubDirectory)
            server.run()
          }
          Console.ExitCodes.ok
        }
      case ConsoleMode ⇒
        implicit val preference = Services.preference(workspace)

        val passwordString = password match {
          case Some(p) ⇒ p
          case None    ⇒ Console.initPassword
        }

        if (!Console.testPassword(passwordString)) {
          println("Password is incorrect")
          Console.ExitCodes.incorrectPassword
        }
        else {
          print(consoleSplash)
          println(consoleUsage)
          Console.dealWithLoadError(loadPlugins, !config.scriptFile.isDefined)
          Services.withServices(workspaceDirectory, passwordString, config.proxyURI, logLevel) { implicit services ⇒
            val console = new Console(config.scriptFile)
            console.run(config.args, config.consoleWorkDirectory)
          }
        }
      case GUIMode ⇒
        implicit val preference = Services.preference(workspace)

        // FIXME switch to a GUI display in the plugin panel
        displayErrors(loadPlugins)

        def browse(url: String) =
          if (Desktop.isDesktopSupported) Desktop.getDesktop.browse(new URI(url))

        GUIServer.lockFile.withFileOutputStream { fos ⇒
          val launch = (config.remote || fos.getChannel.tryLock != null)
          if (launch) {
            GUIServer.initialisePreference(preference)
            val port = config.port.getOrElse(preference(GUIServer.port))

            val extraHeader = config.extraHeader.map { _.content }.getOrElse("")

            def useHTTP = config.http || !config.remote

            def protocol = if (useHTTP) "http" else "https"

            val url = s"$protocol://localhost:$port"

            GUIServer.urlFile.content = url

            GUIServices.withServices(workspace, config.proxyURI, logLevel) { services ⇒
              val server = new GUIServer(port, config.remote, useHTTP, services, config.password, extraHeader, !config.unoptimizedJS, config.httpSubDirectory)
              server.start()
              if (config.browse && !config.remote) browse(url)
              server.launchApplication()
              logger.info(s"Server listening on port $port.")
              server.join() match {
                case GUIServer.Ok      ⇒ Console.ExitCodes.ok
                case GUIServer.Restart ⇒ Console.ExitCodes.restart
              }
            }
          }
          else {
            browse(GUIServer.urlFile.content)
            Console.ExitCodes.ok
          }
        }
      case TestCompile(files) ⇒
        import org.openmole.tool.hash._

        def success(f: File) = f.getParentFileSafe / (f.hash().toString + ".success")
        def toFile(f: File) = if (f.isDirectory) f.listFiles().toList else Seq(f)
        def isTestable(f: File) = f.getName.endsWith(".omt") || f.getName.endsWith(".oms")

        val results = Test.withTmpServices { implicit services ⇒
          import services._
          files.flatMap(toFile).filter(isTestable).map { file ⇒

            def processResult(c: CompileResult) =
              c match {
                case s: ScriptFileDoesNotExists ⇒ util.Failure(new IOException("File doesn't exists"))
                case s: CompilationError        ⇒ util.Failure(s.error)
                case s: Compiled                ⇒ util.Success("Compilation succeeded")
              }

            val res = if (!success(file).exists) {
              println(s"Testing: ${file.getName}")
              file → processResult(Project.compile(file.getParentFileSafe, file, args))
            }
            else {
              file -> util.Success("Compilation succeeded (from previous test)")
            }

            if (res._2.isSuccess) success(file) < "success"
            print("\33[1A\33[2K")
            println(s"${res._1.getName}: ${res._2}")

            res
          }
        }

        val errors =
          results.filter {
            case (_, util.Success(_)) ⇒ false
            case _                    ⇒ true
          }

        if (errors.isEmpty) Console.ExitCodes.ok
        else Console.ExitCodes.compilationError

    }

  }

}
