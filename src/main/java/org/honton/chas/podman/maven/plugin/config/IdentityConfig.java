package org.honton.chas.podman.maven.plugin.config;

import lombok.ToString;
import org.apache.maven.plugins.annotations.Parameter;

/** Container configuration */
@ToString
public class IdentityConfig {

  /** Network alias of the container */
  public String alias;

  /** Name of the container. Defaults to `${network.name}.${container.alias}` */
  @Parameter public String name;

  /** Comma separated dependent container names */
  @Parameter public String requires;
}
