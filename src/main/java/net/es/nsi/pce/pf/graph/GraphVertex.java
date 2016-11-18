/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.pf.graph;

import net.es.nsi.pce.pf.graph.SortedGraphObject;

/**
 *
 * @author hacksaw
 */
public class GraphVertex extends SortedGraphObject {
    public GraphVertex(String id) {
        super(id);
    }

    @Override
    public String toString() {
        return "GraphVertex=" + this.getId();
    }
}
