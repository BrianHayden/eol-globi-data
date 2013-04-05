package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Taxon;

import java.io.IOException;

public class StudyImporterForJRFerrerParis extends BaseStudyImporter {

    public StudyImporterForJRFerrerParis(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public Study importStudy() throws StudyImporterException {
        Study study = nodeFactory.createStudy("JRFerrisParisButterflies",
                "Jose R. Ferrer Paris",
                "Centro de Estudios Botánicos y Agroforestales, Instituto Venezolano de Investigaciones Científicas; Kirstenbosch Research Center, South African National Biodiversity Institute",
                "",
                "Compilation of hostplant records for butterflies.");
        String studyResource = "jr_ferrer_paris/ExampleAssociations.csv";
        try {
            LabeledCSVParser parser = parserFactory.createParser(studyResource);

            while (parser.getLine() != null) {
                Specimen instigatorSpecimen = nodeFactory.createSpecimen(createTaxon(parser, "Lepidoptera Name"));
                study.collected(instigatorSpecimen);

                Specimen targetSpecimen = nodeFactory.createSpecimen(createTaxon(parser, "Hostplant Name"));
                instigatorSpecimen.ate(targetSpecimen);
            }
        } catch (IOException e) {
            throw new StudyImporterException("failed to access resource [" + studyResource + "]");
        }

        return study;
    }

    private Taxon createTaxon(LabeledCSVParser parser, String taxonLabel) throws StudyImporterException {
        String instigatorScientificName = parser.getValueByLabel(taxonLabel);
        if (StringUtils.isBlank(instigatorScientificName)) {
            throw new StudyImporterException("found missing instigator scientific name at line [" + parser.getLastLineNumber() + "]");
        } else {
            try {
                return nodeFactory.getOrCreateTaxon(StringUtils.trim(instigatorScientificName));
            } catch (NodeFactoryException e) {
                throw new StudyImporterException("failed to find/create taxon with name [" + instigatorScientificName + "]");
            }
        }
    }
}
