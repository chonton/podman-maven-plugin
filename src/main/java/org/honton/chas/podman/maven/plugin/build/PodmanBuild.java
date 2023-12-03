package org.honton.chas.podman.maven.plugin.build;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.honton.chas.podman.maven.plugin.PodmanContainerfile;
import org.honton.chas.podman.maven.plugin.cmdline.Cmd;

/** Create a container image from the Containerfile directions and files from context */
@Mojo(name = "build", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true)
public class PodmanBuild extends PodmanContainerfile {

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

  @Override
  protected final void doExecute()
      throws IOException, MojoExecutionException, ExecutionException, InterruptedException {
    BuildCmd buildCmd =
        new BuildCmd(this, image).addArgs(buildArguments).addPlatformAndImage(platforms, image);

    String cf = containerFile();
    if (!defaultContainerFile().equals(cf)) {
      buildCmd.addContainerfile(cf);
    }

    buildCmd.addParameter(shortestPath(contextDir.toPath()));
    executeCommand(buildCmd);

    if (loadDockerCache) {
      loadImage();
    }
  }

  private void loadImage()
      throws IOException, MojoExecutionException, ExecutionException, InterruptedException {
    Path path = dockerImageTar.toPath();
    Files.createDirectories(path.getParent());
    String tarLocation = shortestPath(path);

    executeCommand(
        new Cmd(this, image)
            .addCmd("save")
            .addParameter("--output")
            .addParameter(tarLocation)
            .addParameter(image));

    executeCommand(new Cmd(List.of("docker", "load", "-i", tarLocation), image));
  }
}
