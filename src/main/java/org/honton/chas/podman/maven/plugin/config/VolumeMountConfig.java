package org.honton.chas.podman.maven.plugin.config;

import lombok.ToString;
import org.apache.maven.plugins.annotations.Parameter;

/** Volume mount configuration */
@ToString
public class VolumeMountConfig {
  /** Volume name */
  @Parameter(required = true)
  public String source;

  /** Absolute path of container directory */
  @Parameter(required = true)
  public String destination;

  /** Container access is readonly */
  @Parameter(defaultValue = "false")
  public boolean readonly;
}
