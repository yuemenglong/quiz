package bean

import yy.orm.Orm

/**
  * Created by <yuemenglong@126.com> on 2017/7/19.
  */
class Dao {

}

object dao {
  def main(args: Array[String]): Unit = {
    Orm.init("entity")
    val db = Orm.openDb("localhost", 3306, "root", "root", "rdpac")
    db.rebuild()
  }
}
