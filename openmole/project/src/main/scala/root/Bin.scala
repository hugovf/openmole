package root

import root.libraries.Apache
import sbt._
import Keys._

import org.openmole.buildsystem.OMKeys._
import org.openmole.buildsystem._, Assembly._
import Libraries._
import com.typesafe.sbt.osgi.OsgiKeys._
import sbt.inc.Analysis
import sbtunidoc.Plugin._
import UnidocKeys._
//import fr.iscpif.jsmanager.JSManagerPlugin._

object Bin extends Defaults(Base, Gui, Libraries, ThirdParties, Web) {
  val dir = file("bin")

  private val equinoxDependencies = libraryDependencies ++= Seq(
    equinoxApp intransitive (),
    equinoxContenttype intransitive (),
    equinoxJobs intransitive (),
    equinoxRuntime intransitive (),
    equinoxCommon intransitive (),
    equinoxLauncher intransitive (),
    equinoxRegistry intransitive (),
    equinoxPreferences intransitive (),
    equinoxOsgi intransitive (),
    Libraries.bouncyCastle intransitive ()
  )

  lazy val openmoleui = OsgiProject("org.openmole.ui", singleton = true, buddyPolicy = Some("global")) settings (
    equinoxDependencies,
    bundleType := Set("core"),
    organization := "org.openmole.ui"
  ) /*settings (jsManagerSettings: _*)*/ settings (
      libraryDependencies ++= Seq(jodaTime, scalaLang, jasypt, Apache.config, Apache.ant, jline, Apache.logging, Apache.log4j, scopt, robustIt)
    ) dependsOn (
        base.Misc.workspace, base.Misc.replication, base.Misc.exception, base.Misc.tools, base.Misc.eventDispatcher,
        base.Misc.pluginManager, base.Core.implementation, base.Core.batch, gui.Server.core, gui.Client.client, base.Misc.sftpserver, base.Misc.logging,
        Web.core, base.Misc.console, base.Core.convenience)

  private lazy val openmolePluginDependencies = libraryDependencies ++= Seq(
    Libraries.gridscaleHTTP,
    Libraries.gridscalePBS,
    Libraries.gridscaleSLURM,
    Libraries.gridscaleDirac,
    Libraries.gridscaleGlite,
    Libraries.gridscaleSGE,
    Libraries.gridscaleCondor,
    Libraries.gridscalePBS,
    Libraries.gridscaleOAR
  ) ++ Libraries.gridscaleSSH

  lazy val uiProjects = resourceSets <++= (subProjects ++ Seq(openmoleui.project)).keyFilter(bundleType, (a: Set[String]) ⇒ a contains "core") sendTo "plugins"

  lazy val pluginProjects = resourceSets <++= subProjects.keyFilter(bundleType, (a: Set[String]) ⇒ a contains "plugin", true) sendTo "openmole-plugins"

  lazy val guiPluginProjects = resourceSets <++= subProjects.keyFilter(bundleType, (a: Set[String]) ⇒ a.contains("guiPlugin"), true) sendTo "openmole-plugins-gui"

  lazy val openmole = AssemblyProject("openmole", "plugins", settings = resAssemblyProject ++ uiProjects ++ pluginProjects ++ guiPluginProjects ++ dbserverProjects ++ zipProject, depNameMap =
    Map(
      """org\.eclipse\.equinox\.launcher.*\.jar""".r -> { s ⇒ "org.eclipse.equinox.launcher.jar" },
      """org\.eclipse\.(core|equinox|osgi)""".r -> { s ⇒ s.replaceFirst("-", "_") }
    )
  ) settings (
    equinoxDependencies, libraryDependencies += Libraries.gridscale intransitive (),
    resourceSets <++= (baseDirectory, zip in openmoleRuntime, downloadUrls in openmoleRuntime) map { (bd, zipFile, downloadUrls) ⇒
      Set(bd / "resources" -> "", zipFile -> "runtime") ++ downloadUrls.map(_ -> "runtime")
    },
    resourceSets <+= (baseDirectory) map { _ / "db-resources" -> "dbserver/bin" },
    resourceSets <+= (copyDependencies in openmolePlugins) map { _ -> "openmole-plugins" },
    setExecutable += "openmole",
    tarGZName := Some("openmole"),
    innerZipFolder := Some("openmole"),
    dependencyFilter := DependencyFilter.fnToModuleFilter { m ⇒ m.organization == "org.eclipse.core" || m.organization == "fr.iscpif.gridscale.bundle" || m.organization == "org.bouncycastle" || m.organization == "org.openmole" }
  ) dependsOn (openmoleui) //todo, add dependency mapping or something

