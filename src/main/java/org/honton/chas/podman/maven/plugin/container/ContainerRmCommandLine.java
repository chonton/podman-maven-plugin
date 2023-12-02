package org.honton.chas.podman.maven.plugin.container;

import org.honton.chas.podman.maven.plugin.cmdline.CommandLine;
import org.honton.chas.podman.maven.plugin.config.ContainerConfig;

class ContainerRmCommandLine extends CommandLine {

  ContainerRmCommandLine(PodmanContainerRm goal, ContainerConfig containerConfig) {
    super(goal);
    addCmd("container");
    addCmd("rm");
    addParameter("-f");
    addParameter(goal.containerId(containerConfig));
  }
}
