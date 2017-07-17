import scala.io.Source
import scala.util.matching.Regex

/**
  * Created by <yuemenglong@126.com> on 2017/7/17.
  */
object Parser {
  val titlePattern: Regex = """(\d+)．(.+)""".r
  val aPattern: Regex = """A．(.+)""".r

  def main(args: Array[String]): Unit = {
    val lines = Source.fromResource("all.txt").getLines().toArray
    var no = 1
    var pos = 0
    while (pos < lines.length) {
      lines(pos) match {
        case pattern(n, title) =>
          println(n, title)
        case _ => {}
      }
      pos += 1
    }
  }

  def getFullTitle(lines: Array[String], pos: Int, r: String): (String, String, String) = {
    var title = r
    var p = pos + 1
    while (title.last != '：') {
      if (!lines(p).trim.isEmpty) {
        title += lines(p).trim
      }
      p += 1
    }
    ("title", title, "a")
  }

  def getFullAnswer(name: String, mark: String): (Array[String], Int, String) => (String, String, String) = {
    (lines: Array[String], pos: Int, r: String) => {
      var ret = r
      var p = pos + 1
      while (lines(p)(0).toString != mark.toUpperCase()) {
        if (!lines(p).trim.isEmpty) {
          ret += lines(p).trim
        }
        p += 1
      }
      (name, ret, mark)
    }
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
        case titlePattern(_, t) => getFullTitle(lines, pos, t)
        case _ => (null, null, state)
      }
      case "a" => line match {
        case aPattern(a) => getFullAnswer("a", "b")(lines, pos, a)
      }
    }
  }

  val p = """(\d)-(\d)""".r
  "1-1" match {
    case p(i, j) => println(i, j)
  }

}
