package uk.ac.bournemouth.darwin.html.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;


public class DarwinHtml {

  public static void writeError(@NotNull final HttpServletResponse response, final int status, @NotNull final String title, final Throwable error) {
    response.setStatus(status);
    response.setContentType("text/html");
    try {
      try (@NotNull final PrintWriter out = response.getWriter()){
        writeDarwinHeader(out, title);
        writeStackTrace(out, error);
        writeDarwinFooter(out);
      }
    } catch (final IOException e) {
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      e.printStackTrace();
    }
  }

  private static void writeStackTrace(@NotNull final PrintWriter out, @Nullable final Throwable error) {
    Throwable th = error;
    out.println("<p>");
    while (th != null) {
      out.print("<div class=\"stacktrace\">");
      out.print("<h3>");
      out.print(th.getMessage());
      out.println("</h3>");
      final StackTraceElement[] stacktrace = th.getStackTrace();
      for (final StackTraceElement elem : stacktrace) {
        out.print("  <div>");
        out.print(elem.toString());
        out.println("</div>");
      }
      if (th != th.getCause()) {
        th = th.getCause();
        out.println("</p><p>Caused by:");
      }
    }
    out.println("</p>");
  }

  private static void writeDarwinFooter(@NotNull final PrintWriter out) {
    out.println("    </div>");
    out.println("    </div><!--");
    out.println("    --><div id=\"footer\"><span id=\"divider\"></span>Darwin is a Bournemouth University Project</div>");
    out.println("  </body>");
    out.println("</html>");
  }

  private static void writeDarwinHeader(@NotNull final PrintWriter out, @NotNull final String title) {
    out.println("<!DOCTYPE html>");
    out.println("<html>");
    out.println("  <head>");
    out.print("    <title>");
    out.print(title);
    out.println("</title>");
    out.println("    <link rel=\"stylesheet\" href=\"/css/darwin.css\" />");
    out.println("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" />");
    out.println("  </head>");
    out.println("  <body>");
    out.println("    <div id=\"top\">");
    out.print("    <h1 id=\"header\"><a href=\"/\" id=\"logo\">Darwin</a><span id=\"title\">");
    out.print(title);
    out.println("</span></h1>");
    writeMenu(out);
    out.println("<div id=\"content\">");
  }

  private static void writeMenu(final PrintWriter out) {
    out.println("    <div id=\"menu\">");
    out.println("      <span class=\"menuitem\">Inbox</span>");
    out.println("      <span class=\"menuitem\">Actions</span>");
    out.println("      <span class=\"menuitem\">Processes</span>");
    out.println("      <span class=\"menuitem\">Forms</span>");
    out.println("    </div>");
  }

}
