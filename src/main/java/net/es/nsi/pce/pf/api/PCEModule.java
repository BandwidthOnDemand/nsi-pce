package net.es.nsi.pce.pf.api;

import jakarta.ws.rs.WebApplicationException;


public interface PCEModule {
    public PCEData apply(PCEData pceData) throws WebApplicationException;
}
