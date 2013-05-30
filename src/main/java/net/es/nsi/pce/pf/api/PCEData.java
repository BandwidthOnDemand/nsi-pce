package net.es.nsi.pce.pf.api;


import net.es.nsi.pce.pf.api.cons.Constraint;
import net.es.nsi.pce.pf.api.topo.Topology;

import java.util.HashSet;
import java.util.Set;

public class PCEData {
    private Path path;
    private Set<Constraint> constraints = new HashSet<Constraint>();
    private Topology topo;

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public Set<Constraint> getConstraints() {
        return constraints;
    }

    public Topology getTopo() {
        return topo;
    }

    public void setTopo(Topology topo) {
        this.topo = topo;
    }
}
