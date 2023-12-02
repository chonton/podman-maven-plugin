package org.honton.chas.podman.maven.plugin.container;

import java.io.IOException;
import java.util.List;
import java.util.ListIterator;
import lombok.SneakyThrows;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.honton.chas.podman.maven.plugin.config.ContainerConfig;

/**
 * Remove containers
 *
 * @since 0.0.4
 */
@Mojo(name = "container-rm", defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST, threadSafe = true)
public class PodmanContainerRm extends PodmanContainer {

  @Override
  protected void doExecute(List<ContainerConfig> containerConfigs, String networkName)
      throws IOException, MojoExecutionException {
    ListIterator<ContainerConfig> li = containerConfigs.listIterator(containerConfigs.size());
    while (li.hasPrevious()) {
      rmContainer(li.previous());
    }
    rmNetwork(networkName);
  }

  @SneakyThrows
  private void rmNetwork(String networkName) {
    NetworkCommandLine cmdLine = new NetworkCommandLine(this);
    cmdLine.addCmd("rm");
    cmdLine.addParameter(networkName);
    executeCommand(cmdLine);
  }

  @SneakyThrows
  private void rmContainer(ContainerConfig containerConfig) {
    executeCommand(new ContainerRmCommandLine(this, containerConfig));
  }
}
