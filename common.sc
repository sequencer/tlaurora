import mill._
import mill.scalalib._

trait TLAuroraModule extends ScalaModule {
  def chiselModule: ScalaModule
  def chiselPluginJar: T[PathRef]
  def tilelinkModule: ScalaModule
  override def moduleDeps = Seq(chiselModule, tilelinkModule)
  override def scalacPluginClasspath = T(super.scalacPluginClasspath() ++ Some(chiselPluginJar()))
  override def scalacOptions = T(Seq(s"-Xplugin:${chiselPluginJar().path}"))
}

trait TLAuroraDiplomaticModule extends ScalaModule {
  def tlAuroraModule: TLAuroraModule
  // upstream RocketChip in dev branch
  def rocketchipModule: ScalaModule
  override def moduleDeps = super.moduleDeps :+ tlAuroraModule :+ rocketchipModule
  override def scalacOptions = T(Seq(s"-Xplugin:${tlAuroraModule.chiselPluginJar().path}"))
}
