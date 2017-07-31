package yy.rdpac.bean

import javax.annotation.PostConstruct

import org.springframework.beans.factory.annotation.Value
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

  @Value("${db.host}")
  var host: String = _
  @Value("${db.port}")
  var port: Integer = _
  @Value("${db.user}")
  var user: String = _
  @Value("${db.pwd}")
  var pwd: String = _
  @Value("${db.database}")
  var database: String = _

  @PostConstruct
  def init(): Unit = {
    Orm.init("yy.rdpac.entity")
    db = Orm.openDb(host, port, user, pwd, database)
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


