package org.honton.chas.podman.maven.plugin;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CommandLineGenerator {
  private final List<String> command;

  public CommandLineGenerator(PodmanGoal behavior) {
    command = new ArrayList<>();
    command.add("podman");
    behavior.addSubCommand(command);
    RemoteInfo remoteInfo = behavior.remote;
    if (remoteInfo != null) {
      String url = remoteInfo.url;
      if (url != null) {
        command.add("--url");
        command.add(url);
      }
      String connection = remoteInfo.connection;
      if (connection != null) {
        command.add("--connection");
        command.add(connection);
      }
    }
  }

  public void addArgs(Map<String, String> buildArguments) {
    if (buildArguments != null) {
      buildArguments.forEach(
          (k, v) -> {
            command.add("--build-arg");
            command.add(k + "=" + v);
          });
    }
  }

  /**
   * Add the platform option
   *
   * @param platforms the os/arch of the resulting image(s)
   * @return true if multi-platform
   */
  public boolean addPlatforms(List<String> platforms) {
    if (platforms != null) {
      Set<String> set = new LinkedHashSet<>();
      platforms.forEach(p -> Arrays.stream(p.split(",")).map(String::strip).forEach(set::add));
      if (!set.isEmpty()) {
        command.add("--platform");
        command.add(String.join(",", set));
        return set.size() > 1;
      }
    }
    return false;
  }

  public void addManifest(String name) {
    command.add("--manifest");
    command.add(name);
  }

  public void addTag(String name) {
    command.add("--tag");
    command.add(name);
  }

  public void addContainerFile(Path containerFile) {
    command.add("--file");
    command.add(containerFile.toString());
  }

  public void addContext(Path containerFile) {
    command.add(containerFile.toString());
  }

  public List<String> getCommand() {
    return command;
  }
}
