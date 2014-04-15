package net.es.nsi.pce.pf;

import net.es.nsi.pce.pf.api.PCEData;
import net.es.nsi.pce.pf.api.PCEModule;

import java.util.List;

/**
 * The SequentialPCE will take a list of Path Computation Modules and
 * sequentially invoke passing the supplied pceData.  The results of each
 * module will be passed into the next module to be invoked.
 *
 * The SequentialPCE is a singleton bean instantiated using Spring.
 *
 * @author hacksaw
 */
public class SequentialPCE implements PCEModule {

    // The list of Path Computation Modules to apply the path computation process.
    private List<PCEModule> moduleList;

    /**
     * Apply the list of Path Computation Modules to the supplied data.
     *
     * @param pceData The path request data specified as constraints and a set of already resolved path components.
     * @return The path resulting from the sequential application of the Path Computation Modules.
     * @throws Exception
     */
    @Override
    public PCEData apply(PCEData pceData) {
        for (PCEModule mod : moduleList) {
            pceData = mod.apply(pceData);
        }
        return pceData;
    }

    /**
     * Get the list of Path Computation Modules.
     *
     * @return List of Path Computation Modules.
     */
    public List<PCEModule> getModuleList() {
        return moduleList;
    }

    /**
     * Set the list of Path Computation Modules.
     * @param moduleList The list of Path Computation Modules.
     */
    public void setModuleList(List<PCEModule> moduleList) {
        this.moduleList = moduleList;
    }
}
