package superposition

import engine.core.{Game, Settings}
import engine.graphics.Camera
import engine.util.Color
import engine.util.math.Vec2d
import extras.behaviors.{FPSBehavior, QuitOnEscapeBehavior}

object Superposition {
  def main(args: Array[String]): Unit = {
    Settings.WINDOW_WIDTH = 1280
    Settings.WINDOW_HEIGHT = 720
    Settings.BACKGROUND_COLOR = Color.GRAY
    Game.init()

    Camera.camera2d.upperRight = new Vec2d(16, 9)

    new FPSBehavior().create()
    new QuitOnEscapeBehavior().create()
    Multiverse.init()
    new Multiverse().create()

    Game.run()
  }
}
