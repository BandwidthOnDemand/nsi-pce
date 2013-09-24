/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.pf;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SparseMultigraph;

/**
 *
 * @author hacksaw
 */
public class Test {
    public static void main(String[] args) {
        // Graph<V, E> where V is the type of the vertices
        // and E is the type of the edges
        Graph<Integer, String> g = new SparseMultigraph<Integer, String>();
        // Add some vertices. From above we defined these to be type Integer.
        g.addVertex((Integer)1);
        g.addVertex((Integer)2);
        g.addVertex((Integer)3);
        // Add some edges. From above we defined these to be of type String
        // Note that the default is for undirected edges.
        g.addEdge("Edge-A", 1, 2); // Note that Java 1.5 auto-boxes primitives
        g.addEdge("Edge-B", 2, 3);
        // Let's see what we have. Note the nice output from the
        // SparseMultigraph<V,E> toString() method
        System.out.println("The graph g = " + g.toString());
    }
}
