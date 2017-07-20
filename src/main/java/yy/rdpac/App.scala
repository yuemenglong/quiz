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
  @RequestMapping(value = Array("/quiz"), produces = Array("application/json"))
  def getQuiz: String = dao.beginTransaction(session => {
    // 判断有没有未完成的quiz
    val quizRoot = Orm.root(classOf[Quiz]).asSelect()
    val qtJoin = quizRoot.select("questions")
    qtJoin.on(qtJoin.get("answer").notNull())
    val query = Orm.select(quizRoot).from(quizRoot)
      .where(quizRoot.get("finished").eql(new Boolean(false)))
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
      quiz.count = questions.length
      quiz = Orm.convert(quiz)
      val ex = Orm.insert(quiz)
      ex.insert("questions")
      session.execute(ex)
      val jo = JSON.convert(quiz).asObj()
      jo.remove("questions")
      jo.set("questions", JsonArr(Array()))
      jo.toString()
    } else {
      JSON.stringify(quiz)
    }
  })

  @ResponseBody
  @RequestMapping(value = Array("/quiz/{quizId}/question"), produces = Array("application/json"))
  def getQuizQuestion(@PathVariable quizId: Long): String = dao.beginTransaction(session => {
    val root = Orm.root(classOf[QuizQuestion]).asSelect()
    root.select("info")
    val query = Orm.select(root).from(root)
      .where(root.get("quizId").eql(quizId).and(root.get("answer").isNull))
      .limit(1)
    val question = session.first(query)
    if (question == null) {
      "null"
    } else {
      JSON.stringify(question)
    }
  })

  @ResponseBody
  @RequestMapping(value = Array("/quiz/{quizId}/question/{qtId}"), method = Array(RequestMethod.POST), produces = Array("application/json"))
  def postAnswer(@PathVariable quizId: Long, @PathVariable qtId: Long, @RequestBody body: String): String = dao.beginTransaction(session => {
    val answer = JSON.parse(body).asObj().getStr("answer")
    val root = Orm.root(classOf[QuizQuestion]).asSelect()
    root.select("info")
    val query = Orm.select(root).from(root)
      .where(root.get("id").gte(qtId).and(root.get("answer").isNull))
      .asc(root.get("id"))
      .limit(2)
    val questions = session.query(query)
    if (questions.length > 0 && questions(0).id != qtId) {
      throw new RuntimeException("重复提交")
    }
    if (questions.length > 0) {
      val qt = questions(0)
      qt.answer = answer
      qt.correct = qt.answer == qt.info.answer
      session.execute(Orm.update(qt))
    }
    if (questions.length > 1) {
      JSON.stringify(questions(1))
    } else {
      // 完成考试，设为finish
      val quiz = Orm.empty(classOf[Quiz])
      quiz.id = quizId
      quiz.finished = true
      session.execute(Orm.update(quiz))
      "null"
    }
  })
}

