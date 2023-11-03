package org.honton.chas.podman.maven.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.SneakyThrows;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.honton.chas.podman.maven.plugin.cmdline.CommandLine;

/** podman goal base functionality */
public abstract class PodmanGoal extends AbstractMojo {

  private static final Pattern WARNING =
      Pattern.compile("\\[?(warning)]?:? ?(.+)", Pattern.CASE_INSENSITIVE);

  private static final Pattern ERROR =
      Pattern.compile("\\[?(error)]?:? ?(.+)", Pattern.CASE_INSENSITIVE);

  /** Skip upgrade */
  @Parameter(property = "podman.skip", defaultValue = "false")
  boolean skip;

  /** Url of podman remote service */
  @Parameter public String url;

  /** Remote podman connection name */
  @Parameter public String connection;

  // work variables ...
  protected Path pwd; // current working directory
  protected StringBuilder errorOutput;
  private final AtomicReference<ExecutorService> executor = new AtomicReference<>();

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

  protected ExecutorService getExecutor() {
    ExecutorService foo = executor.get();
    if (foo == null) {
      ExecutorService pool = Executors.newCachedThreadPool();
      if (executor.compareAndSet(null, pool)) {
        foo = pool;
      } else {
        foo = executor.get();
      }
    }
    return foo;
  }

  protected abstract void doExecute() throws MojoExecutionException, IOException;

  void pumpLog(InputStream is, Consumer<String> lineConsumer) {
    try (LineNumberReader reader =
        new LineNumberReader(new InputStreamReader(is, StandardCharsets.UTF_8), 128)) {
      for (; ; ) {
        String line = reader.readLine();
        if (line == null) {
          break;
        }
        lineConsumer.accept(line);
      }
    } catch (IOException e) {
      lineConsumer.accept(e.getMessage());
    }
  }

  @SneakyThrows
  private void throwException(int exitCode) {
    if (exitCode != 0) {
      throw new MojoExecutionException("podman exit value: " + exitCode);
    }
  }

  public void executeCommand(CommandLine generator) throws MojoExecutionException, IOException {
    executeCommand(generator.getCommand());
  }

  protected void executeCommand(List<String> command) throws MojoExecutionException, IOException {
    executeCommand(command, null);
  }

  protected void executeCommand(List<String> command, String stdin)
      throws MojoExecutionException, IOException {
    executeCommand(command, stdin, this::infoLine, this::errorLine, this::throwException);
  }

  protected String executeInfoCommand(List<String> command)
      throws MojoExecutionException, IOException {
    StringBuilder sb = new StringBuilder();
    executeCommand(
        command, null, (l) -> sb.append(l).append('\n'), this::errorLine, this::throwException);
    return sb.toString();
  }

  protected void createProcess(CommandLine generator, Consumer<String> filter) throws IOException {
    createProcess(generator.getCommand(), null, filter, filter);
  }

  protected void executeCommand(CommandLine generator, IntConsumer exitCode)
      throws MojoExecutionException, IOException {
    executeCommand(generator.getCommand(), null, this::infoLine, this::errorLine, exitCode);
  }

  protected void executeCommand(
      List<String> command,
      String stdin,
      Consumer<String> stdout,
      Consumer<String> stderr,
      IntConsumer exitCode)
      throws MojoExecutionException, IOException {
    waitForProcess(exitCode, createProcess(command, stdin, stdout, stderr));
  }

  protected Process createProcess(
      List<String> command, String stdin, Consumer<String> stdout, Consumer<String> stderr)
      throws IOException {
    getLog().info(String.join(" ", command));
    errorOutput = new StringBuilder();

    ExecutorService pool = getExecutor();
    Process process = new ProcessBuilder(command).start();
    pool.execute(() -> pumpLog(process.getInputStream(), stdout));
    pool.execute(() -> pumpLog(process.getErrorStream(), stderr));

    OutputStream os = process.getOutputStream();
    if (stdin != null) {
      os.write(stdin.getBytes(StandardCharsets.UTF_8));
    }
    os.close();
    return process;
  }

  private void waitForProcess(IntConsumer exitCode, Process process) throws MojoExecutionException {
    try {
      exitCode.accept(process.waitFor());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new MojoExecutionException(e.getMessage(), e);
    }
  }

  private void infoLine(String lineText) {
    getLog().info(lineText);
  }

  private void errorLine(String lineText) {
    errorOutput.append(lineText);

    Matcher warning = WARNING.matcher(lineText);
    if (warning.matches()) {
      getLog().warn(warning.group(2));
    } else {
      Matcher error = ERROR.matcher(lineText);
      if (error.matches()) {
        getLog().error(error.group(2));
      } else {
        getLog().info(lineText);
      }
    }
  }
}
