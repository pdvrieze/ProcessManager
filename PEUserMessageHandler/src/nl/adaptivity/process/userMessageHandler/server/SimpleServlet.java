package nl.adaptivity.process.userMessageHandler.server;

import java.io.IOException;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;


public class SimpleServlet implements Servlet {

  private ServletConfig aConfig;

  @Override
  public void destroy() {
  }

  @Override
  public ServletConfig getServletConfig() {
    return aConfig;
  }

  @Override
  public String getServletInfo() {
    return "Simple servlet providing fake REST-style callbacks for testing stuff";
  }

  @Override
  public void init(ServletConfig pConfig) throws ServletException {
    aConfig = pConfig;

  }

  @Override
  public void service(ServletRequest pReq, ServletResponse pRes) throws ServletException, IOException {
    if (pReq instanceof HttpServletRequest) {
      final HttpServletRequest request = (HttpServletRequest) pReq;
      if (request.getMethod().equals("GET")) {
        String pathInfo = request.getPathInfo();
        
        if (pathInfo.equals("/processModels")) {
          getProcessModels(pRes.getOutputStream());
          
        } else {  
          StringBuilder s = new StringBuilder();
          
          
          s.append("contextPath:").append(request.getContextPath()).append('\n');
          s.append("pathInfo:").append(request.getPathInfo()).append('\n');
          s.append("pathTranslated:").append(request.getPathTranslated()).append('\n');
          s.append("queryString:").append(request.getQueryString()).append('\n');
          s.append("queryString:").append(request.getQueryString()).append('\n');
  
          
          
          ServletOutputStream out = pRes.getOutputStream();
          
          out.print(s.toString());
        }        
      }
      
    }
  }

  private void getProcessModels(ServletOutputStream pOutputStream) throws IOException {
    StringBuilder out = new StringBuilder();
    out.append("<processModels>\n");
    out.append("  <processModel name=\"processModel A\" handle=\"1\"/>\n");
    out.append("  <processModel name=\"processModel B\" handle=\"2\"/>\n");
    out.append("</processModels>\n");
    
    pOutputStream.print(out.toString());
  }

}
