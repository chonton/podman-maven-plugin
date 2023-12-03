package org.honton.chas.podman.maven.plugin;

import java.io.File;
import org.apache.maven.plugins.annotations.Parameter;

public abstract class PodmanContainerfile extends PodmanGoal {

  /** Directory containing source content for build */
  @Parameter(required = true, defaultValue = "${project.build.directory}/contextDir")
  public File contextDir;

  /** Build instruction file, relative to contextDir */
  @Parameter(required = true)
  public String containerfile;

  protected String containerFile() {
    if (containerfile != null) {
      return containerfile;
    }
    return defaultContainerFile();
  }

  protected String defaultContainerFile() {
    return cli.contains("podman") ? "Containerfile" : "Dockerfile";
  }
}
