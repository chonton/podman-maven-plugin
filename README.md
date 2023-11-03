# podman-maven-plugin

Use podman to build, push, and run images. This plugin has eight goals:

1. [Login to registry](#login-goal)
2. [Create Containerfile from base image and copy directives](#containerfile-goal)
3. [Build image from Containerfile and context](#build-goal)
4. [Push image to registry](#push-goal)
5. [Create a volume](#volume-create-goal)
6. [Import volume contents](#volume-import-goal)
7. [Run image in container](#container-run-goal)
8. [Remove container](#container-rm-goal)

# Rationale

Build images using [podman](https://docs.podman.io/en/latest/markdown/podman-build.1.html) with this
plugin's [containerfile](#containerfile-goal) and [build](#build-goal) goals.

During the **pre-integration-test** phase, deploy your containers using this plugin's
[container-run](#container-run-goal) goal. Use
[failsafe](https://maven.apache.org/surefire/maven-failsafe-plugin/) to run integration tests.
During the **post-integration-test** phase, undeploy your containers using this plugin's
[container-rm](#container-rm-goal) goal.

## Remote Podman

This plugin supports
[remote podman](https://docs.podman.io/en/v4.4/markdown/podman.1.html#remote-access) using
`connection` or `url` parameters. When using remote podman, bind mounts will not work. You will need
to create a volume, import its contents, and run the container with a volume mount. Port mappings
will map the container port to the remote podman host port.

# Plugin Reports

Plugin reports available at
[plugin info](https://chonton.github.io/podman-maven-plugin/plugin-info.html).

## Login Goal

The [login](https://chonton.github.io/podman-maven-plugin/login-mojo.html) goal binds by default to
the **prepare-package** phase. This goal executes `podman login` with credentials from
**settings.xml** or specified in the configuration.

### Login Configuration from settings.xml

Maven's [settings.xml](https://maven.apache.org/settings.html) contains items that are not specific
to a project or that should not be distributed to artifact consumers. This plugin will read the
[servers](https://maven.apache.org/settings.html#servers) element of settings.xml to find a server
element with an `<id>` element that matches the registry. If found, the `<username>` and
`<password>` from that server element will be used. (The password is
[decrypted](https://maven.apache.org/guides/mini/guide-encryption.html) if needed.)

### Login Configuration from pom.xml

|  Parameter | Required | Description                                            |
|-----------:|:--------:|:-------------------------------------------------------|
| connection |          | Remote podman connection name                          |
|   registry |    ✓     | Registry to authenticate with                          |
|   password |          | If registry not found in settings.xml, use as password |
|       skip |          | Skip login                                             |
|   username |          | If registry not found in settings.xml, use as username |
|        url |          | Url of podman remote service                           |

## Containerfile Goal

The [containerfile](https://chonton.github.io/podman-maven-plugin/containerfile-mojo.html) goal
binds by default to the **prepare-package** phase.
This goal creates *${project.build.directory}/contextDir/Containerfile* from the configuration.
See [Containerfile](https://github.com/containers/common/blob/main/docs/Containerfile.5.md) for more
information about Containerfile syntax.

### Containerfile Configuration

|  Parameter | Required | Description                                                      |
|-----------:|:--------:|:-----------------------------------------------------------------|
|        cmd |          | Default [ShellOrExec Config](#shellorexec-config) command        |
| entrypoint |          | Default [ShellOrExec Config](#shellorexec-config) entrypoint     |
|       from |    ✓     | Base image for subsequent instructions                           |
|     layers |          | List of [Layer Config](#layer-config) to apply                   |
|     labels |          | Map of labels to apply to image                                  |
|        env |          | Map of environment variables that are set when container runs    |
|       user |          | User\[:Group] that runs inside the container. May be uid or name |
|     expose |          | List of ports that the container will expose                     |
|    volumes |          | List of locations in the image filesystem of external mounts     |
|    workDir |          | Working directory for the container's process                    |

### Layer Config

| Parameter | Required | Description                                                               |
|----------:|:--------:|:--------------------------------------------------------------------------|
|     chown |          | Owner\[:Group] of the files in the image                                  |
|     chmod |          | Permissions of the files in the image                                     |
|      srcs |    ✓     | Files relative to the context to be copied.  (golang wildcards supported) |
|      dest |    ✓     | Absolute destination in the image where files are copied                  |

### ShellOrExec Config

| Parameter |  Required  | Description                                                      |
|----------:|:----------:|:-----------------------------------------------------------------|
|      exec | ✓ or shell | List of Executable and parameters, no shell involved             |
|     shell |  ✓ or cmd  | Single line command executed by shell (unused if exec specified) |

## Build Goal

The [build](https://chonton.github.io/podman-maven-plugin/build-mojo.html) goal binds by default to
the **package** phase. This goal executes `podman build` with the proper parameters.

### Build Configuration

|       Parameter | Required | Description                                                                      |
|----------------:|:--------:|:---------------------------------------------------------------------------------|
|  buildArguments |          | Map of build arguments                                                           |
|      connection |          | Remote podman connection name                                                    |
|   containerfile |    ✓     | Instruction file relative to contextDir, default is *Containerfile*              |
|      contextDir |    ✓     | Directory with build content, default is *${project.build.directory}/contextDir* |
|           image |    ✓     | Fully qualified image name; must include *registry/repository:version*           |
| loadDockerCache |          | If set to true, load the local docker image cache with resulting image           |
|       platforms |          | List of platforms.  Each element may contain comma separated *os/arch*           |
|            skip |          | Skip build                                                                       |
|             url |          | Url of podman remote service                                                     |

## Push Goal

The [push](https://chonton.github.io/podman-maven-plugin/push-mojo.html) goal binds by default to
the **deploy** phase. This goal uses `podman push` to push an image to its registry.

### Push Configuration

|  Parameter | Required | Description                                                      |
|-----------:|:--------:|:-----------------------------------------------------------------|
| connection |          | Remote podman connection name                                    |
|      image |    ✓     | The fully qualified image name, with registry/repository:version |
|       skip |          | Skip push                                                        |
|        url |          | Url of podman remote service                                     |

## Volume-Create Goal

The [volume-create](https://chonton.github.io/podman-maven-plugin/volume-create-mojo.html) goal
binds by default to the **prepare-package** phase. This goal uses `podman volume create` to create a
volume.

### Volume-Create Configuration

|  Parameter | Required | Description                   |
|-----------:|:--------:|:------------------------------|
| connection |          | Remote podman connection name |
|     volume |    ✓     | The volume name               |

## Volume-Import Goal

The [volume-import](https://chonton.github.io/podman-maven-plugin/volume-import-mojo.html) goal
binds by default to the **package** phase. This goal uses `podman volume import` to copy contents
into a volume.

### Volume-Create Configuration

|  Parameter | Required | Description                                            |
|-----------:|:--------:|:-------------------------------------------------------|
| connection |          | Remote podman connection name                          |
|     volume |    ✓     | The volume name                                        |
|        src |    ✓     | The content to copy.  Either a tar file or a directory |

## Container-Run Goal

The [container-run](https://chonton.github.io/podman-maven-plugin/container-run-mojo.html) goal
binds by default to the **pre-integration-test** phase. This goal uses `podman network create` to
create a network, `podman container run` to launch containers, and `podman logs` to capture the
containers` output.

After launching, the goal will wait for the container to become healthy based upon the `wait`
configuration. When launching multiple containers, the container ordering is determined by the
`requires` parameter. The `logs` configuration instructs the goal to collect container logs until
the container is removed or maven exits.

### Container-Run Configuration

|  Parameter | Required | Description                                                       |
|-----------:|:--------:|:------------------------------------------------------------------|
| connection |          | Remote podman connection name                                     |
| containers |    ✓     | Map of container aliases to [Container Config](#container-config) |
|    devices |          | List of [Device Config](#device-config)                           |
|    network |          | [Network Config](#network-config)                                 |
|       skip |          | Skip container-run                                                |
|        url |          | Url of podman remote service                                      |

#### Device Config

|   Parameter | Required | Description                                               |
|------------:|:--------:|:----------------------------------------------------------|
|      source |    ✓     | Absolute path of host device                              |
| destination |          | Absolute path of container device. Defaults to host path. |
|      mknode |          | Container allowed to mknod. Defaults to true              |
|        read |          | Container allowed to read. Defaults to true               |
|       write |          | Container allowed to write. Defaults to true              |

### Network Config

| Parameter | Required | Description                                  |
|----------:|:--------:|:---------------------------------------------|
|      name |          | Name of network.  Defaults to `artifactId`   |
|    driver |          | Network driver name.  Defaults to **bridge** |

### Container Config

|  Parameter | Required | Description                                                            |
|-----------:|:--------:|:-----------------------------------------------------------------------|
|       name |          | Name of the container. Defaults to container's alias                   |
|   requires |          | Comma separated dependent container names                              |
|      image |    ✓     | Fully qualified image name to run                                      |
|       wait |          | Post launch [Wait Config](#wait-config)                                |
|        log |          | Post launch [Log Config](#log-config)                                  |
|     memory |          | Memory limit.                                                          |
|     memory |          | Memory plus swap limit.                                                |
|        cmd |          | Override image command to execute                                      |
|       args |          | Override image arguments for command                                   |
| entrypoint |          | Override image entrypoint                                              |
|    envFile |          | File containing environment variables that are set when container runs |
|        env |          | Map of environment variables that are set when container runs          |
|     mounts |          | [Mount Config](#mount-config)                                          |
|      ports |          | Map of host ports to container ports. See [Ports Map](#ports-map)      |

#### Memory Limits

Memory limit must be a number followed by unit of 'b' (bytes), 'k' (kibibytes), 'm' (mebibytes), or
'g' (gibibytes).

#### Mount Config

| Parameter | Required | Description                                       |
|----------:|:--------:|:--------------------------------------------------|
|     binds |          | List of [BindMount Config](#bindmount-config)     |
|     temps |          | List of [TempFsMount Config](#tempfsmount-config) |
|   volumes |          | List of [VolumeMount Config](#volumemount-config) |

#### BindMount Config

|   Parameter | Required | Description                                |
|------------:|:--------:|:-------------------------------------------|
|      source |    ✓     | Absolute path of host directory            |
| destination |    ✓     | Absolute path of container directory       |
|    readonly |          | Defaults to false                          |
| permissions |          | Permissions of directories created on host |

##### Host Directory

Bind mounts will create the directory on the host if it does not exist. The file system permissions
may be defined using
[Symbolic](https://en.wikipedia.org/wiki/File-system_permissions#Symbolic_notation) or
[Numeric](https://en.wikipedia.org/wiki/File-system_permissions#Numeric_notation) notation.

#### TempFsMount Config

|   Parameter | Required | Description                          |
|------------:|:--------:|:-------------------------------------|
| destination |    ✓     | Absolute path of container directory |

#### VolumeMount Config

|   Parameter | Required | Description                          |
|------------:|:--------:|:-------------------------------------|
|      source |    ✓     | Volume name                          |
| destination |    ✓     | Absolute path of container directory |
|    readonly |          | Defaults to false                    |

#### Ports Map

Key is the name of a maven property. If property is set, then that value is used as the host tcp
\[interface:]port; otherwise the property is set to the value of the dynamically assigned host tcp
port. Each entry is the value of an exposed container tcp port.

#### Log Config

|  Parameter | Required | Description                                                                     |
|-----------:|:--------:|:--------------------------------------------------------------------------------|
|       file |          | Name of file to receive logs. Defaults to `target/podman/<container alias>.log` |
| timestamps |          | Display timestamp on each line. Defaults to false.                              |

#### Wait Config

| Parameter | Required | Description                                    |
|----------:|:--------:|:-----------------------------------------------|
|      http |          | [HttpWait Config](#httpwait-config)            |
|       log |          | String to detect in container log.             |
|      time |          | Seconds to wait before failing. Default is 60. |

#### HttpWait Config

| Parameter | Required | Description                                        |
|----------:|:--------:|:---------------------------------------------------|
|       url |    ✓     | Url to invoke                                      |
|    method |          | Http verb to use. Default is 'GET'.                |
|    status |          | Expected status code. Default is 200.              |
|  interval |          | Interval in seconds between probes. Default is 15. |

## Container-Rm Goal

The [container-rm](https://chonton.github.io/podman-maven-plugin/container-rm-mojo.html) goal binds
by default to the **post-integration-test** phase. This goal uses `podman container rm` to delete
containers and `podman network rm` to delete the network. The order of container deletion is the
reverse of the start order.

### Container-Rm Configuration

|  Parameter | Required | Description                                                              |
|-----------:|:--------:|:-------------------------------------------------------------------------|
| connection |          | Remote podman connection name                                            |
| containers |    ✓     | Map of container aliases to [Container Config](#container-configuration) |
|    network |          | [Network Config](#network-configuration)                                 |
|       skip |          | Skip container-rm                                                        |
|        url |          | Url of podman remote service                                             |

### Container Configuration

| Parameter | Required | Description                                          |
|----------:|:--------:|:-----------------------------------------------------|
|      name |          | Name of the container. Defaults to container's alias |

### Network Configuration

| Parameter | Required | Description                                |
|----------:|:--------:|:-------------------------------------------|
|      name |          | Name of network.  Defaults to `artifactId` |

# Examples

## Typical Use

```xml

<build>
  <properties>
    <!-- override with -D build.platform=linux/amd64,linux/arm64 for multi-architecture build -->
    <build.platform></build.platform>
  </properties>

  <pluginManagement>
    <plugins>
      <plugin>
        <groupId>org.honton.chas</groupId>
        <artifactId>podman-maven-plugin</artifactId>
        <version>0.0.5</version>
      </plugin>
    </plugins>
  </pluginManagement>

  <plugins>
    <plugin>
      <groupId>org.honton.chas</groupId>
      <artifactId>podman-maven-plugin</artifactId>
      <executions>

        <execution>
          <id>build-java-based-container</id>
          <goals>
            <goal>containerfile</goal>
            <goal>build</goal>
            <goal>login</goal>
            <goal>push</goal>
          </goals>
          <configuration>
            <buildArguments combine.children="append">
              <alpineBase>docker.io/library/alpine:3.18.0</alpineBase>
            </buildArguments>
            <contextDir>target/quarkus-app</contextDir>
            <containerfile>src/image/Dockerfile</containerfile>
            <image>artifactory.xmple.com/dev/${project.artifactId}:${project.version}</image>
            <platforms>
              <platform>${build.platforms}</platform>
            </platforms>
          </configuration>
        </execution>

        <execution>
          <id>integration-test</id>
          <goals>
            <goal>container-run</goal>
            <goal>container-rm</goal>
          </goals>
          <configuration>
            <containers>
              <uat>
                <image>artifactory.xmple.com/dev/${project.artifactId}:${project.version}</image>
                <requires>s3</requires>
                <log/>
                <wait>
                  <http>
                    <url>http://localhost:9000/q/health/ready</url>
                  </http>
                </wait>
                <mounts>
                  <binds>
                    <bind>
                      <source>${project.build.testOutputDirectory}/service/data</source>
                      <destination>/service/data</destination>
                      <permissions>777</permissions>
                    </bind>
                  </binds>
                </mounts>
                <ports>
                  <management.port>9000</management.port>
                  <service.port>8080</service.port>
                </ports>
              </uat>
              <s3>
                <image>docker.io/adobe/s3mock:2.11.0</image>
                <ports>
                  <s3.port>9090</s3.port>
                </ports>
                <wait>
                  <log>Jetty started on port</log>
                  <time>20</time>
                </wait>
                <log/>
                <env>
                  <initialBuckets>exmple-cmpny-us-east-1</initialBuckets>
                </env>
              </s3>
            </containers>
          </configuration>
        </execution>
      </executions>

      <configuration>
        <!-- set this value elsewhere, maybe jenkins starts a server process -->
        <url>${podman.url}</url>
      </configuration>
    </plugin>
  </plugins>
</build>
```
