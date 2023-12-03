package org.honton.chas.podman.maven.plugin.container;

import org.honton.chas.podman.maven.plugin.PodmanGoal;

public class NetworkRmCmd extends NetworkCmd {

  public NetworkRmCmd(PodmanGoal goal, String networkName) {
    super(goal, networkName);
    addCmd("rm");
    addParameter(networkName);
  }
}
