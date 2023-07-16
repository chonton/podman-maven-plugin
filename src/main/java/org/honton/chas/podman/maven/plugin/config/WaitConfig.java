package org.honton.chas.podman.maven.plugin.config;

import lombok.ToString;
import org.apache.maven.plugins.annotations.Parameter;

/** Container wait configuration */
@ToString
public class WaitConfig {
  /** Http probe configuration */
  @Parameter public HttpWaitConfig http;

  /** String to detect in log */
  @Parameter public String log;

  /** Seconds to wait before failing */
  @Parameter(defaultValue = "60")
  public int time;
}
