package org.honton.chas.podman.maven.plugin.container;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.honton.chas.podman.maven.plugin.PodmanGoal;
import org.honton.chas.podman.maven.plugin.config.IdentityConfig;
import org.honton.chas.podman.maven.plugin.config.LogConfig;
import org.honton.chas.podman.maven.plugin.config.NetworkConfig;

public abstract class PodmanContainer<T extends IdentityConfig> extends PodmanGoal {

  /** Map of container alias to container configuration. */
  @Parameter Map<String, T> containers;

  /** Network Configuration */
  @Parameter NetworkConfig network;

  // Current maven project
  @Parameter(defaultValue = "${project}", readonly = true)
  MavenProject project;

  private static String sanitize(String jobName) {
    return jobName.replaceAll("[^a-zA-Z0-9_.-]", ".");
  }

  protected static String containerIdPropertyName(IdentityConfig containerConfig) {
    return "container." + containerConfig.alias + ".id";
  }

  @Override
  protected final void doExecute() throws IOException, MojoExecutionException {
    String networkName = getNetworkName();
    List<T> ordered = IdentityConfigHelper.order(networkName, containers, getLog());
    doExecute(ordered, networkName);
  }

  private String getNetworkName() {
    if (network != null && network.name != null) {
      return network.name;
    }
    String jobName = System.getenv("JOB_NAME");
    if (jobName != null) {
      return sanitize(jobName);
    }
    return project.getArtifactId();
  }

  protected abstract void doExecute(List<T> containerConfigs, String networkName)
      throws IOException, MojoExecutionException;

  protected void setProperty(String mavenPropertyName, String mavenPropertyValue) {
    getLog().info("Setting " + mavenPropertyName + " to " + mavenPropertyValue);
    project.getProperties().setProperty(mavenPropertyName, mavenPropertyValue);
  }

  protected String getProperty(String mavenPropertyName) {
    return project.getProperties().getProperty(mavenPropertyName);
  }

  String containerId(IdentityConfig containerConfig) {
    String containerId = getProperty(containerIdPropertyName(containerConfig));
    if (containerId != null) {
      return containerId;
    }
    return containerConfig.name;
  }

  BufferedWriter createBufferedWriter(LogConfig logConfig, String alias)
      throws IOException, MojoExecutionException {
    if (logConfig == null) {
      return null;
    }

    Path path;
    if (logConfig.file != null) {
      path = Path.of(logConfig.file);
    } else if (alias != null) {
      path = project.getBasedir().toPath().resolve(Path.of("target", "container", alias + ".log"));
    } else {
      throw new MojoExecutionException("container exec log requires file parameter");
    }

    Files.createDirectories(path.getParent());
    return Files.newBufferedWriter(
        path, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
  }
}
