package uk.ac.bournemouth.darwin.html.util;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletResponse;


public class DarwinHtml {

  public static void writeError(HttpServletResponse pResponse, int pStatus, String pTitle, Throwable pError) {
    pResponse.setStatus(pStatus);
    pResponse.setContentType("text/html");
    try {
      PrintWriter out = pResponse.getWriter();
      try {
        writeDarwinHeader(out, pTitle);
        writeStackTrace(out, pError);
        writeDarwinFooter(out);
      } finally {
        out.close();
      }
    } catch (IOException e) {
      pResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      e.printStackTrace();
    }
  }

  private static void writeStackTrace(PrintWriter pOut, Throwable pError) {
    Throwable th = pError;
    pOut.println("<p>");
    while (th!=null) {
      pOut.print("<div class=\"stacktrace\">");
      pOut.print("<h3>");
      pOut.print(th.getMessage());
      pOut.println("</h3>");
      StackTraceElement[] stacktrace = th.getStackTrace();
      for (StackTraceElement elem: stacktrace) {
        pOut.print("  <div>"); pOut.print(elem.toString()); pOut.println("</div>");
      }
      if (th!=th.getCause()) { 
        th = th.getCause(); 
        pOut.println("</p><p>Caused by:"); 
      }
    }
    pOut.println("</p>");
  }

  private static void writeDarwinFooter(PrintWriter pOut) {
    pOut.println("    </div>");
    pOut.println("    </div><!--");
    pOut.println("    --><div id=\"footer\"><span id=\"divider\"></span>Darwin is a Bournemouth University Project</div>");
    pOut.println("  </body>");
    pOut.println("</html>");
  }

  private static void writeDarwinHeader(PrintWriter pOut, String pTitle) {
    pOut.println("<!DOCTYPE html>");
    pOut.println("<html>");
    pOut.println("  <head>");
    pOut.print  ("    <title>"); pOut.print(pTitle); pOut.println("</title>");
    pOut.println("    <link rel=\"stylesheet\" href=\"/css/darwin.css\" />");
    pOut.println("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" />");
    pOut.println("  </head>");
    pOut.println("  <body>");
    pOut.println("    <div id=\"top\">");
    pOut.print  ("    <h1 id=\"header\"><a href=\"/\" id=\"logo\">Darwin</a><span id=\"title\">");
    pOut.print(pTitle);
    pOut.println("</span></h1>");
    writeMenu(pOut);
    pOut.println("<div id=\"content\">");
  }

  private static void writeMenu(PrintWriter pOut) {
    pOut.println("    <div id=\"menu\">");
    pOut.println("      <span class=\"menuitem\">Inbox</span>");
    pOut.println("      <span class=\"menuitem\">Actions</span>");
    pOut.println("      <span class=\"menuitem\">Processes</span>");
    pOut.println("      <span class=\"menuitem\">Forms</span>");
    pOut.println("    </div>");
  }

}
