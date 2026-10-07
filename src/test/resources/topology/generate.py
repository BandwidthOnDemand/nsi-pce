#!/usr/bin/env python3
"""Generate the synthetic DDS collections the path-finding tests run against.

Each collection is described below as domains, their ports and VLAN ranges, and which ports face
each other. Running this script rewrites the collection XML files next to it; commit both.

    python3 src/test/resources/topology/generate.py

Documents are embedded as plain XML in CDATA (8bit, text/plain) rather than gzip+base64, so a
fixture can be read and diffed in review. They carry no XML declaration, which may not follow the
whitespace before them. Nothing here describes a real network.
"""

from dataclasses import dataclass, field
from pathlib import Path

HERE = Path(__file__).parent
VERSION = "2026-01-01T00:00:00Z"
EXPIRES = "2099-01-01T00:00:00Z"
EVTS = "http://services.ogf.org/nsi/2013/12/descriptions/EVTS.A-GOLE"
AGGREGATOR = "urn:ogf:network:aggregator.example.net:2026:nsa"


@dataclass(frozen=True)
class Port:
    name: str
    vlans: str
    peer: tuple[str, str] | None = None  # (domain, port) on the far side of the link


@dataclass(frozen=True)
class Domain:
    name: str
    label_swapping: bool
    ports: list[Port] = field(default_factory=list)

    @property
    def network(self) -> str:
        return f"urn:ogf:network:{self.name}.example.net:2026:topology"

    @property
    def nsa(self) -> str:
        return f"urn:ogf:network:{self.name}.example.net:2026:nsa"


def port_id(domain: str, port: str) -> str:
    return f"urn:ogf:network:{domain}.example.net:2026:topology:{port}"


def port_group(domain: Domain, port: Port, direction: str) -> str:
    """A unidirectional port with its VLANs and, for a link, the far end it is an alias of."""
    far = {"in": "out", "out": "in"}[direction]
    alias = (
        f"""
                <nml:Relation type="http://schemas.ogf.org/nml/2013/05/base#isAlias">
                    <nml:PortGroup id="{port_id(*port.peer)}:{far}"/>
                </nml:Relation>"""
        if port.peer
        else ""
    )
    return f"""
            <nml:PortGroup id="{port_id(domain.name, port.name)}:{direction}" encoding="http://schemas.ogf.org/nml/2012/10/ethernet">
                <nml:LabelGroup labeltype="http://schemas.ogf.org/nml/2012/10/ethernet#vlan">{port.vlans}</nml:LabelGroup>{alias}
            </nml:PortGroup>"""


def topology_document(domain: Domain) -> str:
    def refs(direction: str) -> str:
        return "".join(
            f'\n                    <nml:PortGroup id="{port_id(domain.name, p.name)}:{direction}"/>' for p in domain.ports
        )

    bidirectional = "".join(
        f"""
    <nml:BidirectionalPort id="{port_id(domain.name, p.name)}">
        <nml:PortGroup id="{port_id(domain.name, p.name)}:in"/>
        <nml:PortGroup id="{port_id(domain.name, p.name)}:out"/>
    </nml:BidirectionalPort>"""
        for p in domain.ports
    )
    sd = f"{domain.network}:sd:EVTS.A-GOLE"
    return f"""<nml:Topology xmlns:nml="http://schemas.ogf.org/nml/2013/05/base#" xmlns:sd="http://schemas.ogf.org/nsi/2013/12/services/definition" id="{domain.network}" version="{VERSION}">
    <nml:name>{domain.name}</nml:name>{bidirectional}
    <sd:serviceDefinition id="{sd}">
        <name>EVTS.A-GOLE</name>
        <serviceType>{EVTS}</serviceType>
    </sd:serviceDefinition>
    <nml:Relation type="http://schemas.ogf.org/nml/2013/05/base#hasService">
        <nml:SwitchingService id="{domain.network}:switch:EVTS.A-GOLE" encoding="http://schemas.ogf.org/nml/2012/10/ethernet" labelSwapping="{str(domain.label_swapping).lower()}" labelType="http://schemas.ogf.org/nml/2012/10/ethernet#vlan">
            <nml:Relation type="http://schemas.ogf.org/nml/2013/05/base#hasInboundPort">{refs("in")}
            </nml:Relation>
            <nml:Relation type="http://schemas.ogf.org/nml/2013/05/base#hasOutboundPort">{refs("out")}
            </nml:Relation>
            <sd:serviceDefinition id="{sd}"/>
        </nml:SwitchingService>
    </nml:Relation>
    <nml:Relation type="http://schemas.ogf.org/nml/2013/05/base#hasInboundPort">{"".join(port_group(domain, p, "in") for p in domain.ports)}
    </nml:Relation>
    <nml:Relation type="http://schemas.ogf.org/nml/2013/05/base#hasOutboundPort">{"".join(port_group(domain, p, "out") for p in domain.ports)}
    </nml:Relation>
</nml:Topology>
"""


