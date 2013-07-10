package org.eol.globi.export;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.LifeStage;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.BodyPart;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.PhysiologicalState;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.junit.Test;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.io.StringWriter;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class EOLExporterMeasurementOrFactTest extends GraphDBTestCase {

    @Test
    public void exportMissingLength() throws IOException, NodeFactoryException, ParseException {
        createTestData(null);

        String expected =
                "\nglobi:occur:stomach_volume:3,globi:occur:3,yes,,stomach volume,666.0,http://purl.obolibrary.org/obo/UO_0000098,,1992-03-30T08:00:00Z,,,,myStudy,,"
                        + "\nglobi:occur:volume:6,globi:occur:6,yes,,volume,124.0,http://purl.obolibrary.org/obo/UO_0000098,,1992-03-30T08:00:00Z,,,,myStudy,,"
                        + "\nglobi:occur:volume:8,globi:occur:8,yes,,volume,18.0,http://purl.obolibrary.org/obo/UO_0000098,,1992-03-30T08:00:00Z,,,,myStudy,,";


        Study myStudy1 = nodeFactory.findStudy("myStudy");

        StringWriter row = new StringWriter();

        new EOLExporterMeasurementOrFact().exportStudy(myStudy1, row, false);

        assertThat(row.getBuffer().toString(), equalTo(expected));
    }

    private void createTestData(Double length) throws NodeFactoryException, ParseException {
        Study myStudy = nodeFactory.createStudy("myStudy");
        Specimen specimen = nodeFactory.createSpecimen("Homo sapiens");
        specimen.setStomachVolumeInMilliLiter(666.0);
        specimen.setLifeStage(LifeStage.JUVENILE);
        specimen.setPhysiologicalState(PhysiologicalState.DIGESTATE);
        specimen.setBodyPart(BodyPart.BONE);
        Relationship collected = myStudy.collected(specimen);
        Transaction transaction = myStudy.getUnderlyingNode().getGraphDatabase().beginTx();
        try {
            collected.setProperty(Specimen.DATE_IN_UNIX_EPOCH, getUTCTestTime());
            transaction.success();
        } finally {
            transaction.finish();
        }
        Specimen otherSpecimen = nodeFactory.createSpecimen("Canis lupus");
        otherSpecimen.setVolumeInMilliLiter(124.0);

        specimen.ate(otherSpecimen);

        otherSpecimen = nodeFactory.createSpecimen("Canis lupus");
        otherSpecimen.setVolumeInMilliLiter(18.0);
        specimen.ate(otherSpecimen);
        if (null != length) {
            specimen.setLengthInMm(length);
        }

        Location location = nodeFactory.getOrCreateLocation(123.0, 345.9, -60.0);
        specimen.caughtIn(location);
    }


    @Test
    public void darwinCoreMetaTable() throws IOException {
        EOLExporterMeasurementOrFact exporter = new EOLExporterMeasurementOrFact();
        StringWriter writer = new StringWriter();
        exporter.exportDarwinCoreMetaTable(writer, "testtest.csv");

        assertThat(writer.toString(), is(exporter.getMetaTablePrefix() + "testtest.csv" + exporter.getMetaTableSuffix()));
    }

}