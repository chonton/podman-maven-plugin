package org.honton.chas.podman.maven.plugin.config;

import java.util.List;
import java.util.Map;
import lombok.ToString;
import org.apache.maven.plugins.annotations.Parameter;

@ToString(callSuper = true)
public class ExecConfig extends IdentityConfig {
  /** Post launch wait configuration */
  @Parameter public WaitConfig wait;

  /** Post launch log configuration */
  @Parameter public LogConfig log;

  /** Override image command to execute */
  @Parameter public String cmd;

  /** Override image arguments for command */
  @Parameter public List<String> args;

  /** Override working directory */
  @Parameter public String workDir;

  /** File containing environment variables that are set when container runs */
  @Parameter public String envFile;

  /** Map of environment variables that are set when container runs */
  @Parameter public Map<String, String> env;
}
