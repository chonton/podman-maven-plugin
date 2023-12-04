package org.honton.chas.podman.maven.plugin.container;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import lombok.Getter;
import org.honton.chas.podman.maven.plugin.config.ExecConfig;

class ContainerExecCmd extends ContainerEnvCmd {

  @Getter private final Map<Integer, String> portToPropertyName = new HashMap<>();

  ContainerExecCmd(
      PodmanContainerExec goal, ExecConfig execConfig, Consumer<String> warn, String containerId) {
    super(goal, execConfig, warn);
    if (execConfig.log != null || execConfig.wait != null && execConfig.wait.log != null) {
      addParameter("-it");
    }
    addParameter(containerId);
    addContainerCmd(execConfig);
  }

  @Override
  String subCommand() {
    return "exec";
  }
}
