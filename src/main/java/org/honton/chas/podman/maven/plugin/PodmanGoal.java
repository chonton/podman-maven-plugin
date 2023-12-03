package org.honton.chas.podman.maven.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.honton.chas.podman.maven.plugin.cmdline.CommandLine;
import org.honton.chas.podman.maven.plugin.config.ConnectionCfg;

/** podman goal base functionality */
public abstract class PodmanGoal extends AbstractMojo implements ConnectionCfg {

  private static final Pattern WARNING =
      Pattern.compile("\\[?(warning)]?:? ?(.+)", Pattern.CASE_INSENSITIVE);

  private static final Pattern ERROR =
      Pattern.compile("\\[?(error)]?:? ?(.+)", Pattern.CASE_INSENSITIVE);

  private final AtomicReference<ScheduledExecutorService> executor = new AtomicReference<>();

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

  private static void waitNoError(ExecutorCompletionService<Object> done)
      throws InterruptedException, MojoExecutionException, ExecutionException {
    int exitCode = waitForResult(done);
    if (exitCode != 0) {
      throw new MojoExecutionException("command exited with error - " + exitCode);
    }
  }

  private static int waitForResult(ExecutorCompletionService<Object> done)
      throws InterruptedException, MojoExecutionException, ExecutionException {
    for (; ; ) {
      Future<Object> poll = done.poll(30, TimeUnit.SECONDS);
      if (poll == null) {
        throw new MojoExecutionException("timed out");
      }
      Object result = poll.get();
      if (result instanceof Integer) {
        return (Integer) result;
      }
      // null from poll.get() would indicate that stdout or stderr was closed
    }
  }

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

  public ScheduledExecutorService executorService() {
    ScheduledExecutorService executorService = executor.get();
    if (executorService == null) {
      ScheduledExecutorService pool =
          Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
      if (executor.compareAndSet(null, pool)) {
        executorService = pool;
      } else {
        executorService = executor.get();
      }
    }
    return executorService;
  }

  public ExecutorCompletionService<Object> completionService() {
    return new ExecutorCompletionService<>(executorService());
  }

  protected abstract void doExecute()
      throws MojoExecutionException, IOException, ExecutionException, InterruptedException;

  Void pumpLog(InputStream is, Consumer<String> lineConsumer) throws IOException {
    try (LineNumberReader reader =
        new LineNumberReader(new InputStreamReader(is, StandardCharsets.UTF_8), 128)) {
      for (; ; ) {
        String line = reader.readLine();
        lineConsumer.accept(line);
        if (line == null) {
          return null;
        }
      }
    }
  }

  public void executeCommand(CommandLine generator)
      throws MojoExecutionException, IOException, ExecutionException, InterruptedException {
    executeCommand(generator.getCommand());
  }

  protected void executeCommand(List<String> command)
      throws MojoExecutionException, IOException, ExecutionException, InterruptedException {
    executeCommand(command, null);
  }

  protected void executeCommand(List<String> command, String stdin)
      throws MojoExecutionException, IOException, ExecutionException, InterruptedException {

    ExecutorCompletionService<Object> completions = completionService();
    createProcess(completions, command, stdin, this::infoLine, this::errorLine);
    waitForResult(completions);
  }

  protected String execYieldString(List<String> command)
      throws MojoExecutionException, IOException, InterruptedException, ExecutionException {
    StringBuilder sb = new StringBuilder();

    ExecutorCompletionService<Object> completions = completionService();
    createProcess(completions, command, null, (l) -> sb.append(l).append('\n'), this::errorLine);
    waitNoError(completions);

    return sb.toString();
  }

  protected int execYieldInt(List<String> command)
      throws MojoExecutionException, IOException, ExecutionException, InterruptedException {

    ExecutorCompletionService<Object> completions = completionService();
    createProcess(completions, command, null, this::infoLine, this::errorLine);
    return waitForResult(completions);
  }

  public void createProcess(
      ExecutorCompletionService<Object> completionService,
      List<String> command,
      String stdin,
      Consumer<String> stdout,
      Consumer<String> stderr)
      throws IOException {
    getLog().info(String.join(" ", command));
    errorOutput = new StringBuilder();

    Process process = new ProcessBuilder(command).start();
    completionService.submit(() -> pumpLog(process.getInputStream(), stdout));
    completionService.submit(() -> pumpLog(process.getErrorStream(), stderr));

    OutputStream os = process.getOutputStream();
    if (stdin != null) {
      os.write(stdin.getBytes(StandardCharsets.UTF_8));
    }
    os.close();

    completionService.submit(process::waitFor);
  }

  public void infoLine(String lineText) {
    if (lineText != null) {
      getLog().info(lineText);
    }
  }

  public void errorLine(String lineText) {
    if (lineText != null) {
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

  public Path relativePath(Path dst) {
    return pwd.relativize(dst);
  }

  protected String shortestPath(Path dst) {
    String relative = relativePath(dst).toString();
    if (dst.isAbsolute()) {
      String absolute = dst.toString();
      if (absolute.length() < relative.length()) {
        return absolute;
      }
    }
    return relative.isEmpty() ? "./" : relative;
  }
}
