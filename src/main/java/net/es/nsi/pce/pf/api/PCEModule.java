package net.es.nsi.pce.pf.api;


public interface PCEModule {
    public PCEData apply(PCEData pceData) throws Exception;
}
