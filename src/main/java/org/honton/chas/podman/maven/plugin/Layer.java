package org.honton.chas.podman.maven.plugin;

import java.util.List;
import org.apache.maven.plugins.annotations.Parameter;

/** Layer of files in the image */
public class Layer {
  /** Owner:Group of the files in the image */
  String chown;

  /** Permissions of the files in the image */
  String chmod;

  /** Files relative to the context to be copied. (golang wildcards supported) */
  @Parameter(required = true)
  List<String> srcs;

  /** Absolute destination in the image where files are copied */
  @Parameter(required = true)
  String dest;
}
