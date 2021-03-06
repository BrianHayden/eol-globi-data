package org.eol.globi.data;

import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.TaxonNode;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class StudyImporterForCookTest extends GraphDBTestCase {

    @Test
    public void importFirstFewLines() throws IOException, NodeFactoryException, StudyImporterException {
        String firstFiveLines = "\"Date\",\"Individual\",\"Fish Length\",\"Infected\",\"Iso 1\",\"Iso 2\",\"Iso 3\",\"Iso 4 \",\"Iso 5\",\"Iso #\",,\n" +
                "8/10/2010,22,15.6,1,\"NA\",\"NA\",0,0,0,2,,\"Notes: All fish collected were Atlantic Croaker (Micropogonias undulatus) and measured in total length (cm). Isopods collected were Cymothoa excisa. For the Infected column 1= infected and 0= not infected. Numbers for the isopods are total length in cm and the Iso# represents the number of isopods found per fish. \"\n" +
                "8/10/2010,1,14.7,1,1.6,0.67,0,0,0,2,,\n" +
                "8/10/2010,5,14.2,1,1.53,0.7,0,0,0,2,,\n" +
                "8/10/2010,2,13.2,1,1.42,0.71,0.52,0.45,0,4,,\n";


        StudyImporterForCook importer = new StudyImporterForCook(new TestParserFactory(firstFiveLines), nodeFactory);
        Study study = importer.importStudy();

        TaxonNode hostTaxon = nodeFactory.findTaxonByName("Micropogonias undulatus");
        assertThat(hostTaxon, is(notNullValue()));
        TaxonNode parasiteTaxon = nodeFactory.findTaxonByName("Cymothoa excisa");
        assertThat(parasiteTaxon, is(notNullValue()));
        assertThat("missing location", nodeFactory.findLocation(27.85, -(97.0 + 8.0 / 60.0), -3.0), is(notNullValue()));

        int count = 0;
        boolean foundFirstHost = false;
        Iterable<Relationship> specimens = study.getSpecimens();
        for (Relationship collected_rel : specimens) {
            assertThat(collected_rel.getProperty(Specimen.DATE_IN_UNIX_EPOCH), is(notNullValue()));
            Node specimen = collected_rel.getEndNode();
            if (specimen.hasProperty(Specimen.LENGTH_IN_MM)) {
                Object property = specimen.getProperty(Specimen.LENGTH_IN_MM);
                if (new Double(156.0).equals(property)) {
                    assertTaxonClassification(specimen, hostTaxon.getUnderlyingNode());
                    foundFirstHost = true;
                    Iterable<Relationship> parasiteRel = specimen.getRelationships(Direction.INCOMING, InteractType.PARASITE_OF);
                    for (Relationship relationship : parasiteRel) {
                        Node parasite = relationship.getStartNode();
                        assertThat(parasite.hasProperty(Specimen.LENGTH_IN_MM), is(false));
                        assertTaxonClassification(parasite, parasiteTaxon.getUnderlyingNode());
                    }
                }
            }
            count++;
        }

        assertThat(count, is(14));
        assertThat(foundFirstHost, is(true));

    }

    @Test
    public void importAll() throws StudyImporterException {
        StudyImporterForCook importer = new StudyImporterForCook(new ParserFactoryImpl(), nodeFactory);
        Study study = importer.importStudy();
        Iterable<Relationship> specimens = study.getSpecimens();
        int count = 0;
        for (Relationship specimen : specimens) {
            count++;
        }

        assertThat(count, is(1372));
    }

    private void assertTaxonClassification(Node parasite, Node underlyingNode) {
        Iterable<Relationship> classifiedAsRels = parasite.getRelationships(Direction.OUTGOING, RelTypes.CLASSIFIED_AS);
        for (Relationship classifiedAsRel : classifiedAsRels) {
            assertThat(classifiedAsRel.getEndNode(), is(underlyingNode));
        }
    }


}
