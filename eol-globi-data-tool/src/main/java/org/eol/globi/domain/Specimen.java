package org.eol.globi.domain;

import org.eol.globi.service.Term;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import java.util.List;

public class Specimen extends NodeBacked {

    public static final String LENGTH_IN_MM = "lengthInMm";
    public static final String VOLUME_IN_ML = "volumeInMilliLiter";
    public static final String STOMACH_VOLUME_ML = "stomachVolumeInMilliLiter";
    public static final String DATE_IN_UNIX_EPOCH = "dateInUnixEpoch";
    public static final String LIFE_STAGE_LABEL = "lifeStageLabel";
    public static final String LIFE_STAGE_ID = "lifeStageId";
    public static final String PHYSIOLOGICAL_STATE_LABEL = "physiologicalStateLabel";
    private static final String PHYSIOLOGICAL_STATE_ID = "physiologicalStateId";
    public static final String BODY_PART_LABEL = "bodyPartLabel";
    public static final String BODY_PART_ID = "bodyPartId";

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
            setPropertyWithTx(LENGTH_IN_MM, lengthInMm);
        }
    }

    public void setVolumeInMilliLiter(Double volumeInMm3) {
        setPropertyWithTx(VOLUME_IN_ML, volumeInMm3);
    }

    public void setStomachVolumeInMilliLiter(Double volumeInMilliLiter) {
        setPropertyWithTx(STOMACH_VOLUME_ML, volumeInMilliLiter);
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

    public void setLifeStage(List<Term> lifeStages) {
        if (lifeStages != null && lifeStages.size() > 0) {
            setLifeStage(lifeStages.get(0));
        }
    }

    public void setLifeStage(Term lifeStage) {
        setPropertyWithTx(Specimen.LIFE_STAGE_LABEL, lifeStage.getName());
        setPropertyWithTx(Specimen.LIFE_STAGE_ID, lifeStage.getId());
    }

    public void setPhysiologicalState(Term physiologicalState) {
        setPropertyWithTx(Specimen.PHYSIOLOGICAL_STATE_LABEL, physiologicalState.getName());
        setPropertyWithTx(Specimen.PHYSIOLOGICAL_STATE_ID, physiologicalState.getId());
    }

    public void setBodyPart(Term bodyPart) {
        setPropertyWithTx(Specimen.BODY_PART_LABEL, bodyPart.getName());
        setPropertyWithTx(Specimen.BODY_PART_ID, bodyPart.getId());
    }
}