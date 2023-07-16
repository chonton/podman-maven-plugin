package org.honton.chas.podman.maven.plugin.config;

import lombok.ToString;
import org.apache.maven.plugins.annotations.Parameter;

/** Network configuration */
@ToString
public class NetworkConfig {
  /** Name of the network. Defaults to the project's artifactId */
  @Parameter public String name;

  /** Driver that manages the network */
  @Parameter(defaultValue = "bridge")
  public String driver;
}
