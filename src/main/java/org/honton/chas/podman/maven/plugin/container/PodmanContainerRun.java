package org.honton.chas.podman.maven.plugin.container;

import com.fasterxml.jackson.jr.ob.JSON;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import lombok.Data;
import lombok.SneakyThrows;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.honton.chas.podman.maven.plugin.cmdline.CommandLine;
import org.honton.chas.podman.maven.plugin.config.ContainerConfig;

/**
 * Create containers
 *
 * @since 0.0.4
 */
@Mojo(name = "container-run", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST, threadSafe = true)
public class PodmanContainerRun extends PodmanContainer<ContainerConfig> {

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

    int exitCode = execYieldInt(cmdLine.getCommand());
    if (exitCode != 0 && !errorOutput.toString().contains("network already exists")) {
      throw new MojoExecutionException("podman exit value: " + exitCode);
    }
  }

  @SneakyThrows
  private void runContainer(ContainerConfig containerConfig, String networkName) {
    ContainerRunCommandLine runCommandLine =
        new ContainerRunCommandLine(this, containerConfig)
            .addContainerName()
            .addContainerOptions(networkName)
            .addMounts()
            .addPorts()
            .addEnvironment(getLog()::warn)
            .addContainerImage()
            .addContainerCmd();

    String containerId = execYieldString(runCommandLine.getCommand());
    setProperty(containerIdPropertyName(containerConfig), containerId);

    Map<Integer, String> portToPropertyName = runCommandLine.getPortToPropertyName();
    if (!portToPropertyName.isEmpty()) {
      setAssignedPorts(containerId, portToPropertyName);
    }

    new ExecConfigHelper<>(this)
        .startAndWait(
            logConfig -> new LogsCommandLine(this, logConfig, containerId), containerConfig);
  }

  private void setAssignedPorts(String containerId, Map<Integer, String> portToPropertyName)
      throws MojoExecutionException, IOException, ExecutionException, InterruptedException {
    String inspect =
        execYieldString(
            new CommandLine(this)
                .addCmd("container")
                .addCmd("inspect")
                .addParameter(containerId)
                .addParameter("--format")
                .addParameter("{{.HostConfig}}")
                .getCommand());

    Map<String, List<PortBinding>> portToBindings =
        JSON.std.beanFrom(HostConfig.class, inspect).PortBindings;

    portToPropertyName.forEach(
        (port, name) -> {
          List<PortBinding> portBindings = portToBindings.get(port + "/tcp");
          if (portBindings != null && !portBindings.isEmpty()) {
            // tbd - what to do with HostIp? why would there be multiple bindings? probably for
            // multi-homed hosts?
            setProperty(name, portBindings.get(0).HostPort);
          } else {
            getLog().warn(name + " not set; container port " + port + " has no mapping");
          }
        });
  }

  public String lookupProperty(String mavenPropertyName) {
    return project.getProperties().getProperty(mavenPropertyName);
  }

  @Data
  static class HostConfig {
    Map<String, List<PortBinding>> PortBindings;
  }

  @Data
  static class PortBinding {
    String HostIp;
    String HostPort;
  }
}
