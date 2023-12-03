package org.honton.chas.podman.maven.plugin.config;

import lombok.ToString;
import org.apache.maven.plugins.annotations.Parameter;

/** Volume mount configuration */
@ToString
public class DeviceMountConfig {
  /** Absolute path of host device name */
  @Parameter(required = true)
  public String source;

  /** Absolute path of container device name */
  @Parameter public String destination;

  /** Container read access */
  @Parameter(defaultValue = "true")
  public Boolean read;

  /** Container write access */
  @Parameter(defaultValue = "true")
  public Boolean write;

  /** Container mknod access */
  @Parameter(defaultValue = "true")
  public Boolean mknod;
}
