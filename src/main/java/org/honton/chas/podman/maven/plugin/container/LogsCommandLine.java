package org.honton.chas.podman.maven.plugin.container;

import org.honton.chas.podman.maven.plugin.cmdline.CommandLine;
import org.honton.chas.podman.maven.plugin.config.LogConfig;

class LogsCommandLine extends CommandLine {

  LogsCommandLine(PodmanContainerRun goal, LogConfig logConfig, String containerName) {
    super(goal);
    addCmd("logs");
    addCmd("--follow");
    if (logConfig.timestamps) {
      addParameter("--timestamps");
    }
    addParameter(containerName);
  }
}
