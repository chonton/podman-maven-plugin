package org.honton.chas.podman.maven.plugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
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

  /** Skip upgrade */
  @Parameter(property = "podman.skip", defaultValue = "false")
  boolean skip;

  /** Url of podman remote service */
  @Parameter public String url;

  /** Remote podman connection name */
  @Parameter public String connection;

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

  void executeCommand(CommandLineGenerator generator) throws MojoExecutionException, IOException {
    executeCommand(generator.getCommand(), null);
  }

  void executeCommand(List<String> command, String stdin)
      throws MojoExecutionException, IOException {
    getLog().info(String.join(" ", command));
    Process process = new ProcessBuilder(command).start();

    ForkJoinPool pool = ForkJoinPool.commonPool();
    pool.execute(() -> pumpLog(process.getInputStream(), getLog()::info));
    pool.execute(() -> pumpLog(process.getErrorStream(), this::errorLine));

    OutputStream os = process.getOutputStream();
    if (stdin != null) {
      os.write(stdin.getBytes(StandardCharsets.UTF_8));
    }
    os.close();

    try {
      if (process.waitFor() != 0) {
        throw new MojoExecutionException("podman exit value: " + process.exitValue());
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new MojoExecutionException(e.getMessage(), e);
    }
  }

  private void errorLine(String s) {
    Matcher warning = WARNING.matcher(s);
    if (warning.matches()) {
      getLog().warn(warning.group(2));
    } else {
      getLog().info(s);
    }
  }
}
