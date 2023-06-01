package org.honton.chas.podman.maven.plugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;

/** podman goal base functionality */
public abstract class PodmanGoal extends AbstractMojo {
  private static final Pattern WARNING =
      Pattern.compile("\\[?(warning)]?:? ?(.+)", Pattern.CASE_INSENSITIVE);

  private static final Pattern ERROR =
      Pattern.compile("\\[?(error)]?:? ?(.+)", Pattern.CASE_INSENSITIVE);

  /** Skip upgrade */
  @Parameter(property = "podman.skip", defaultValue = "false")
  boolean skip;

  /** Remote podman instance */
  @Parameter(property = "remote")
  RemoteInfo remote;

  // work variables ...
  Path pwd; // current working directory

  public final void execute() throws MojoFailureException, MojoExecutionException {
    if (skip) {
      getLog().info("skipping podman");
    } else {
      try {
        pwd = Path.of("").toAbsolutePath();
        doExecute();
      } catch (IOException e) {
        throw new MojoFailureException(e.getMessage(), e);
      }
    }
  }

  protected abstract void doExecute() throws MojoExecutionException, IOException;

  void pumpLog(InputStream is, Consumer<String> lineConsumer) {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
      reader.lines().forEach(lineConsumer);
    } catch (IOException e) {
      lineConsumer.accept(e.getMessage());
    }
  }

  void executeCommand(List<String> command) throws MojoExecutionException, IOException {
    try {
      getLog().info(String.join(" ", command));
      Process process = new ProcessBuilder(command).start();

      ForkJoinPool pool = ForkJoinPool.commonPool();
      pool.execute(() -> pumpLog(process.getInputStream(), getLog()::info));
      pool.execute(() -> pumpLog(process.getErrorStream(), this::logLine));

      if (process.waitFor() != 0) {
        throw new MojoExecutionException("helm exit value: " + process.exitValue());
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new MojoExecutionException(e.getMessage(), e);
    }
  }

  private void logLine(String s) {
    Matcher error = ERROR.matcher(s);
    if (error.matches()) {
      getLog().error(error.group(2));
    } else {
      Matcher warning = WARNING.matcher(s);
      if (warning.matches()) {
        getLog().warn(warning.group(2));
      } else {
        getLog().info(s);
      }
    }
  }
}
