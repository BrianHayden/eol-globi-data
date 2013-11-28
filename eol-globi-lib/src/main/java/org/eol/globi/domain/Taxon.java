package org.eol.globi.domain;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import static org.eol.globi.domain.RelTypes.IS_A;

public class Taxon extends NamedNode {
    public static final String PATH = "path";
    public static final String COMMON_NAMES = "commonNames";

    public Taxon(Node node) {
        super(node);
    }

    public Taxon(Node node, String name) {
        this(node);
        setName(name);
    }

    public Node isA() {
        Relationship singleRelationship = getUnderlyingNode().getSingleRelationship(IS_A, Direction.OUTGOING);
        return singleRelationship == null ? null : singleRelationship.getEndNode();
    }

    public String getPath() {
        return getUnderlyingNode().hasProperty(PATH) ?
                (String) getUnderlyingNode().getProperty(PATH) : null;
    }

    public void setPath(String path) {
        if (path != null) {
            getUnderlyingNode().setProperty(PATH, path);
        }
    }

    public String getCommonNames() {
        return getUnderlyingNode().hasProperty(COMMON_NAMES) ?
                (String) getUnderlyingNode().getProperty(COMMON_NAMES) : null;
    }

    public void setCommonNames(String commonNames) {
        if (commonNames != null) {
            getUnderlyingNode().setProperty(COMMON_NAMES, commonNames);
        }
    }
}
