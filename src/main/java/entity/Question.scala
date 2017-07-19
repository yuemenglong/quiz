package entity

import yy.orm.lang.anno.{Entity, Id}
import java.lang.Long
import java.lang.Boolean

/**
  * Created by <yuemenglong@126.com> on 2017/7/17.
  */
@Entity
class Question {

  @Id(auto = true)
  var id: Long = _

  def getId: Long = id

  var chapter: Integer = _

  var index: Integer = _

  var title: String = _

  var a: String = _

  var b: String = _

  var c: String = _

  var d: String = _

  var answer: String = _

  var multi: Boolean = _
}
