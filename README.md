# podman-maven-plugin

Use podman to build and push images. This has two goals:

1. Build Containerfile from base image and copy glob directives
2. Build image from Dockerfile/Containerfile and context

# Rationale

Build the containers using [podman](https://docs.podman.io/en/latest/markdown/podman-build.1.html).

Deploy your containers to [docker desktop](https://docs.docker.com/desktop/kubernetes/) or
[minikube](https://minikube.sigs.k8s.io/docs/) for integration testing
using [podman charts](https://github.com/chonton/podmanrepo-maven-plugin). Start k8s pods/deployments/services
during **pre-integration-test** phase. Use [failsafe](https://maven.apache.org/surefire/maven-failsafe-plugin/) to run
integration tests during **integration-test** phase. Capture logs and uninstall k8s pods/deployments/services during the
**post-integration-test** phase.

# Plugin

Plugin reports available at [plugin info](https://chonton.github.io/podman-maven-plugin/0.0.1/plugin-info.html).

## Build Goal

The [build](https://chonton.github.io/podman-maven-plugin/0.0.1/build.html) goal binds by default to the
**package** phase. This goal will execute `podman build` with the proper parameters.

### Configuration

|         Parameter | Required | Description                                                            |
|------------------:|:--------:|:-----------------------------------------------------------------------|
|    buildArguments |          | Map of build arguments                                                 |
|     containerFile |    ✓     | Build instruction file                                                 |
|        contextDir |    ✓     | Directory containing source content for build                          |
|              name |    ✓     | Name of image.  Must be of form registry/repository:version            |
|         platforms |          | List of platforms.  Each element may contain comma separated *os/arch* |
| remote.connection |          | Remote podman connection name                                          |
|        remote.url |          | Url of podman remote service                                           |
|              skip |          | Skip the build                                                         |

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
        <containerFile>src/image/Dockerfile</containerFile>
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

## Use as a packaging extension

```xml

<project>
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example.podman</groupId>
    <artifactId>chart</artifactId>
    <packaging>tgz</packaging>

    <build>
        <extensions>
            <extension>
                <groupId>org.honton.chas</groupId>
                <artifactId>podman-maven-plugin</artifactId>
                <version>0.0.2</version>
            </extension>
        </extensions>
    </build>

</project>
```
