package org.honton.chas.podman.maven.plugin.volume;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.tar.TarArchiver;
import org.codehaus.plexus.archiver.util.DefaultFileSet;
import org.honton.chas.podman.maven.plugin.PodmanGoal;
import org.honton.chas.podman.maven.plugin.cmdline.CommandLine;

/**
 * Copy content to a volume
 *
 * @since 0.0.6
 */
@Mojo(name = "volume-import", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true)
public class PodmanImportVolume extends PodmanGoal {

  /** Volume name */
  @Parameter(required = true)
  String volume;

  /** Contents to transfer to volume. This may be a tar file or a directory */
  @Parameter(required = true)
  File src;

  @Parameter(defaultValue = "${project}", required = true, readonly = true)
  MavenProject project;

  @Override
  protected final void doExecute()
      throws IOException, MojoExecutionException, ExecutionException, InterruptedException {
    File tar;
    if (src.isDirectory()) {
      tar =
          project.getBasedir().toPath().resolve(Path.of("target", src.getName() + ".tar")).toFile();
      tarArchive(src, tar);
    } else {
      tar = src;
    }

    executeCommand(
        new CommandLine(this)
            .addCmd("volume")
            .addParameter("import")
            .addParameter(volume)
            .addParameter(tar.getAbsolutePath())
            .getCommand());
  }

  private void tarArchive(File src, File dst) throws IOException {
    DefaultFileSet fileSet = DefaultFileSet.fileSet(src);
    fileSet.setIncludes(new String[] {project.getArtifactId() + "/**/*.*"});

    TarArchiver archiver = new TarArchiver();
    archiver.addFileSet(fileSet);
    archiver.setDestFile(dst);
    archiver.createArchive();
  }
}
