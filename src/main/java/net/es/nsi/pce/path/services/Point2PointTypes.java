package net.es.nsi.pce.path.services;

/**
 *
 * @author hacksaw
 */
public class Point2PointTypes {
    public static final String NAMESPACE_P2P = "http://schemas.ogf.org/nsi/2013/12/services/point2point";
    public static final String P2PS = "http://schemas.ogf.org/nsi/2013/12/services/point2point#p2ps";
    public static final String CAPACITY = "http://schemas.ogf.org/nsi/2013/12/services/point2point#p2ps/capacity";
    public static final String DIRECTIONALITY = "http://schemas.ogf.org/nsi/2013/12/services/point2point#p2ps/directionality";
    public static final String SYMMETRICPATH = "http://schemas.ogf.org/nsi/2013/12/services/point2point#p2ps/symmetricPath";
    public static final String SOURCESTP = "http://schemas.ogf.org/nsi/2013/12/services/point2point#p2ps/sourceSTP";
    public static final String DESTSTP = "http://schemas.ogf.org/nsi/2013/12/services/point2point#p2ps/destSTP";
    public static final String ERO = "http://schemas.ogf.org/nsi/2013/12/services/point2point#p2ps/ero";

    private static final Namespace p2ps = new Namespace(NAMESPACE_P2P, P2PS, "p2ps");
    private static final Namespace capacity = new Namespace(P2PS, CAPACITY, "capacity");
    private static final Namespace directionality = new Namespace(P2PS, DIRECTIONALITY, "directionality");
    private static final Namespace symmetricPath = new Namespace(P2PS, SYMMETRICPATH, "symmetricPath");
    private static final Namespace sourceStp = new Namespace(P2PS, SOURCESTP, "sourceSTP");
    private static final Namespace destStp = new Namespace(P2PS, DESTSTP, "destSTP");
    private static final Namespace ero = new Namespace(P2PS, ERO, "ero");

    /**
     * @return the p2ps
     */
    public static Namespace getP2ps() {
        return p2ps;
    }

    /**
     * @return the capacity
     */
    public static Namespace getCapacity() {
        return capacity;
    }

    /**
     * @return the directionality
     */
    public static Namespace getDirectionality() {
        return directionality;
    }

    /**
     * @return the symmetricPath
     */
    public static Namespace getSymmetricPath() {
        return symmetricPath;
    }

    /**
     * @return the sourceStp
     */
    public static Namespace getSourceStp() {
        return sourceStp;
    }

    /**
     * @return the destStp
     */
    public static Namespace getDestStp() {
        return destStp;
    }

    /**
     * @return the ero
     */
    public static Namespace getEro() {
        return ero;
    }
}
