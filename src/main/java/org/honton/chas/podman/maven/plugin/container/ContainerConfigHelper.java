package org.honton.chas.podman.maven.plugin.container;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import lombok.ToString;
import lombok.experimental.UtilityClass;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.honton.chas.podman.maven.plugin.config.ContainerConfig;

@UtilityClass
class ContainerConfigHelper {

  public List<ContainerConfig> order(Map<String, ContainerConfig> containers, Log log)
      throws MojoExecutionException {
    if (containers == null) {
      return List.of();
    }

    AtomicBoolean failed = new AtomicBoolean();
    Map<String, RequirementsNode> aliasToNode = new HashMap<>();
    containers.forEach(
        (alias, containerConfig) -> {
          if (containerConfig == null) {
            log.error("container " + alias + " is misconfigured");
            failed.set(true);
          } else {
            containerConfig.alias = alias;
            if (containerConfig.name == null) {
              containerConfig.name = alias;
            }
            aliasToNode.put(alias, new RequirementsNode(alias, containerConfig));
          }
        });
    if (failed.get()) {
      throw new MojoExecutionException("containers mis-configured");
    }

    // fill out dependents
    aliasToNode.forEach(
        (alias, node) ->
            node.requires.forEach(
                require -> {
                  RequirementsNode requirements = aliasToNode.get(require);
                  if (requirements == null) {
                    throw new IllegalArgumentException(
                        "Missing definition for require " + require + " on release " + alias);
                  }
                  requirements.addDependent(require, node);
                }));

    List<ContainerConfig> releaseOrder = new ArrayList<>();
    while (!aliasToNode.isEmpty()) {
      List<RequirementsNode> solved =
          aliasToNode.values().stream()
              .filter(RequirementsNode::isSolved)
              .collect(Collectors.toList());

      if (log.isDebugEnabled()) {
        solved.forEach(node -> log.debug("Solved: " + node));
      }

      if (solved.isEmpty()) {
        throw new IllegalArgumentException(
            "Could not determine container ordering for: "
                + String.join(", ", aliasToNode.keySet()));
      }

      solved.forEach(
          requirement -> {
            releaseOrder.add(requirement.containerConfig);
            aliasToNode.remove(requirement.alias);
            requirement.removeRequiresFromDependents();
          });
    }
    return releaseOrder;
  }

  @ToString(of = {"alias", "requires"})
  private static class RequirementsNode {
    final String alias;
    final ContainerConfig containerConfig;

    /** The containers that this container requires before deployment */
    final Set<String> requires;

    /** The containers that depend upon this container */
    final Map<String, RequirementsNode> depends;

    public RequirementsNode(String alias, ContainerConfig containerConfig) {
      this.alias = alias;
      this.containerConfig = containerConfig;
      this.requires = asSet(containerConfig.requires);
      depends = new HashMap<>();
    }

    private static Set<String> asSet(String commaSeparated) {
      return commaSeparated == null
          ? Set.of()
          : Arrays.stream(commaSeparated.split(",")).map(String::strip).collect(Collectors.toSet());
    }

    public boolean isSolved() {
      return requires.isEmpty();
    }

    void removeRequiresFromDependents() {
      depends.values().forEach(dependent -> dependent.removeRequirement(alias));
    }

    private void removeRequirement(String name) {
      requires.remove(name);
    }

    public void addDependent(String name, RequirementsNode value) {
      depends.put(name, value);
    }
  }
}
