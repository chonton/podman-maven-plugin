package org.honton.chas.podman.maven.plugin.cmdline;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.honton.chas.podman.maven.plugin.build.PodmanBuild;

public class BuildCommandLine extends CommandLine {
  public BuildCommandLine(PodmanBuild goal) {
    super(goal);
    addCmd("build");
  }

  public BuildCommandLine addArgs(Map<String, String> buildArguments) {
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

  public BuildCommandLine addPlatformAndImage(List<String> platforms, String image) {
    if (addPlatforms(platforms)) {
      command.add("--manifest");
    } else {
      command.add("--tag");
    }
    command.add(image);
    return this;
  }

  public BuildCommandLine addContainerfile(String containerfile) {
    if (!"Containerfile".equals(containerfile)) {
      command.add("--file");
      command.add(containerfile);
    }
    return this;
  }

  public BuildCommandLine addContext(Path contextDir) {
    command.add("./" + contextDir.toString());
    return this;
  }
}
