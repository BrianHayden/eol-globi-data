package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.neo4j.graphdb.Relationship;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.*;

public class StudyImporterForBlaremore extends BaseStudyImporter {
    private static final Log LOG = LogFactory.getLog(StudyImporterForBlaremore.class);
    private static final String DATA_SOURCE = "baremore/ANGELSHARK_DIET_DATAREQUEST_10012012.csv";


    public StudyImporterForBlaremore(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public Study importStudy() throws StudyImporterException {
        Study study = null;
        try {
            LabeledCSVParser parser = parserFactory.createParser(DATA_SOURCE, CharsetConstant.UTF8);
            String[] line = null;

            study = nodeFactory.getOrCreateStudy(StudyImporterFactory.Study.BAREMORE_ANGEL_SHARK.toString(),
                    "Ivy E. Baremore",
                    "University of Florida, Department of Fisheries and Aquatic Sciences",
                    "2005",
                    "Prey Selection By The Atlantic Angel Shark Squatina Dumeril In The Northeastern Gulf Of Mexico.");
            Location collectionLocation = nodeFactory.getOrCreateLocation(29.219302, -87.06665, null);

            Map<Integer, Specimen> specimenMap = new HashMap<Integer, Specimen>();

            while ((line = parser.getLine()) != null) {
                Integer sharkId = Integer.parseInt(line[0]);
                String collectionDateString = line[1];
                if (isBlank(collectionDateString)) {
                    LOG.warn("line [" + parser.getLastLineNumber() + "] in [" + DATA_SOURCE + "]: missing collection date");
                } else {
                    Specimen predatorSpecimen = specimenMap.get(sharkId);
                    if (predatorSpecimen == null) {
                        predatorSpecimen = nodeFactory.createSpecimen("Squatina dumeril");
                        predatorSpecimen.caughtIn(collectionLocation);
                        Relationship collectedRel = study.collected(predatorSpecimen);
                        try {
                            addCollectionDate(collectionDateString, collectedRel);
                        } catch (ParseException ex) {
                            throw new StudyImporterException("failed to parse collection date at line [" + parser.getLastLineNumber() + "] in [" + DATA_SOURCE + "]", ex);
                        }
                    }
                    specimenMap.put(sharkId, predatorSpecimen);

                    String totalLengthInCm = line[3];
                    try {
                        Double lengthInMm = Double.parseDouble(totalLengthInCm) * 10.0;
                        predatorSpecimen.setLengthInMm(lengthInMm);
                    } catch (NumberFormatException ex) {
                        throw new StudyImporterException("failed to parse length [" + totalLengthInCm);
                    }
                    String preySpeciesDescription = line[7];
                    Specimen preySpecimen = nodeFactory.createSpecimen(preySpeciesDescription);
                    predatorSpecimen.ate(preySpecimen);
                }
            }
        } catch (IOException e) {
            throw new StudyImporterException("failed to parse labels", e);
        } catch (NodeFactoryException e) {
            throw new StudyImporterException("failed to create node", e);
        }

        return study;
    }

    private void addCollectionDate(String s, Relationship collectedRel) throws ParseException {
        Date collectionDate = new SimpleDateFormat("MM/dd/yyyy").parse(s);
        nodeFactory.setUnixEpochProperty(collectedRel, collectionDate);
    }
}
