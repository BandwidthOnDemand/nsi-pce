package net.es.nsi.pce.pf;

import net.es.nsi.pce.pf.api.PCEData;
import net.es.nsi.pce.pf.api.PCEModule;

import java.util.List;

public class SequentialPCE implements PCEModule {
    private List<PCEModule> moduleList;
    public PCEData apply(PCEData pceData) throws Exception {
        for (PCEModule mod : moduleList) {
            pceData = mod.apply(pceData);
        }
        return pceData;
    }

    public List<PCEModule> getModuleList() {
        return moduleList;
    }

    public void setModuleList(List<PCEModule> moduleList) {
        this.moduleList = moduleList;
    }
}