  lazy val openmolePlugins = AssemblyProject("openmole-plugins") settings (openmolePluginDependencies, //TODO: This project is only necessary thanks to the lack of dependency mapping in AssemblyProject
    dependencyFilter := DependencyFilter.fnToModuleFilter { m ⇒ m.extraAttributes get ("project-name") map (_ == projectName) getOrElse (m.organization == "fr.iscpif.gridscale.bundle") }
  )

  lazy val dbserverProjects = resourceSets <++= subProjects.keyFilter(bundleType, (a: Set[String]) ⇒ a contains "dbserver") sendTo "dbserver/lib"

  lazy val runtimeProjects = resourceSets <++= subProjects.keyFilter(bundleType, (a: Set[String]) ⇒ a contains "runtime") sendTo "plugins"

  lazy val java368URL = new URL("http://maven.iscpif.fr/thirdparty/com/oracle/java-jre-linux-386/20-b17/java-jre-linux-386-20-b17.tgz")
  lazy val javax64URL = new URL("http://maven.iscpif.fr/thirdparty/com/oracle/java-jre-linux-x64/20-b17/java-jre-linux-x64-20-b17.tgz")

  lazy val openmoleRuntime = AssemblyProject("runtime", "plugins",
    depNameMap = Map("""org\.eclipse\.equinox\.launcher.*\.jar""".r -> { s ⇒ "org.eclipse.equinox.launcher.jar" },
      """org\.eclipse\.(core|equinox|osgi)""".r -> { s ⇒ s.replaceFirst("-", "_") }), settings = resAssemblyProject ++ zipProject ++ urlDownloadProject ++ runtimeProjects) settings
    (equinoxDependencies, resourceDirectory <<= baseDirectory / "resources",
      urls <++= target { t ⇒ Seq(java368URL -> t / "jvm-386.tar.gz", javax64URL -> t / "jvm-x64.tar.gz") },
      libraryDependencies += Libraries.gridscale intransitive (),
      tarGZName := Some("runtime"),
      setExecutable += "run.sh",
      resourceSets <+= baseDirectory map { _ / "resources" -> "." },
      dependencyFilter := DependencyFilter.fnToModuleFilter { m ⇒ (m.organization == "org.eclipse.core" || m.organization == "fr.iscpif.gridscale.bundle" || m.organization == "org.openmole") })

  lazy val daemonProjects =
    resourceSets <++= subProjects.keyFilter(bundleType, (a: Set[String]) ⇒ (a contains "core") || (a contains "daemon")) sendTo "plugins"

  lazy val openmoleDaemon = AssemblyProject("daemon", "plugins", settings = resAssemblyProject ++ daemonProjects, depNameMap =
    Map("""org\.eclipse\.equinox\.launcher.*\.jar""".r -> { s ⇒ "org.eclipse.equinox.launcher.jar" }, """org\.eclipse\.(core|equinox|osgi)""".r -> { s ⇒ s.replaceFirst("-", "_") })) settings
    (resourceSets <+= baseDirectory map { _ / "resources" -> "." },
      equinoxDependencies,
      libraryDependencies += gridscale,
      libraryDependencies ++= gridscaleSSH,
      libraryDependencies += bouncyCastle,
      setExecutable += "openmole-daemon",
      dependencyFilter := DependencyFilter.fnToModuleFilter { m ⇒ m.extraAttributes get ("project-name") map (_ == projectName) getOrElse (m.organization == "org.eclipse.core" || m.organization == "fr.iscpif.gridscale.bundle" || m.organization == "org.bouncycastle" || m.organization == "org.openmole") })

  lazy val docProj = Project("documentation", dir / "documentation") aggregate ((Base.subProjects ++ Gui.subProjects ++ Web.subProjects): _*) settings (
    unidocSettings: _*
  ) settings (compile := Analysis.Empty, scalacOptions in (ScalaUnidoc, unidoc) += "-Ymacro-no-expand",
      unidocProjectFilter in (ScalaUnidoc, unidoc) := inAnyProject -- inProjects(Libraries.subProjects: _*) -- inProjects(ThirdParties.subProjects: _*)
    )
}
