/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.pf;

import net.es.nsi.pce.path.jaxb.FindPathAlgorithmType;

/**
 *
 * @author hacksaw
 */
public class Algorithms {
    public static boolean contains(FindPathAlgorithmType algorithm) {
        for (FindPathAlgorithmType type : FindPathAlgorithmType.values()) {
            if (type.equals(algorithm)) {
                return true;
            }
        }
        return false;
    }
}
