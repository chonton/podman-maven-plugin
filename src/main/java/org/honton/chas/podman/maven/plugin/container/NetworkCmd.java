package org.honton.chas.podman.maven.plugin.container;

import org.honton.chas.podman.maven.plugin.PodmanGoal;
import org.honton.chas.podman.maven.plugin.cmdline.Cmd;

class NetworkCmd extends Cmd {

  NetworkCmd(PodmanGoal goal, String name) {
    super(goal, name);
    addCmd("network");
  }
}
