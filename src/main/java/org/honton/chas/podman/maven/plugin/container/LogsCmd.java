package org.honton.chas.podman.maven.plugin.container;

import org.honton.chas.podman.maven.plugin.cmdline.Cmd;
import org.honton.chas.podman.maven.plugin.config.LogConfig;

class LogsCmd extends Cmd {

  LogsCmd(PodmanContainer<?> goal, LogConfig logConfig, String containerName) {
    super(goal);
    addCmd("logs");
    addCmd("--follow");
    if (logConfig != null && logConfig.timestamps) {
      addParameter("--timestamps");
    }
    addParameter(containerName);
  }
}
