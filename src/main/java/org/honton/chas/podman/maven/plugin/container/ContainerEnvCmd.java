package org.honton.chas.podman.maven.plugin.container;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import org.honton.chas.podman.maven.plugin.cmdline.Cmd;
import org.honton.chas.podman.maven.plugin.config.ConnectionCfg;
import org.honton.chas.podman.maven.plugin.config.ExecConfig;

abstract class ContainerEnvCmd extends Cmd {

  public ContainerEnvCmd(ConnectionCfg goal, ExecConfig execConfig, Consumer<String> warn) {
    super(goal, execConfig.name);
    addCmd("container");
    addCmd(subCommand());
    addEnvironment(execConfig, warn);
  }

  abstract String subCommand();

  private void addEnvironment(ExecConfig execConfig, Consumer<String> warn) {
    if (execConfig.envFile != null) {
      if (Files.isReadable(Path.of(execConfig.envFile))) {
        addParameter("--env-file").addParameter(execConfig.envFile);
      } else {
        warn.accept("ignoring env file " + execConfig.envFile);
      }
    }

    if (execConfig.env != null) {
      execConfig.env.forEach((k, v) -> addParameter("--env").addParameter(k + '=' + v));
    }
  }

  void addContainerCmd(ExecConfig execConfig) {
    if (execConfig.cmd != null) {
      addParameter(execConfig.cmd);
    }
    if (execConfig.args != null) {
      execConfig.args.forEach(this::addParameter);
    }
  }
}
