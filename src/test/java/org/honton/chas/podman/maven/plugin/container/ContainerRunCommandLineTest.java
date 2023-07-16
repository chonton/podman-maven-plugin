package org.honton.chas.podman.maven.plugin.container;

import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ContainerRunCommandLineTest {

  @Test
  void posixFilePermissions() {
    assertBit("001", PosixFilePermission.OTHERS_EXECUTE);
    assertBit("002", PosixFilePermission.OTHERS_WRITE);
    assertBit("004", PosixFilePermission.OTHERS_READ);

    assertBit("010", PosixFilePermission.GROUP_EXECUTE);
    assertBit("020", PosixFilePermission.GROUP_WRITE);
    assertBit("040", PosixFilePermission.GROUP_READ);

    assertBit("100", PosixFilePermission.OWNER_EXECUTE);
    assertBit("200", PosixFilePermission.OWNER_WRITE);
    assertBit("400", PosixFilePermission.OWNER_READ);
  }

  private static void assertBit(String mode, PosixFilePermission filePermission) {
    Set<PosixFilePermission> actual = ContainerRunCommandLine.posixFilePermissions(mode);
    Assertions.assertEquals(1, actual.size());
    Assertions.assertEquals(EnumSet.of(filePermission), actual);
  }
}
