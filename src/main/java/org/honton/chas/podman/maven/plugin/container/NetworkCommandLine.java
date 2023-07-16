package org.honton.chas.podman.maven.plugin.container;

import org.honton.chas.podman.maven.plugin.PodmanGoal;
import org.honton.chas.podman.maven.plugin.cmdline.CommandLine;

class NetworkCommandLine extends CommandLine {

  NetworkCommandLine(PodmanGoal goal) {
    super(goal);
    addCmd("network");
  }
}
