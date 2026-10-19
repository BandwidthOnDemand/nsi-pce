**Note: The DDS is now a standalone package that will require a separate install.  This documentation needs to be updated.

# NSI Path Computation Engine

The Network Services Interface Path Computation Engine (NSI-PCE) is a conglomeration of components from the NSI 2.0 series of specifications [OGF NSF]:

* PCE Core - provides a RESTful interface for requesting path resolution within a network of NSI Connection Service enabled domains.  It utilizes the Spring Framework to provide an infrastructure for pluggable path computation algorithms, allowing users to incorporate their own path computation algorithms into the PCE.

* NSI Topology Service – provides an NSI Topology Representation [OGF NSI-TS] compliant service with an independent RESTful interface giving access to discovered network topology.

* NSI DDS – A full implementation of the proposed NSI Document Discover Service [OGF NSI-DS] with backwards-compatible support for dynamic discovery of  both A-GOLE and Gang-of-Three (Gof3) topology.

![Image of PCE](https://raw.githubusercontent.com/BandwidthOnDemand/nsi-pce/master/images/pce.png)

In the near future the NSI-PCE will be decoupled into two separate runtime packages, the NSI-PCE and the NSI-DDS, however, for the near future they are contained in a single runtime.

## Getting Started

The NSI-PCE needs Java 17 and builds with the Maven wrapper (`./mvnw`).  Once you have downloaded the `nsi-pce` project just type `maven clean install` in the project directory to build the NSI-PCE application.  The project includes a set of REST API test cases that demonstrate the use of available REST API.  To bypass running of the test suite during build include the maven skip test option `mvn install -Dmaven.test.skip=true`.
```
> git clone https://github.com/BandwidthOnDemand/nsi-pce.git
Cloning into 'nsi-pce'...
remote: Reusing existing pack: 4508, done.
remote: Counting objects: 11, done.
remote: Compressing objects: 100% (10/10), done.
remote: Total 4519 (delta 4), reused 0 (delta 0)
Receiving objects: 100% (4519/4519), 1.16 MiB | 541.00 KiB/s, done.
Resolving deltas: 100% (2171/2171), done.
Checking connectivity... done.
> cd nsi-pce
> mvm clean install
```

## Quickstart Configuration

If the NSI-PCE is being used in conjunction with a collocated NSI-Safnari NSA aggregator instance, then all configuration files can remain at their defaults, except for the DDS configuration `nsi-pce/config/dds.xml` which will need to be configured with the peer NSA information.  See the section titled *"DDS configuration (dds.xml)"* for more information.

Once configured, run the command `./run.sh` from within the `nsi-pce` directory.  The NSI-PCE will start up and discover the network.

## Configuring

The default runtime configuration directory for the NSI-PCE is `nsi-pce/config` but this can be changed through command line input.  There are three files that will need to be changed for a customized installation.

* http.json
* topology-dds.xml
* dds.xml

Alternatives to these files can also be specified on the command line.  The contents of each file will be discussed in their individual sections.

### HTTP configuration (http.json)

Configuration of the imbedded HTTP server is controlled through the `nsi-pce/config/http.json` file.  This file contains a simple JSON formatted list of configuration parameters:

```
{
    "pce": {
        "url": "http://localhost:8400/",
        "packageName": "net.es.nsi.pce",
        "staticPath": "config/www/",
        "wwwPath": "/www"
    }
}
```

* *url* - This is the public facing URL that applications will use to access the NSI-PCE RESTful interface.  If the NSI-PCE will only be accessed by application on localhost, then this URL can remain `http://localhost:8400/`, otherwise it will need to be changed to the public hostname.

* *packageName* - This is the package name of NSI-PCE module and the root from which the HTTP service will scan for available REST interfaces.  This should not be changed.

* *staticPath* - The NSI-PCE can be used to serve static content by specifying a local directory that will contain the content to be served.  This can be helpful when testing the DDS A-GOLE or Gof3 doscovery mechanism.  In most cases you will leave this as an empty string, thereby disabling the static content feature.

* *wwwPath* - The relative root path URL exposed through the NSI-PCE for static content.  In this example, the absolute URL for static content will be `http://localhost:8400/www`.  A path must be specified is the *staticPath* parameter is specified.  The relative path cannot be an empty string or the string "/".

### Topology source configuration (topology-dds.xml)

The NSI-PCE's internal topology service utilizes the NSI-DDS service (NSI Document Discovery Service v1) for collection of NSA Description and NML Topology documents from all NSA with the network.  The internal topology service is currently using the RESTful polling interface of the NSI-DDS, instead of the github manifest as supported in Topology Discovery v1.

The prototype NSI-DDS implementation used by the topology service is currently embedded within the NSI-PCE, but this will be moved to a separate process in the near future.  The topology source configuration is controlled through the `nsi-pce/config/topology-dds.xml` file.  This file contains a simple XML formatted list of configuration parameters:

```
<?xml version="1.0" encoding="UTF-8"?>
<top:topology xmlns:top="http://schemas.es.net/nsi/2013/08/pce/topology/configuration">
    <location>http://localhost:8400/discovery</location>
	<auditInterval>300</auditInterval>
    <defaultServiceType>http://services.ogf.org/nsi/2013/12/descriptions/EVTS.A-GOLE</defaultServiceType>
</top:topology>
```

* *location* - The base URL of the NSI DDS service used as a document source for NSA description and NML topology documents.  This will point to the localhost endpoint associated with the imbedded instance of the DDS service (same port as NSI-PCE interface).

* *auditInterval* - The interval (in seconds) the PCE topology service will audit the configured DDS server for document changes.  At the moment is controls the polling interval.

* *defaultServiceType* - The service type used as a default for networks not announcing a service type in NML topology.  If not present, this value will be used to create a default *ServiceDefinition* entry and *ServiceDomain* entries in the NSI topology for the discovered network.

### Path computation (beans.xml)

`config/beans.xml` wires the path computation modules. The `dijkstraPCE` bean has one setting:

* *avoidLoops* (default `true`) - Routes the legs of a request, split at each ERO STP, together.
  The VLAN at an ERO STP is chosen to fit the leg after it, and within a leg a path:
  * never leaves a network and comes back through the same port, a segment the uPA refuses;
  * re-enters a network through another port only when no other path exists;
  * keeps the VLAN of the STP before it where it can.

  A hairpin across ERO STPs, leaving a network at one and coming back at a later one, is what the
  request asked for and is kept. `false` restores the earlier behaviour: each leg is routed on its
  own and the VLAN at an ERO STP without one is picked at random.

```
<bean id="dijkstraPCE" class="net.es.nsi.pce.pf.DijkstraPCE" scope="prototype">
    <property name="avoidLoops" value="true" />
</bean>
```

### DDS configuration (dds.xml)

The NSI-PCE's internal NSI-DDS engine (NSI Document Discovery Service v1) is used for collection of NSA Description and NML Topology documents from all NSA with the network.  The DDS then makes these discovered documents available for application use thfought its RESTful API.  The NSI-PCE's internal topology services utilizes this DDS RESTful API for building a network topology view.

The `nsi-pce/config/dds.xml` configuration file controls runtime configuration of the NSI-DDS discovery engine.  The following XML elements are supported in this configuration file:

```
<?xml version="1.0" encoding="UTF-8"?>
<tns:discovery xmlns:tns="http://schemas.es.net/nsi/2014/03/pce/discovery/configuration"
 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <nsaId>urn:ogf:network:surfnet.nl:1990:nsa:bod-acc</nsaId>
    <documents>config/documents</documents>
    <cache>config/cache</cache>
    <auditInterval>300</auditInterval>
    <expiryInterval>600</expiryInterval>
    <actorPool>10</actorPool>
    <baseURL>http://localhost:8400/discovery</baseURL>
    <peerURL type="application/vnd.ogf.nsi.dds.v1+xml">
        http://dds.example.com:8400/discovery
    </peerURL>
    <peerURL type="application/vnd.ogf.nsi.nsa.v1+xml">
        https://nsa1.example.com/nsa-discovery
    </peerURL>
    <peerURL type="application/vnd.ogf.nsi.topology.v1+xml">
        https://example.com/master/manifest.xml
    </peerURL>
</tns:discovery>
```

* **nsaId** - The NSA identifier of the local NSA assocated with this DDS instance.  This value will be used to determine which documents in the DDS document space are associated with the `/local` URL query.  The NSI-PCE topology service utilizes the `/local` query to populate local NSA information.

* **documents** - The local directory the DDS will monitor for document file content to auto load into the DDS document space.  This directory is checked for new content every auditInterval.  This element is optional.

* **cache** - The local directory used to store discovered documents that will be reloaded after a restart of the DDS.  One reloaded an audit occurs to refresh any documents with new versions available.  This element is optional.

* **auditInterval** - The interval (in seconds) the DDS will audit all peer DDS servers, Gof3 NSA and topology documents, or A-GOLE topology.

* **expiryInterval** - The number of seconds the DDS will maintain a document after the document's lifetime has been reached.

* **actorPool** - The number of actors to instantiate per discovery type (DDS, Gof3, A-GOLE).

* **baseURL** - The base URL of the local DDS service that will be used when registering with peer DDS services.  Is only needed if a peerURL type of "application/vnd.ogf.nsi.dds.v1+xml" is configured.

* **peerURL** - Lists URL for peer data sources for the DDS service to utilize for document discovery. The following type of peerURL are supported (mixed types are supported):

```
application/vnd.ogf.nsi.dds.v1+xml
```

- A peer DDS server supporting NSI-DDS v1.  Each peer DDS server must have its own peerURL entry.

```
application/vnd.ogf.nsi.nsa.v1+xml
```

- An NSA supporting the Gof3 discovery protocol.  Each peer NSA must have its own peerURL entry.

```
application/vnd.ogf.nsi.topology.v1+xml
```
- The Automated GOLE topology discovery v1 which utilizes a manifest file.

## Versioning
The release git tag is the only place a version is written by hand. The `pom.xml` version is the
placeholder `0.0.0-SNAPSHOT`; `.github/workflows/build-push-container.yml` checks out with
`fetch-depth: 0`, resolves the version with `git describe --tags --always`, and passes it as
`--build-arg VERSION=...`. The `Dockerfile` then runs `versions:set` before packaging, so a tag
builds `2.1.2` and any other commit builds `2.1.2-3-g1a2b3c4`.

## Logging
The PCE logs through SLF4J and Logback. At startup it loads `logback.xml` from the configuration
directory, or the file named by `-Dlogback.configurationFile`; without either it logs at INFO to
the console, from the `logback.xml` in the jar. HTTP requests and payloads to the DDS and `replyTo`
are logged at DEBUG. `config/logback.xml` also writes the error, info and debug files under
`var/log`. The TopologyViewer reads `logback-viewer.xml` from that directory if present.

The Helm chart mounts every file in its ConfigMap, so a `logback.xml` in `config.inlineData`
replaces the default. With `scan="true"` a changed level takes effect without a restart.

## Tests
`.github/workflows/ci.yml` runs `./mvnw -B verify` on JDK 17 under Xvfb for every pull request and
every push to `master`. The suite takes about seven minutes; each test class runs in its own JVM
with a 4 GB heap. `TopologyViewerTest` needs a display and is skipped without one.

`ApiCharacterizationTest` calls every REST endpoint and FindPath over plain HTTP against
`topology/api.xml` and compares status, content type and body with the files in
`src/test/resources/golden`. Those files were recorded on the Java 8 code (2.2.0), so a difference
is a change in what clients see. After an intended change, rewrite them and review the diff:

```
$ ./mvnw -B test -Dtest=ApiCharacterizationTest -Dgolden.update=true
```

Path-finding tests run against synthetic topologies in `src/test/resources/topology/`. The
collection files there are generated: change `generate.py` and rerun it rather than editing the
XML. `vlan-loop.xml` is a triangle where one domain cannot swap labels; `two-links.xml` adds a
second link into that domain, for the last-resort detour and a hairpin requested by the ERO.
`api.xml` is the triangle with three VLANs per port, which keeps the golden files small.

```
$ python3 src/test/resources/topology/generate.py
$ ./mvnw -B verify
```

A container build without the argument fails rather than producing a mislabelled image:

```
$ docker build --build-arg VERSION="$(git describe --tags --always)" -t nsi-pce .
```

## Running the NSI-PCE
`./run.sh` starts the PCE from `target/pce.jar` with the configuration in `config/`. Extra arguments
are passed to the PCE, for example `-c <configDir>` or `-topologyConfigFile <file>`.

## TLS
The PCE serves plain HTTP; put a TLS-terminating proxy in front of it for inbound HTTPS. For its
outbound calls, fetching topology from the DDS and posting FindPath results to `replyTo`, it can
present a client certificate and use its own trusted CAs.

From PEM files, through these system properties:

| Property | File |
|----------|------|
| `nsi.tls.certificate` | Client certificate, optionally followed by its chain |
| `nsi.tls.privateKey` | Its private key, PKCS#8 (`BEGIN PRIVATE KEY`) |
| `nsi.tls.trustedCertificates` | CA bundle for the DDS and `replyTo` servers |

The certificate and key go together; the CA bundle can be set on its own. The bundle replaces the
JVM's default trust, so it must cover every server the PCE calls. Convert another key format with
`openssl pkcs8 -topk8 -nocrypt -in <key> -out <key.pkcs8>`. `run.sh` and `viewer.sh` pass the files
named by `TLS_CERT`, `TLS_KEY` and `TLS_CA`:

```
$ TLS_CERT=client.crt TLS_KEY=client.key TLS_CA=ca.crt ./run.sh
```

Whatever the `nsi.tls.*` properties leave unset comes from the standard `javax.net.ssl.keyStore`,
`keyStorePassword`, `trustStore` and `trustStorePassword` properties, so existing JKS or PKCS12
stores still work, alone or with a PEM CA bundle. A changed certificate takes effect after a restart.

The Helm chart supports either under `config.tls.client`, selected by `mode`:

```yaml
config:
  tls:
    client:
      mode: pem
      pem:
        identitySecretName: nsi-pce-client-tls  # kubernetes.io/tls: tls.crt and tls.key
        caSecretName: nsi-pce-peer-ca            # ca.crt
```

```yaml
config:
  tls:
    client:
      mode: keystore
      keystore:
        secretName: nsi-pce-stores               # keystore.p12 and truststore.p12
        passwordSecretName: nsi-pce-store-passwords
```

The `wait-for-dds` init container uses the same PEM files, so `config.ddsPortalUrl` can be HTTPS.
A cert-manager client Certificate for `pem.identitySecretName` needs `privateKey.encoding: PKCS8`
and `usages: [client auth]`; the chart's own `certificate` sets PKCS8 by default. The chart fails
to render when the selected mode lacks a Secret. Store passwords reach the JVM through the
environment, not the chart values.

# References

**[OGF NSF]** Guy Roberts, et al. “OGF Network Service Framework v2.0”, Group Working Draft (GWD), candidate Recommendation Proposed (R-P), January 28, 2014.

**[OGF NSI-CS]** Guy Roberts, et al. “OGF NSI Connection Service v2.0”, Group Working Draft (GWD), candidate Recommendation Proposed (R-P), January 12, 2014.

**[OGF NSI-TS]** Jeroen van der Ham, GWD-R-P Network Service Interface Topology Representation, Group Working Draft (GWD), candidate Recommendations Proposed (R-P), January 2013.

**[OGF NSI-DS]** John MacAuley, et al. “Network Service Interface Discovery Protocol v1.0”, Group Working Draft (GWD), candidate Recommendation Proposed (R-P), February 18, 2014.

**[OGF NSI-DS]** John MacAuley, et al. “Network Service Interface NSA Description Document v1.0”, Group Working Draft (GWD), candidate Recommendation Proposed (R-P), February 16, 2014.

**[OGF NML]** OGF GFD.206: Network Markup Language Base Schema version 1, http://www.gridforum.org/documents/GFD.206.pdf
