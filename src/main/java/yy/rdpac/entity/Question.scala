package yy.rdpac.entity

import yy.orm.lang.anno._
import java.lang.Long
import java.lang.Boolean
import java.util.Date

/**
  * Created by <yuemenglong@126.com> on 2017/7/17.
  */
@Entity
class Question {
  @Id(auto = true)
  var id: Long = _
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

@Entity
class QuizQuestion {
  @Id(auto = true)
  var id: Long = _
  @Pointer
  var question: Question = _
  @Pointer
  var quiz: Quiz = _
  var answer: String = _
  var userAnswer: String = _
  var correct: Boolean = _
}

@Entity
class Quiz {
  @Id(auto = true)
  var id: Long = _
  @DateTime
  var createTime: Date = _
  @OneToMany
  var qts: Array[QuizQuestion] = Array()
}
