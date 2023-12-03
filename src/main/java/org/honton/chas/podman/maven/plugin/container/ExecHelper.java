package org.honton.chas.podman.maven.plugin.container;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.Interpolator;
import org.codehaus.plexus.interpolation.PropertiesBasedValueSource;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;
import org.honton.chas.podman.maven.plugin.PodmanGoal;
import org.honton.chas.podman.maven.plugin.cmdline.Cmd;
import org.honton.chas.podman.maven.plugin.config.HttpWaitConfig;
import org.honton.chas.podman.maven.plugin.config.WaitConfig;

public class ExecHelper {

  private static final Pattern WARNING =
      Pattern.compile("\\[?(warning)]?:? ?(.+)", Pattern.CASE_INSENSITIVE);

  private static final Pattern ERROR =
      Pattern.compile("\\[?(error)]?:? ?(.+)", Pattern.CASE_INSENSITIVE);

  private final ScheduledExecutorService executorService;
  private final ExecutorCompletionService<Object> completionService;
  private final Consumer<String> debugLine;
  private final Consumer<String> infoLine;
  private final Consumer<String> errorLine;
  private final StringBuilder errorOutput;
  private final String prompt;
  private final List<Future<?>> cancellableTasks = new ArrayList<>();

  public ExecHelper(PodmanGoal goal, String name) {
    prompt = name;
    errorOutput = new StringBuilder();

    Log log = goal.getLog();
    debugLine =
        (lineText) -> {
          if (lineText != null) {
            log.debug(prompt + ':' + lineText);
          }
        };
    infoLine =
        (lineText) -> {
          if (lineText != null) {
            log.info(prompt + ':' + lineText);
          }
        };
    errorLine =
        (lineText) -> {
          if (lineText != null) {
            errorOutput.append(lineText);
            Matcher warning = WARNING.matcher(lineText);
            if (warning.matches()) {
              log.warn(prompt + ':' + warning.group(2));
            } else {
              Matcher error = ERROR.matcher(lineText);
              if (error.matches()) {
                log.error(prompt + ':' + error.group(2));
              } else {
                log.info(prompt + ':' + lineText);
              }
            }
          }
        };

    // threads for stdout, stderr, process.waitFor(), http
    executorService = new ScheduledThreadPoolExecutor(1);
    completionService = new ExecutorCompletionService<>(executorService);
  }

  private static Consumer<String> composite(Consumer<String> matcher, Consumer<String> logger) {
    return matcher == null
        ? logger
        : (line) -> {
          matcher.accept(line);
          logger.accept(line);
        };
  }

  public void createProcess(
      List<String> command,
      String stdin,
      Consumer<String> stdout,
      Consumer<String> stderr,
      boolean cancelStreams)
      throws MojoExecutionException {

    try {

      infoLine.accept(String.join(" ", command));

      Process process = new ProcessBuilder(command).start();
      startPump(process.getInputStream(), stdout, cancelStreams);
      startPump(process.getErrorStream(), stderr, cancelStreams);

      OutputStream os = process.getOutputStream();
      if (stdin != null) {
        os.write(stdin.getBytes(StandardCharsets.UTF_8));
      }
      os.close();

      cancellableTasks.add(completionService.submit(process::waitFor));
    } catch (IOException ex) {
      throw new MojoExecutionException(ex);
    }
  }

  private void startPump(InputStream process, Consumer<String> std, boolean cancelStreams) {
    Future<Void> err = executorService.submit(() -> pumpLog(process, std));
    if (cancelStreams) {
      cancellableTasks.add(err);
    }
  }

