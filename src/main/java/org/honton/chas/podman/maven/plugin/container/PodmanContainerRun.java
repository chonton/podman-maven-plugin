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
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import lombok.SneakyThrows;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.Interpolator;
import org.codehaus.plexus.interpolation.PropertiesBasedValueSource;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;
import org.honton.chas.podman.maven.plugin.config.ContainerConfig;
import org.honton.chas.podman.maven.plugin.config.HttpWaitConfig;
import org.honton.chas.podman.maven.plugin.config.LogConfig;
import org.honton.chas.podman.maven.plugin.config.WaitConfig;

/**
 * Create containers
 *
 * @since 0.0.4
 */
@Mojo(name = "container-run", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST, threadSafe = true)
public class PodmanContainerRun extends PodmanContainer {

  private BiConsumer<Integer, String> createConsumer(
      BufferedWriter writer, String alias, Consumer<String> matcher) {
    return (Integer n, String line) -> {
      try {
        Log log = getLog();
        if (log.isDebugEnabled()) {
          log.debug(alias + '(' + n + "): " + line);
        }
        matcher.accept(line);
        writer.append(line);
        writer.newLine();
        writer.flush();
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    };
  }

  @Override
  protected void doExecute(List<ContainerConfig> containerConfigs, String networkName)
      throws IOException, MojoExecutionException {
    createNetwork(networkName);
    containerConfigs.forEach(containerConfig -> runContainer(containerConfig, networkName));
  }

  @SneakyThrows
  private void createNetwork(String networkName) {
    NetworkCommandLine cmdLine = new NetworkCommandLine(this);
    cmdLine.addCmd("create");
    if (network != null && network.driver != null) {
      cmdLine.addParameter("--driver").addParameter(network.driver);
    }
    cmdLine.addParameter(networkName);
    executeCommand(cmdLine, this::checkExists);
  }

  @SneakyThrows
  void checkExists(int exitCode) {
    if (exitCode != 0 && !errorOutput.toString().contains("network already exists")) {
      throw new MojoExecutionException("podman exit value: " + exitCode);
    }
  }

  @SneakyThrows
  private void runContainer(ContainerConfig containerConfig, String networkName) {
    executeCommand(
        new ContainerRunCommandLine(this, containerConfig)
            .addContainerName()
            .addContainerOptions(networkName)
            .addEnvironment(getLog()::warn)
            .addDevices(devices)
            .addMounts()
            .addPorts()
            .addContainerCmd());

    CountDownLatch logFragmentWait = startLogSpooler(containerConfig);
    waitForStartup(containerConfig.wait, logFragmentWait);
  }

  public String lookupProperty(String mavenPropertyName) {
    return project.getProperties().getProperty(mavenPropertyName);
  }

  public void setProperty(String mavenPropertyName, String mavenPropertyValue) {
    getLog().info("Setting " + mavenPropertyName + " to " + mavenPropertyValue);
    project.getProperties().setProperty(mavenPropertyName, mavenPropertyValue);
  }

  private CountDownLatch startLogSpooler(ContainerConfig containerConfig)
      throws IOException, MojoExecutionException {

    WaitConfig waitConfig = containerConfig.wait;
    boolean logWait = waitConfig != null && waitConfig.log != null;
    CountDownLatch logDone = logWait ? new CountDownLatch(1) : null;
    Consumer<String> matcher =
        logWait
            ? line -> {
              if (line.contains(waitConfig.log)) {
                logDone.countDown();
              }
            }
            : line -> {};

    LogConfig logConfig = containerConfig.log;
    if (logWait && logConfig == null) {
      // force log collection if wait condition includes log fragment
      logConfig = new LogConfig();
    }
    if (logConfig != null) {
      Path path =
          logConfig.file != null
              ? Path.of(logConfig.file)
              : pwd.relativize(
                  project
                      .getBasedir()
                      .toPath()
                      .resolve(Path.of("target", "podman", containerConfig.alias + ".log")));
      Files.createDirectories(path.getParent());

      createProcess(
          new LogsCommandLine(this, logConfig, containerConfig.name),
          createConsumer(
              Files.newBufferedWriter(
                  path,
                  StandardCharsets.UTF_8,
                  StandardOpenOption.CREATE,
                  StandardOpenOption.WRITE),
              containerConfig.alias,
              matcher));
    }

    return logDone;
  }

  private void waitForStartup(WaitConfig waitConfig, CountDownLatch logFragmentWait)
      throws MojoExecutionException, IOException, InterpolationException {
    if (waitConfig != null) {
      int time = waitConfig.time != 0 ? waitConfig.time : 60;
      long startTime = System.currentTimeMillis();
      long endTime = startTime + TimeUnit.SECONDS.toMillis(time);
      getLog().debug("start:" + startTime + ", end: " + endTime);
      try {
        waitForLog(waitConfig, logFragmentWait, endTime);
        waitForHttp(waitConfig.http, endTime);
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
        throw new MojoExecutionException("interrupted", ie);
      }
    }
  }

  private void waitForLog(WaitConfig waitConfig, CountDownLatch logFragmentWait, long endTime)
      throws InterruptedException, MojoExecutionException {
    if (logFragmentWait != null
        && !logFragmentWait.await(endTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS)) {
      throw new MojoExecutionException("Did not see '" + waitConfig.log + "' in log");
    }
  }

  private void waitForHttp(HttpWaitConfig waitConfig, long endTime)
      throws InterruptedException, IOException, InterpolationException {
    if (waitConfig != null) {
      HttpClient client =
          HttpClient.newBuilder()
              .version(HttpClient.Version.HTTP_1_1)
              .connectTimeout(Duration.ofSeconds(10))
              .build();

      URI uri = URI.create(createInterpolator().interpolate(waitConfig.url));
      String method = waitConfig.method != null ? waitConfig.method : "GET";
      int status = waitConfig.status != 0 ? waitConfig.status : 200;
      int interval = waitConfig.interval != 0 ? waitConfig.interval : 15;

      for (; ; ) {
        HttpRequest request =
            HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofMillis(endTime - System.currentTimeMillis()))
                .header("Accept", "*/*")
                .method(method, HttpRequest.BodyPublishers.noBody())
                .build();
        try {
          HttpResponse<byte[]> resp = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
          if (resp.statusCode() == status) {
            getLog().debug(method + ' ' + uri + " => " + resp.statusCode());
            return;
          }
        } catch (IOException ex) {
          getLog().debug(method + ' ' + uri + " => " + ex.getMessage());
        }
        Thread.sleep(TimeUnit.SECONDS.toMillis(interval));
      }
    }
  }

  private Interpolator createInterpolator() {
    StringSearchInterpolator interpolator = new StringSearchInterpolator();
    interpolator.setEscapeString("\\");
    interpolator.addValueSource(new PropertiesBasedValueSource(project.getProperties()));
    return interpolator;
  }
}
