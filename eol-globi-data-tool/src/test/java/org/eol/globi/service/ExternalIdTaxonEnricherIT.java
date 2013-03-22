package org.eol.globi.service;

import org.apache.commons.lang3.time.StopWatch;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Taxon;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

public class ExternalIdTaxonEnricherIT extends GraphDBTestCase {

    private TaxonProcessor taxonProcessor;

    public static final String[] TAXON_NAMES = new String[]{
            "Zalieutes mcgintyi",
            "Serranus atrobranchus",
            "Peprilus burti",
            "Prionotus longispinosus",
            "Neopanope sayi"
    };

    @Before
    public void init() {
        taxonProcessor = new ExternalIdTaxonEnricher(nodeFactory.getGraphDb());
    }

    @Test
    public void matchPredatorTaxon() throws NodeFactoryException, IOException {
        matchTaxon("Syacium gunteri");
    }

    @Test
    public void matchPreyTaxon() throws IOException, NodeFactoryException {
        enrichPreyTaxon("Syacium gunteri");
    }

    @Test
    public void matchNameTooShort() throws IOException, NodeFactoryException {
        String predatorTaxonName = "blabla";
        Taxon taxon = nodeFactory.getOrCreateTaxon(predatorTaxonName);
        Study study = nodeFactory.createStudy("bla");
        Specimen predator = nodeFactory.createSpecimen();
        predator.classifyAs(taxon);

        Specimen prey = nodeFactory.createSpecimen();
        Taxon g = nodeFactory.getOrCreateTaxon("G");
        assertThat(g.getExternalId(), is(nullValue()));
        prey.classifyAs(g);
        predator.ate(prey);

        study.collected(predator);

        taxonProcessor.process();

        Taxon taxonOfType = nodeFactory.findTaxonOfType("G");
        assertThat(taxonOfType.getExternalId(), is("no:match"));
    }

    private void enrichPreyTaxon(String preyName) throws IOException, NodeFactoryException {
        String predatorTaxonName = "blabla";
        Taxon taxon = nodeFactory.getOrCreateTaxon(predatorTaxonName);
        Study study = nodeFactory.createStudy("bla");
        Specimen predator = nodeFactory.createSpecimen();
        predator.classifyAs(taxon);

        Specimen prey = nodeFactory.createSpecimen();
        prey.classifyAs(nodeFactory.getOrCreateTaxon(preyName));
        predator.ate(prey);

        study.collected(predator);

        taxonProcessor.process();

        Taxon taxonOfType = nodeFactory.findTaxonOfType(preyName);
        assertThat("failed to match [" + preyName + "]", taxonOfType.getExternalId(), containsString(EOLTaxonImageService.EOL_LSID_PREFIX));
    }

    private void matchTaxon(String speciesName) throws IOException, NodeFactoryException {
        Taxon taxon = nodeFactory.getOrCreateTaxon(speciesName);
        Study study = nodeFactory.createStudy("bla");
        Specimen specimen = nodeFactory.createSpecimen();
        specimen.classifyAs(taxon);
        study.collected(specimen);

        taxonProcessor.process();

        Taxon taxonOfType = nodeFactory.findTaxonOfType(speciesName);
        assertThat("failed to match [" + speciesName + "]", taxonOfType.getExternalId(), containsString(EOLTaxonImageService.EOL_LSID_PREFIX));
    }


    @Ignore
    @Test
    public void matchManyPredatorTaxons() throws IOException, NodeFactoryException {

        //warm-up
        matchTaxon("Syacium gunteri");

        StopWatch stopwatch = new StopWatch();
        stopwatch.start();
        for (String taxonName : TAXON_NAMES) {
            matchTaxon(taxonName);
        }
        stopwatch.stop();

        float rate = 1000.0f * TAXON_NAMES.length / stopwatch.getTime();
        assertThat("rate of term matching [" + rate + "] is less than 1 term/s", rate > 1.0, is(true));
    }

    @Ignore
    @Test
    public void matchManyPreyTaxons() throws IOException, NodeFactoryException {

        //warm-up
        enrichPreyTaxon("Syacium gunteri");

        StopWatch stopwatch = new StopWatch();
        stopwatch.start();
        for (String taxonName : TAXON_NAMES) {
            enrichPreyTaxon(taxonName);
        }
        stopwatch.stop();

        float rate = 1000.0f * TAXON_NAMES.length / stopwatch.getTime();
        assertThat("rate of term matching [" + rate + "] is less than 1 term/s", rate > 1.0, is(true));
    }


}
