package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Study;
import org.hamcrest.core.Is;
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.io.IOException;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertThat;

public class StudyImporterForRoopnarineTest extends GraphDBTestCase {

    @Test
    public void importLine() throws StudyImporterException, NodeFactoryException {

        // note that this test data is a modified version of the actual data - please don't use this data for actual analysis purposes
        final String trophicGuildsToSpeciesLookup = "\"Guild Number\",\"Taxa\",\"Fish Body Length\",\"Cayman Islands\",\"Cuba\",\"Jamaica\"\n" +
                "1,\"something something\",,,,\n" +
                "2,\"Skeletonema tropicum        \",,,,\n" +
                "3,\"Something else        \",,,,\n" +
                "11,\" Epinephelus striatus          \",122,\"x \",\"x\",\"x\"\n" +
                "12,\" Negaprion brevirostris        \",240,\"x \",\" \",\" \"\n" +
                "13,\" Carcharhinus perezi           \",300,\"x \",\"x\",\"x\"\n" +
                "14,\" Rhizoprionodon porosus        \",75,\"x \",\"x\",\" \"\n" +
                "14,\" Sphyrna tiburo                \",80,\"x \",\" \",\" \"\n" +
                "14,\" Galeocerdo cuvieri            \",500,\"  \",,";

        final String trophicInteractions = "\"Guild Number\",\"Guild Description\",\"Foraging Habitat\",\"Number of Prey\",\"Prey\"\n" +
                "1,\"Planktonic bacteria   \",\".\",0,\".\"\n" +
                "2,\"Phytoplankton         \",\".\",0,\".\"\n" +
                "3,\"Nanno-zooplankton     \",\".\",30,\"1, 2\"\n" +
                "4,\"Filamentous macroalgae\",\".\",0,\".\"\n" +
                "5,\"Sheet macroalgae      \",\".\",0,\".\"\n" +
                "6,\"Coarsely branched macroalgae                 \",\".\",0,\".\"\n" +
                "7,\"Jointed calcareous macroalgae                \",\".\",0,\".\"\n" +
                "8,\"Thick leathery macroalgae                    \",\".\",0,\".\"\n" +
                "9,\"Crustose coralline algae                     \",\".\",0,\".\"\n" +
                "10,\"Seagrasses            \",\".\",0,\".\"\n" +
                "11,\"Epibenthic sponges    \",\".\",30,\"1,  2\"\n" +
                "12,\"Endolithic sponges    \",\".\",30,\"1,   2\"\n" +
                "13,\"Ahermatypic benthic corals\",\".\",35,\"1\"\n" +
                "14,\"Hermatypic corals\",\".\",36,\"2\"";


        StudyImporterForRoopnarine importer = new StudyImporterForRoopnarine(new ParserFactory() {
            @Override
            public LabeledCSVParser createParser(String studyResource, String characterEncoding) throws IOException {
                ParserFactory factory;
                if (studyResource.contains("4.csv")) {
                    factory = new TestParserFactory(trophicGuildsToSpeciesLookup);
                } else {
                    factory = new TestParserFactory(trophicInteractions);
                }
                return factory.createParser("", characterEncoding);
            }
        }, nodeFactory);

        Study study = importer.importStudy();
        assertNotNull(nodeFactory.findTaxonByName("Negaprion brevirostris"));
        assertNotNull(nodeFactory.findTaxonByName("Carcharhinus perezi"));
        assertNotNull(nodeFactory.findTaxonByName("Galeocerdo cuvieri"));

        Iterable<Relationship> collectedRels = study.getSpecimens();
        int totalRels = validateSpecimen(collectedRels);

        assertThat(totalRels, Is.is(51));
    }

    @Ignore ("roopnarine imports eats more memory that other study imports")
    @Test
    public void importAll() throws StudyImporterException, NodeFactoryException {
        StudyImporterForRoopnarine studyImporterFor = new StudyImporterForRoopnarine(new ParserFactoryImpl(), nodeFactory);

        Study study = studyImporterFor.importStudy();

        Iterable<Relationship> collectedRels = study.getSpecimens();
        int totalRels = validateSpecimen(collectedRels);
        assertThat(totalRels, Is.is(1939));

        assertNotNull(nodeFactory.findTaxonByName("Lestrigonus bengalensis"));
        assertNotNull(nodeFactory.findTaxonByName("Bracyscelus crusculum"));
    }

    private int validateSpecimen(Iterable<Relationship> collectedRels) {
        int totalRels = 0;
        for (Relationship rel : collectedRels) {
            Node specimen = rel.getEndNode();
            assertNotNull(specimen);
            Relationship collectedAtRelationship = specimen.getSingleRelationship(RelTypes.COLLECTED_AT, Direction.OUTGOING);
            assertNotNull("missing location information", collectedAtRelationship);
            Node locationNode = collectedAtRelationship.getEndNode();
            assertNotNull(locationNode);
            assertTrue(locationNode.hasProperty(Location.LATITUDE));
            assertTrue(locationNode.hasProperty(Location.LONGITUDE));
            totalRels++;
        }
        return totalRels;
    }
}
