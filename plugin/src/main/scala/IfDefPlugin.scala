package com.eed3si9n.ifdef.sbtifdef

import sbt.*
import Keys.*

object IfDefPlugin extends AutoPlugin {
  private final val macroSetting = "com.eed3si9n.ifdef.declare:"

  override def requires = plugins.JvmPlugin
  override def trigger = allRequirements

  object autoImport extends IfDefKeys
  import autoImport._
  override lazy val globalSettings: Seq[Def.Setting[_]] = Seq(
    ifDefDeclations := Nil,
  )
  override lazy val projectSettings: Seq[Def.Setting[_]] = Seq(
    Compile / scalacOptions ++= {
      if (scalaVersion.value.startsWith("2.13")) List("-Ymacro-annotations")
      else Nil
    },
    libraryDependencies += "com.eed3si9n.ifdef" %% "ifdef-macro" % BuildInfo.version,
    libraryDependencies ++= {
      if (scalaVersion.value.startsWith("2.12.")) List(compilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full))
      else Nil
    },
    Compile / ifDefDeclations += "compile",
    Compile / scalacOptions ++= {
      val sv = scalaVersion.value
      val decls = (Compile / ifDefDeclations).value
      toMacroSettings(sv, decls.toList)
    },
    Test / ifDefDeclations += "test",
    Test / scalacOptions ++= {
      val sv = scalaVersion.value
      val decls = (Test / ifDefDeclations).value
      toMacroSettings(sv, decls.toList)
    },
    Test / managedSources ++= (Compile / sources).value,
    Test / internalDependencyClasspath := {
      val orig = (Test / internalDependencyClasspath).value
      val compileOut = (Compile / classDirectory).value
      orig.filter { x =>
        x.data != compileOut
      }
    },
  )

  def toMacroSettings(sv: String, decls: List[String]): List[String] = {
    if (sv.startsWith("2."))
      decls.flatMap { decl =>
        List("-Xmacro-settings", s"$macroSetting$decl")
      }
    else
      decls.flatMap { decl =>
        List(s"-Xmacro-settings:$macroSetting$decl")
      }
  }
}

trait IfDefKeys {
  lazy val ifDefDeclations = taskKey[Seq[String]]("Declarations")
}