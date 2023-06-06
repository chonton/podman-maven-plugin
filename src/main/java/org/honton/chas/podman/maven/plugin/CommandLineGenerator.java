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

  public CommandLineGenerator(PodmanGoal goal) {
    command = new ArrayList<>();
    command.add("podman");

    if (goal.url != null) {
      command.add("--url");
      command.add(goal.url);
    }
    if (goal.connection != null) {
      command.add("--connection");
      command.add(goal.connection);
    }
  }

  public CommandLineGenerator addCmd(String cmd) {
    command.add(cmd);
    return this;
  }

  public CommandLineGenerator addParameter(String parameter) {
    command.add(parameter);
    return this;
  }

  public CommandLineGenerator addArgs(Map<String, String> buildArguments) {
    if (buildArguments != null) {
      buildArguments.forEach(
          (k, v) -> {
            command.add("--build-arg");
            command.add(k + "=" + v);
          });
    }
    return this;
  }

  /**
   * Add the platform option
   *
   * @param platforms the os/arch of the resulting image(s)
   * @return true if multi-platform
   */
  private boolean addPlatforms(List<String> platforms) {
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

  public CommandLineGenerator addPlatformAndImage(List<String> platforms, String image) {
    if (addPlatforms(platforms)) {
      command.add("--manifest");
    } else {
      command.add("--tag");
    }
    command.add(image);
    return this;
  }

  public CommandLineGenerator addContainerfile(String containerfile) {
    if (!"Containerfile".equals(containerfile)) {
      command.add("--file");
      command.add(containerfile);
    }
    return this;
  }

  public CommandLineGenerator addContext(Path contextDir) {
    command.add("./" + contextDir.toString());
    return this;
  }

  public List<String> getCommand() {
    return command;
  }
}
