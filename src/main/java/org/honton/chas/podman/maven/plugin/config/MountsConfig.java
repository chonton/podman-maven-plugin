package org.honton.chas.podman.maven.plugin.config;

import java.util.List;
import lombok.ToString;
import org.apache.maven.plugins.annotations.Parameter;

/** Mounts configuration */
@ToString
public class MountsConfig {
  /** List of bindings from host to container */
  @Parameter public List<BindMountConfig> binds;

  /** List of Temporary file systems */
  @Parameter public List<TempFsMountConfig> temps;

  /** List of volume mappings. */
  @Parameter public List<VolumeMountConfig> volumes;
}
