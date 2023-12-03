package org.honton.chas.podman.maven.plugin.config;

import lombok.ToString;
import org.apache.maven.plugins.annotations.Parameter;

/** Volume mount configuration */
@ToString
public class BindMountConfig {
  /** Absolute path of host directory */
  @Parameter(required = true)
  public String source;

  /** Absolute path of container directory */
  @Parameter(required = true)
  public String destination;

  /** Permissions for any created host directories */
  @Parameter public String permissions;

  /** Container access is readonly */
  @Parameter(defaultValue = "false")
  public boolean readonly;
}
