package net.es.nsi.pce.pf.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Path {
    private List<PathSegment> pathSegments = new ArrayList<>();

    public Path() {
    }

    public Path(PathSegment... pathSegments) {
        this.pathSegments.addAll(Arrays.asList(pathSegments));
    }

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
