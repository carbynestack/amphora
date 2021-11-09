/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.amphora.service.testconfig;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.PostgreSQLContainer;

public class ReusablePostgreSQLContainerExtension
    extends PostgreSQLContainer<ReusablePostgreSQLContainerExtension>
    implements AfterEachCallback, BeforeEachCallback {
  private static final String IMAGE_VERSION = "postgres:11.1";
  private static ReusablePostgreSQLContainerExtension container;

  private ReusablePostgreSQLContainerExtension() {
    super(IMAGE_VERSION);
  }

  public static ReusablePostgreSQLContainerExtension getInstance() {
    if (container == null) {
      container = new ReusablePostgreSQLContainerExtension();
    }
    return container;
  }

  @Override
  public void beforeEach(ExtensionContext extensionContext) {
    super.start();
    System.setProperty("POSTGRESQL_URL", container.getJdbcUrl());
    System.setProperty("POSTGRESQL_USERNAME", container.getUsername());
    System.setProperty("POSTGRESQL_PASSWORD", container.getPassword());
  }

  @Override
  public void afterEach(ExtensionContext extensionContext) {
    // container should stay alive until JVM shuts it down
  }
}
