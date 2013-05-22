package net.es.nsi.pce.api;


import javax.xml.bind.annotation.XmlElement;

public class PathObject {
    @XmlElement(name = "source-stp")
    public StpObject sourceStp;
    @XmlElement(name = "destination-stp")
    public StpObject destinationStp;
    @XmlElement(name = "nsa")
    public String nsa;
    @XmlElement(name = "provider-url")
    public String providerUrl;
    @XmlElement(name = "auth")
    public AuthObject auth;
}
