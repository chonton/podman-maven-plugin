package org.honton.chas.podman.maven.plugin.config;

import org.apache.maven.plugins.annotations.Parameter;

/** Http probe wait configuration */
public class HttpWaitConfig {
  /** Url to invoke */
  @Parameter(required = true)
  public String url;

  /** method to use */
  @Parameter(required = true, defaultValue = "GET")
  public String method;

  /** Expected status code */
  @Parameter(required = true, defaultValue = "200")
  public int status;

  /** Interval (in seconds) between polls */
  @Parameter(required = true, defaultValue = "15")
  public int interval;
}
