package superposition.game.component

import com.badlogic.ashley.core.{Component, ComponentMapper}
import com.badlogic.gdx.Gdx.gl
import com.badlogic.gdx.graphics.Color.RED
import com.badlogic.gdx.graphics.GL20.GL_BLEND
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.{Filled, Line}
import superposition.game.component.Beam.{BeamDuration, FadeDuration, Length}
import superposition.math.{Direction, Vector2i}
import superposition.quantum.{Gate, MetaId, StateId, Universe}

import scala.math.min

final class Beam(
  multiverse: Multiverse,
  val gate: Gate[StateId[Boolean]],
  val source: Vector2i,
  val direction: Direction,
  val controls: Iterable[Vector2i])
  extends Component {

  val lastTarget: MetaId[Option[Vector2i]] = multiverse.allocateMeta(None)

  val elapsedTime: MetaId[Double] = multiverse.allocateMeta(0)

  val path: LazyList[Vector2i] = LazyList.iterate(source)(_ + direction.toVec2i).tail.take(Length)

  private val shapeRenderer = new ShapeRenderer

  def draw(universe: Universe): Unit = {
    shapeRenderer.setProjectionMatrix(multiverse.camera.combined)

    if (multiverse.selected(source)) {
      shapeRenderer.begin(Line)
      shapeRenderer.setColor(RED)
      shapeRenderer.rect(source.x, source.y, 1, 1)
      shapeRenderer.end()
    }

    universe.meta(lastTarget) match {
      case Some(target) if universe.meta(elapsedTime) <= BeamDuration + FadeDuration =>
        val opacity = min(FadeDuration, BeamDuration + FadeDuration - universe.meta(elapsedTime)) / FadeDuration
        gl.glEnable(GL_BLEND)
        shapeRenderer.begin(Filled)
        shapeRenderer.setColor(1, 0, 0, opacity.toFloat)
        shapeRenderer.rect(source.x + 0.5f, source.y + 0.375f, target.x - source.x, 0.25f)
        shapeRenderer.end()
        gl.glDisable(GL_BLEND)
      case _ => ()
    }
  }
}

object Beam {
  val Mapper: ComponentMapper[Beam] = ComponentMapper.getFor(classOf[Beam])

  private val Length: Int = 25

  private val BeamDuration: Double = 0.2

  private val FadeDuration: Double = 0.3
}
