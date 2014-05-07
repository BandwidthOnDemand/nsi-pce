/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.topology.model;

/**
 *
 * @author hacksaw
 */
public class SimpleStpPair {
    private SimpleStp src;
    private SimpleStp dst;

    public SimpleStpPair(SimpleStp src, SimpleStp dst) {
        this.src = src;
        this.dst = dst;
    }

    /**
     * @return the src
     */
    public SimpleStp getSrc() {
        return src;
    }

    /**
     * @param src the src to set
     */
    public void setSrc(SimpleStp src) {
        this.src = src;
    }

    /**
     * @return the dst
     */
    public SimpleStp getDst() {
        return dst;
    }

    /**
     * @param dst the dst to set
     */
    public void setDst(SimpleStp dst) {
        this.dst = dst;
    }
}
