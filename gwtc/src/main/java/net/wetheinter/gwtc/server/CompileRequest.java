package net.wetheinter.gwtc.server;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CompileRequest implements Serializable {

  private String id;
  private String args;
  private String type;
  private List<String> log;
  private boolean finished;

  public CompileRequest(String type) {
    this.type = type;
    id = UUID.randomUUID().toString();
    log = new ArrayList<>();
  }

  public void addLog(String log) {
    this.log.add(log);
  }

  public String getId() {
    return id;
  }

  public void setArgs(String args) {
    this.args = args;
  }

  public boolean isFinished() {
    return finished;
  }

  public void setFinished(boolean finished) {
    this.finished = finished;
  }

  public boolean hasNewMessages(int pos) {
    return pos < log.size();
  }

  public String getMessage(int pos) {
    return log.get(pos);
  }

  public String[] getArgs() {
    return args.replaceAll("[ ][ ]+", " ").split(" ");
  }

}
