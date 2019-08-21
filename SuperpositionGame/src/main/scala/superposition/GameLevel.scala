package superposition

import engine.core.Behavior.Entity
import engine.core.Game.{declareSystem, dt}
import engine.core.Input
import engine.graphics.Camera
import engine.graphics.Camera.Camera2d
import engine.graphics.opengl.{Framebuffer, Shader, Texture}
import engine.graphics.sprites.Sprite
import engine.util.Color._
import engine.util.math.{Transformation, Vec2d}
import org.lwjgl.glfw.GLFW._

import scala.math.{Pi, pow, sqrt}

private object GameLevel {
  private object Gate extends Enumeration {
    val X, Z, T, H = Value
  }

  private val NumObjects: Int = 2

  def init(): Unit =
    declareSystem(classOf[GameLevel], (level: GameLevel) => level.step())

  private def getStateId(universe: Universe): Int =
    universe.state
      .zipWithIndex
      .map({ case (state, index) =>
        if (state.onOff) pow(2, index).toInt
        else 0
      })
      .sum
}

private class GameLevel extends Entity {
  import GameLevel._

  private var universes: List[Universe] = List(new Universe(NumObjects))
  private val frameBuffer: Framebuffer = new Framebuffer()
  private val colorBuffer: Texture = frameBuffer.attachColorBuffer()
  private val shader: Shader = Shader.load("universe")
  private var time: Double = 0.0

  private def applyGate(gate: Gate.Value, target: Int, controls: Int*): Unit = {
    for (u <- universes.filter(u => controls.forall(u.state(_).onOff))) {
      gate match {
        case Gate.X => u.state(target).onOff = !u.state(target).onOff
        case Gate.Z =>
          if (u.state(target).onOff) {
            u.amplitude *= Complex(-1.0)
          }
        case Gate.T =>
          if (u.state(target).onOff) {
            u.amplitude *= Complex.polar(1.0, Pi / 4.0)
          }
        case Gate.H =>
          u.amplitude /= Complex(sqrt(2.0))
          val copy = u.copy()
          if (u.state(target).onOff) {
            u.amplitude *= Complex(-1.0)
          }
          copy.state(target).onOff = !copy.state(target).onOff
          universes = copy :: universes
      }
    }
  }

  private def combine(): Unit =
    universes = universes
      .groupMapReduce(getStateId)(identity)((u1, u2) => {
        u2.amplitude += u1.amplitude
        u2
      })
      .values
      .filter(_.amplitude.magnitudeSquared > 1e-6)
      .toList

  private def normalize(): Unit = {
    val sum = universes.map(_.amplitude.magnitudeSquared).sum
    for (u <- universes) {
      u.amplitude /= Complex(sqrt(sum))
    }
  }

  private def step(): Unit = {
    val selected = universes
      .flatMap(_.state.zipWithIndex)
      .filter(_._1.position.sub(Input.mouse()).length() < 0.5)
      .map(_._2)
      .toSet
    for (i <- selected) {
      if (Input.keyJustPressed(GLFW_KEY_X)) {
        applyGate(Gate.X, i)
      }
      if (Input.keyJustPressed(GLFW_KEY_Z)) {
        applyGate(Gate.Z, i)
      }
      if (Input.keyJustPressed(GLFW_KEY_T)) {
        applyGate(Gate.T, i)
      }
      if (Input.keyJustPressed(GLFW_KEY_H)) {
        applyGate(Gate.H, i)
      }
    }

    for (u <- universes) {
      u.physicsStep()
    }

    combine()
    normalize()
    draw()
  }

  private def draw(): Unit = {
    time += dt()
    shader.setUniform("time", time.asInstanceOf[Float])

    var minVal = 0.0
    for (u <- universes) {
      val maxVal = minVal + u.amplitude.magnitudeSquared

      frameBuffer.clear(CLEAR)
      for (s <- u.state) {
        val color = if (s.onOff) WHITE else BLACK
        Sprite.load("cat.png").draw(Transformation.create(s.position, 0, 1), color)
      }

      val camera = new Camera2d()
      camera.lowerLeft = new Vec2d(-1, -1)
      Camera.current = camera
      shader.setMVP(Transformation.IDENTITY)
      shader.setUniform("minVal", minVal.asInstanceOf[Float])
      shader.setUniform("maxVal", maxVal.asInstanceOf[Float])
      shader.setUniform("hue", (u.amplitude.phase / (2.0 * Pi)).asInstanceOf[Float])
      Framebuffer.drawToWindow(colorBuffer, shader)
      Camera.current = Camera.camera2d

      minVal = maxVal
    }
  }
}
