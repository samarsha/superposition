package superposition.math

import cats.data.NonEmptyList

/** A quantum unitary transformation. */
sealed trait Unitary {
  /** Applies the gate within a universe.
    *
    * @param universe the universe in which to apply the unitary
    * @return the universes produced by the unitary
    */
  def apply(universe: Universe): NonEmptyList[Universe]

  /** The reverse of the unitary. */
  def adjoint: Unitary
}

object Unitary {

  /** Operations on unitaries.
    *
    * @param unitary the unitary to apply the operations to
    */
  implicit final class Ops(val unitary: Unitary) extends AnyVal {

    /** Applies the unitary on all of the universes.
      *
      * @param universes the universes in which to apply the unitary
      * @return the new universes
      */
    def applyToAll(universes: Iterable[Universe]): Iterable[Universe] =
      universes flatMap (unitary(_).toList)

    /** The product of this unitary with another. */
    def *(unitary2: Unitary): Unitary = Unitary(
      universe => unitary.apply(universe) flatMap unitary2.apply,
      unitary2.adjoint * unitary.adjoint)
  }

  def apply(f: Universe => NonEmptyList[Universe], g: => Unitary): Unitary = new Unitary {
    override def apply(universe: Universe): NonEmptyList[Universe] = f(universe)
    override def adjoint: Unitary = g
  }

  val identity: Unitary = Unitary(NonEmptyList.one, identity)
}
