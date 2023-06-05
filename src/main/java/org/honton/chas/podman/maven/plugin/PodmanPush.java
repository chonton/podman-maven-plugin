package org.honton.chas.podman.maven.plugin;

import java.io.IOException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Push image to registry
 *
 * @since 0.0.2
 */
@Mojo(name = "push", defaultPhase = LifecyclePhase.DEPLOY, threadSafe = true)
public class PodmanPush extends PodmanGoal {

  /** Fully qualified image name containing registry prefix, repository name, and version */
  @Parameter(required = true)
  String image;

  protected final void doExecute() throws IOException, MojoExecutionException {
    CommandLineGenerator generator = new CommandLineGenerator(remote);
    generator.addCmd("push");
    generator.addParameter(image);
    executeCommand(generator);
  }
}
