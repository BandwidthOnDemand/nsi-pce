package net.es.nsi.pce.api;


import javax.xml.bind.annotation.XmlElement;
import java.util.ArrayList;

public class StpObject {
    @XmlElement(name = "network-id")
    public String networkId;
    @XmlElement(name = "local-id")
    public String localId;
    @XmlElement(name = "labels")
    public ArrayList<String> labels = new ArrayList<String>();

}
