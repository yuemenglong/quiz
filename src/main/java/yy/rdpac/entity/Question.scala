package yy.rdpac.entity

import io.github.yuemenglong.orm.lang.anno._
import io.github.yuemenglong.orm.lang.types.Types._

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
  var info: Question = _
  @Pointer
  var quiz: Quiz = _

  var idx: Integer = _
  var answer: String = _
  var fails: Integer = 0
  var correct: Boolean = false

  var quizId: Long = _
  var infoId: Long = _
}

@Entity
class Quiz {
  @Id(auto = true)
  var id: Long = _
  @DateTime
  var createTime: Date = new Date()
  @OneToMany(right = "quizId")
  var questions: Array[QuizQuestion] = Array()
  var count: Integer = _

  var answered: Boolean = false
  var corrected: Boolean = false

  var answerIdx: Integer = 0
  var reviewIdx: Integer = 0

  @Pointer
  var user: User = _
  var userId: Long = _
}

@Entity
class User {
  @Id(auto = true)
  var id: Long = _
  var wxId: String = _

  @Pointer
  var study: Study = new Study
  @OneToMany
  var quizs: Array[Quiz] = Array()
}

@Entity
class DebugInfo {

  @Id(auto = true)
  var id: Long = _
  var userId: Long = _
  var info: String = _
  @DateTime
  var createTime: Date = new Date
}

@Entity
class Study {
  @Id(auto = true)
  var id: Long = _
  // 学习到第几题了
  var studyIdx: Integer = -1
  var finishIdx: Integer = -1
  // 与学习相关的quiz
  @Pointer
  var quiz: Quiz = _
  var quizId: Long = _
}
