package org.honton.chas.podman.maven.plugin.containerfile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.honton.chas.podman.maven.plugin.PodmanContainerfile;
import org.honton.chas.podman.maven.plugin.config.LayerConfig;
import org.honton.chas.podman.maven.plugin.config.ShellOrExecConfig;

/** Create a Containerfile. */
@Mojo(name = "containerfile", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, threadSafe = true)
public class CreateContainerfile extends PodmanContainerfile {

  /** Base image for subsequent instructions */
  @Parameter(required = true)
  String from;

  /** Default command / parameters for the executing container */
  @Parameter ShellOrExecConfig cmd;

  /** Command that the container executes */
  @Parameter ShellOrExecConfig entrypoint;

  /** File layers to copy from the context into the image file system */
  @Parameter List<LayerConfig> layers;

  /** Label to be applied to image */
  @Parameter Map<String, String> labels;

  /** Map of environment variables that are set when container runs */
  @Parameter Map<String, String> env;

  /** The User[:Group] that runs inside the container. May be uid or name. */
  @Parameter String user;

  /** List of ports that the container will expose */
  @Parameter List<String> expose;

  /** List of locations in the image filesystem where external mounts are expected */
  @Parameter List<String> volumes;

  /** Working directory for the container's process */
  @Parameter String workdir;

  @Override
  protected void doExecute() throws IOException {
    StringBuilder sb = new StringBuilder();
    if (from != null) {
      sb.append("FROM ").append(from).append('\n');
    }

    if (layers != null) {
      layers.forEach(l -> writeLayer(sb, l));
    }

    if (entrypoint != null) {
      writeShellOrExec(sb, "ENTRYPOINT ", entrypoint);
    }

    if (cmd != null) {
      writeShellOrExec(sb, "CMD ", cmd);
    }

    if (labels != null) {
      labels.forEach(
          (k, v) -> sb.append("LABEL ").append(k).append("=\"").append(v).append("\"\n"));
    }

    if (env != null) {
      env.forEach((k, v) -> sb.append("ENV ").append(k).append('=').append(v).append('\n'));
    }

    if (expose != null) {
      expose.forEach(p -> sb.append("EXPOSE ").append(p).append('\n'));
    }

    if (volumes != null) {
      volumes.forEach(p -> sb.append("VOLUME ").append(p).append('\n'));
    }

    if (user != null) {
      sb.append("USER ").append(user).append('\n');
    }

    if (workdir != null) {
      sb.append("WORKDIR ").append(workdir).append('\n');
    }

    Path contextPath = contextDir.toPath();
    Files.createDirectories(contextPath);
    Files.writeString(contextPath.resolve(containerFile()), sb, StandardCharsets.UTF_8);
  }

  private void writeShellOrExec(StringBuilder sb, String directive, ShellOrExecConfig shellOrExec) {
    sb.append(directive);
    if (shellOrExec.exec != null) {
      sb.append('[');
      shellOrExec.exec.forEach(e -> sb.append('"').append(e).append("\","));
      sb.setCharAt(sb.length() - 1, ']');
    } else if (shellOrExec.shell != null) {
      sb.append(shellOrExec.shell);
    }
    sb.append('\n');
  }

  private void writeLayer(StringBuilder sb, LayerConfig layer) {
    sb.append("COPY ");
    if (layer.chmod != null) {
      sb.append("--chmod=").append(layer.chmod).append(' ');
    }
    if (layer.chown != null) {
      sb.append("--chown=").append(layer.chown).append(' ');
    }

    layer.srcs.forEach(s -> sb.append('"').append(s).append("\","));
    sb.setCharAt(sb.length() - 1, ' ');
    sb.append('"').append(layer.dest).append("\"\n");
  }
}
