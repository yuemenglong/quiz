package yy.rdpac.kit

/**
  * Created by <yuemenglong@126.com> on 2017/7/31.
  */
object Shaffle {
  def shaffle[T](arr: Array[T]): Seq[T] = {
    val assist = arr.indices.map(_ => Math.random())
    arr.zipWithIndex.sortBy(p => assist(p._2)).map(_._1)
  }
}
