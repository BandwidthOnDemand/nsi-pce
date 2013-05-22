package net.es.nsi.pce.api;


import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.Date;

@XmlRootElement(name = "main")
public class FindPathRequest {

    @XmlElement(name = "source-stp")
    public StpObject sourceStp;
    @XmlElement(name = "destination-stp")
    public StpObject destinationStp;
    @XmlElement(name = "start-time")
    public Date startTime;
    @XmlElement(name = "end-time")
    public Date endTime;
    @XmlElement(name = "bandwidth")
    public Long bandwidth;
    @XmlElement(name = "reply-to")
    public String replyTo;
    @XmlElement(name = "correlation-id")
    public String correlationId;
    @XmlElement(name = "algorithm")
    public FindPathAlgorithm algorithm;
    @XmlElement(name = "constraints")
    public ArrayList<String> constraints;

}
