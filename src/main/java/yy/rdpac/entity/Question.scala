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
  var idx: Integer = _
  @Column(length = 500)
  var title: String = _
  @Column(length = 500)
  var a: String = _
  @Column(length = 500)
  var b: String = _
  @Column(length = 500)
  var c: String = _
  @Column(length = 500)
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
  var userAnswer: String = _
  var correct: Boolean = _

  var quizId: Long = _
  var questionId: Long = _
}

@Entity
class Quiz {
  @Id(auto = true)
  var id: Long = _
  @DateTime
  var createTime: Date = new Date()
  @OneToMany
  var questions: Array[QuizQuestion] = Array()
  var finished: Boolean = false
}
