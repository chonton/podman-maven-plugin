package org.honton.chas.podman.maven.plugin.config;

import lombok.ToString;
import org.apache.maven.plugins.annotations.Parameter;

/** Temporary file system mount configuration */
@ToString
public class TempFsMountConfig {
  /** Absolute path of container directory */
  @Parameter(required = true)
  public String destination;
}
