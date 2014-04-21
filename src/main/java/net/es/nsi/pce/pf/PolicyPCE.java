package net.es.nsi.pce.pf;


import net.es.nsi.pce.pf.api.PCEData;
import net.es.nsi.pce.pf.api.PCEModule;

public class PolicyPCE implements PCEModule {

    @Override
    public PCEData apply(PCEData pceData) {
        return pceData;
    }
}
