package org.trophic.graph.export;

import org.junit.Test;
import org.trophic.graph.data.GraphDBTestCase;
import org.trophic.graph.data.NodeFactoryException;
import org.trophic.graph.domain.Specimen;
import org.trophic.graph.domain.Study;
import org.trophic.graph.domain.Taxon;

import java.io.IOException;
import java.io.StringWriter;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class StudyExporterPredatorPreyEOLTest extends GraphDBTestCase {

    @Test
    public void export() throws NodeFactoryException, IOException {
        String predatorExternalId = "sapiensId";
        String preyExternalId = "lupusId";
        Study study = createStudy(predatorExternalId, preyExternalId);

        StringWriter writer = new StringWriter();
        new StudyExporterPredatorPreyEOL(getGraphDb()).exportStudy(study, writer, true);

        assertThat(writer.toString(), is("\"sapiensId\",\"lupusId\",\"feeds on\"\n"));
    }

    private Study createStudy(String predatorExternalId, String preyExternalId) {
        Study study = nodeFactory.createStudy("my study");
        Specimen predatorSpecimen = nodeFactory.createSpecimen();
        Taxon homoSapiens = nodeFactory.createTaxonOfType("Homo sapiens", Taxon.SPECIES, predatorExternalId);
        predatorSpecimen.classifyAs(homoSapiens);
        addCanisLupus(predatorSpecimen, preyExternalId);
        study.collected(predatorSpecimen);
        return study;
    }

    private void addCanisLupus(Specimen predatorSpecimen, String externalId) {
        Specimen preySpecimen = nodeFactory.createSpecimen();
        Taxon canisLupus = nodeFactory.createTaxonOfType("Canis lupus", Taxon.SPECIES, externalId);
        preySpecimen.classifyAs(canisLupus);
        predatorSpecimen.ate(preySpecimen);
    }

    @Test
    public void exportPreyNoExternalId() throws NodeFactoryException, IOException {
        Study study = createStudy("some external id", null);
        StringWriter writer = new StringWriter();
        new StudyExporterPredatorPreyEOL(getGraphDb()).exportStudy(study, writer, true);
        assertThat(writer.toString(), is(""));
    }

    @Test
    public void exportNoPredatorExternalId() throws NodeFactoryException, IOException {
        Study study = createStudy(null, "some external id");
        StringWriter writer = new StringWriter();
        new StudyExporterPredatorPreyEOL(getGraphDb()).exportStudy(study, writer, true);
        assertThat(writer.toString(), is(""));
    }

@Test
    public void exportNoPredatorExternalIdNoPreyExternalId() throws NodeFactoryException, IOException {
        Study study = createStudy(null, null);
        StringWriter writer = new StringWriter();
        new StudyExporterPredatorPreyEOL(getGraphDb()).exportStudy(study, writer, true);
        assertThat(writer.toString(), is(""));
    }


    @Test
    public void exportOnePredatorTwoPrey() throws NodeFactoryException, IOException {
        Study study = nodeFactory.createStudy("my study");
        Specimen predatorSpecimen = nodeFactory.createSpecimen();
        Taxon homoSapiens = nodeFactory.createTaxonOfType("Homo sapiens", Taxon.SPECIES, "homoSapiensId");
        predatorSpecimen.classifyAs(homoSapiens);
        addCanisLupus(predatorSpecimen, "canisLupusId");
        addCanisLupus(predatorSpecimen, "canisLupusId");
        Specimen preySpecimen = nodeFactory.createSpecimen();
        Taxon canisLupus = nodeFactory.createTaxonOfType("Canis lupus other", Taxon.SPECIES, "canisLupusId2");
        preySpecimen.classifyAs(canisLupus);
        predatorSpecimen.ate(preySpecimen);
        study.collected(predatorSpecimen);
        StringWriter writer = new StringWriter();
        new StudyExporterPredatorPreyEOL(getGraphDb()).exportStudy(study, writer, true);
        assertThat(writer.toString(), is("\"homoSapiensId\",\"canisLupusId2\",\"feeds on\"\n\"homoSapiensId\",\"canisLupusId\",\"feeds on\"\n"));
    }
}
