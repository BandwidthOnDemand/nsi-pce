package net.es.nsi.pce.svc.api;

import javax.xml.bind.annotation.XmlElement;

public class AuthObject {
    @XmlElement(name = "method")
    public AuthMethod method;
    @XmlElement(name = "username")
    public String username;
    @XmlElement(name = "password")
    public String password;
    @XmlElement(name = "token")
    public String token;

}