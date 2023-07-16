# podman-maven-plugin

Use podman to build and push images. This plugin has six goals:

1. Create Containerfile from base image and copy directives
2. Build image from Containerfile and context
3. Login to registry
4. Push image to registry
5. Run image in container
6. Remove container

# Rationale

Build container images with [podman](https://docs.podman.io/en/latest/markdown/podman-build.1.html).

Deploy your containers to [docker desktop](https://docs.docker.com/desktop/kubernetes/) or
[minikube](https://minikube.sigs.k8s.io/docs/) for integration testing using
[helmrepo](https://github.com/chonton/helmrepo-maven-plugin). Or, use this plugin's `run` goal to start loose container.
Start k8s pods/deployments/services or podman containers during **pre-integration-test** phase. Use
[failsafe](https://maven.apache.org/surefire/maven-failsafe-plugin/) to run integration tests during
**integration-test** phase. Capture logs and uninstall k8s pods/deployments/services or container during the
**post-integration-test** phase.

# Plugin

Plugin reports available at
[plugin info](https://chonton.github.io/podman-maven-plugin/plugin-info.html).

## Containerfile Goal

The [containerfile](https://chonton.github.io/podman-maven-plugin/containerfile-mojo.html) goal
binds by default to the **prepare-package** phase.
This goal creates *${project.build.directory}/contextDir/Containerfile* from the configuration.
See [Containerfile](https://github.com/containers/common/blob/main/docs/Containerfile.5.md) for more
information about Containerfile syntax.

### Configuration

|  Parameter | Required | Description                                                                  |
|-----------:|:--------:|:-----------------------------------------------------------------------------|
|        cmd |          | Default command / parameters for the executing container                     |
| entrypoint |          | Command that the container executes                                          |
|       from |    ✓     | Base image for subsequent instructions                                       |
|     layers |          | List of files to copy from the context into the image file system            |
|     labels |          | Map of labels to apply to image                                              |
|        env |          | Map of environment variables that are set when container runs                |
|       user |          | User\[:Group] that runs inside the container. May be uid or name             |
|     expose |          | List of ports that the container will expose                                 |
|    volumes |          | List of locations in the image filesystem where external mounts are expected |
|    workDir |          | Working directory for the container's process                                |

### layer attributes

| Parameter | Required | Description                                                               |
|----------:|:--------:|:--------------------------------------------------------------------------|
|     chown |          | Owner\[:Group] of the files in the image                                  |
|     chmod |          | Permissions of the files in the image                                     |
|      srcs |    ✓     | Files relative to the context to be copied.  (golang wildcards supported) |
|      dest |    ✓     | Absolute destination in the image where files are copied                  |

### entrypoint or cmd attributes

| Parameter |  Required  | Description                                                      |
|----------:|:----------:|:-----------------------------------------------------------------|
|      exec | ✓ or shell | List of Executable and parameters, no shell involved             |
|     shell |  ✓ or cmd  | Single line command executed by shell (unused if exec specified) |

## Build Goal

The [build](https://chonton.github.io/podman-maven-plugin/build-mojo.html) goal binds by default to
the **package** phase. This goal executes `podman build` with the proper parameters.

### Configuration

|       Parameter | Required | Description                                                                      |
|----------------:|:--------:|:---------------------------------------------------------------------------------|
|  buildArguments |          | Map of build arguments                                                           |
|      connection |          | Remote podman connection name                                                    |
|   containerfile |    ✓     | Instruction file relative to contextDir, default is *Containerfile*)             |
|      contextDir |    ✓     | Directory with build content, default is *${project.build.directory}/contextDir* |
|           image |    ✓     | Fully qualified image name; must include registry/repository:version             |
| loadDockerCache |          | If set to true, load the local docker image cache with resulting image           |
|       platforms |          | List of platforms.  Each element may contain comma separated *os/arch*           |
|            skip |          | Skip the build                                                                   |
|             url |          | Url of podman remote service                                                     |

## Login Goal

The [login](https://chonton.github.io/podman-maven-plugin/login-mojo.html) goal binds by default to
the **prepare-package** phase. This goal executes `podman login` with credentials from
**settings.xml** or specified in the configuration.

### Configuration from settings.xml

Maven's [settings.xml](https://maven.apache.org/settings.html) contains items that are not specific
to a project or that should not be distributed to artifact consumers. This plugin will read the
[servers](https://maven.apache.org/settings.html#servers) element of settings.xml to find a server
element with an `<id>` element that matches the registry. If found, the `<username>` and
`<password>` from that server element will be used. (The password is
[decrypted](https://maven.apache.org/guides/mini/guide-encryption.html) if needed.)

### Configuration from pom.xml

|  Parameter | Required | Description                                            |
|-----------:|:--------:|:-------------------------------------------------------|
| connection |          | Remote podman connection name                          |
|   registry |    ✓     | Registry to authenticate with                          |
|   password |          | If registry not found in settings.xml, use as password |
|       skip |          | Skip login                                             |
|   username |          | If registry not found in settings.xml, use as username |
|        url |          | Url of podman remote service                           |

## Push Goal

The [push](https://chonton.github.io/podman-maven-plugin/push-mojo.html) goal binds by default to
the **deploy** phase. This goal uses `podman` to `push` an image to its registry.

### Configuration

|  Parameter | Required | Description                                                      |
|-----------:|:--------:|:-----------------------------------------------------------------|
| connection |          | Remote podman connection name                                    |
|      image |    ✓     | The fully qualified image name, with registry/repository:version |
|       skip |          | Skip push                                                        |
|        url |          | Url of podman remote service                                     |

## Container-Run Goal

The [container-run](https://chonton.github.io/podman-maven-plugin/container-run-mojo.html) goal binds by default to
the **pre-integration-test** phase. This goal uses `podman network rm` to create a network, `podman container run`
to launch containers, and `podman logs` to capture the containers` output.

After launching, the goal will wait for the container to become healthy based upon the `wait` configuration. When
launching multiple containers, the container ordering is determined by the `requires` parameter.  The `logs`
configuration instructs the goal to collect container logs until the container is removed or maven exits.

### Configuration

|  Parameter | Required | Description                                         |
|-----------:|:--------:|:----------------------------------------------------|
| connection |          | Remote podman connection name                       |
| containers |    ✓     | Map of container aliases to container configuration |
|    network |          | Network configuration                               |
|       skip |          | Skip container run                                  |
|        url |          | Url of podman remote service                        |

### Network Configuration

| Parameter | Required | Description                                  |
|----------:|:--------:|:---------------------------------------------|
|      name |          | Name of network.  Defaults to `artifactId`   |
|    driver |          | Network driver name.  Defaults to **bridge** |

### Container Configuration

|  Parameter | Required | Description                                                            |
|-----------:|:--------:|:-----------------------------------------------------------------------|
|       name |          | Name of the container. Defaults to container's alias                   |
|   requires |          | Comma separated dependent container names                              |
|      image |    ✓     | Fully qualified image name to run                                      |
|       wait |          | Post launch wait configuration                                         |
|        log |          | Post launch logging configuration                                      |
|     memory |          | Memory limit.                                                          |
|     memory |          | Memory plus swap limit.                                                |
|        cmd |          | Override image command to execute                                      |
|       args |          | Override image arguments for command                                   |
| entrypoint |          | Override image entrypoint                                              |
|    envFile |          | File containing environment variables that are set when container runs |
|        env |          | Map of environment variables that are set when container runs          |
|     mounts |          | Volume mappings                                                        |
|      ports |          | Map of host ports to container ports.                                  |

#### Memory Limits
Memory limit must be a number followed by unit of 'b' (bytes), 'k' (kibibytes), 'm' (mebibytes), or 'g' (gibibytes)

#### Mounts Configuration

| Parameter | Required | Description         |
|----------:|:--------:|:--------------------|
|     binds |          | List of BindMount   |
|     temps |          | List of TempFs      |
|   volumes |          | List of VolumeMount |

#### BindMount Configuration

|   Parameter | Required | Description                                |
|------------:|:--------:|:-------------------------------------------|
|      source |    ✓     | Absolute path of host directory            |
| destination |    ✓     | Absolute path of container directory       |
|    readonly |          | Defaults to false                          |
| permissions |          | Permissions of directories created on host |

##### Host Directory

Bind mounts will create the directory on the host if it does not exist.  The file system permissions may be defined 
using [Symbolic](https://en.wikipedia.org/wiki/File-system_permissions#Symbolic_notation) or 
[Numeric](https://en.wikipedia.org/wiki/File-system_permissions#Numeric_notation) notation.

#### TempFs Configuration

|   Parameter | Required | Description                                |
|------------:|:--------:|:-------------------------------------------|
| destination |    ✓     | Absolute path of container directory       |

#### BindMount Configuration

|   Parameter | Required | Description                                |
|------------:|:--------:|:-------------------------------------------|
|      source |    ✓     | Volume name                                |
| destination |    ✓     | Absolute path of container directory       |
|    readonly |          | Defaults to false                          |

#### Ports Map
Key is the name of a maven property. If property is set, then that value is used as host the \[interface:]port;
otherwise the property is set to the value of the dynamically assigned host port. Each entry is the value of an exposed
container port.

#### Log Configuration

|  Parameter | Required | Description                                                                     |
|-----------:|:--------:|:--------------------------------------------------------------------------------|
|       file |          | Name of file to receive logs. Defaults to `target/podman/<container alias>.log` |
| timestamps |          | Display timestamp on each line. Defaults to false.                              |

#### Wait Configuration

| Parameter | Required | Description                                    |
|----------:|:--------:|:-----------------------------------------------|
|      http |          | Http probe options                             |
|       log |          | String to detect in container log.             |
|      time |          | Seconds to wait before failing. Default is 60. |

#### HttpWait Configuration

| Parameter | Required | Description                                        |
|----------:|:--------:|:---------------------------------------------------|
|       url |    ✓     | Url to invoke                                      |
|    method |          | Http verb to use. Default is 'GET'.                |
|    status |          | Expected status code. Default is 200.              |
|  interval |          | Interval in seconds between probes. Default is 15. |

## Container-Rm Goal

The [container-rm](https://chonton.github.io/podman-maven-plugin/container-rm-mojo.html) goal binds by default to the
**post-integration-test** phase. This goal uses `podman container run` to delete containers and `podman network rm` to 
delete the network.  The order of container deletion is reverse of the start order.

### Configuration

|  Parameter | Required | Description                                         |
|-----------:|:--------:|:----------------------------------------------------|
| connection |          | Remote podman connection name                       |
| containers |    ✓     | Map of container aliases to container configuration |
|    network |          | Network configuration                               |
|       skip |          | Skip container rm                                   |
|        url |          | Url of podman remote service                        |

### Network Configuration

| Parameter | Required | Description                                  |
|----------:|:--------:|:---------------------------------------------|
|      name |          | Name of network.  Defaults to `artifactId`   |

### Container Configuration

|  Parameter | Required | Description                                          |
|-----------:|:--------:|:-----------------------------------------------------|
|       name |          | Name of the container. Defaults to container's alias |

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
        <version>0.0.4</version>
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
            <image>corp.artifactory.xmple.com/dev-repo/${project.artifactId}:${project.version}</image>
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
                <image>corp.artifactory.xmple.com/dev-repo/${project.artifactId}:${project.version}</image>
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
