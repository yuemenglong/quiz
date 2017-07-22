package yy.rdpac

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import yy.orm.Orm
import yy.rdpac.bean.Dao
import yy.rdpac.entity.{Question, Quiz, QuizQuestion}
import java.lang.Boolean
import java.lang.Long

import yy.json.JSON
import yy.json.parse.JsonArr

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
  @RequestMapping(value = Array("/quiz"), method = Array(RequestMethod.GET), produces = Array("application/json"))
  def getCurrentQuiz: String = dao.beginTransaction(session => {
    val root = Orm.root(classOf[Quiz]).asSelect()
    val query = Orm.select(root).from(root).where(root.get("finished").eql(new Boolean(false)))
    val quiz = session.first(query)
    JSON.stringify(quiz)
  })

  @ResponseBody
  @RequestMapping(value = Array("/quiz"), method = Array(RequestMethod.POST), produces = Array("application/json"))
  def newQuiz: String = dao.beginTransaction(session => {
    // 产生新的quiz并返回, 随机120单选30多选
    val root = Orm.root(classOf[Question]).asSelect()
    val questions = session.query(Orm.select(root).from(root))
    val single: Array[Question] = questions.filter(_.multi == false).take(12)
    val multi: Array[Question] = questions.filter(_.multi == true).take(3)
    var quiz = new Quiz
    val quizQuestions: Array[QuizQuestion] = (single ++ multi).map(qt => {
      val ret = Orm.convert(new QuizQuestion)
      ret.info = qt
      ret
    })
    quiz.questions = quizQuestions
    quiz.count = quizQuestions.length
    quiz = Orm.convert(quiz)
    val ex = Orm.insert(quiz)
    ex.insert("questions")
    session.execute(ex)
    val ret = JSON.convert(quiz).asObj()
    ret.remove("questions")
    ret.toString()
  })

  @ResponseBody
  @RequestMapping(value = Array("/quiz/{id}"), method = Array(RequestMethod.GET), produces = Array("application/json"))
  def getQuizById(@PathVariable id: Long): String = dao.beginTransaction(session => {
    val root = Orm.root(classOf[Quiz]).asSelect()
    root.select("questions")
    val query = Orm.select(root).from(root)
      .where(root.get("id").eql(id))
    val quiz = session.first(query)
    JSON.stringify(quiz)
  })

  @ResponseBody
  @RequestMapping(value = Array("/question/{id}"), method = Array(RequestMethod.GET), produces = Array("application/json"))
  def getQuestion(@PathVariable id: Long): String = dao.beginTransaction(session => {
    val root = Orm.root(classOf[Question]).asSelect()
    val query = Orm.select(root).from(root).where(root.get("id").eql(id)).limit(1)
    val info = session.first(query)
    JSON.stringify(info)
  })

  @ResponseBody
  @RequestMapping(value = Array("/quiz/{qzId}/question/{qtId}"), method = Array(RequestMethod.PUT), produces = Array("application/json"))
  def putAnswer(@PathVariable qzId: Long, @PathVariable qtId: Long, @RequestBody body: String): String = dao.beginTransaction(session => {
    val answer = JSON.parse(body).asObj().getStr("answer")
    val root = Orm.root(classOf[QuizQuestion]).asSelect()
    root.select("info")
    val question = session.first(Orm.from(root).where(root.get("id").eql(qtId)))
    // 1. 设置答案，正确性与错误次数
    require(question != null)
    question.answer = answer
    question.correct = question.info.answer == answer
    if (!question.correct) {
      question.fails += 1
    }
    // 2. 更新question
    session.execute(Orm.update(question))
    val jo = JSON.convert(question).asObj()
    jo.remove("info")
    jo.toString()
  })

  @ResponseBody
  @RequestMapping(value = Array("/quiz/{id}"), method = Array(RequestMethod.PUT), produces = Array("application/json"))
  def putQuiz(@PathVariable id: Long, @RequestBody body: String): String = dao.beginTransaction(session => {
    val finished = JSON.parse(body).asObj().getBool("finished")
    val quiz = Orm.empty(classOf[Quiz])
    quiz.id = id
    quiz.finished = finished
    session.execute(Orm.update(quiz))
    body
  })
}

