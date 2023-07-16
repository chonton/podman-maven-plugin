package org.honton.chas.podman.maven.plugin.config;

import lombok.ToString;
import org.apache.maven.plugins.annotations.Parameter;

/** Log configuration */
@ToString
public class LogConfig {
  /** Name of the file to receive logs. Defaults to container name in target/podman directory */
  @Parameter public String file;

  /** Show timestamps in output */
  @Parameter(defaultValue = "false")
  public boolean timestamps;
}
