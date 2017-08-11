package yy.rdpac.parser

import io.github.yuemenglong.orm.Orm
import yy.json.JSON
import yy.rdpac.entity.User

/**
  * Created by <yuemenglong@126.com> on 2017/8/11.
  */
object StateReader {
  def main(args: Array[String]): Unit = {
    Orm.init("yy.rdpac.entity")
    val db = Orm.openDb("localhost", 3306, "root", "root", "rdpac")
    db.beginTransaction(session => {
      val root = Orm.root(classOf[User]).asSelect()
      root.select("quizs")
      root.select("study")
      val query = Orm.select(root).from(root).where(root.get("id").eql(new Integer(2)))
      val res = session.first(query)
      val json = JSON.stringify(res)
      println(json)
    })
  }
}
