package org.honton.chas.podman.maven.plugin.container;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import lombok.SneakyThrows;
import org.apache.maven.plugin.MojoExecutionException;
import org.honton.chas.podman.maven.plugin.cmdline.CommandLine;
import org.honton.chas.podman.maven.plugin.config.BindMountConfig;
import org.honton.chas.podman.maven.plugin.config.ContainerConfig;
import org.honton.chas.podman.maven.plugin.config.DeviceMountConfig;
import org.honton.chas.podman.maven.plugin.config.TempFsMountConfig;
import org.honton.chas.podman.maven.plugin.config.VolumeMountConfig;

class ContainerRunCommandLine extends CommandLine {
  private final ContainerConfig containerConfig;
  private final PodmanContainerRun goal;

  ContainerRunCommandLine(PodmanContainerRun goal, ContainerConfig containerConfig) {
    super(goal);
    addCmd("container");
    addCmd("run");
    addParameter("--detach");
    this.containerConfig = containerConfig;
    this.goal = goal;
  }

  public static Set<PosixFilePermission> posixFilePermissions(String permissions) {
    int mode = Integer.parseInt(permissions, 8);
    Set<PosixFilePermission> perms = EnumSet.noneOf(PosixFilePermission.class);
    for (int i = PosixFilePermission.values().length - 1; mode != 0; --i) {
      if ((mode & 1) != 0) {
        perms.add(PosixFilePermission.values()[i]);
      }
      mode >>>= 1;
    }
    return perms;
  }

  ContainerRunCommandLine addContainerName() {
    addParameter("--name");
    addParameter(containerConfig.name);
    if (!containerConfig.alias.equalsIgnoreCase(containerConfig.name)) {
      addParameter("--network-alias");
      addParameter(containerConfig.alias);
    }
    return this;
  }

  ContainerRunCommandLine addContainerCmd() throws MojoExecutionException {
    if (containerConfig.image == null) {
      throw new MojoExecutionException("Missing image for container " + containerConfig.alias);
    }
    addParameter(containerConfig.image);

    if (containerConfig.cmd != null) {
      addParameter(containerConfig.cmd);
    }
    if (containerConfig.args != null) {
      containerConfig.args.forEach(this::addParameter);
    }
    return this;
  }

  ContainerRunCommandLine addContainerOptions(String network) {
    addParameter("--network").addParameter(network);

    if (containerConfig.memory != null) {
      addParameter("--memory").addParameter(containerConfig.memory);
    }
    if (containerConfig.memorySwap != null) {
      addParameter("--memory-swap").addParameter(containerConfig.memorySwap);
    }

    if (containerConfig.entrypoint != null) {
      addParameter("--entrypoint").addParameter(containerConfig.entrypoint);
    }
    return this;
  }

  ContainerRunCommandLine addEnvironment(Consumer<String> warn) {
    if (containerConfig.envFile != null) {
      if (Files.isReadable(Path.of(containerConfig.envFile))) {
        addParameter("--env-file").addParameter(containerConfig.envFile);
      } else {
        warn.accept("ignoring env file " + containerConfig.envFile);
      }
    }

    if (containerConfig.env != null) {
      containerConfig.env.forEach((k, v) -> addParameter("--env").addParameter(k + '=' + v));
    }
    return this;
  }

  private static String getMountOptions(DeviceMountConfig config) {
    StringBuilder options = new StringBuilder();
    if (config.read != Boolean.FALSE) {
      options.append("r");
    }
    if (config.write != Boolean.FALSE) {
      options.append("w");
    }
    if (config.mknod != Boolean.FALSE) {
      options.append("m");
    }
    return options.toString();
  }

  public ContainerRunCommandLine addDevices(List<DeviceMountConfig> devices) {
    if (devices != null) {
      for (DeviceMountConfig config : devices) {
        StringBuilder sb = new StringBuilder().append(config.source);

        if (config.destination != null && !config.destination.equals(config.source)) {
          sb.append(':').append(config.destination);
        }

        String ops = getMountOptions(config);
        boolean needOpts = !ops.equals("rwm");
        if (needOpts) {
          sb.append(':').append(ops);
        }

        addParameter("--device").addParameter(sb.toString());
      }
    }
    return this;
  }

  ContainerRunCommandLine addMounts() throws IOException {
    if (containerConfig.mounts != null) {
      addBinds(containerConfig.mounts.binds);
      addTemps(containerConfig.mounts.temps);
      addVolumes(containerConfig.mounts.volumes);
    }
    return this;
  }

  private void addTemps(List<TempFsMountConfig> temps) {
    if (temps != null) {
      temps.forEach(this::addTempFs);
    }
  }

  private void addTempFs(TempFsMountConfig tempFsMount) {
    addParameter("--tempfs").addParameter(tempFsMount.destination);
  }

  private void addBinds(List<BindMountConfig> binds) throws IOException {
    if (binds != null) {
      for (BindMountConfig bindMount : binds) {

        Path path = Path.of(bindMount.source);
        if (Files.notExists(path)) {
          createDirs(path, getFileAttributes(bindMount.permissions));
        }

        StringBuilder sb =
            new StringBuilder().append(bindMount.source).append(':').append(bindMount.destination);
        if (bindMount.readonly) {
          sb.append(":ro");
        }
        addParameter("--volume").addParameter(sb.toString());
      }
    }
  }

  private static void createDirs(Path path, Set<PosixFilePermission> fileAttributes)
      throws IOException {
    Path parent = path.getParent();
    if (Files.notExists(parent)) {
      createDirs(parent, fileAttributes);
    }
    Files.createDirectory(path);
    if (!fileAttributes.isEmpty()) {
      Files.setPosixFilePermissions(path, fileAttributes);
    }
  }

  private Set<PosixFilePermission> getFileAttributes(String permissions) {
    if (permissions == null) {
      return Set.of();
    }
    if (permissions.length() == 9) {
      return PosixFilePermissions.fromString(permissions);
    }
    return posixFilePermissions(permissions);
  }

  private void addVolumes(List<VolumeMountConfig> volumes) {
    if (volumes != null) {
      for (VolumeMountConfig volumeMount : volumes) {
        StringBuilder sb =
            new StringBuilder()
                .append(volumeMount.source)
                .append(':')
                .append(volumeMount.destination);
        if (volumeMount.readonly) {
          sb.append(":ro");
        }
        addParameter("--volume").addParameter(sb.toString());
      }
    }
  }

  ContainerRunCommandLine addPorts() {
    if (containerConfig.ports != null) {
      containerConfig.ports.forEach(this::addPort);
    }
    return this;
  }

  private void addPort(String mavenPropertyName, Integer containerPort) {
    String hostPortAndInterface = goal.lookupProperty(mavenPropertyName);
    if (hostPortAndInterface == null) {
      hostPortAndInterface = allocatePort();
      goal.setProperty(mavenPropertyName, hostPortAndInterface);
    }
    addParameter("--publish").addParameter(hostPortAndInterface + ':' + containerPort);
  }

  @SneakyThrows
  private String allocatePort() {
    try (ServerSocket serverSocket = new ServerSocket(0)) {
      return Integer.toString(serverSocket.getLocalPort());
    }
  }
}
