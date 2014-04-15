package net.es.nsi.pce.pf;

import net.es.nsi.pce.pf.api.PCEData;
import net.es.nsi.pce.pf.api.PCEModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * The Authentication and Authorization Path Computation module.  At the moment
 * this module looks up NSA credentials relating to the networks involved in
 * the path result.
 *
 * @author hacksaw
 */
public class AuthPCE implements PCEModule {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public PCEData apply(PCEData pceData) {
        return pceData;
    }
}
