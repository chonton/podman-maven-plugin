package org.honton.chas.podman.maven.plugin.cmdline;

import java.util.ArrayList;
import java.util.List;
import org.honton.chas.podman.maven.plugin.PodmanGoal;

public class CommandLine {
  protected final List<String> command;

  public CommandLine(PodmanGoal goal) {
    command = new ArrayList<>();
    command.add("podman");

    if (goal.url != null) {
      command.add("--url");
      command.add(goal.url);
    }
    if (goal.connection != null) {
      command.add("--connection");
      command.add(goal.connection);
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
