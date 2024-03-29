package org.honton.chas.podman.maven.plugin.volume;

import java.io.IOException;
import java.util.List;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.honton.chas.podman.maven.plugin.PodmanGoal;
import org.honton.chas.podman.maven.plugin.cmdline.CommandLine;

/**
 * Create a Volume
 *
 * @since 0.0.6
 */
@Mojo(name = "volume-create", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, threadSafe = true)
public class PodmanCreateVolume extends PodmanGoal {

  /** Volume name */
  @Parameter(required = true)
  String volume;

  protected final void doExecute() throws IOException, MojoExecutionException {
    List<String> command =
        new CommandLine(this)
            .addCmd("volume")
            .addParameter("create")
            .addParameter(volume)
            .getCommand();
    executeCommand(command);
  }
}
