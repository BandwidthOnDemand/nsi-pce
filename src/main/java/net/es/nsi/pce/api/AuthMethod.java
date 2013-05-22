package net.es.nsi.pce.api;


import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "method")
public enum AuthMethod {
    NONE,
    BASIC,
    OAUTH2
}
