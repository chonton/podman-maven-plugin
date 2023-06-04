# podman-maven-plugin

Use podman to build and push images. This has two goals:

1. Build Containerfile from base image and copy glob directives
2. Build image from Dockerfile/Containerfile and context

# Rationale

Build the containers using [podman](https://docs.podman.io/en/latest/markdown/podman-build.1.html).

Deploy your containers to [docker desktop](https://docs.docker.com/desktop/kubernetes/) or
[minikube](https://minikube.sigs.k8s.io/docs/) for integration testing using
[podman charts](https://github.com/chonton/podmanrepo-maven-plugin).
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
|     chown |          | Owner:Group of the files in the image                                     |
|     chmod |          | Permissions of the files in the image                                     |
|      srcs |    ✓     | Files relative to the context to be copied.  (golang wildcards supported) |
|      dest |    ✓     | Absolute destination in the image where files are copied                  |

### entrypoint or cmd attributes

| Parameter |  Required  | Description                                                                     |
|----------:|:----------:|:--------------------------------------------------------------------------------|
|      exec | ✓ or shell | List of Executable and parameters, no shell involved                            |
|     shell |  ✓ or cmd  | Single line command that will be executed by shell (not used if exec specified) |

## Build Goal

The [build](https://chonton.github.io/podman-maven-plugin/build-mojo.html) goal binds by default to
the **package** phase. This goal will execute `podman build` with the proper parameters.

### Configuration

|         Parameter | Required | Description                                                                      |
|------------------:|:--------:|:---------------------------------------------------------------------------------|
|    buildArguments |          | Map of build arguments                                                           |
|     containerfile |    ✓     | Instruction file relative to contextDir, default is *Containerfile*)             |
|        contextDir |    ✓     | Directory with build content, default is *${project.build.directory}/contextDir* |
|              name |    ✓     | Name of image.  Must be of form registry/repository:version                      |
|         platforms |          | List of platforms.  Each element may contain comma separated *os/arch*           |
| remote.connection |          | Remote podman connection name                                                    |
|        remote.url |          | Url of podman remote service                                                     |
|              skip |          | Skip the build                                                                   |

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
        <version>0.0.1</version>
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
            <goal>build</goal>
          </goals>
        </execution>
      </executions>
      <configuration>
        <buildArguments combine.children="append">
          <alpineBase>docker.io/library/alpine:3.18.0</alpineBase>
        </buildArguments>
        <contextDir>target/quarkus-app</contextDir>
        <containerfile>src/image/Dockerfile</containerfile>
        <name>corp.artifactory.example.com/dev-repo/${project.artifactId}:${project.version}</name>
        <platforms>
          <platform>${build.platforms}</platform>
        </platforms>
        <remote>
          <!-- set this value elsewhere, maybe jenkins creates this process -->
          <url>${podman.url}</url>
        </remote>
      </configuration>
    </plugin>
  </plugins>
</build>
```
