package yy.rdpac

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import io.github.yuemenglong.orm.Orm
import yy.rdpac.bean.Dao
import yy.rdpac.entity._
import java.lang.Long
import javax.validation.constraints.NotNull

import yy.json.JSON
import yy.rdpac.kit.Shaffle

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
  def registUser(@RequestBody body: String): String = dao.beginTransaction(session => {
    val user = JSON.parse(body, classOf[User])
    user.study = Orm.convert(new Study)
    val ex = Orm.insert(user)
    ex.insert("study")
    session.execute(ex)
    val ret = JSON.stringify(user)
    println(ret)
    ret
  })

  @ResponseBody
  @RequestMapping(value = Array("/user"), method = Array(RequestMethod.GET), produces = Array("application/json"))
  def getUserInfo(@NotNull wxId: String): String = dao.beginTransaction(session => {
    val root = Orm.root(classOf[User]).asSelect()
    root.select("quizs")
    root.select("marks")
    root.select("study").select("quiz")
    val query = Orm.select(root).from(root).where(root.get("wxId").eql(wxId))
    val user = session.first(query)
    //    if (user == null) {
    //      return "null"
    //    }
    //    if (user.study == null) {
    //      val study = Orm.convert(new Study)
    //      val ex = Orm.insert(study)
    //      session.execute(ex)
    //      user.study = study
    //      val ex2 = Orm.update(user)
    //      session.execute(ex2)
    //    }
    JSON.stringify(user)
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
  def newQuiz(@RequestBody body: String, single: Integer, multi: Integer, start: Integer, end: Integer): String = dao.beginTransaction(session => {
    val root = Orm.root(classOf[Question]).asSelect()
    val questions = session.query(Orm.select(root).from(root))
    val selected = (single, multi, start, end) match {
      case (null, null, s, e) if s != null && e != null => questions.slice(s, e + 1)
      case (s, m, null, null) =>
        val shuffled = Shaffle.shaffle(questions)
        val sm: (Integer, Integer) = (s, m) match {
          case (null, null) => (24, 6)
          case (null, _) => (24, 6)
          case (_, null) => (24, 6)
          case (_, _) => (s, m)
        }
        val singleQuestion: Array[Question] = shuffled.filter(_.multi == false).take(sm._1).toArray
        val multiQuestion: Array[Question] = shuffled.filter(_.multi == true).take(sm._2).toArray
        singleQuestion ++ multiQuestion
      case (_, _, _, _) => throw new RuntimeException("Invalid Get Quiz Params")
    }
    val jo = JSON.parse(body).asObj()
    var quiz = new Quiz
    quiz.userId = jo.getLong("userId")
    quiz.mode = jo.getStr("mode")
    quiz.tag = jo.getStr("tag")
    val quizQuestions: Array[QuizQuestion] = selected.zipWithIndex.map { case (qt, idx) =>
      val ret = Orm.convert(new QuizQuestion)
      ret.infoId = qt.id
      ret.idx = idx + 1
      ret
    }
    quiz.questions = quizQuestions
    quiz.count = quizQuestions.length
    quiz = Orm.convert(quiz)
    val ex = Orm.insert(quiz)
    ex.insert("questions")
    session.execute(ex)
    JSON.stringify(quiz)
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
    // 查询题目
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
    // 3. 更新quiz上的answerIdx
    val quiz = new Quiz
    quiz.answerIdx = question.idx
    quiz.id = question.quizId
    session.execute(Orm.update(Orm.convert(quiz)))
    // 4. 返回计算后的question
    val jo = JSON.convert(question).asObj()
    jo.remove("info")
    jo.toString()
  })

  @ResponseBody
  @RequestMapping(value = Array("/quiz/{id}"), method = Array(RequestMethod.PUT), produces = Array("application/json"))
  def putQuiz(@PathVariable id: Long, @RequestBody body: String): String = dao.beginTransaction(session => {
    val quiz = JSON.parse(body, classOf[Quiz])
    quiz.id = id
    session.execute(Orm.update(quiz))
    JSON.stringify(quiz)
  })

  @ResponseBody
  @RequestMapping(value = Array("/debug"), method = Array(RequestMethod.POST), produces = Array("application/json"))
  def postDebugInfo(@NotNull userId: Long, @RequestBody body: String): String = dao.beginTransaction(session => {
    val info = new DebugInfo
    info.info = body
    info.userId = userId
    val ex = Orm.insert(Orm.convert(info))
    session.execute(ex)
    ""
  })

  @ResponseBody
  @RequestMapping(value = Array("/study/{id}"), method = Array(RequestMethod.PUT), produces = Array("application/json"))
  def putStudy(@PathVariable id: Long, @RequestBody body: String): String = dao.beginTransaction(session => {
    val study = JSON.parse(body, classOf[Study])
    study.id = id
    val ex = Orm.update(study)
    session.execute(ex)
    body
  })

  @ResponseBody
  @RequestMapping(value = Array("/user/{userId}/mark"), method = Array(RequestMethod.POST), produces = Array("application/json"))
  def postMark(@PathVariable userId: Long, @RequestBody body: String): String = dao.beginTransaction(session => {
    val jo = JSON.parse(body).asObj()
    val mark = new Mark
    mark.infoId = jo.getLong("infoId")
    mark.userId = userId
    val e = Orm.convert(mark)
    session.execute(Orm.insert(e))
    JSON.stringify(e)
  })

  @ResponseBody
  @RequestMapping(value = Array("/mark/{id}"), method = Array(RequestMethod.DELETE), produces = Array("application/json"))
  def deleteMark(@PathVariable id: Long): Unit = dao.beginTransaction(session => {
    val mark = new Mark
    mark.id = id
    val e = Orm.convert(mark)
    session.execute(Orm.delete(e))
  })

}

