package org.honton.chas.podman.maven.plugin.login;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.honton.chas.podman.maven.plugin.PodmanGoal;
import org.honton.chas.podman.maven.plugin.cmdline.Cmd;
import org.sonatype.plexus.components.sec.dispatcher.DefaultSecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcherException;

/**
 * Set credentials for use when pushing to (or pulling from) registry.
 *
 * @since 0.0.3
 */
@Mojo(name = "login", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, threadSafe = true)
public class PodmanLogin extends PodmanGoal {

  /** Registry location; used to look up the credentials in settings.xml and locate the registry */
  @Parameter(required = true)
  String registry;

  /** The username for the registry if no entry in settings.xml matches registry name */
  @Parameter String username;

  /** The password for the registry if no entry in settings.xml matches registry name */
  @Parameter String password;

  @Component(role = SecDispatcher.class, hint = "default")
  private SecDispatcher securityDispatcher;

  @Parameter(defaultValue = "${settings}", required = true, readonly = true)
  private Settings settings;

  @Override
  protected final void doExecute()
      throws IOException, MojoExecutionException, ExecutionException, InterruptedException {
    Server server = getAuthInfo();
    Cmd command =
        new Cmd(this, "login")
            .addCmd("login")
            .addParameter("--username")
            .addParameter(server.getUsername())
            .addParameter("--password-stdin")
            .addParameter(registry);
    executeCommand(command, server.getPassword());
  }

  private Server getAuthInfo() throws MojoExecutionException {
    Server server = settings.getServer(registry);
    if (server == null) {
      getLog().info("No credentials for " + registry + " in settings.xml");
      server = new Server();
    }
    ensure(server::getUsername, server::setUsername, username);

    if (ensure(server::getPassword, server::setPassword, password)) {
      if (securityDispatcher instanceof DefaultSecDispatcher) {
        ((DefaultSecDispatcher) securityDispatcher)
            .setConfigurationFile("~/.m2/settings-security.xml");
      }
      try {
        server.setPassword(securityDispatcher.decrypt(server.getPassword()));
      } catch (SecDispatcherException e) {
        throw new MojoExecutionException("unable to decrypt password for " + registry, e);
      }
    }

    return server;
  }

  /**
   * ensure pojo value is set
   *
   * @param getter Supplier of pojo value
   * @param setter Pojo consumer of value
   * @param fallback value to set if pojo value is null
   * @return true if pojo was non-null value
   * @throws MojoExecutionException when pojo value and fallback both null
   */
  private <T> boolean ensure(Supplier<T> getter, Consumer<T> setter, T fallback)
      throws MojoExecutionException {
    if (getter.get() != null) {
      return true;
    }
    if (fallback == null) {
      throw new MojoExecutionException("Missing username or password for " + registry);
    }
    setter.accept(fallback);
    return false;
  }
}
