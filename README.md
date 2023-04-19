# Carbyne Stack Amphora Secret Share Store

[![codecov](https://codecov.io/gh/carbynestack/amphora/branch/master/graph/badge.svg?token=Oc5cDCTJsB)](https://codecov.io/gh/carbynestack/amphora)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/eb2caa9e77074e1384789ae168fdcb29)](https://www.codacy.com?utm_source=github.com&utm_medium=referral&utm_content=carbynestack/amphora&utm_campaign=Badge_Grade)
[![Known Vulnerabilities](https://snyk.io/test/github/carbynestack/amphora/badge.svg)](https://snyk.io/test/github/carbynestack/amphora)
[![pre-commit](https://img.shields.io/badge/pre--commit-enabled-brightgreen?logo=pre-commit&logoColor=white)](https://github.com/pre-commit/pre-commit)
[![Contributor Covenant](https://img.shields.io/badge/Contributor%20Covenant-2.1-4baaaa.svg)](CODE_OF_CONDUCT.md)

Amphora is an open source object store for secret shared data and part of
[Carbyne Stack](https://github.com/carbynestack).

> **DISCLAIMER**: Carbyne Stack Amphora is *alpha* software. The software is not
> ready for production use. It has neither been developed nor tested for a
> specific use case.

Please have a look at the underlying modules for more information on how to run
an Amphora service or how to interact with it using the Java client:

- [Amphora Common](amphora-common) - A shared library of commonly used
  functionality.
- [Amphora Service](amphora-service) - The microservice implementing the object
  store for secret shared data.
- [Amphora Java Client](amphora-java-client) - A Java Client library to interact
  with Amphora service(s). The module provides client implementations to
  communicate with
  - the Amphora service from within the Virtual Cloud Provider
  - other Amphora services participating in the Virtual Cloud
  - all Amphora services participating in the Virtual Cloud to share secret or
    recombine secret shared data.

## Namesake

As of [wikipedia](https://en.wikipedia.org/wiki/Amphora) an *amphora* (from
Greek amphoras) is a type of container of a characteristic shape and size.
Amphorae were used in vast numbers for the transport and storage of various
products, both liquid and dry, but mostly for wine.

## Secure Secret Sharing and Recombination

The Amphora client and service use an additive secret sharing scheme and
implement the *Input Supply* and *Output Delivery* Protocol \[1\] of Damgard et
al. to provide secure secret sharing and recombination methods in the
client/server model of MPC. Therefore, sharing a secret will consume three
*Input Masks* as well as two *Multiplication Triples* for each word (BigInteger)
to be shared, and two *InputMasks* as well as two *Multiplication Triples* for
each secret word to be retrieved.

By this, providing and retrieving a secret comes at high cost in terms of
*tuple* consumption, but will reveal if any of the MPC parties behaves malicious
when providing shared data. The `DefaultAmphoraClient` will throw an exception
in case the verification fails according to the *Output Delivery Protocol*.

## License

Carbyne Stack *Amphora Secret Sharing Object Store* is open-sourced under the
Apache License 2.0. See the [LICENSE](LICENSE) file for details.

### 3rd Party Licenses

For information on how license obligations for 3rd party OSS dependencies are
fulfilled see the [README](https://github.com/carbynestack/carbynestack) file of
the Carbyne Stack repository.

## Contributing

Please see the Carbyne Stack
[Contributor's Guide](https://github.com/carbynestack/carbynestack/blob/master/CONTRIBUTING.md)
.

## References

1. Ivan Damgård, Kasper Damgård, Kurt Nielsen, Peter Sebastian Nordholt, Tomas
   Toft: *Confidential Benchmarking based on Multiparty Computation*. IACR
   Cryptology ePrint Archive 2015: 1006 (2015)
   [https://eprint.iacr.org/2015/1006](https://eprint.iacr.org/2015/1006)
