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
import java.util.Date
import javax.validation.constraints.NotNull

import io.github.yuemenglong.json.JSON
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
  def regist(@RequestBody body: String): String = dao.beginTransaction(session => {
    val user = JSON.parse(body, classOf[User])
    user.code = new Date().toString
    val ex = Orm.insert(user)
    ex.insert("wxUserInfo")
    session.execute(ex)
    JSON.stringify(user)
  })

  @ResponseBody
  @RequestMapping(value = Array("/user"), method = Array(RequestMethod.GET), produces = Array("application/json"))
  def fetchUser(@NotNull code: String): String = dao.beginTransaction(session => {
    val root = Orm.root(classOf[User]).asSelect()
    root.select("wxUserInfo")
    root.select("quiz").select("questions")
    root.select("study").select("questions")
    root.select("marked").select("questions")
    root.select("marks")
    val query = Orm.select(root).from(root).where(root.get("code").eql(code))
    val user = session.first(query)
    JSON.stringify(user)
  })

  @ResponseBody
  @RequestMapping(value = Array("/quiz"), method = Array(RequestMethod.POST), produces = Array("application/json"))
  def newQuiz(@RequestBody body: String,
              single: Integer, multi: Integer,
              start: Integer, end: Integer,
              chapter: Integer,
              marked: Boolean
             ): String = dao.beginTransaction(session => {
    val jo = JSON.parse(body).asObj()
    var quiz = new Quiz
    quiz.userId = jo.getLong("userId")
    quiz.mode = jo.getStr("mode")
    quiz.tag = jo.getStr("tag")

    //1 先删除该用户下其他quiz
    {
      val root = Orm.root(classOf[Quiz])
      session.execute(Orm.delete(root).where(root.get("userId").eql(quiz.userId)))
    }
    if (marked) {
      // 收藏模式
      val root = Orm.root(classOf[Mark]).asSelect()
      root.select("info")
      val query = Orm.select(root).from(root).where(root.get("userId").eql(quiz.userId))
      val marks = Shaffle.shaffle(session.query(query))
      quiz.questions = marks.zipWithIndex.map { case (mark, idx) =>
        val qq = new QuizQuestion
        qq.infoId = mark.infoId
        qq.idx = idx + 1
        qq
      }.toArray
      quiz.count = marks.length
      quiz = Orm.convert(quiz)
      val ex = Orm.insert(quiz)
      ex.insert("questions")
      session.execute(ex)
      JSON.stringify(quiz)
    } else if (chapter != null) {
      // 学习模式
      val root = Orm.root(classOf[Question]).asSelect()
      val query = Orm.select(root).from(root).where(root.get("chapter").eql(chapter))
      val questions = session.query(query)
      quiz.count = questions.length
      quiz.questions = questions.zipWithIndex.map(p => {
        val (q, idx) = p
        val ret = new QuizQuestion
        ret.infoId = q.id
        ret.idx = idx + 1
        ret
      })
      quiz = Orm.convert(quiz)
      val ex = Orm.insert(quiz)
      ex.insert("questions")
      session.execute(ex)
      JSON.stringify(quiz)
    } else {
      // 考试模式
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
    }
  })

  @ResponseBody
  @RequestMapping(value = Array("/quiz/{id}"), method = Array(RequestMethod.DELETE), produces = Array("application/json"))
  def deleteQuiz(@PathVariable id: Long): Unit = dao.beginTransaction(session => {
    {
      // 删除所有QuizQuestion
      val root = Orm.root(classOf[QuizQuestion])
      session.execute(Orm.delete(root).where(root.get("quizId").eql(id)))
    }
    {
      // 删除该Quiz
      val root = Orm.root(classOf[Quiz])
      session.execute(Orm.delete(root).where(root.get("id").eql(id)))
    }
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
  def putQuizQuestion(@PathVariable qzId: Long, @PathVariable qtId: Long, @RequestBody body: String): String = dao.beginTransaction(session => {
    val answer = JSON.parse(body).asObj().getStr("answer")
    val root = Orm.root(classOf[QuizQuestion]).asSelect()
    root.select("info")
    // 查询题目
    val question = session.first(Orm.from(root).where(root.get("id").eql(qtId)))
    // 1. 设置答案，正确性与错误次数
    require(question != null)
    question.answer = answer
    question.correct = question.getCorrectAnswer == answer
    if (!question.correct) {
      question.fails += 1
    }
    // 2. 更新question
    session.execute(Orm.update(question))
    // 3. 更新quiz上的idx
    val quiz = Orm.empty(classOf[Quiz])
    quiz.idx = question.idx
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

