package org.eol.globi.domain;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

public class Specimen extends NodeBacked {

    public static final String LENGTH_IN_MM = "lengthInMm";
    public static final String VOLUME_IN_ML = "volumeInMilliLiter";
    public static final String STOMACH_VOLUME_ML = "stomachVolumeInMilliLiter";
    public static final String DATE_IN_UNIX_EPOCH = "dateInUnixEpoch";

    public Specimen(Node node) {
        super(node);
    }

    public Specimen(Node node, Double lengthInMm) {
        this(node);
        getUnderlyingNode().setProperty(TYPE, Specimen.class.getSimpleName());
        if (null != lengthInMm) {
            getUnderlyingNode().setProperty(LENGTH_IN_MM, lengthInMm);
        }
    }

    @Override
    public String toString() {
        return String.format("[%s]", getUnderlyingNode().getProperty(TYPE));
    }

    public Iterable<Relationship> getStomachContents() {
        return getUnderlyingNode().getRelationships(InteractType.ATE, Direction.OUTGOING);
    }

    public Location getSampleLocation() {
        Relationship singleRelationship = getUnderlyingNode().getSingleRelationship(RelTypes.COLLECTED_AT, Direction.OUTGOING);
        return singleRelationship == null ? null : new Location(singleRelationship.getEndNode());
    }

    public void ate(Specimen specimen) {
        createRelationshipTo(specimen, InteractType.ATE);
    }

    public void caughtIn(Location sampleLocation) {
        if (null != sampleLocation) {
            createRelationshipTo(sampleLocation, RelTypes.COLLECTED_AT);
        }
    }

    public Season getSeason() {
        Relationship singleRelationship = getUnderlyingNode().getSingleRelationship(RelTypes.CAUGHT_DURING, Direction.OUTGOING);
        return singleRelationship == null ? null : new Season(singleRelationship.getEndNode());
    }

    public void caughtDuring(Season season) {
        createRelationshipTo(season, RelTypes.CAUGHT_DURING);
    }

    public Double getLengthInMm() {
        return (Double) getUnderlyingNode().getProperty(LENGTH_IN_MM);
    }

    public Iterable<Relationship> getClassifications() {
        return getUnderlyingNode().getRelationships(Direction.OUTGOING, RelTypes.CLASSIFIED_AS);
    }

    public void classifyAs(Taxon taxon) {
        createRelationshipTo(taxon, RelTypes.CLASSIFIED_AS);
    }

    public void setLengthInMm(Double lengthInMm) {
        if (lengthInMm != null) {
            Transaction transaction = getUnderlyingNode().getGraphDatabase().beginTx();
            try {
                getUnderlyingNode().setProperty(LENGTH_IN_MM, lengthInMm);
                transaction.success();
            } finally {
                transaction.finish();
            }
        }
    }

    public void setVolumeInMilliLiter(Double volumeInMm3) {
        Transaction transaction = getUnderlyingNode().getGraphDatabase().beginTx();
        try {
            getUnderlyingNode().setProperty(VOLUME_IN_ML, volumeInMm3);
            transaction.success();
        } finally {
            transaction.finish();
        }
    }

    public void setStomachVolumeInMilliLiter(Double volumeInMilliLiter) {
        Transaction transaction = getUnderlyingNode().getGraphDatabase().beginTx();
        try {
            getUnderlyingNode().setProperty(STOMACH_VOLUME_ML, volumeInMilliLiter);
            transaction.success();
        } finally {
            transaction.finish();
        }
    }

    public void interactsWith(Specimen recipientSpecimen, RelType relType) {
        createRelationshipTo(recipientSpecimen, relType);
    }

    public String getOriginalTaxonDescription() {
        Relationship singleRelationship = getUnderlyingNode().getSingleRelationship(RelTypes.ORIGINALLY_DESCRIBED_AS, Direction.OUTGOING);
        return singleRelationship == null ? null : new Taxon(singleRelationship.getEndNode()).getName();
    }

    public void setOriginalTaxonDescription(String taxonName) {
        Transaction transaction = getUnderlyingNode().getGraphDatabase().beginTx();
        try {
            Taxon taxon = new Taxon(getUnderlyingNode().getGraphDatabase().createNode(), taxonName);
            createRelationshipTo(taxon, RelTypes.ORIGINALLY_DESCRIBED_AS);
            transaction.success();
        } finally {
            transaction.finish();
        }
    }
}