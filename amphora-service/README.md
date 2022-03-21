# Amphora Service - An Object Store for Secret Shared Data

The Amphora service (hereinafter referred to as _Amphora_ for short) is an
object store for secret shared data as used for MPC-based distributed
computation on encrypted data in the
[Carbyne Stack](https://github.com/carbynestack) platform.

Envisioned to be a full-fledged bucket/object store like AWS S3, the current
implementation does not provide any advanced features other than object tagging.
Tags can be used to filter secrets based on the assigned values.

## Authentication and Authorization

Currently, all secrets can be accessed by every user that has access to the
respective service endpoints. No access control has been implemented so far.

The [Amphora Client](../amphora-java-client) has built in support for
OAuth2-based bearer token authorization. On the service side, the access tokens
can be checked by the
[respective mechanisms](https://istio.io/latest/docs/tasks/security/authentication/authn-policy/#end-user-authentication)
provided by the [Istio](https://istio.io/) service mesh.

## Endpoints

Amphora provides three interfaces:

- **User facing**: Used by clients for secret sharing, accessing, and managing
  secret shares and their tags (`/input-masks`, `/masked-input`,
  `/secret-shares`).
- **Intra VCP**: Used by services running in the same Virtual Cloud Provider to
  access locally stored secret shares, e.g. for ingesting into a computation
  (`/intra-vcp`).
- **Inter VCP**: Used for communication with other Virtual Cloud Providers in
  the same Virtual Cloud (`/inter-vcp`).

> :warning: When setting up the system, it is necessary to limit access to
> non-public endpoints (`/intra-vcp` and `/inter-vcp`) to local services or the
> services provided by remote Virtual Cloud Providers.

## Getting Started

Amphora is part of [Carbyne Stack](https://github.com/carbynestack) and only one
of multiple services each provider needs to run in order to participate in a
Virtual Cloud. The recommended way to start a Virtual Cloud locally for
development is using the [Carbyne Stack SDK](https://github.com/carbynestack).
Nevertheless, Amphora can also be run in isolation e.g. using helm (see
[charts/amphora/README.md](charts/amphora/README.md) for further details), or
using docker directly (see below).

### Docker Image

A docker image is available in the GitHub Container Repository. The latest image
can be pulled using:

```bash
docker pull ghcr.io/carbynestack/amphora-service:latest
```

### Build from source

Amphora uses [Maven](https://maven.apache.org) for build automation and
dependency management.

To build a custom Amphora docker image, run:

```bash
../mvnw clean package docker:build
```

### Deploy locally

In order to deploy Amphora locally, the following additional services are
required:

- **Minio**: To persist the secret share's data.
- **PostgreSQL**: To store the metadata (e.g. tags) of a secret share.
- **Redis**: To cache Tuples which are required for secret sharing, or interim
  values for cluster internal communication.
- **Castor Service**: To provide the cryptographic material (Tuples), which are
  required for the given SPDZ related operations.

#### Configuration

Amphora requires a set of configuration parameters in order to run successful.
Please see
[charts/amphora/README.md#configuration](charts/amphora/README.md#configuration)
for a complete set of configuration parameters.

The following example shows how to start an Amphora docker container with the
required set of environment variables:

```bash
cat << 'EOF' > amphora.conf
PLAYER_ID=0
MAC_KEY=<<MAC_KEY>>
SPDZ_PRIME=<<PRIME>>
SPDZ_R=<<R>>
SPDZ_R_INV=<<R_INV>>
AMPHORA_VC_PARTNERS=http://localhost:20000
CASTOR_SERVICE_URI=http://localhost:10100
REDIS_HOST=localhost
REDIS_PORT=6379
DB_USER=user
DB_PASSWORD=secret
MINIO_ENDPOINT=http://localhost:9000
EOF
docker run --env-file amphora.conf iotspecs/amphora-service 
```
