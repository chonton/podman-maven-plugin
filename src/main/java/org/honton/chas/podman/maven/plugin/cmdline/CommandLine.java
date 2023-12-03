package org.honton.chas.podman.maven.plugin.cmdline;

import java.util.ArrayList;
import java.util.List;
import org.honton.chas.podman.maven.plugin.config.ConnectionCfg;

public class CommandLine {
  protected final List<String> command;

  public CommandLine(ConnectionCfg goal) {
    command = new ArrayList<>();
    command.add(goal.getCli());

    if (goal.getUrl() != null) {
      command.add("--url");
      command.add(goal.getUrl());
    }
    if (goal.getConnection() != null) {
      command.add("--connection");
      command.add(goal.getConnection());
    }
  }

  public CommandLine addCmd(String cmd) {
    command.add(cmd);
    return this;
  }

  public CommandLine addParameter(String parameter) {
    command.add(parameter);
    return this;
  }

  public List<String> getCommand() {
    return command;
  }
}
