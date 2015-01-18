package net.wetheinter.gwtc.server;

import static net.wetheinter.gwtc.server.GwtcMonitorService.addCompileRequest;
import static net.wetheinter.gwtc.server.GwtcMonitorService.addToWatchers;

import java.io.IOException;

import javax.servlet.AsyncContext;
import javax.servlet.ServletContext;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import xapi.io.X_IO;

@WebServlet(name="compileServlet", urlPatterns={"/io"}, asyncSupported=true)
public class CompilerServlet extends HttpServlet {
  // track bid prices
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) {
     AsyncContext async = request.startAsync(request, response);
     async.getResponse().setContentType("text/event-stream");
     async.getResponse().setCharacterEncoding("UTF-8");
     ServletContext ctx = request.getServletContext();

     String ident = request.getParameter("compile");
     addToWatchers(ctx, ident, async);
  }

  // initialize a compile request
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // run in a transactional context
    // save a new bid
    String input = X_IO.toStringUtf8(request.getInputStream());

    int index = input.indexOf("::");
    String type = input.substring(0, index);
    input = input.substring(index+2);

    CompileRequest req = new CompileRequest(type);
    req.setArgs(input);
    addCompileRequest(request.getServletContext(), req);

    response.setContentType("text/plain");
    X_IO.drain(response.getOutputStream(), X_IO.toStreamUtf8(req.getId()));
  }
}
