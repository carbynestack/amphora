# Amphora Client - A Java library to interact with Amphora

This Java library provides clients to communicate with one or multiple
[Amphora](../amphora-service) service(s) over REST interfaces. Details about the
individual clients are described in the following.

## Provided Clients

### AmphoraClient

The `AmphoraClient` is used to communicate with one or multiple
[Amphora](../amphora-service) services in order to upload, download or delete
secrets or to manipulate their metadata.

The interface is described in `io.carbynestack.amphora.client.AmphoraClient` and
the default implementation is
`io.carbynestack.amphora.client.DefaultAmphoraCLient`.

The `DefaultAmphoraClient` provides the functionality to secret share and
retrieve _raw_ `BigIntegers` only. Specialized logics to store and retrieve
other types like _Longs_, _Floats_ or even _Strings_ needs to be implemented on
application level for the time being.

#### Usage Example

The following example shows a class that is instantiated with a list of _Amphora
Service_ endpoint URIs, and the SPDZ parameters matching the backend service
configuration. The method called `uploadSensorValue` provides basic
functionality to upload sensor data to the _Carbyne Stack_ Virtual Cloud. For
further processing, tags are attached to the secret in order to identify the
sensor type and its ID. When up- and downloading secrets, it is important that
the configuration parameters for `prime`, `r` and `rInv` match the configuration
of the _Amphora Services_. In case of a mismatch, related operations will
produce incorrect values which can neither be processed by the services nor be
reassembled to the initial secret.

```java
import io.carbynestack.amphora.client.AmphoraClient;
import io.carbynestack.amphora.client.DefaultAmphoraClient;
import io.carbynestack.amphora.client.Secret;
import io.carbynestack.amphora.common.AmphoraServiceUri;
import io.carbynestack.amphora.common.Tag;
import io.carbynestack.amphora.common.TagValueType;
import io.carbynestack.amphora.common.exceptions.AmphoraClientException;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class Example {

    final private AmphoraClient amphoraClient;

    public Example(List<String> amphoraEndpoints,
                   BigInteger prime,
                   BigInteger r,
                   BigInteger rInv)
            throws AmphoraClientException {
        amphoraClient = DefaultAmphoraClient.builder()
                .endpoints(amphoraEndpoints.stream()
                        .map(AmphoraServiceUri::new)
                        .collect(Collectors.toList()))
                .prime(prime)
                .r(r)
                .rInv(rInv)
                .build();
    }

    public UUID uploadSensorValue(long secretValue,
                                  long sensorId,
                                  String sensorType)
            throws AmphoraClientException {
        List<Tag> tags = Arrays.asList(
                Tag.builder().key("type").value(sensorType).build(),
                Tag.builder()
                        .key("sensorId")
                        .value(Long.toString(sensorId))
                        .valueType(TagValueType.LONG)
                        .build());
        return amphoraClient.createSecret(
                Secret.of(tags,
                        new BigInteger[]{BigInteger.valueOf(secretValue)}));
    }

}
```

The `DefaultAmphoraClient` also provides functionality to filter and retrieve
all secrets stored in _Amphora_ based on a set of tags. The `creation-data` tag
referenced in the example below, is automatically attached to secrets stored in
_Amphora_ and can also be used for filtering.

```java
import io.carbynestack.amphora.client.BearerTokenProvider;
import io.carbynestack.amphora.client.DefaultAmphoraClient;
import io.carbynestack.amphora.common.AmphoraServiceUri;
import io.carbynestack.amphora.common.exceptions.AmphoraClientException;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;
import java.util.Collections;

@Slf4j
public class AmphoraExample {

    private AmphoraClient amphoraClient;

    public AmphoraExample(List<AmphoraServiceUri> endpoints,
                          BearerTokenProvider<AmphoraServiceUri> tokenProvider,
                          BigInteger prime,
                          BigInteger r,
                          BigInteger rInv) throws AmphoraClientException {
        amphoraClient = DefaultAmphoraClient.builder()
                .prime(prime)
                .r(r)
                .rInv(rInv)
                .endpoints(endpoints)
                .bearerTokenProvider(tokeProvider)
                .build();
    }

    public List<Long> retrieveSensorValues(String sensorType,
                                           long sensorId,
                                           long createdAfter) {
        List<TagFilter> filterCriteria =
                Arrays.asList(
                        TagFilter.with("type",
                                sensorType,
                                TagFilterOperator.EQUALS),
                        TagFilter.with("sensorID",
                                Long.toString(sensorId),
                                TagFilterOperator.EQUALS),
                        TagFilter.with("creation-date",
                                Long.toString(createdAfter),
                                TagFilterOperator.GREATER_THAN));
        return retrieveSecretsByFilter(filterCriteria)
                .map(secretList ->
                        secretList.stream()
                                .map(Secret::getData)
                                .map(data -> data[0].longValue())
                                .collect(Collectors.toList()))
                .recover(t -> {
                    log.error("Failed fetching sensor values.", t);
                    return Collections.emptyList();
                })
                .get();
    }

    private Try<List<Secret>>
    retrieveSecretsByFilter(List<TagFilter> filterCriteria) {
        return Try.of(() ->
                        amphoraClient.getSecrets(filterCriteria))
                .map(metadataMatchingCriteria ->
                        metadataMatchingCriteria.stream()
                                .map(Metadata::getSecretId)
                                .map(id -> {
                                    try {
                                        return amphoraClient.getSecret(id);
                                    } catch (AmphoraClientException ace) {
                                        System.err.printf(
                                                "Error while fetching Secret "
                                                + "with id #%s: %s",
                                                id,
                                                ace.getMessage());
                                        return null;
                                    }
                                })
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList()));
    }

}
```

### AmphoraInterVcpClient

The `AmphoraInterVcpClient` is used to communicate with other _Amphora Service_
instances in a _Carbyne Stack_ Virtual Cloud. It provides the functionality to
share data with other parties, which is used when performing multiplications
while providing secret shares according to the _Output Delivery Protocol_ (see
the Amphora top-level [README](../README.md) for more information). This
functionality is not supposed to be used by external parties, and the related
endpoints must be protected from unauthorized access.

### AmphoraIntraVcpClient

The `AmphoraIntraVcpClient` is used for Virtual Cloud Provider internal
service-to-service communication as required e.g. by
[Ephemeral](https://github.com/carbynestack/ephemeral) in order to retrieve
secret shares as input to program executions, or write results back to _Amphora_
. Since related endpoints are expected to be blocked from external access, no
authentication mechanisms are provided as of now.

## Getting Started

Amphora uses [Maven](https://maven.apache.org/) for dependency and build
management. You can add _AmphoraClient_ to your project by declaring the
following maven dependency in your `pom.xml`:

```xml
<dependency>
    <groupId>io.carbynestack</groupId>
    <artifactId>amphora-java-client</artifactId>
    <version>0.1-SNAPSHOT</version>
</dependency>
```

### Building from Source

The _AmphoraClient_ library can be build and installed into the local maven
repository using:

```bash
mvn install
```
