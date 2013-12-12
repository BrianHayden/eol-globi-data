package org.eol.globi.export;

import org.apache.commons.io.FileUtils;
import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.ImportFilter;
import org.eol.globi.data.NodeFactory;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.data.StudyImporterForSPIRE;
import org.eol.globi.domain.Study;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class GlobiOWLExporterIT extends GraphDBTestCase {

    @Test
    public void importSPIREExportTTL() throws OWLOntologyCreationException, OWLOntologyStorageException, IOException, StudyImporterException {
        StudyImporterForSPIRE importer = new StudyImporterForSPIRE(null, nodeFactory);
        importer.setFilter(new ImportFilter() {
            @Override
            public boolean shouldImportRecord(Long recordNumber) {
                return recordNumber < 100;
            }
        });
        importer.importStudy();
        List<Study> studies = NodeFactory.findAllStudies(getGraphDb());

        FileUtils.forceMkdir(new File("target"));
        String tgt = "target/spire-as-globi.ttl";
        Writer writer = new FileWriter(tgt);
        GlobiOWLExporter globiOWLExporter = new GlobiOWLExporter();
        for (Study study : studies) {
            globiOWLExporter.exportStudy(study, writer, true);
        }
        writer.flush();
        writer.close();

        assertTrue(new File(tgt).exists());
    }


}
