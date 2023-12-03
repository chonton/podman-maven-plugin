package org.honton.chas.podman.maven.plugin.container;

import org.honton.chas.podman.maven.plugin.cmdline.Cmd;
import org.honton.chas.podman.maven.plugin.config.IdentityConfig;

class ContainerRmCmd extends Cmd {

  ContainerRmCmd(PodmanContainerRm goal, IdentityConfig containerConfig) {
    super(goal);
    addCmd("container");
    addCmd("rm");
    addParameter("-f");
    addParameter(goal.containerId(containerConfig));
  }
}
