package yy.rdpac.bean

import javax.annotation.PostConstruct

import org.springframework.stereotype.Component
import yy.json.JSON
import yy.orm.Orm
import yy.orm.Session.Session
import yy.orm.db.Db

/**
  * Created by <yuemenglong@126.com> on 2017/7/19.
  */
@Component
class Dao {

  var db: Db = _

  @PostConstruct
  def init(): Unit = {
    Orm.init("yy.rdpac.entity")
    db = Orm.openDb("localhost", 3306, "root", "root", "rdpac")
    JSON.setConstructorMap(Orm.getEmptyConstructorMap)
  }

  def beginTransaction[T](fn: (Session) => T): T = {
    val session = db.openSession()
    val tx = session.beginTransaction()
    try {
      val ret = fn(session)
      tx.commit()
      ret
    } catch {
      case e: Exception =>
        tx.rollback()
        throw e
    } finally {
      session.close()
    }
  }

}


