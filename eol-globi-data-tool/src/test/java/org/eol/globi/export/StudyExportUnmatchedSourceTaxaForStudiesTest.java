package org.eol.globi.export;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactory;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.data.taxon.TaxonNameCorrector;
import org.eol.globi.data.taxon.TaxonServiceImpl;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Taxon;
import org.eol.globi.service.TaxonPropertyEnricher;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.io.StringWriter;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class StudyExportUnmatchedSourceTaxaForStudiesTest extends GraphDBTestCase {

    public static final String EXPECTED_HEADER = "\"original source taxon name\",\"original source external id\",\"unmatched normalized source taxon name\",\"unmatched normalized source external id\",\"study\",\"source\"";

    @Test
    public void exportOnePredatorTwoPrey() throws NodeFactoryException, IOException {
        final TaxonPropertyEnricher taxonEnricher = new TaxonPropertyEnricher() {
            @Override
            public void enrich(Taxon taxon) {
                if ("Homo sapiens".equals(taxon.getName())) {
                    taxon.setExternalId("homoSapiensId");
                    taxon.setPath("one two three");
                } else if ("Canis lupus".equals(taxon.getName())) {
                    taxon.setExternalId("canisLupusId");
                    taxon.setPath("four five six");
                }
            }
        };
        nodeFactory = new NodeFactory(getGraphDb(), new TaxonServiceImpl(taxonEnricher, new TaxonNameCorrector(), getGraphDb()));
        Study study = nodeFactory.getOrCreateStudy("my study", "my first source", null);

        nodeFactory.getOrCreateTaxon("Homo sapiens");
        Specimen predatorSpecimen = nodeFactory.createSpecimen("Homo sapiens");
        nodeFactory.getOrCreateTaxon("Canis lupus");
        addCanisLupus(predatorSpecimen);
        addCanisLupus(predatorSpecimen);
        Specimen preySpecimen = nodeFactory.createSpecimen("Caniz");
        predatorSpecimen.createRelationshipTo(preySpecimen, InteractType.ATE);
        study.collected(predatorSpecimen);

        Specimen predatorSpecimen23 = nodeFactory.createSpecimen("Homo sapiens2");
        addCanisLupus(predatorSpecimen23);
        study.collected(predatorSpecimen23);
        Specimen predatorSpecimen22 = nodeFactory.createSpecimen("Homo sapiens2");
        addCanisLupus(predatorSpecimen22);
        study.collected(predatorSpecimen22);

        Study study2 = nodeFactory.getOrCreateStudy("my study2", "my source2", null);
        Specimen predatorSpecimen21 = nodeFactory.createSpecimen("Homo sapiens2");
        addCanisLupus(predatorSpecimen21);
        study2.collected(predatorSpecimen21);

        Specimen predatorSpecimen2 = nodeFactory.createSpecimen("Homo sapiens3", PropertyAndValueDictionary.NO_MATCH);
        addCanisLupus(predatorSpecimen2);
        study.collected(predatorSpecimen2);


        StringWriter writer = new StringWriter();
        new StudyExportUnmatchedSourceTaxaForStudies().exportStudy(study, writer, true);
        assertThat(writer.toString(), is(EXPECTED_HEADER + "\n" +
                        "\"Homo sapiens2\",,\"Homo sapiens2\",,\"my study\",\"my first source\"\n" +
                        "\"Homo sapiens3\",,\"Homo sapiens3\",,\"my study\",\"my first source\"\n"
        ));

        writer = new StringWriter();
        new StudyExportUnmatchedTargetTaxaForStudies().exportStudy(study, writer, true);
        assertThat(writer.toString(), is("\"original target taxon name\",\"original target external id\",\"unmatched normalized target taxon name\",\"unmatched normalized target external id\",\"study\",\"source\"" + "\n" +
                        "\"Caniz\",,\"Caniz\",,\"my study\",\"my first source\"\n"
        ));
    }

    @Test
    public void exportOnePredatorNoPathButWithSameAs() throws NodeFactoryException, IOException {
        final TaxonPropertyEnricher taxonEnricher = new TaxonPropertyEnricher() {
            @Override
            public void enrich(Taxon taxon) {

            }
        };
        nodeFactory = new NodeFactory(getGraphDb(), new TaxonServiceImpl(taxonEnricher, new TaxonNameCorrector(), getGraphDb()));
        Study study = nodeFactory.getOrCreateStudy("my study", "my first source", null);

        Specimen predatorSpecimen = nodeFactory.createSpecimen("Homo sapienz");
        Specimen preySpecimen = nodeFactory.createSpecimen("Caniz");
        predatorSpecimen.createRelationshipTo(preySpecimen, InteractType.ATE);
        study.collected(predatorSpecimen);

        predatorSpecimen = nodeFactory.createSpecimen("Homo sapiens");
        Node synonymNode = nodeFactory.getOrCreateTaxon("Homo sapiens Synonym").getUnderlyingNode();
        Node node = nodeFactory.getOrCreateTaxon("Homo sapiens").getUnderlyingNode();
        Transaction tx = getGraphDb().beginTx();
        try {
            node.createRelationshipTo(synonymNode, RelTypes.SAME_AS);
            tx.success();
        } finally {
            tx.finish();
        }

        preySpecimen = nodeFactory.createSpecimen("Canis");
        predatorSpecimen.createRelationshipTo(preySpecimen, InteractType.ATE);
        study.collected(predatorSpecimen);

        StringWriter writer = new StringWriter();
        new StudyExportUnmatchedSourceTaxaForStudies().exportStudy(study, writer, true);
        assertThat(writer.toString(), is(EXPECTED_HEADER + "\n" +
                        "\"Homo sapienz\",,\"Homo sapienz\",,\"my study\",\"my first source\"\n"
        ));

        writer = new StringWriter();
        new StudyExportUnmatchedTargetTaxaForStudies().exportStudy(study, writer, true);
        assertThat(writer.toString(), is("\"original target taxon name\",\"original target external id\",\"unmatched normalized target taxon name\",\"unmatched normalized target external id\",\"study\",\"source\"" + "\n" +
                        "\"Caniz\",,\"Caniz\",,\"my study\",\"my first source\"\n" +
                        "\"Canis\",,\"Canis\",,\"my study\",\"my first source\"\n"
        ));
    }

    private void addCanisLupus(Specimen predatorSpecimen) throws NodeFactoryException {
        Specimen preySpecimen = nodeFactory.createSpecimen("Canis lupus");
        predatorSpecimen.createRelationshipTo(preySpecimen, InteractType.ATE);
    }

}