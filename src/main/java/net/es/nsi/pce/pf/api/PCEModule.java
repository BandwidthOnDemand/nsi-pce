package net.es.nsi.pce.pf.api;

import javax.ws.rs.WebApplicationException;


public interface PCEModule {
    public PCEData apply(PCEData pceData) throws WebApplicationException;
}
