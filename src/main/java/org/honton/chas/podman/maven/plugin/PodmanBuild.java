package org.honton.chas.podman.maven.plugin;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/** Package a podman chart and attach as secondary artifact */
@Mojo(name = "build", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true)
public class PodmanBuild extends PodmanGoal {
  /** Directory containing source content for build */
  @Parameter(required = true)
  File contextDir;

  /** Build instruction file */
  @Parameter File containerFile;

  /** Map of build arguments */
  @Parameter Map<String, String> buildArguments;

  /** The image name */
  @Parameter(required = true)
  String name;

  /** The os/arch of the built image(s) */
  @Parameter List<String> platforms;

  protected final void doExecute() throws IOException, MojoExecutionException {
    CommandLineGenerator generator = new CommandLineGenerator(this);
    generator.addArgs(buildArguments);
    if (generator.addPlatforms(platforms)) {
      generator.addManifest(name);
    } else {
      generator.addTag(name);
    }
    generator.addContainerFile(pwd.relativize(containerFile.toPath()));
    generator.addContext(pwd.relativize(contextDir.toPath()));
    executeCommand(generator.getCommand());
  }

  @Override
  public void addSubCommand(List<String> command) {
    command.add("build");
  }
}
