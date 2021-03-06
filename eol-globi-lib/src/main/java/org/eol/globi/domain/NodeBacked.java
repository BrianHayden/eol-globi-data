package org.eol.globi.domain;

import org.eol.globi.util.NodeUtil;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

public class NodeBacked {

    private final Node underlyingNode;

    public NodeBacked(Node node) {
        this.underlyingNode = node;
    }

    public Node getUnderlyingNode() {
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

    public Relationship createRelationshipTo(NodeBacked endNode, RelType relType) {
        Transaction tx = getUnderlyingNode().getGraphDatabase().beginTx();
        Relationship rel = null;
        try {
            rel = createRelationshipToNoTx(endNode, relType);
            tx.success();
        } finally {
            tx.finish();
        }
        return rel;
    }

    protected Relationship createRelationshipToNoTx(NodeBacked endNode, RelType relType) {
        Relationship rel = null;
        if (!this.equals(endNode)) {
            Iterable<Relationship> relationships = getUnderlyingNode().getRelationships(Direction.OUTGOING, relType);
            boolean hasRelationship = false;
            while (relationships.iterator().hasNext() && !hasRelationship) {
                Relationship relationship = relationships.iterator().next();
                hasRelationship = endNode.equals(relationship.getEndNode());
            }
            if (!hasRelationship) {
                rel = getUnderlyingNode().createRelationshipTo(endNode.getUnderlyingNode(), relType);
            }
        }
        return rel;
    }

    public long getNodeID() {
        return getUnderlyingNode().getId();
    }

    protected void setPropertyWithTx(String propertyName, Object propertyValue) {
        if (propertyValue != null) {
            Transaction transaction = getUnderlyingNode().getGraphDatabase().beginTx();
            try {
                getUnderlyingNode().setProperty(propertyName, propertyValue);
                transaction.success();
            } finally {
                transaction.finish();
            }
        }
    }

    protected Object getPropertyValueOrNull(String propertyName) {
        return getUnderlyingNode().hasProperty(propertyName) ? getUnderlyingNode().getProperty(propertyName) : null;
    }

    protected String getPropertyStringValueOrNull(String propertyName) {
        Node node = getUnderlyingNode();
        return NodeUtil.getPropertyStringValueOrNull(node, propertyName);
    }

    public void setExternalId(String externalId) {
        setPropertyWithTx(PropertyAndValueDictionary.EXTERNAL_ID, externalId);
    }

    public String getExternalId() {
        String propertyName = PropertyAndValueDictionary.EXTERNAL_ID;
        Object propertyValueOrNull = getPropertyValueOrNull(propertyName);
        return propertyValueOrNull == null ? null : (String) propertyValueOrNull;
    }

}
