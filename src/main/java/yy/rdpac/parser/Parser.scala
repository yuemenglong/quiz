package yy.rdpac.parser

import yy.rdpac.entity.Question
import yy.orm.Orm

import scala.collection.mutable.ArrayBuffer
import scala.io.Source
import scala.util.matching.Regex

/**
  * Created by <yuemenglong@126.com> on 2017/7/17.
  */
object Parser {
  val titlePattern: Regex = """(\d+)．(.+)""".r
  val aPattern: Regex = """A．(.+)""".r
  val bPattern: Regex = """B．(.+)""".r
  val cPattern: Regex = """C．(.+)""".r
  val dPattern: Regex = """D．(.+)""".r
  val answerPattern: Regex = """答案： ([abcd]+)""".r

  def main(args: Array[String]): Unit = {
    Orm.init("yy.rdpac.entity")
    val db = Orm.openDb("localhost", 3306, "root", "root", "rdpac")
    db.rebuild()
    val res = Orm.converts(parse())
    println(res.length)
    val ex = Orm.insert(classOf[Question]).values(res)
    db.openSession().execute(ex)
  }

  def parse(): Array[Question] = {
    val lines = Source.fromResource("all.txt").getLines().toArray
    var pos = 0
    var state = "title"
    var map = Map[String, String]()
    var ret = ArrayBuffer[Question]()
    while (pos < lines.length) {
      try {
        val (name, value, next) = parse(lines, pos, state)
        if (name != null && value != null) {
          //          println(name, value)
          map += (name -> value)
          if (map.size == 6) {
            ret += createQuestion(map)
            map = Map[String, String]()
          }
        }
        pos += 1
        state = next
      } catch {
        case e: Throwable =>
          println(pos, e)
          throw e
      }
    }
    var idx = 1
    var chapter = 1
    ret.foreach(q => {
      println(q.idx, q.title)
      if (q.idx == idx) {
        idx += 1
      } else if (q.idx == 1) {
        idx = 2
        chapter += 1
      } else {
        throw new RuntimeException("Not Continued")
      }
      q.chapter = chapter
    })
    ret.toArray
  }

  def createQuestion(map: Map[String, String]): Question = {
    val ret = new Question
    val re = """(\d+)-(.+)""".r
    map("title") match {
      case re(index, title) =>
        ret.idx = index.toInt
        ret.title = title
    }
    ret.a = map("a")
    ret.b = map("b")
    ret.c = map("c")
    ret.d = map("d")
    ret.answer = map("answer")
    ret.multi = ret.answer.length > 1
    ret
  }

  def getFullTitle(lines: Array[String], pos: Int, r: String): (String, String, String) = {
    var title = r
    var p = pos + 1
    while (title.last != '：' && !lines(p).startsWith("A．")) {
      if (!lines(p).trim.isEmpty) {
        title += lines(p).trim
      }
      p += 1
    }
    ("title", title, "a")
  }

  def getFullChoice(name: String, mark: String): (Array[String], Int, String) => (String, String, String) = {
    (lines: Array[String], pos: Int, r: String) => {
      var ret = r
      var p = pos + 1
      while (lines(p).length < 2 || lines(p).length > 2 &&
        (lines(p)(0).toString != mark.toUpperCase() || lines(p)(1).toString != "．")) {
        if (!lines(p).trim.isEmpty) {
          ret += lines(p).trim
        }
        p += 1
      }
      (name, ret, mark)
    }
  }

  def getFullDChoice(lines: Array[String], pos: Int, r: String): (String, String, String) = {
    var ret = r
    var p = pos + 1
    while (!lines(p).startsWith("答案")) {
      if (!lines(p).trim.isEmpty) {
        ret += lines(p).trim
      }
      p += 1
    }
    ("d", ret, "answer")
  }

  // key value state
  def parse(lines: Array[String], pos: Int, state: String): (String, String, String) = {
    val line = lines(pos).trim
    if (line.isEmpty) {
      return (null, null, state)
    }
    //
    state match {
      case "title" => line match {
        case titlePattern(n, t) => getFullTitle(lines, pos, s"$n-$t")
        case _ => (null, null, state)
      }
      case "a" => line match {
        case aPattern(a) => getFullChoice("a", "b")(lines, pos, a)
        case _ => (null, null, state)
      }
      case "b" => line match {
        case bPattern(b) => getFullChoice("b", "c")(lines, pos, b)
        case _ => (null, null, state)
      }
      case "c" => line match {
        case cPattern(c) => getFullChoice("c", "d")(lines, pos, c)
        case _ => (null, null, state)
      }
      case "d" => line match {
        case dPattern(d) => getFullDChoice(lines, pos, d)
        case _ => (null, null, state)
      }
      case "answer" => line match {
        case answerPattern(answer) => ("answer", answer, "title")
        case _ => (null, null, state)
      }
    }
  }

}
