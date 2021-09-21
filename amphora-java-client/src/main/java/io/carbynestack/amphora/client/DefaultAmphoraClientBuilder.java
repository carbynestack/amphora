/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.amphora.client;

import io.carbynestack.amphora.common.AmphoraServiceUri;
import io.carbynestack.amphora.common.exceptions.AmphoraClientException;
import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

/** Builder class to configure and create a new {@link DefaultAmphoraClient}. */
@Accessors(chain = true, fluent = true)
public class DefaultAmphoraClientBuilder {
  /**
   * A list of <i>Amphora</i> service endpoints the client will communicate with.
   *
   * <p>The endpoints can either be configured at once passing a set of uris using {@link
   * #endpoints(List)} or be added individually using {@link #addEndpoint(AmphoraServiceUri)}.
   *
   * <p>When building the {@link DefaultAmphoraClient}, the list must neither be null nor empty.
   */
  @Getter private List<AmphoraServiceUri> serviceUris;
  /**
   * Sets a list of certificates (.pem) to be added to the trust store.<br>
   * This allows tls secured communication with services that do not have a certificate issued by an
   * official CA (certificate authority).
   */
  @Getter private List<File> trustedCertificates;
  /**
   * Disable SSL validation
   *
   * <p><b>WARNING</b><br>
   * Please be aware, that this option leads to insecure web connections and is meant to be used in
   * a local test setup only. Using this option in a productive environment is explicitly <u>not
   * recommended</u>.
   */
  @Getter private boolean noSslValidation = false;
  /**
   * Sets a provider for getting a backend specific bearer token that is injected as an
   * authorization header to REST HTTP calls emitted by the client.
   */
  @Setter @Getter private BearerTokenProvider<AmphoraServiceUri> bearerTokenProvider;
  /**
   * Sets the Prime as used by the MPC backend.
   *
   * <p>This field is mandatory and must not be <i>null</i> when instantiating the client using
   * {@link #build()}.
   */
  @Setter @Getter @NonNull private BigInteger prime;
  /**
   * Define the auxiliary modulus R as used by the MPC backend.
   *
   * <p>This field is mandatory and must not be <i>null</i> when instantiating the client using
   * {@link #build()}.
   */
  @Setter @Getter @NonNull private BigInteger r;
  /**
   * Define the multiplicative inverse for the auxiliary modulus R as used by the MPC backend.
   *
   * <p>This field is mandatory and must not be <i>null</i> when instantiating the client using
   * {@link #build()}.
   */
  @Setter @Getter @NonNull private BigInteger rInv;

  public DefaultAmphoraClientBuilder() {
    this.serviceUris = new ArrayList<>();
    this.trustedCertificates = new ArrayList<>();
  }

  /**
   * Adds an AmphoraServiceUri to the list of endpoints, the DefaultAmphoraClient should communicate
   * with
   *
   * @param uri Endpoint URL of a backend Amphora Service
   * @throws NullPointerException if the provided endpoint uri is null
   */
  public DefaultAmphoraClientBuilder addEndpoint(@NonNull AmphoraServiceUri uri) {
    this.serviceUris.add(uri);
    return this;
  }

  /**
   * Sets the endpoints for the {@link DefaultAmphoraClient} to communicate with.
   *
   * <p>All already defined endpoints will be replaced. To add additional endpoints use {@link
   * #addEndpoint(AmphoraServiceUri)}.
   *
   * <p>The provided list must not be <i>null</i>. The items in the list will be put in an ArrayList
   * for internal processing. <i>null</i> entries will be removed before storing.
   *
   * @param endpoints A List of endpoints which will be used to communicate with.
   * @throws NullPointerException if the provided list is null
   */
  public DefaultAmphoraClientBuilder endpoints(@NonNull List<AmphoraServiceUri> endpoints) {
    this.serviceUris =
        endpoints.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(ArrayList::new));
    return this;
  }

  /**
   * Disables the SSL certificate validation check.
   *
   * <p><b>WARNING</b><br>
   * Please be aware, that this option leads to insecure web connections and is meant to be used in
   * a local test setup only. Using this option in a productive environment is explicitly <u>not
   * recommended</u>.
   */
  public DefaultAmphoraClientBuilder withoutSslCertificateValidation() {
    this.noSslValidation = true;
    return this;
  }

  /**
   * Sets the SSL certificates to be trusted on validation check.
   *
   * <p>All already defined certificates will be replaced. To add additional certificates use {@link
   * #addTrustedCertificate(File)}.
   *
   * <p>The provided list must not be <i>null</i>. The items in the list will be put in an ArrayList
   * for internal processing. <i>null</i> entries will be removed before storing.
   *
   * <p><b>WARNING</b><br>
   * Please be aware, that this option leads to insecure web connections and is meant to be used in
   * a local test setup only. Using this option in a productive environment is explicitly <u>not
   * recommended</u>.
   *
   * @param trustedCertificates A List of certificates which will be trusted during communication.
   * @throws NullPointerException if the provided list is null
   */
  public DefaultAmphoraClientBuilder trustedCertificates(@NonNull List<File> trustedCertificates) {
    this.trustedCertificates =
        trustedCertificates.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(ArrayList::new));
    return this;
  }

  /**
   * Adds a certificate (.pem) to the trust store.<br>
   * This allows tls secured communication with services that do not have a certificate issued by an
   * official CA (certificate authority).
   *
   * @param trustedCertificate Public certificate.
   * @throws NullPointerException if the given file is null
   */
  public DefaultAmphoraClientBuilder addTrustedCertificate(@NonNull File trustedCertificate) {
    this.trustedCertificates.add(trustedCertificate);
    return this;
  }

  /**
   * Builds and returns a new {@link DefaultAmphoraClient} according to the given configuration.
   *
   * @throws AmphoraClientException if creating the client failed
   * @throws IllegalArgumentException if the service endpoints have not been configured {@link
   *     #serviceUris}
   * @throws NullPointerException if either {@link #prime}, {@link #r} or {@link #rInv} are not
   *     properly defined
   */
  public DefaultAmphoraClient build() throws AmphoraClientException {
    Objects.requireNonNull(prime, "Prime must not be null");
    Objects.requireNonNull(r, "Auxiliary modulus R must not be null");
    Objects.requireNonNull(
        rInv, "Multiplicative inverse for the auxiliary modulus R must not be null");
    if (this.serviceUris == null || this.serviceUris.isEmpty()) {
      throw new IllegalArgumentException("At least one amphora service uri has to be provided.");
    }
    return new DefaultAmphoraClient(this);
  }
}
