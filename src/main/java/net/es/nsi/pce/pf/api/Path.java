package net.es.nsi.pce.pf.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Path {
    private final List<PathSegment> pathSegments = new ArrayList<>();

    public Path(PathSegment... pathSegments) {
        this.pathSegments.addAll(Arrays.asList(pathSegments));
    }

    public Path(List<PathSegment> pathSegments) {
        this.pathSegments.addAll(pathSegments);
    }

    public List<PathSegment> getPathSegments() {
        return pathSegments;
    }

}
