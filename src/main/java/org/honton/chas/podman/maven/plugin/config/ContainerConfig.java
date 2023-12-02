package org.honton.chas.podman.maven.plugin.config;

import java.util.List;
import java.util.Map;
import lombok.ToString;
import org.apache.maven.plugins.annotations.Parameter;

/** Container configuration */
@ToString
public class ContainerConfig {

  /** Network alias of the container */
  public String alias;

  /** Name of the container. Defaults to `${network.name}.${container.alias}` */
  @Parameter public String name;

  /** Comma separated dependent container names */
  @Parameter public String requires;

  /** Image to run */
  @Parameter(required = true)
  public String image;

  /** Post launch wait configuration */
  @Parameter public WaitConfig wait;

  /** Post launch log configuration */
  @Parameter public LogConfig log;

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

  /** Override image command to execute */
  @Parameter public String cmd;

  /** Override image arguments for command */
  @Parameter public List<String> args;

  /** Override image entrypoint */
  @Parameter public String entrypoint;

  /** File containing environment variables that are set when container runs */
  @Parameter public String envFile;

  /** Map of environment variables that are set when container runs */
  @Parameter public Map<String, String> env;

  /** Volume mappings */
  @Parameter public MountsConfig mounts;

  /**
   * Map of host ports to container ports. Key is the name of a maven property. If property is set,
   * then that value is used as host [interface:]port; otherwise the property is set to the value of
   * the dynamically assigned host port. The value is the container port
   */
  @Parameter public Map<String, Integer> ports;
}
