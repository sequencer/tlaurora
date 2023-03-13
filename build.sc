import mill._
import mill.scalalib._
import mill.scalalib.publish._
import mill.scalalib.scalafmt._
import coursier.maven.MavenRepository
import $file.dependencies.cde.build
import $file.dependencies.`berkeley-hardfloat`.build
import $file.dependencies.`rocket-chip`.common
import $file.dependencies.chisel.build
import $file.dependencies.tilelink.common
import $file.common

object v {
  val scala = "2.13.10"
}

object mychisel extends dependencies.chisel.build.Chisel(v.scala) {
  override def millSourcePath = os.pwd / "dependencies" / "chisel"
}

// RocketChip
object mycde extends dependencies.cde.build.cde(v.scala) with PublishModule {
  override def millSourcePath = os.pwd /  "dependencies" / "cde" / "cde"
}
object myhardfloat extends dependencies.`berkeley-hardfloat`.build.hardfloat {
  override def millSourcePath = os.pwd /  "dependencies" / "berkeley-hardfloat"
  override def scalaVersion = v.scala
  def chisel3Module: Option[PublishModule] = Some(mychisel)
  override def scalacPluginClasspath = T { super.scalacPluginClasspath() ++ Agg(mychisel.pluginModule.jar()) }
  override def scalacOptions = T(Seq(s"-Xplugin:${mychisel.pluginModule.jar().path}"))
}
object myrocketchip extends dependencies.`rocket-chip`.common.CommonRocketChip {
  override def millSourcePath = os.pwd /  "dependencies" / "rocket-chip"
  def chisel3Module: Option[PublishModule] = Some(mychisel)
  def hardfloatModule: PublishModule = myhardfloat
  def cdeModule: PublishModule = mycde
  override def scalaVersion = v.scala
  override def scalacPluginClasspath = T { super.scalacPluginClasspath() ++ Agg(mychisel.pluginModule.jar()) }
  override def scalacOptions = T(Seq(s"-Xplugin:${mychisel.pluginModule.jar().path}"))
}
// Standalone TileLink
object mytilelink extends dependencies.tilelink.common.TileLinkModule {
  override def millSourcePath = os.pwd /  "dependencies" / "tilelink" / "tilelink"
  def scalaVersion = T(v.scala)
  def chisel3Module = mychisel
  def chisel3PluginJar = T(mychisel.pluginModule.jar())
}

object tlaurora extends common.TLAuroraModule { m =>
  def scalaVersion = T(v.scala)
  def chiselModule = mychisel
  def chiselPluginJar = T(mychisel.pluginModule.jar())
  def tilelinkModule = mytilelink
}
object tlauroradiplomatic extends common.TLAuroraDiplomaticModule { m =>
  def scalaVersion = T(v.scala)
  def tlAuroraModule = tlaurora
  def rocketchipModule = myrocketchip
}
