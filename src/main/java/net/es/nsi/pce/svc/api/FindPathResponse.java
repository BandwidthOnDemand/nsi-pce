package net.es.nsi.pce.svc.api;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;

@XmlRootElement(name = "main")
public class FindPathResponse {
    @XmlElement(name = "correlation-id")
    public String correlationId;
    @XmlElement(name = "status")
    public FindPathStatus status;
    @XmlElement(name = "message")
    public String message;
    @XmlElement(name = "path")
    public ArrayList<PathObject> path = new ArrayList<PathObject>();


}

