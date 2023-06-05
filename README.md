# podman-maven-plugin

Use podman to build and push images. This plugin has four goals:

1. Create Containerfile from base image and copy directives
2. Build image from Containerfile and context
3. Login to registry
4. Push image to registry

# Rationale

Build container images with [podman](https://docs.podman.io/en/latest/markdown/podman-build.1.html).

Deploy your containers to [docker desktop](https://docs.docker.com/desktop/kubernetes/) or
[minikube](https://minikube.sigs.k8s.io/docs/) for integration testing using
[helmrepo](https://github.com/chonton/helmrepo-maven-plugin).
Start k8s pods/deployments/services during **pre-integration-test** phase. Use
[failsafe](https://maven.apache.org/surefire/maven-failsafe-plugin/) to run integration tests during
**integration-test** phase. Capture logs and uninstall k8s pods/deployments/services during the
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

|         Parameter | Required | Description                                                                      |
|------------------:|:--------:|:---------------------------------------------------------------------------------|
|    buildArguments |          | Map of build arguments                                                           |
|     containerfile |    ✓     | Instruction file relative to contextDir, default is *Containerfile*)             |
|        contextDir |    ✓     | Directory with build content, default is *${project.build.directory}/contextDir* |
|             image |    ✓     | Fully qualified image name; must include registry/repository:version             |
|   loadDockerCache |          | If set to true, load the local docker image cache with resulting image           |
|         platforms |          | List of platforms.  Each element may contain comma separated *os/arch*           |
| remote.connection |          | Remote podman connection name                                                    |
|        remote.url |          | Url of podman remote service                                                     |
|              skip |          | Skip the build                                                                   |

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

|         Parameter | Required | Description                                            |
|------------------:|:--------:|:-------------------------------------------------------|
|          registry |    ✓     | Registry to authenticate with                          |
|          password |          | If registry not found in settings.xml, use as password |
| remote.connection |          | Remote podman connection name                          |
|        remote.url |          | Url of podman remote service                           |
|              skip |          | Skip login                                             |
|          username |          | If registry not found in settings.xml, use as username |

## Push Goal

The [push](https://chonton.github.io/podman-maven-plugin/push-mojo.html) goal binds by default to
the **deploy** phase. This goal uses `podman` to `push` an image to its registry.

### Configuration

|         Parameter | Required | Description                                                      |
|------------------:|:--------:|:-----------------------------------------------------------------|
|             image |    ✓     | The fully qualified image name, with registry/repository:version |
| remote.connection |          | Remote podman connection name                                    |
|        remote.url |          | Url of podman remote service                                     |
|              skip |          | Skip push                                                        |

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
        <version>0.0.2</version>
      </plugin>
    </plugins>
  </pluginManagement>

  <plugins>
    <plugin>
      <groupId>org.honton.chas</groupId>
      <artifactId>podman-maven-plugin</artifactId>
      <executions>
        <execution>
          <goals>
            <goal>containerfile</goal>
            <goal>build</goal>
            <goal>login</goal>
            <goal>push</goal>
          </goals>
        </execution>
      </executions>
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
        <remote>
          <!-- set this value elsewhere, maybe jenkins starts a server process -->
          <url>${podman.url}</url>
        </remote>
      </configuration>
    </plugin>
  </plugins>
</build>
```
