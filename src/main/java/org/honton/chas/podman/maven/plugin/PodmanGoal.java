package org.honton.chas.podman.maven.plugin;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import lombok.Getter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.honton.chas.podman.maven.plugin.cmdline.Cmd;
import org.honton.chas.podman.maven.plugin.config.ConnectionCfg;
import org.honton.chas.podman.maven.plugin.container.ExecHelper;

/** podman goal base functionality */
public abstract class PodmanGoal extends AbstractMojo implements ConnectionCfg {

  /** podman command line interface */
  @Parameter(property = "podman.cli", defaultValue = "podman")
  @Getter
  public String cli;

  /** Url of podman remote service */
  @Parameter(property = "podman.url")
  @Getter
  public String url;

  /** Remote podman connection name */
  @Parameter(property = "podman.connection")
  @Getter
  public String connection;

  protected StringBuilder errorOutput;

  /** Skip upgrade */
  @Parameter(property = "podman.skip", defaultValue = "false")
  boolean skip;

  // work variables ...
  private Path pwd; // current working directory

  public final void execute() throws MojoFailureException, MojoExecutionException {
    if (skip) {
      getLog().info("skipping podman");
    } else {
      try {
        pwd = Path.of("").toAbsolutePath();
        doExecute();
      } catch (IOException | ExecutionException | InterruptedException e) {
        throw new MojoFailureException(e.getMessage(), e);
      }
    }
  }

  protected abstract void doExecute()
      throws MojoExecutionException, IOException, ExecutionException, InterruptedException;

  public void executeCommand(Cmd generator) throws MojoExecutionException {
    executeCommand(generator, null);
  }

  protected void executeCommand(Cmd generator, String stdin) throws MojoExecutionException {
    new ExecHelper(this, generator.name).createAndWait(generator.getCommand(), stdin);
  }

  protected String execYieldString(Cmd generator) throws MojoExecutionException {
    return new ExecHelper(this, generator.name).yieldString(generator.getCommand());
  }

  protected int execYieldInt(Cmd generator) throws MojoExecutionException {
    return new ExecHelper(this, generator.name).yieldInt(generator.getCommand());
  }

  protected String shortestPath(Path dst) {
    String relative = pwd.relativize(dst).toString();
    if (dst.isAbsolute()) {
      String absolute = dst.toString();
      if (absolute.length() < relative.length()) {
        return absolute;
      }
    }
    return relative.isEmpty() ? "./" : relative;
  }
}
