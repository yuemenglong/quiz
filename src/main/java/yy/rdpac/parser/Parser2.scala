package yy.rdpac.parser

import io.github.yuemenglong.json.JSON
import io.github.yuemenglong.orm.Orm
import yy.rdpac.entity.{Chapter, Question}
import yy.rdpac.parser.Parser.parse

import scala.collection.mutable.ArrayBuffer
import scala.io.Source
import scala.util.matching.Regex

/**
  * Created by <yuemenglong@126.com> on 2017/8/17.
  */


object Parser2 {
  // 章节
  // 题目类型
  // title a b c d answer
  // 要点

  val re: Regex = """((.+、单选题)|(.+、多选题)|(\d+．.+)|([A-D]．.+)|(答案：.+)|(答案要点：.*))""".r

  def readAndNormalizeContent(): Array[String] = {
    val lines = Source.fromFile(Parser2.getClass.getClassLoader.getResource("all.txt").getFile)
      .getLines().map(_.replaceAll(" ", "")).filter(_.trim.length != 0)
    val ret = ArrayBuffer[String]()
    var last = ""
    lines.zipWithIndex.foreach { case (line, idx) =>
      line match {
        case re(l, _*) =>
          ret += last
          last = l
        case l: String =>
          last += l
      }
    }
    ret += last
    ret.remove(0)
    ret.toArray
  }

  def parse(): Array[Chapter] = {
    val chapters = ArrayBuffer[Chapter]()
    val questions = ArrayBuffer[Question]()
    var currentChapter: Chapter = null
    var currentQuestion: Question = null
    readAndNormalizeContent().foreach {
      case re(_, single, multi, title, abcd, answer, analyze) =>
        if (single != null || multi != null) {
          println(single, multi)
          val ty = (single, multi) match {
            case (s, null) if s != null => "单选题"
            case (null, m) if m != null => "多选题"
          }
          if (currentChapter != null) {
            currentChapter.questions = questions.toArray
            chapters += currentChapter
            questions.clear()
          }
          currentChapter = new Chapter
          currentChapter.ty = ty
        } else if (title != null) {
          println(title)
          val re = """(\d+)．(.+)""".r
          require(currentChapter.ty != null)
          currentQuestion = new Question
          title match {
            case re(idx, content) =>
              require(questions.length + 1 == idx.toInt)
              currentQuestion.title = content
              currentQuestion.idx = idx.toInt
          }
        } else if (abcd != null) {
          println(abcd)
          require(currentChapter.ty != null)
          require(currentQuestion.title != null)
          val re = """([A-D])．(.+)""".r
          abcd match {
            case re(no, content) => no match {
              case "A" =>
                require(currentQuestion.a == null)
                currentQuestion.a = content
              case "B" =>
                require(currentQuestion.b == null)
                currentQuestion.b = content
              case "C" =>
                require(currentQuestion.c == null)
                currentQuestion.c = content
              case "D" =>
                require(currentQuestion.d == null)
                currentQuestion.d = content
            }
          }
        } else if (answer != null) {
          println(answer)
          require(currentQuestion.answer == null)
          currentQuestion.answer = answer
          require(currentQuestion.title != null)
          require(currentQuestion.a != null)
          require(currentQuestion.b != null)
          require(currentQuestion.c != null)
          require(currentQuestion.d != null)
          require(currentQuestion.answer != null)
          questions += currentQuestion
        }
    }
    currentChapter.questions = questions.toArray
    chapters += currentChapter
    chapters.toArray
  }

  def findDiff(): Unit = {
    val newQs = parse().flatMap(_.questions)
    val oldQs = {
      Orm.init("yy.rdpac.entity")
      val db = Orm.openDb("localhost", 3306, "root", "root", "rdpac")
      db.beginTransaction(session => {
        val root = Orm.root(classOf[Question]).asSelect()
        session.query(Orm.select(root).from(root))
      })
    }
    newQs.zipWithIndex.foreach { case (q, idx) =>
      try {
        require(q.title == oldQs(idx).title)
        require(q.a == oldQs(idx).a)
        require(q.b == oldQs(idx).b)
        require(q.c == oldQs(idx).c)
        require(q.d == oldQs(idx).d)
      } catch {
        case e: Exception =>
          println(JSON.stringify(q))
          println(oldQs(idx))
      }
    }
  }

  def main(args: Array[String]): Unit = {
    Orm.init("yy.rdpac.entity")
    val db = Orm.openDb("localhost", 3306, "root", "root", "rdpac")
    db.rebuild()
    val res = Orm.converts(parse()).flatMap(_.questions)
    println(res.length)
    val ex = Orm.insert(classOf[Question]).values(res)
    db.openSession().execute(ex)
  }
}
