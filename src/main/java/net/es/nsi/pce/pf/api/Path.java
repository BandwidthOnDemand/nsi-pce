package net.es.nsi.pce.pf.api;

import java.util.ArrayList;
import java.util.List;

public class Path {
    private List<PathSegment> pathSegments = new ArrayList<>();

    /**
     * @return the pathSegments
     */
    public List<PathSegment> getPathSegments() {
        return pathSegments;
    }

    /**
     * @param pathSegments the pathSegments to set
     */
    public void setPathSegments(List<PathSegment> pathSegments) {
        this.pathSegments = pathSegments;
    }
}
