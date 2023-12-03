package org.honton.chas.podman.maven.plugin.config;

import java.util.Map;
import lombok.ToString;
import org.apache.maven.plugins.annotations.Parameter;

/** Container configuration */
@ToString(callSuper = true)
public class ContainerConfig extends ExecConfig {

  /** Image to run */
  @Parameter(required = true)
  public String image;

  /**
   * Memory limit. Must be number followed by unit of 'b' (bytes), 'k' (kibibytes), 'm' (mebibytes),
   * or 'g' (gibibytes)
   */
  @Parameter public String memory;

  /**
   * Memory plus swap limit. Must be number followed by unit of 'b' (bytes), 'k' (kibibytes), 'm'
   * (mebibytes), or 'g' (gibibytes)
   */
  @Parameter public String memorySwap;

  /** Override image entrypoint */
  @Parameter public String entrypoint;

  /** Volume mappings */
  @Parameter public MountsConfig mounts;

  /**
   * Map of host ports to container ports. Key is the name of a maven property. If property is set,
   * then that value is used as host [interface:]port; otherwise the property is set to the value of
   * the dynamically assigned host port. The value is the container port
   */
  @Parameter public Map<String, Integer> ports;
}
