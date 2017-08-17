package yy.rdpac.entity

import io.github.yuemenglong.orm.Orm
import io.github.yuemenglong.orm.lang.anno._
import io.github.yuemenglong.orm.lang.types.Types._
import yy.rdpac.kit.Shaffle

/**
  * Created by <yuemenglong@126.com> on 2017/7/17.
  */
@Entity
class Chapter {
  @Id(auto = true)
  var id: Long = _
  var name: String = _
  var ty: String = _

  @OneToMany
  var questions: Array[Question] = _
}

@Entity
class Question {
  @Id(auto = true)
  var id: Long = _
  @Pointer
  var chapter: Chapter = _
  var chapterId: Long = _

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
  @Column(length = 16)
  var seq: String = Shaffle.shaffle("abcd".split("")).mkString("")
  @Pointer
  var quiz: Quiz = _

  var idx: Integer = _
  var answer: String = _
  var fails: Integer = 0
  var correct: Boolean = false

  var quizId: Long = _
  var infoId: Long = _

  def getCorrectAnswer: String = {
    info.answer.split("").map(c => {
      "abcd".charAt(seq.indexOf(c))
    }).sorted.mkString("")
  }

  def getQuestion(no: String): String = {
    val infoNo = seq.charAt("abcd".indexOf(no))
    infoNo match {
      case 'a' => info.a
      case 'b' => info.b
      case 'c' => info.c
      case 'd' => info.d
    }
  }
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

  var mode: String = "answer"
  @Column(nullable = false)
  var tag: String = _

  var finished: Boolean = false

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

  @OneToOne
  var study: Study = new Study
  @OneToMany
  var quizs: Array[Quiz] = Array()

  @OneToMany(right = "userId")
  var marks: Array[Mark] = Array()
}

@Entity
class Mark {
  @Id(auto = true)
  var id: Long = _
  var infoId: Long = _
  @Pointer
  var info: Question = _
  var userId: Long = _
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
  // 与学习相关的quiz
  @Pointer
  var quiz: Quiz = _
  var quizId: Long = _
}

//object Main {
//  def main(args: Array[String]): Unit = {
//    Orm.init("yy.rdpac.entity")
//    val db = Orm.openDb("211.159.173.48", 3306, "work", "work", "rdpac")
//    db.beginTransaction(session => {
//      val r1 = Orm.root(classOf[Mark]).asSelect()
//      val q1 = Orm.select(r1).from(r1).where(r1.get("userId").eql(new Integer(2)))
//      val marks = session.query(q1)
//      val infoIds = marks.map(_.infoId.asInstanceOf[Object])
//      val r2 = Orm.root(classOf[QuizQuestion]).asSelect()
//      val q2 = Orm.select(r2).from(r2).where(r2.get("id").in(infoIds))
//      val qqs = session.query(q2)
//      val qqMap: Map[Long, QuizQuestion] = qqs.map(qq => (qq.id, qq))(collection.breakOut)
//      marks.foreach(mark => {
//        val infoId = mark.infoId
//        if (qqMap.contains(infoId)) {
//          val realId = qqMap(infoId).infoId
//          println(s"${infoId} => ${realId}")
//        } else {
//          println(s"Not Found: ${infoId}")
//        }
//      })
//      //      marks.foreach(println)
//      //      qqs.foreach(println)
//    })
//  }
//}
