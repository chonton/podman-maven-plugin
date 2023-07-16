package org.honton.chas.podman.maven.plugin.config;

import java.util.List;
import lombok.ToString;
import org.apache.maven.plugins.annotations.Parameter;

/** Description of an entrypoint or command */
@ToString
public class ShellOrExecConfig {
  /** Executable and parameters, no shell involved */
  @Parameter public List<String> exec;

  /** Single line command that will be executed by shell (not used if exec specified) */
  @Parameter public String shell;
}
