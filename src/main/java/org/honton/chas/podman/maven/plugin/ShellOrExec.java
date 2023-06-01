package org.honton.chas.podman.maven.plugin;

import java.util.List;

/** Description of an entrypoint or command */
public class ShellOrExec {
  /** Executable and parameters, no shell involved */
  List<String> exec;

  /** Single line command that will be executed by shell (not used if exec specified) */
  String shell;
}
