package yy.rdpac.exception

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import org.springframework.stereotype.Component
import org.springframework.web.servlet.{HandlerExceptionResolver, ModelAndView}

/**
  * Created by <yuemenglong@126.com> on 2017/9/6.
  */
@Component
class AppExceptionHandler extends HandlerExceptionResolver {
  override def resolveException(httpServletRequest: HttpServletRequest, httpServletResponse: HttpServletResponse,
                                o: scala.Any, e: Exception): ModelAndView = {
    val message = e match {
      case _: CodeNotExistsException => "注册码不存在"
      case _: CodeAlreadyUsedException => "注册码已失效"
      case _ =>
        e.printStackTrace()
        "请求出错"
    }
    httpServletResponse.setStatus(500)
    httpServletResponse.setHeader("Content-type", "text/html; charset=utf-8")
    httpServletResponse.getWriter.write(message)
    new ModelAndView()
  }
}
