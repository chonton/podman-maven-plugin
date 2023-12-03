package org.honton.chas.podman.maven.plugin.container;

import java.io.IOException;
import java.util.List;
import lombok.SneakyThrows;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.honton.chas.podman.maven.plugin.config.ExecConfig;

/**
 * Exec in containers
 *
 * @since 0.0.5
 */
@Mojo(name = "container-exec", defaultPhase = LifecyclePhase.INTEGRATION_TEST, threadSafe = true)
public class PodmanContainerExec extends PodmanContainer<ExecConfig> {

  @Override
  protected void doExecute(List<ExecConfig> containerConfigs, String networkName)
      throws IOException, MojoExecutionException {
    containerConfigs.forEach(this::runContainer);
  }

  @SneakyThrows
  private void runContainer(ExecConfig containerConfig) {
    ContainerExecCmd execCommandLine =
        new ContainerExecCmd(this, containerConfig, getLog()::warn, containerId(containerConfig));

    new ExecConfigHelper<>(this).startAndWait(logConfig -> execCommandLine, containerConfig);
  }
}
