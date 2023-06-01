package org.honton.chas.podman.maven.plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
  @Parameter(required = true, defaultValue = "${project.build.directory}/contextDir")
  File contextDir;

  /** Build instruction file, relative to contextDir */
  @Parameter(required = true, defaultValue = "Containerfile")
  String containerfile;

  /** Map of build arguments */
  @Parameter Map<String, String> buildArguments;

  /** The image name */
  @Parameter(required = true)
  String name;

  /** The os/arch of the built image(s) */
  @Parameter List<String> platforms;

  /** Load resulting image into docker image cache */
  @Parameter(defaultValue = "false")
  boolean loadIntoDockerImageCache;

  /** image tar */
  @Parameter(
      required = true,
      readonly = true,
      defaultValue = "${project.build.directory}/dockerImage.tar")
  File dockerImageTar;

  protected final void doExecute() throws IOException, MojoExecutionException {
    CommandLineGenerator generator = new CommandLineGenerator(remote);
    generator.addCmd("build");
    generator.addArgs(buildArguments);
    if (generator.addPlatforms(platforms)) {
      generator.addManifest(name);
    } else {
      generator.addTag(name);
    }
    generator.addContainerfile(containerfile);
    generator.addContext(pwd.relativize(contextDir.toPath()));
    executeCommand(generator.getCommand());

    if (loadIntoDockerImageCache) {
      loadImage();
    }
  }

  private void loadImage() throws IOException, MojoExecutionException {
    CommandLineGenerator generator = new CommandLineGenerator(remote);
    generator.addCmd("save");
    generator.addCmd("--output");

    Path path = dockerImageTar.toPath();
    Files.createDirectories(path.getParent());
    String tarLocation = pwd.relativize(path).toString();
    generator.addCmd(tarLocation);

    generator.addCmd(name);
    executeCommand(generator.getCommand());

    executeCommand(List.of("docker", "load", "-i", tarLocation));
  }
}
