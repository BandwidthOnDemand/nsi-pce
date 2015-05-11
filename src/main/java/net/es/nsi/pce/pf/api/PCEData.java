package net.es.nsi.pce.pf.api;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.es.nsi.pce.pf.api.cons.AttrConstraints;
import net.es.nsi.pce.pf.api.cons.Constraint;
import net.es.nsi.pce.topology.model.NsiTopology;

/**
 * Defines the data passed into and between Path Computation Modules.
 *
 * path - contains the computed path so far.  Each PC module can further
 * refine the path for the next PC module in the sequence.
 *
 * constraints - contains list of constraints to guide path finding.  Constraints
 * will contain user requested parameters such as source and destination STP,
 * as well as vlan tags, ERO, etc.  It can also contain additional constraints
 * added by the system.
 *
 * topology - the current network topology utilized for pathfinding.
 *
 * @author hacksaw
 */
public class PCEData {
    private Path path = new Path();
    private final Set<Constraint> constraints = new HashSet<>();
    private NsiTopology topology = new NsiTopology();
    private List<String> trace;

    public PCEData() {
    }

    public PCEData(Constraint... constraints) {
        this.constraints.addAll(Arrays.asList(constraints));
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public Set<Constraint> getConstraints() {
        return constraints;
    }

    public AttrConstraints getAttrConstraints() {
        return new AttrConstraints(constraints);
    }

    public NsiTopology getTopology() {
        return topology;
    }

    public void setTopology(NsiTopology topology) {
        this.topology = topology;
    }

    public boolean addConstraint(Constraint constraint) {
        return constraints.add(constraint);
    }

    public boolean addConstraints(Set<Constraint> constraints) {
        return this.constraints.addAll(constraints);
    }

    public List<String> getTrace() {
        return trace;
    }

    public void setTrace(List<String> trace) {
        this.trace = trace;
    }
}
