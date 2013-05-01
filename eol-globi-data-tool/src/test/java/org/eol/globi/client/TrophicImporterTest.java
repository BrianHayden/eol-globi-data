package org.eol.globi.client;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.data.StudyImporterForCook;
import org.eol.globi.data.StudyImporterForSimons;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Taxon;
import org.eol.globi.service.TaxonPropertyEnricher;
import org.hamcrest.core.Is;
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;

import java.io.IOException;
import java.util.List;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class TrophicImporterTest extends GraphDBTestCase {

    @Test
    public void doSingleImport() throws IOException, StudyImporterException {
        TrophicImporter trophicImporter = new TrophicImporter();

        GraphDatabaseService graphService = getGraphDb();
        trophicImporter.importData(graphService, new TaxonPropertyEnricher() {
            @Override
            public boolean enrich(Taxon taxon) throws IOException {
                return false;
            }
        }, StudyImporterForSimons.class);

        List<Study> allStudies = trophicImporter.findAllStudies(graphService);
        assertThat(allStudies.size(), Is.is(1));
        assertThat(allStudies.get(0).getTitle(), Is.is("Simons 1997"));

        assertNotNull(graphService.getNodeById(1));
        assertNotNull(graphService.getNodeById(200));
    }

}