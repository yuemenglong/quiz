package yy.rdpac.bean

import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import org.springframework.stereotype.Component

/**
  * Created by <yuemenglong@126.com> on 2017/7/21.
  */
@Component
class AccessLog extends Filter {
  override def init(filterConfig: FilterConfig): Unit = {
    println("Init AccessLog Bean")
  }

  override def destroy(): Unit = {}

  override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain): Unit = {
    val request = servletRequest.asInstanceOf[HttpServletRequest]
    val response = servletResponse.asInstanceOf[HttpServletResponse]
    response.setHeader("Cache-Control", "no-cache")
    response.setHeader("Pragma", "no-cache")
    response.setDateHeader("expires", -1)
    println(request.getMethod, request.getRequestURI, request.getQueryString)
    filterChain.doFilter(servletRequest, servletResponse)
  }
}
