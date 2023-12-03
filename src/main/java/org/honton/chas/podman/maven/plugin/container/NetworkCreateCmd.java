package org.honton.chas.podman.maven.plugin.container;

import org.honton.chas.podman.maven.plugin.PodmanGoal;
import org.honton.chas.podman.maven.plugin.config.NetworkConfig;

public class NetworkCreateCmd extends NetworkCmd {

  public NetworkCreateCmd(PodmanGoal goal, NetworkConfig network, String networkName) {
    super(goal);

    addCmd("create");
    if (network != null && network.driver != null) {
      addParameter("--driver").addParameter(network.driver);
    }
    addParameter(networkName);
  }
}
