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

/** Create a container image from the Containerfile directions and files from context */
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
  String image;

  /** The os/arch of the built image(s) */
  @Parameter List<String> platforms;

  /** Load resulting image into docker image cache */
  @Parameter(defaultValue = "false")
  boolean loadDockerCache;

  /** image tar */
  @Parameter(
      required = true,
      readonly = true,
      defaultValue = "${project.build.directory}/dockerImage.tar")
  File dockerImageTar;

  protected final void doExecute() throws IOException, MojoExecutionException {
    executeCommand(
        new CommandLineGenerator(this)
            .addCmd("build")
            .addArgs(buildArguments)
            .addPlatformAndImage(platforms, image)
            .addContainerfile(containerfile)
            .addContext(pwd.relativize(contextDir.toPath())));

    if (loadDockerCache) {
      loadImage();
    }
  }

  private void loadImage() throws IOException, MojoExecutionException {
    Path path = dockerImageTar.toPath();
    Files.createDirectories(path.getParent());
    String tarLocation = pwd.relativize(path).toString();

    executeCommand(
        new CommandLineGenerator(this)
            .addCmd("save")
            .addParameter("--output")
            .addParameter(tarLocation)
            .addParameter(image));

    executeCommand(List.of("docker", "load", "-i", tarLocation), null);
  }
}
