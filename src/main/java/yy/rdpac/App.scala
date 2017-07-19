package yy.rdpac

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestMapping, ResponseBody}
import yy.orm.Orm
import yy.rdpac.bean.Dao
import yy.rdpac.entity.{Question, Quiz, QuizQuestion}
import java.lang.Boolean

/**
  * Created by <yuemenglong@126.com> on 2017/7/19.
  */
object App {
  def main(args: Array[String]): Unit = {
    SpringApplication.run(classOf[App])
  }
}

@Controller
@RequestMapping(Array("/"))
@SpringBootApplication(scanBasePackages = Array("yy.rdpac"))
class App {

  @Autowired
  var dao: Dao = _

  @ResponseBody
  @RequestMapping(Array("/"))
  def index: String = "index"

  @ResponseBody
  @RequestMapping(Array("/quiz"))
  def getQuiz: String = {
    dao.beginTransaction(session => {
      // 判断有没有未完成的quiz
      val qr = Orm.root(classOf[Quiz]).asSelect()
      val query = Orm.select(qr).from(qr).where(qr.get("finished").eql(new Boolean(true))).limit(1)
      val currentQuiz = session.first(query)
      if (currentQuiz == null) {
        "再来一次"
        // 产生新的quiz并返回, 随机120单选30多选
        val qtr = Orm.root(classOf[Question]).asSelect()
        val allQuestions = session.query(Orm.select(qtr).from(qtr))
        val single: Array[Question] = allQuestions.filter(_.multi == false).take(120)
        val multi: Array[Question] = allQuestions.filter(_.multi == true).take(30)
        val newQuiz = new Quiz
        val questions: Array[QuizQuestion] = (single ++ multi).map(qt => {
          val ret = new QuizQuestion
          ret.questionId = qt.id
          ret
        })
        newQuiz.questions = questions
        val ex = Orm.insert(Orm.convert(newQuiz))
        session.execute(ex)
        s"${newQuiz.id}"
      } else {
        "继续上次"
      }
    })
  }
}
