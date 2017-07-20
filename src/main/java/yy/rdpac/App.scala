package yy.rdpac

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, RequestMapping, ResponseBody}
import yy.orm.Orm
import yy.rdpac.bean.Dao
import yy.rdpac.entity.{Question, Quiz, QuizQuestion}
import java.lang.Boolean

import yy.json.JSON

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
  def getQuiz: String = dao.beginTransaction(session => {
    // 判断有没有未完成的quiz
    val quizRoot = Orm.root(classOf[Quiz]).asSelect()
    val query = Orm.select(quizRoot).from(quizRoot).where(quizRoot.get("finished").eql(new Boolean(false))).limit(1)
    var quiz = session.first(query)
    if (quiz == null) {
      // 产生新的quiz并返回, 随机120单选30多选
      val questionRoot = Orm.root(classOf[Question]).asSelect()
      val allQuestions = session.query(Orm.select(questionRoot).from(questionRoot))
      val single: Array[Question] = allQuestions.filter(_.multi == false).take(12)
      val multi: Array[Question] = allQuestions.filter(_.multi == true).take(3)
      quiz = new Quiz
      val questions: Array[QuizQuestion] = (single ++ multi).map(qt => {
        val ret = new QuizQuestion
        ret.infoId = qt.id
        ret
      })
      quiz.questions = questions
      quiz = Orm.convert(quiz)
      val ex = Orm.insert(quiz)
      ex.insert("questions")
      session.execute(ex)
      s"${quiz.id}"
    } else {
      s"${quiz.id}"
    }
  })

  @ResponseBody
  @RequestMapping(Array("/quiz/{quizId}/question"))
  def getQuizQuestion(@PathVariable quizId: String): String = dao.beginTransaction(session => {
    val root = Orm.root(classOf[QuizQuestion]).asSelect()
    root.select("info")
    val query = Orm.select(root).from(root).where(root.get("answer").isNull).limit(1)
    val question = session.first(query)
    JSON.stringify(question)
  })
}
