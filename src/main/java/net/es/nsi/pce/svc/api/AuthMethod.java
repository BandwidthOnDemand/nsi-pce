package net.es.nsi.pce.svc.api;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "method")
public enum AuthMethod {
    NONE,
    BASIC,
    OAUTH2
}