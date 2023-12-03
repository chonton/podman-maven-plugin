package org.honton.chas.podman.maven.plugin.container;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import org.honton.chas.podman.maven.plugin.cmdline.CommandLine;
import org.honton.chas.podman.maven.plugin.config.ConnectionCfg;
import org.honton.chas.podman.maven.plugin.config.ExecConfig;

public class ContainerExecCommandLine<
        R extends ContainerExecCommandLine<R, C>, C extends ExecConfig>
    extends CommandLine {

  protected final C containerConfig;

  public ContainerExecCommandLine(ConnectionCfg goal, C containerConfig) {
    super(goal);
    addCmd("container");
    this.containerConfig = containerConfig;
  }

  String subCommand() {
    return "exec";
  }

  R addEnvironment(Consumer<String> warn) {
    if (containerConfig.envFile != null) {
      if (Files.isReadable(Path.of(containerConfig.envFile))) {
        addParameter("--env-file").addParameter(containerConfig.envFile);
      } else {
        warn.accept("ignoring env file " + containerConfig.envFile);
      }
    }

    if (containerConfig.env != null) {
      containerConfig.env.forEach((k, v) -> addParameter("--env").addParameter(k + '=' + v));
    }
    return (R) this;
  }

  R addContainerCmd() {
    if (containerConfig.cmd != null) {
      addParameter(containerConfig.cmd);
    }
    if (containerConfig.args != null) {
      containerConfig.args.forEach(this::addParameter);
    }
    return (R) this;
  }
}
