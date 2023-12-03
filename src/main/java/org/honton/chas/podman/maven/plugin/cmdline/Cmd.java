package org.honton.chas.podman.maven.plugin.cmdline;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.honton.chas.podman.maven.plugin.config.ConnectionCfg;

@RequiredArgsConstructor
public class Cmd {
  protected final List<String> command;
  public final String name;

  public Cmd(ConnectionCfg goal, String name) {
    this(new ArrayList<>(), name);
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

  public Cmd addCmd(String cmd) {
    command.add(cmd);
    return this;
  }

  public Cmd addParameter(String parameter) {
    command.add(parameter);
    return this;
  }

  public List<String> getCommand() {
    return command;
  }
}
