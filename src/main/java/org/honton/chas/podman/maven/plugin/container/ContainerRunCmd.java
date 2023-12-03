package org.honton.chas.podman.maven.plugin.container;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import lombok.Getter;
import org.apache.maven.plugin.MojoExecutionException;
import org.honton.chas.podman.maven.plugin.config.BindMountConfig;
import org.honton.chas.podman.maven.plugin.config.ContainerConfig;
import org.honton.chas.podman.maven.plugin.config.DeviceMountConfig;
import org.honton.chas.podman.maven.plugin.config.TempFsMountConfig;
import org.honton.chas.podman.maven.plugin.config.VolumeMountConfig;

class ContainerRunCmd extends ContainerEnvCmd {

  private final PodmanContainerRun goal;

  @Getter private final Map<Integer, String> portToPropertyName = new HashMap<>();

  ContainerRunCmd(
      PodmanContainerRun goal,
      ContainerConfig containerConfig,
      Consumer<String> warn,
      String networkName)
      throws MojoExecutionException, IOException {
    super(goal, containerConfig, warn);
    this.goal = goal;
    addParameter("--detach");
    addContainerName(containerConfig);
    addContainerOptions(containerConfig, networkName);
    addMounts(containerConfig);
    addPorts(containerConfig);
    addContainerImage(containerConfig);
    addContainerCmd(containerConfig);
  }

  static Set<PosixFilePermission> posixFilePermissions(String permissions) {
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

  @Override
  String subCommand() {
    return "run";
  }

  private void addContainerName(ContainerConfig containerConfig) {
    addParameter("--name");
    addParameter(containerConfig.name);
    if (!containerConfig.alias.equals(containerConfig.name)) {
      addParameter("--network-alias");
      addParameter(containerConfig.alias);
    }
  }

  private void addContainerImage(ContainerConfig containerConfig) throws MojoExecutionException {
    if (containerConfig.image == null) {
      throw new MojoExecutionException("Missing image for container " + containerConfig.alias);
    }
    addParameter(containerConfig.image);
  }

  private void addContainerOptions(ContainerConfig containerConfig, String network) {
    addParameter("--network").addParameter(network);

    if (containerConfig.memory != null) {
      addParameter("--memory").addParameter(containerConfig.memory);
    }
    if (containerConfig.memorySwap != null) {
      addParameter("--memory-swap").addParameter(containerConfig.memorySwap);
    }

    if (containerConfig.workDir != null) {
      addParameter("-w").addParameter(containerConfig.workDir);
    }

    if (containerConfig.entrypoint != null) {
      addParameter("--entrypoint").addParameter(containerConfig.entrypoint);
    }
  }

  private void addDevices(List<DeviceMountConfig> devices) {
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
  }

  private void addMounts(ContainerConfig containerConfig) throws IOException {
    if (containerConfig.mounts != null) {
      addBinds(containerConfig.mounts.binds);
      addTemps(containerConfig.mounts.temps);
      addVolumes(containerConfig.mounts.volumes);
      addDevices(containerConfig.mounts.devices);
    }
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

  private void addPorts(ContainerConfig containerConfig) {
    if (containerConfig.ports != null) {
      containerConfig.ports.forEach(this::addPort);
    }
  }

  private void addPort(String mavenPropertyName, Integer containerPort) {
    String hostPortAndInterface = goal.lookupProperty(mavenPropertyName);
    if (hostPortAndInterface == null) {
      hostPortAndInterface = "";
      goal.setProperty(mavenPropertyName, hostPortAndInterface);
      portToPropertyName.put(containerPort, mavenPropertyName);
    }
    addParameter("--publish").addParameter(hostPortAndInterface + ':' + containerPort);
  }
}
