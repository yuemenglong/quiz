package yy.rdpac.bean

import java.util.regex.Pattern
import javax.annotation.PostConstruct

import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.`type`.filter.RegexPatternTypeFilter
import org.springframework.stereotype.Component
import yy.json.JSON
import yy.orm.Orm
import yy.orm.Session.Session
import yy.orm.db.Db

import scala.reflect.ClassTag

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
    val provider = new ClassPathScanningCandidateComponentProvider(false)
    provider.addIncludeFilter(new RegexPatternTypeFilter(Pattern.compile(".*")))
    val classes = provider.findCandidateComponents("yy.rdpac")
    val paths = classes.toArray.map(_.asInstanceOf[BeanDefinition].getBeanClassName)
    //val paths = classes.stream().map(_.getBeanClassName).toArray(ClassTag[String](classOf[String]))
    Orm.init(paths)
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