def nsa_document(nsa_id: str, name: str, role: str, networks: list[str], peers: list[str]) -> str:
    network_ids = "".join(f"\n    <networkId>{n}</networkId>" for n in networks)
    peers_with = "".join(f'\n    <peersWith role="RA">{p}</peersWith>' for p in peers)
    return f"""<nsa:nsa xmlns:nsa="http://schemas.ogf.org/nsi/2014/02/discovery/nsa" id="{nsa_id}" version="{VERSION}">
    <name>{name}</name>{network_ids}
    <interface>
        <type>application/vnd.ogf.nsi.cs.v2.provider+soap</type>
        <href>https://{name}.example.net/nsi/provider</href>
    </interface>
    <feature type="vnd.ogf.nsi.cs.v2.role.{role}"/>{peers_with}
</nsa:nsa>
"""


def dds_document(doc_id: str, nsa_id: str, doc_type: str, body: str) -> str:
    return f"""        <ns2:document id="{doc_id}" version="{VERSION}" expires="{EXPIRES}">
            <nsa>{nsa_id}</nsa>
            <type>{doc_type}</type>
            <content contentType="text/plain" contentTransferEncoding="8bit"><![CDATA[{body}]]></content>
        </ns2:document>
"""


def collection(domains: list[Domain]) -> str:
    """The aggregator's own NSA document is local; the domains' NSA and topology documents are not."""
    aggregator = nsa_document(AGGREGATOR, "aggregator", "aggregator", [], [d.nsa for d in domains])
    documents = "".join(
        dds_document(d.nsa, d.nsa, "vnd.ogf.nsi.nsa.v1+xml", nsa_document(d.nsa, d.name, "uPA", [d.network], []))
        + dds_document(d.network, d.nsa, "vnd.ogf.nsi.topology.v2+xml", topology_document(d))
        for d in domains
    )
    return f"""<?xml version="1.0" encoding="UTF-8"?>
<!-- Generated by generate.py; edit that and rerun instead of editing this file. -->
<ns2:collection xmlns:ns2="http://schemas.ogf.org/nsi/2014/02/discovery/types">
    <ns2:local>
{dds_document(AGGREGATOR, AGGREGATOR, "vnd.ogf.nsi.nsa.v1+xml", aggregator)}    </ns2:local>
    <ns2:documents>
{documents}    </ns2:documents>
</ns2:collection>
"""


def link(a: tuple[str, str], b: tuple[str, str], vlans: str) -> tuple[Port, Port]:
    return Port(a[1], vlans, b), Port(b[1], vlans, a)


# A triangle where B cannot swap labels. A path from A into B that is pinned to one VLAN on the
# A-B link can only reach a different VLAN at b-edge by leaving B for C, swapping there, and coming
# back: the loop the uPAs refuse. C is the only domain that can swap between its two links into B.
ab, ba = link(("domain-a", "a-to-b"), ("domain-b", "b-to-a"), "1000-1099")
bc, cb = link(("domain-b", "b-to-c"), ("domain-c", "c-to-b"), "1000-1099")
ac, ca = link(("domain-a", "a-to-c"), ("domain-c", "c-to-a"), "1000-1099")
VLAN_LOOP = [
    Domain("domain-a", label_swapping=True, ports=[Port("a-edge", "1000-1099"), ab, ac]),
    Domain("domain-b", label_swapping=False, ports=[Port("b-edge", "1000-1099"), ba, bc]),
    Domain("domain-c", label_swapping=True, ports=[Port("c-edge", "1000-1099"), ca, cb]),
]

if __name__ == "__main__":
    (HERE / "vlan-loop.xml").write_text(collection(VLAN_LOOP))
