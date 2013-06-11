package net.es.nsi.pce.pf;

import net.es.nsi.pce.pf.api.PCEData;
import net.es.nsi.pce.pf.api.PCEModule;

public class ComposerPCE implements PCEModule {
    public PCEData apply(PCEData pceData) throws Exception {
        AuthPCE authPCE = new AuthPCE();
        DijkstraPCE dijkstraPCE = new DijkstraPCE();

        PCEData authTrimmed = authPCE.apply(pceData);
        return dijkstraPCE.apply(authTrimmed);
    }
}