  private Void pumpLog(InputStream is, Consumer<String> lineConsumer) throws IOException {
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

  public void startAndWait(
      Supplier<Cmd> logCmd, WaitConfig waitConfig, BufferedWriter writer, Properties props)
      throws MojoExecutionException {

    try {
      Consumer<String> matcher = null;

      if (waitConfig != null) {
        String match = waitConfig.log;
        if (match != null) {
          matcher = logMatcher(match);
        }

        HttpWaitConfig httpWaitConfig = waitConfig.http;
        if (httpWaitConfig != null) {
          httpMatcher(httpWaitConfig, props);
        }
      }

      startSpooler(logCmd.get(), writer, matcher);

      if (waitConfig != null) {
        waitForStartup(waitConfig.time);
      }
    } catch (InterpolationException | InterruptedException | ExecutionException ex) {
      throw new MojoExecutionException(ex);
    }
  }

  private Consumer<String> logMatcher(String match) {
    CompletableFuture<String> logDone = new CompletableFuture<>();
    cancellableTasks.add(completionService.submit(logDone::get));
    return line -> {
      if (line == null || line.contains(match)) {
        logDone.complete(match);
      }
    };
  }

  private void startSpooler(Cmd command, BufferedWriter writer, Consumer<String> matcher)
      throws MojoExecutionException {

    boolean streamToFile = writer != null;
    if (streamToFile) {
      matcher = composite(matcher, createConsumer(writer));
    }
    createProcess(command.getCommand(), null, matcher, matcher, !streamToFile);
  }

  private void waitForStartup(int duration)
      throws InterruptedException, MojoExecutionException, ExecutionException {

    Future<Object> poll = completionService.poll(duration, TimeUnit.SECONDS);
    stopTasks();
    if (poll == null) {
      throw new MojoExecutionException(prompt + ": timed out waiting for startup");
    }
    Object result = poll.get();
    if (result instanceof Integer) {
      // process exited
      if (0 != (Integer) result) {
        throw new MojoExecutionException(prompt + ": exited " + result);
      }
    } else {
      infoLine.accept(result.toString());
    }
  }

  private Consumer<String> createConsumer(BufferedWriter writer) {
    return (String line) -> {
      try {
        debugLine.accept(line);
        writer.append(line);
        writer.newLine();
        writer.flush();
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    };
  }

  private void httpMatcher(HttpWaitConfig httpWaitConfig, Properties projectProperties)
      throws InterpolationException {
    CompletableFuture<String> httpDone = new CompletableFuture<>();
    cancellableTasks.add(completionService.submit(httpDone::get));

    HttpClient client =
        HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    URI uri = URI.create(createInterpolator(projectProperties).interpolate(httpWaitConfig.url));
    String method = httpWaitConfig.method != null ? httpWaitConfig.method : "GET";
    int status = httpWaitConfig.status != 0 ? httpWaitConfig.status : 200;
    int interval = httpWaitConfig.interval != 0 ? httpWaitConfig.interval : 15;

    Callable<String> getHealth =
        new Callable<>() {
          @Override
          public String call() throws Exception {
            HttpRequest request =
                HttpRequest.newBuilder()
                    .uri(uri)
                    .header("Accept", "*/*")
                    .method(method, HttpRequest.BodyPublishers.noBody())
                    .build();
            try {
              HttpResponse<byte[]> resp =
                  client.send(request, HttpResponse.BodyHandlers.ofByteArray());
              if (resp.statusCode() == status) {
                httpDone.complete(method + ' ' + uri + " => " + resp.statusCode());
              }
            } catch (IOException ex) {
              debugLine.accept(method + ' ' + uri + " => " + ex.getMessage());
            }
            cancellableTasks.add(executorService.schedule(this, interval, TimeUnit.SECONDS));
            return null;
          }
        };
    cancellableTasks.add(executorService.submit(getHealth));
  }

  private Interpolator createInterpolator(Properties projectProperties) {
    StringSearchInterpolator interpolator = new StringSearchInterpolator();
    interpolator.setEscapeString("\\");
    interpolator.addValueSource(new PropertiesBasedValueSource(projectProperties));
    return interpolator;
  }

  public void waitNoError() throws MojoExecutionException {
    int exitCode = waitForResult();
    if (exitCode != 0) {
      throw new MojoExecutionException("command exited with error - " + exitCode);
    }
  }

  public int waitForResult() throws MojoExecutionException {
    try {
      Future<Object> poll = completionService.poll(30, TimeUnit.SECONDS);
      stopTasks();
      if (poll == null) {
        throw new MojoExecutionException("timed out");
      }
      return (Integer) poll.get();
    } catch (InterruptedException | ExecutionException ex) {
      throw new MojoExecutionException(ex);
    }
  }

  private void stopTasks() {
    cancellableTasks.forEach(future -> future.cancel(true));
  }

  public void createAndWait(List<String> command, String stdin) throws MojoExecutionException {
    createProcess(command, stdin, infoLine, errorLine, false);
    waitNoError();
  }

  public String yieldString(List<String> command) throws MojoExecutionException {
    StringBuilder sb = new StringBuilder();
    Consumer<String> info = (l) -> sb.append(l).append('\n');

    createProcess(command, null, info, errorLine, false);
    waitNoError();

    return sb.toString();
  }

  public int yieldInt(List<String> command) throws MojoExecutionException {
    createProcess(command, null, infoLine, errorLine, false);
    return waitForResult();
  }
}
