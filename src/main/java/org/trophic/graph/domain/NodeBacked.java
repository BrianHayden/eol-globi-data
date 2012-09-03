package org.trophic.graph.domain;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

public class NodeBacked {

    protected final static String TYPE = "type";
    private final Node underlyingNode;

    public NodeBacked(Node node) {
        this.underlyingNode = node;
    }

    protected Node getUnderlyingNode() {
        return underlyingNode;
    }


    @Override
    public int hashCode() {
        return underlyingNode.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof NodeBacked &&
                underlyingNode.equals(((NodeBacked) o).getUnderlyingNode());
    }

    public void createRelationshipTo(NodeBacked nodeBacked, RelTypes relType) {
        Transaction tx = getUnderlyingNode().getGraphDatabase().beginTx();
        try {
            if (!this.equals(nodeBacked)) {
                Relationship rel = getFirstIncomingRelationshipOfType(nodeBacked, relType);
                if (rel == null) {
                    getUnderlyingNode().createRelationshipTo(nodeBacked.getUnderlyingNode(), relType);
                }
                tx.success();
            }
        } finally {
            tx.finish();
        }
    }

    private Relationship getFirstIncomingRelationshipOfType(NodeBacked otherTaxon, RelTypes relType) {
        Node otherNode = otherTaxon.getUnderlyingNode();
        for (Relationship rel : getUnderlyingNode().getRelationships(Direction.INCOMING, relType)) {
            if (rel.getOtherNode(getUnderlyingNode()).equals(otherNode)) {
                return rel;
            }
        }
        return null;
    }

    public long getNodeID() {
        return getUnderlyingNode().getId();
    }
}
