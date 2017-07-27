package yy.rdpac

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import yy.orm.Orm
import yy.rdpac.bean.Dao
import yy.rdpac.entity.{Question, Quiz, QuizQuestion, User}
import java.lang.Long
import javax.validation.constraints.NotNull

import yy.json.JSON
import yy.rdpac.kit.Kit

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
  @RequestMapping(value = Array("/user"), method = Array(RequestMethod.POST), produces = Array("application/json"))
  def registUser(@RequestBody body: String): Unit = dao.beginTransaction(session => {
    val user = JSON.parse(body, classOf[User])
    val ex = Orm.insert(user)
    session.execute(ex)
    JSON.stringify(user)
  })

  @ResponseBody
  @RequestMapping(value = Array("/user"), method = Array(RequestMethod.GET), produces = Array("application/json"))
  def getUserInfo(@NotNull wxId: String): String = dao.beginTransaction(session => {
    val root = Orm.root(classOf[User]).asSelect()
    root.select("quizs")
    val query = Orm.select(root).from(root).where(root.get("wxId").eql(wxId))
    val res = session.first(query)
    JSON.stringify(res)
  })

  @ResponseBody
  @RequestMapping(value = Array("/quizs"), method = Array(RequestMethod.GET), produces = Array("application/json"))
  def getQuizs(@NotNull wxId: String): String = dao.beginTransaction(session => {
    val root = Orm.root(classOf[Quiz]).asSelect()
    val query = Orm.select(root).from(root).where(root.join("user").get("wxId").eql(wxId))
    val res = session.query(query)
    JSON.stringify(res)
  })

  @ResponseBody
  @RequestMapping(value = Array("/quiz"), method = Array(RequestMethod.POST), produces = Array("application/json"))
  def newQuiz(@RequestBody body: String): String = dao.beginTransaction(session => {
    val jo = JSON.parse(body).asObj()
    // 产生新的quiz并返回, 随机120单选30多选
    val root = Orm.root(classOf[Question]).asSelect()
    val questions = session.query(Orm.select(root).from(root))
    val single: Array[Question] = questions.filter(_.multi == false).take(12)
    val multi: Array[Question] = questions.filter(_.multi == true).take(3)
    var quiz = new Quiz
    val quizQuestions: Array[QuizQuestion] = (single ++ multi).zipWithIndex
      .map { case (qt, idx) =>
        val ret = Orm.convert(new QuizQuestion)
        ret.info = qt
        ret.idx = idx + 1
        ret
      }
    quiz.questions = quizQuestions
    quiz.count = quizQuestions.length
    quiz.userId = jo.getLong("userId")
    quiz = Orm.convert(quiz)
    val ex = Orm.insert(quiz)
    ex.insert("questions")
    session.execute(ex)
    val ret = JSON.convert(quiz).asObj()
    ret.toJsString
  })

  @ResponseBody
  @RequestMapping(value = Array("/quiz/{id}"), method = Array(RequestMethod.GET), produces = Array("application/json"))
  def getQuiz(@PathVariable id: Long): String = dao.beginTransaction(session => {
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
    val corrected = JSON.parse(body).asObj().getBool("corrected")
    val quiz = Orm.empty(classOf[Quiz])
    quiz.id = id
    if (finished) {
      quiz.answered = finished
    }
    if (corrected) {
      quiz.corrected = corrected
    }
    session.execute(Orm.update(quiz))
    body
  })
}
