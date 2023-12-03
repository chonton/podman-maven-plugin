package org.honton.chas.podman.maven.plugin.container;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.Interpolator;
import org.codehaus.plexus.interpolation.PropertiesBasedValueSource;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;
import org.honton.chas.podman.maven.plugin.cmdline.CommandLine;
import org.honton.chas.podman.maven.plugin.config.ExecConfig;
import org.honton.chas.podman.maven.plugin.config.HttpWaitConfig;
import org.honton.chas.podman.maven.plugin.config.IdentityConfig;
import org.honton.chas.podman.maven.plugin.config.LogConfig;
import org.honton.chas.podman.maven.plugin.config.WaitConfig;

public class ExecConfigHelper<C extends IdentityConfig> {

  private final PodmanContainer<C> goal;
  private final ScheduledExecutorService executorService;
  private final ExecutorCompletionService<Object> completionService;

  public ExecConfigHelper(PodmanContainer<C> goal) {
    this.goal = goal;
    executorService = goal.executorService();
    completionService = goal.completionService();
  }

  private static Consumer<String> composite(Consumer<String> matcher, Consumer<String> logger) {
    return matcher == null
        ? logger
        : (line) -> {
          matcher.accept(line);
          logger.accept(line);
        };
  }

  public void startAndWait(Function<LogConfig, CommandLine> logCommand, ExecConfig containerConfig)
      throws IOException,
          InterpolationException,
          MojoExecutionException,
          ExecutionException,
          InterruptedException {

    Consumer<String> matcher = null;

    WaitConfig waitConfig = containerConfig.wait;
    if (waitConfig != null) {
      String match = waitConfig.log;
      if (match != null) {
        matcher = logMatcher(match);
      }

      HttpWaitConfig httpWaitConfig = waitConfig.http;
      if (httpWaitConfig != null) {
        httpMatcher(httpWaitConfig);
      }
    }

    startLogSpooler(logCommand, containerConfig, matcher);

    if (waitConfig != null) {
      waitForStartup(containerConfig.alias, waitConfig.time);
    }
  }

  private Consumer<String> logMatcher(String match) {
    CompletableFuture<String> logDone = new CompletableFuture<>();
    completionService.submit(logDone::get);
    return line -> {
      if (line == null || line.contains(match)) {
        logDone.complete(match);
      }
    };
  }

  private void startLogSpooler(
      Function<LogConfig, CommandLine> logCmd, ExecConfig config, Consumer<String> matcher)
      throws IOException {

    Consumer<String> stdOut;
    Consumer<String> stdErr;

    LogConfig logConfig = config.log;
    if (logConfig != null) {
      stdOut = stdErr = composite(matcher, createLogConsumer(config.alias, logConfig.file));
    } else {
      stdOut = composite(matcher, goal::infoLine);
      stdErr = composite(matcher, goal::errorLine);
    }

    CommandLine command = logCmd.apply(logConfig);
    goal.createProcess(completionService, command.getCommand(), null, stdOut, stdErr);
  }

  private void waitForStartup(String alias, int duration)
      throws InterruptedException, MojoExecutionException, ExecutionException {

    long endTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(duration);

    for (; ; ) {
      long delay = endTime - System.currentTimeMillis();
      Future<Object> poll = completionService.poll(delay, TimeUnit.MILLISECONDS);
      if (poll == null) {
        throw new MojoExecutionException(alias + ": timed out waiting for startup");
      }
      Object result = poll.get();
      if (result instanceof Integer) {
        // process exited
        if (0 != (Integer) result) {
          throw new MojoExecutionException(alias + ": exited " + result);
        }
        break;
      } else if (result instanceof String) {
        goal.getLog().info(alias + ": " + result);
        break;
      }
      // null from poll.get() would indicate that stdout or stderr was closed
    }
  }

  private Consumer<String> createLogConsumer(String alias, String logFile) throws IOException {
    Path path = logFile != null ? Path.of(logFile) : defaultLogFile(alias);
    Files.createDirectories(path.getParent());
    BufferedWriter writer =
        Files.newBufferedWriter(
            path, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
    return createConsumer(writer, alias);
  }

  private Path defaultLogFile(String alias) {
    return goal.relativePath(
        goal.project.getBasedir().toPath().resolve(Path.of("target", "podman", alias + ".log")));
  }

  private Consumer<String> createConsumer(BufferedWriter writer, String alias) {
    return (String line) -> {
      try {
        Log log = goal.getLog();
        if (log.isDebugEnabled()) {
          log.debug(alias + ": " + line);
        }
        writer.append(line);
        writer.newLine();
        writer.flush();
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    };
  }

  private void httpMatcher(HttpWaitConfig waitConfig) throws InterpolationException {
    CompletableFuture<String> httpDone = new CompletableFuture<>();
    completionService.submit(httpDone::get);

    HttpClient client =
        HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    URI uri = URI.create(createInterpolator().interpolate(waitConfig.url));
    String method = waitConfig.method != null ? waitConfig.method : "GET";
    int status = waitConfig.status != 0 ? waitConfig.status : 200;
    int interval = waitConfig.interval != 0 ? waitConfig.interval : 15;

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
              goal.getLog().debug(method + ' ' + uri + " => " + ex.getMessage());
            }
            executorService.schedule(this, interval, TimeUnit.SECONDS);
            return null;
          }
        };
    executorService.submit(getHealth);
  }

  private Interpolator createInterpolator() {
    StringSearchInterpolator interpolator = new StringSearchInterpolator();
    interpolator.setEscapeString("\\");
    interpolator.addValueSource(new PropertiesBasedValueSource(goal.project.getProperties()));
    return interpolator;
  }
}
