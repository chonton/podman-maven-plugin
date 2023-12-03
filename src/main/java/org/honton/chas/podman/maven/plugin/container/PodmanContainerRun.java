package org.honton.chas.podman.maven.plugin.container;

import com.fasterxml.jackson.jr.ob.JSON;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.SneakyThrows;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.honton.chas.podman.maven.plugin.cmdline.Cmd;
import org.honton.chas.podman.maven.plugin.config.ContainerConfig;
import org.honton.chas.podman.maven.plugin.config.LogConfig;

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
    NetworkCmd cmdLine = new NetworkCreateCmd(this, network, networkName);

    int exitCode = execYieldInt(cmdLine);
    if (exitCode != 0 && !errorOutput.toString().contains("network already exists")) {
      throw new MojoExecutionException("podman exit value: " + exitCode);
    }
  }

  @SneakyThrows
  private void runContainer(ContainerConfig config, String networkName) {
    ContainerRunCmd runCommandLine = new ContainerRunCmd(this, config, getLog()::warn, networkName);

    String containerId = execYieldString(runCommandLine);
    setProperty(containerIdPropertyName(config), containerId);

    Map<Integer, String> portToPropertyName = runCommandLine.getPortToPropertyName();
    if (!portToPropertyName.isEmpty()) {
      setAssignedPorts(containerId, portToPropertyName);
    }

    LogConfig logConfig = config.log;
    BufferedWriter bufferedWriter = createBufferedWriter(logConfig, config.alias);
    new ExecHelper(this, config.alias)
        .startAndWait(
            () -> new LogsCmd(this, logConfig, containerId),
            config.wait,
            bufferedWriter,
            project.getProperties());
  }

  private void setAssignedPorts(String containerId, Map<Integer, String> portToPropertyName)
      throws MojoExecutionException, IOException {
    String inspect =
        execYieldString(
            new Cmd(this, containerId)
                .addCmd("container")
                .addCmd("inspect")
                .addParameter(containerId)
                .addParameter("--format")
                .addParameter("{{.HostConfig}}"));

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
