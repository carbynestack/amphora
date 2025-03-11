# Amphora

Carbyne Stack Amphora service to store and manage secret shares.

## TL;DR

```bash
# Testing configuration
$ helm install amphora
```

## Introduction

This chart bootstraps a
[Amphora Service](https://github.com/carbynestack/amphora) deployment on a
[Kubernetes](http://kubernetes.io) cluster using the [Helm](https://helm.sh)
package manager.

> **Tip**: This chart is used in the `helmfile.d` based deployment specification
> available in the top-level directory of this repository.

## Prerequisites

- Kubernetes 1.10+ (may also work on earlier versions but has not been tested)
- A Docker Hub account with read access permission for the organization
  `iotspecs` and a Kubernetes Secret with your registry credentials (see the
  section on [Registry Credentials](#registry-credentials)).
- A Redis RDS, Postgres DBMS, and MinIO cluster to serve as the persistence
  layer for Amphora. See the [SPECS SDK](https://github.com/carbynestack) for
  details.
- A [Castor](https://github.com/carbynestack/castor) instance to provide Tuples
  that are required for secret sharing and securely downloading a secret share
  from Amphora.

## Installing the Chart

To install the chart with the release name `my-release`:

```bash
$ helm install --name my-release amphora
```

Make sure that your current working directory is `<specs-sdk-base-dir>/charts`.
The command deploys Amphora on the Kubernetes cluster in the default
configuration. The [configuration](#configuration) section lists the parameters
that can be configured during installation.

> **Tip**: List all releases using `helm list`

## Uninstalling the Chart

To uninstall/delete the `my-release` deployment:

```bash
$ helm delete my-release
```

The command removes all the Kubernetes components associated with the chart and
deletes the release.

## Configuration

The following table lists the (main) configurable parameters of the
`postgres-dbms` chart and their default values. For the full list of
configuration parameters see `values.yaml`. More information on the format
required for the parameters `users` and `databases` can be found in the
documentation for the
[Zalando postgres operator](https://github.com/zalando-incubator/postgres-operator).

| Parameter                                   | Description                                                                               | Default                                       |
| ------------------------------------------- | ----------------------------------------------------------------------------------------- | --------------------------------------------- |
| `amphora.port    `                          | Port the Amphora service is bound to                                                      | `10000`                                       |
| `amphora.playerId`                          | ID identifying the service in the Carbyne Stack virtual cloud                             | `0`                                           |
| `amphora.vcPartners`                        | A list of of the URLs for all partner Amphora services in the virtual cloud               | `[]`                                          |
| `amphora.openingTimeout`                    | Number of seconds to wait for all partners to open their multiplication input             | `5000`                                        |
| `amphora.noSslValidation`                   | Defines whether SSL verification should be disabled for inter VC communication            | `false`                                       |
| `amphora.trustedCertificates`               | Path to certificates that should be used to verify SSL connections                        | \`\`                                          |
| `amphora.image.registry`                    | Amphora Image registry                                                                    | `docker.io`                                   |
| `amphora.image.repository`                  | Amphora Image name                                                                        | `iotspecs/amphora-service`                    |
| `amphora.image.tag`                         | Amphora Image tag                                                                         | `latest`                                      |
| `amphora.image.pullPolicy`                  | Amphora Image pull policy                                                                 | `Never`                                       |
| `amphora.springActiveProfiles`              | Defines the Amphora's Spring profiles to be loaded                                        | `k8s`                                         |
| `amphora.macKey`                            | Defines the MacKey used for secret sharing                                                | \`\`                                          |
| `amphora.castor.serviceUri`                 | Defines the URI for Castor service                                                        | `castor.default.svc.cluster.local`            |
| `amphora.redis.host`                        | The host address to the redis key/value store                                             | `redis.default.svc.cluster.local`             |
| `amphora.redis.port`                        | The port of the redis key/value store                                                     | `6379`                                        |
| `amphora.minio.endpoint`                    | The minio secret store endpoint                                                           | `http://minio.default.svc.cluster.local:9000` |
| `amphora.db.host`                           | The postgres database host                                                                | `dbms-repl.default.svc.cluster.local`         |
| `amphora.db.port`                           | The postgres database port                                                                | `5432`                                        |
| `amphora.db.userSecretName`                 | Name of an existing secret to be used for the database username                           | \`\`                                          |
| `amphora.db.passwordSecretName`             | Name of an existing secret to be used for the database password                           | \`\`                                          |
| `amphora.probes.liveness.initialDelay`      | Number of seconds after the container has started before the liveness probe is initiated  | `60`                                          |
| `amphora.probes.liveness.period`            | How often (in seconds) to perform the liveness probe                                      | `10`                                          |
| `amphora.probes.liveness.failureThreshold`  | How often to fail the liveness probe before finally be marked as unsuccessful             | `3`                                           |
| `amphora.probes.readiness.initialDelay`     | Number of seconds after the container has started before the readiness probe is initiated | `0`                                           |
| `amphora.probes.readiness.period`           | How often (in seconds) to perform the readiness probe                                     | `5`                                           |
| `amphora.probes.readiness.failureThreshold` | How often to fail the readiness probe before finally be marked as unsuccessful            | `3`                                           |

Specify each parameter using the `--set key=value[,key=value]` argument to
`helm install`. For example,

```bash
$ helm install --name my-release \
  --set amphora.image.tag=2018-10-16_15 \
    amphora
```

The above command sets the Amphora image version to `2018-10-16_15`.

Alternatively, a YAML file that specifies the values for the parameters can be
provided while installing the chart. For example,

```bash
$ helm install --name my-release -f values.yaml amphora
```

> **Tip**: You can use the default [values.yaml](values.yaml)

## Registry Credentials

The Amphora docker image is currently hosted on Azure Container Registry which
has a tight integration with the AKS. It allows worker nodes to pull docker
images from acr without explicitly specifying the credentials. As long as the
VCPs are deployed on AKS, no additional credentials are required.
