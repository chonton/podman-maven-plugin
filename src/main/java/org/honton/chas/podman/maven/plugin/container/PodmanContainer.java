package org.honton.chas.podman.maven.plugin.container;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.honton.chas.podman.maven.plugin.PodmanGoal;
import org.honton.chas.podman.maven.plugin.config.ContainerConfig;
import org.honton.chas.podman.maven.plugin.config.NetworkConfig;

public abstract class PodmanContainer extends PodmanGoal {
  /** Map of container alias to container configuration. */
  @Parameter Map<String, ContainerConfig> containers;

  /** Map of networks */
  @Parameter NetworkConfig network;

  // Current maven project
  @Parameter(defaultValue = "${project}", readonly = true)
  MavenProject project;

  @Override
  protected final void doExecute() throws IOException, MojoExecutionException {
    doExecute(ContainerConfigHelper.order(containers, getLog()), getNetworkName());
  }

  final String getNetworkName() {
    if (network != null && network.name != null) {
      return network.name;
    }
    return project.getArtifactId();
  }

  protected abstract void doExecute(List<ContainerConfig> containerConfigs, String networkName)
      throws IOException, MojoExecutionException;
}
