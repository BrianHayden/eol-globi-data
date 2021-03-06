package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.domain.Term;
import org.eol.globi.service.TermLookupService;
import org.eol.globi.service.TermLookupServiceException;
import org.eol.globi.util.ExternalIdUtil;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudyImporterForGoMexSI extends BaseStudyImporter {
    private static final Log LOG = LogFactory.getLog(StudyImporterForGoMexSI.class);

    public static final String GOMEXI_SOURCE_DESCRIPTION = "http://gomexsi.tamucc.edu";
    public static final String STOMACH_COUNT_TOTAL = "stomachCountTotal";
    public static final String STOMACH_COUNT_WITH_FOOD = "stomachCountWithFood";
    public static final String STOMACH_COUNT_WITHOUT_FOOD = "stomachCountWithoutFood";
    public static final String GOMEXSI_NAMESPACE = "GOMEXSI:";

    private static final Collection KNOWN_INVALID_DOUBLE_STRINGS = new ArrayList<String>() {{
        add("na");
        add("> .001");
        add("tr");
        add("< 2");
    }};

    private static final Collection KNOWN_INVALID_INTEGER_STRINGS = new ArrayList<String>() {{
        add("na");
        add("numerous");
        add("a few");
        add("several");
    }};

    private String sourceCitation = GOMEXI_SOURCE_DESCRIPTION;
    private String baseUrl = "gomexsi";


    public StudyImporterForGoMexSI(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    protected String getPreyResourcePath() {
        return getBaseUrl() + "/Prey.csv";
    }

    private String getBaseUrl() {
        return baseUrl;
    }

    protected String getPredatorResourcePath() {
        return getBaseUrl() + "/Predators.csv";
    }

    protected String getReferencesResourcePath() {
        return getBaseUrl() + "/References.csv";
    }

    protected String getLocationsResourcePath() {
        return getBaseUrl() + "/Locations.csv";
    }

    @Override
    public Study importStudy() throws StudyImporterException {
        Study study = nodeFactory.getOrCreateStudy("GoMexSI",
                "James D. Simons",
                "Center for Coastal Studies, Texas A&M University - Corpus Christi, United States",
                "",
                "<a href=\"http://www.ingentaconnect.com/content/umrsmas/bullmar/2013/00000089/00000001/art00009\">Building a Fisheries Trophic Interaction Database for Management and Modeling Research in the Gulf of Mexico Large Marine Ecosystem.</a>"
                , null
                , GOMEXI_SOURCE_DESCRIPTION, null);
        final Map<String, Map<String, String>> predatorIdToPredatorNames = new HashMap<String, Map<String, String>>();
        final Map<String, List<Map<String, String>>> predatorIdToPreyNames = new HashMap<String, List<Map<String, String>>>();
        Map<String, Study> referenceIdToStudy = new HashMap<String, Study>();
        addSpecimen(getPredatorResourcePath(), "PRED_SCI_NAME", new ParseEventHandler() {
            @Override
            public void onSpecimen(String predatorUID, Map<String, String> properties) {
                predatorIdToPredatorNames.put(predatorUID, properties);
            }
        });
        addSpecimen(getPreyResourcePath(), "DATABASE_PREY_NAME", new ParseEventHandler() {
            @Override
            public void onSpecimen(String predatorUID, Map<String, String> properties) {
                List<Map<String, String>> preyList = predatorIdToPreyNames.get(predatorUID);
                if (preyList == null) {
                    preyList = new ArrayList<Map<String, String>>();
                    predatorIdToPreyNames.put(predatorUID, preyList);
                }
                preyList.add(properties);
            }
        });
        addReferences(referenceIdToStudy);
        addObservations(predatorIdToPredatorNames, referenceIdToStudy, predatorIdToPreyNames, study);

        // TODO figure out a way to introduce the separation of study and reference.

        return study;
    }

    private void addReferences(Map<String, Study> referenceIdToStudy) throws StudyImporterException {
        String referenceResource = getReferencesResourcePath();
        try {
            LabeledCSVParser parser = parserFactory.createParser(referenceResource, CharsetConstant.UTF8);
            while (parser.getLine() != null) {
                String refId = getMandatoryValue(referenceResource, parser, "REF_ID");
                String lastName = getMandatoryValue(referenceResource, parser, "AUTH_L_NAME");
                String firstName = getMandatoryValue(referenceResource, parser, "AUTH_F_NAME");
                Study study = referenceIdToStudy.get(refId);
                if (study == null) {
                    addNewStudy(referenceIdToStudy, referenceResource, parser, refId, lastName, firstName);
                } else {
                    updateContributorList(lastName, firstName, study);
                }

            }
        } catch (IOException e) {
            throw new StudyImporterException("failed to open resource [" + referenceResource + "]", e);
        }
    }

    private void updateContributorList(String lastName, String firstName, Study study) {
        Transaction transaction = study.getUnderlyingNode().getGraphDatabase().beginTx();
        try {
            String contributor = study.getContributor();
            study.setContributor(contributor + ", " + firstName + " " + lastName);
            transaction.success();
        } finally {
            transaction.finish();
        }
    }

    private void addNewStudy(Map<String, Study> referenceIdToStudy, String referenceResource, LabeledCSVParser parser, String refId, String lastName, String firstName) throws StudyImporterException {
        Study study;
        String refTag = getMandatoryValue(referenceResource, parser, "REF_TAG");
        String externalId = getMandatoryValue(referenceResource, parser, "GAME_ID");
        String description = getMandatoryValue(referenceResource, parser, "TITLE_REF");
        String publicationYear = getMandatoryValue(referenceResource, parser, "YEAR_PUB");
        String universityName = getMandatoryValue(referenceResource, parser, "UNIV_NAME");
        String universityCity = getMandatoryValue(referenceResource, parser, "UNIV_CITY");

        String universityState = getMandatoryValue(referenceResource, parser, "UNIV_STATE");

        String universityCountry = getMandatoryValue(referenceResource, parser, "UNIV_COUNTRY");
        StringBuilder institution = new StringBuilder();
        if (StringUtils.isNotBlank(universityName)
                && StringUtils.isNotBlank(universityCity)
                && StringUtils.isNotBlank(universityState)
                && StringUtils.isNotBlank(universityCountry)) {
            institution.append(universityName);
            institution.append(", ");
            institution.append(universityCity);
            institution.append(", ");
            institution.append(universityState);
            institution.append(", ");
            institution.append(universityCountry);
        }
        study = nodeFactory.getOrCreateStudy(refTag, firstName + " " + lastName, institution.toString(), "", description
                , publicationYear
                , getSourceCitation(), null);
        Transaction transaction = study.getUnderlyingNode().getGraphDatabase().beginTx();
        try {
            study.setPublicationYear(publicationYear);
            if (StringUtils.isNotBlank(externalId)) {
                study.setExternalId(ExternalIdUtil.infoURLForExternalId(TaxonomyProvider.ID_PREFIX_GAME + externalId));
            }
            transaction.success();
        } finally {
            transaction.finish();
        }
        referenceIdToStudy.put(refId, study);
    }

    private void addObservations(Map<String, Map<String, String>> predatorIdToPredatorSpecimen, Map<String, Study> refIdToStudyMap, Map<String, List<Map<String, String>>> predatorUIToPreyLists, Study metaStudy) throws StudyImporterException {
        String locationResource = getLocationsResourcePath();
        try {
            TermLookupService cmecsService = new CMECSService();
            LabeledCSVParser parser = parserFactory.createParser(locationResource, CharsetConstant.UTF8);
            while (parser.getLine() != null) {
                String refId = getMandatoryValue(locationResource, parser, "REF_ID");
                if (!refIdToStudyMap.containsKey(refId)) {
                    getLogger().warn(metaStudy, "failed to find study for ref id [" + refId + "] on related to observation location in [" + locationResource + ":" + parser.getLastLineNumber() + "]");
                } else {
                    Study study = refIdToStudyMap.get(refId);
                    String specimenId = getMandatoryValue(locationResource, parser, "PRED_ID");
                    Double latitude = getMandatoryDoubleValue(locationResource, parser, "LOC_CENTR_LAT");
                    Double longitude = getMandatoryDoubleValue(locationResource, parser, "LOC_CENTR_LONG");
                    Double depth = getMandatoryDoubleValue(locationResource, parser, "MN_DEP_SAMP");
                    String habitatSystem = getMandatoryValue(locationResource, parser, "HAB_SYSTEM");
                    String habitatSubsystem = getMandatoryValue(locationResource, parser, "HAB_SUBSYSTEM");
                    String habitatTidalZone = getMandatoryValue(locationResource, parser, "TIDAL_ZONE");

                    Location location = getLocation(metaStudy, locationResource, cmecsService, parser, latitude, longitude, depth, habitatSystem, habitatSubsystem, habitatTidalZone);

                    String predatorId = refId + specimenId;
                    Map<String, String> predatorProperties = predatorIdToPredatorSpecimen.get(predatorId);
                    if (predatorProperties == null) {
                        getLogger().warn(metaStudy, "failed to lookup location for predator [" + refId + ":" + specimenId + "] from [" + locationResource + ":" + parser.getLastLineNumber() + "]");
                    } else {
                        addObservation(predatorUIToPreyLists, metaStudy, parser, study, location, predatorId, predatorProperties);
                    }
                }
            }
        } catch (IOException e) {
            throw new StudyImporterException("failed to open resource [" + locationResource + "]", e);
        }

    }

    private Location getLocation(Study metaStudy, String locationResource, TermLookupService cmecsService, LabeledCSVParser parser, Double latitude, Double longitude, Double depth, String habitatSystem, String habitatSubsystem, String habitatTidalZone) {
        Location location = null;
        try {
            location = nodeFactory.getOrCreateLocation(latitude, longitude, depth == null ? null : -depth);
        } catch (NodeFactoryException e) {
            //
        }
        if (location == null) {
            getLogger().warn(metaStudy, "failed to find location for [" + latitude + "], longitude" + " [" + longitude + "] in [" + locationResource + ":" + parser.getLastLineNumber() + "]");
        } else {
            List<Term> terms;
            String cmecsLabel = habitatSystem + " " + habitatSubsystem + " " + habitatTidalZone;
            String msg = "failed to map CMECS habitat [" + cmecsLabel + "] on line [" + parser.lastLineNumber() + "] of image [" + locationResource + "]";
            try {
                terms = cmecsService.lookupTermByName(cmecsLabel);
                if (terms.size() == 0) {
                    getLogger().warn(metaStudy, msg);
                }
                nodeFactory.addEnvironmentToLocation(location, terms);
            } catch (TermLookupServiceException e) {
                getLogger().warn(metaStudy, msg);
            }

        }
        return location;
    }

    private void addObservation(Map<String, List<Map<String, String>>> predatorUIToPreyLists, Study metaStudy, LabeledCSVParser parser, Study study, Location location, String predatorId, Map<String, String> predatorProperties) throws StudyImporterException {
        try {
            Specimen predatorSpecimen = createSpecimen(study, predatorProperties);
            predatorSpecimen.setExternalId(predatorId);
            if (location == null) {
                getLogger().warn(metaStudy, "no location for predator with id [" + predatorSpecimen.getExternalId() + "]");
            } else {
                predatorSpecimen.caughtIn(location);
            }
            List<Map<String, String>> preyList = predatorUIToPreyLists.get(predatorId);
            checkStomachDataConsistency(predatorId, predatorProperties, preyList, metaStudy);
            if (preyList != null) {
                for (Map<String, String> preyProperties : preyList) {
                    if (preyProperties != null) {
                        try {
                            Specimen prey = createSpecimen(study, preyProperties);
                            prey.caughtIn(location);
                            predatorSpecimen.ate(prey);
                        } catch (NodeFactoryException e) {
                            getLogger().warn(metaStudy, "failed to add prey [" + preyProperties + "] for predator with id + [" + predatorId + "]: [" + predatorProperties + "]: [" +  e.getMessage() + "]");
                        }
                    }
                }
            }
        } catch (NodeFactoryException e) {
            throw new StudyImporterException("failed to create specimen for location on line [" + parser.getLastLineNumber() + "]", e);
        }
    }

    private void checkStomachDataConsistency(String predatorId, Map<String, String> predatorProperties, List<Map<String, String>> preyList, Study metaStudy) throws StudyImporterException {
        Integer total = integerValueOrNull(predatorProperties, STOMACH_COUNT_TOTAL);
        Integer withoutFood = integerValueOrNull(predatorProperties, STOMACH_COUNT_WITHOUT_FOOD);
        Integer withFood = integerValueOrNull(predatorProperties, STOMACH_COUNT_WITH_FOOD);
        if (total != null && withoutFood != null) {
            if (preyList == null || preyList.size() == 0) {
                if (!total.equals(withoutFood)) {
                    getLogger().warn(metaStudy, "no prey for predator with id [" + predatorId + "], but found [" + withFood + "] stomachs with food");
                }
            } else {
                if (total.equals(withoutFood)) {
                    getLogger().warn(metaStudy, "found prey for predator with id [" + predatorId + "], but found only stomachs without food");
                }
            }
        }
    }

    private Integer integerValueOrNull(Map<String, String> props, String key) throws StudyImporterException {
        String value = props.get(key);
        try {
            return StringUtils.isBlank(value) || KNOWN_INVALID_INTEGER_STRINGS.contains(StringUtils.lowerCase(value)) ? null : Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new StudyImporterException("failed to parse key [" + key + "] with value [" + value + "]", ex);
        }
    }

    private Double doubleValueOrNull(Map<String, String> props, String key) throws StudyImporterException {
        String value = props.get(key);
        try {
            return StringUtils.isBlank(value) || KNOWN_INVALID_DOUBLE_STRINGS.contains(StringUtils.lowerCase(value)) ? null : Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            throw new StudyImporterException("failed to parse key [" + key + "] with value [" + value + "]", ex);
        }
    }

    private Specimen createSpecimen(Study study, Map<String, String> properties) throws NodeFactoryException, StudyImporterException {
        Specimen specimen = nodeFactory.createSpecimen(study, properties.get(PropertyAndValueDictionary.NAME));
        specimen.setLengthInMm(doubleValueOrNull(properties, Specimen.LENGTH_IN_MM));
        specimen.setFrequencyOfOccurrence(doubleValueOrNull(properties, Specimen.FREQUENCY_OF_OCCURRENCE));
        specimen.setTotalCount(integerValueOrNull(properties, Specimen.TOTAL_COUNT));
        specimen.setTotalVolumeInMl(doubleValueOrNull(properties, Specimen.TOTAL_VOLUME_IN_ML));
        addLifeStage(properties, specimen);
        addPhysiologicalState(properties, specimen);
        addBodyPart(properties, specimen);

        return specimen;
    }

    private void addLifeStage(Map<String, String> properties, Specimen specimen) throws StudyImporterException {
        try {
            String lifeStageName = properties.get(Specimen.LIFE_STAGE_LABEL);
            Term term = nodeFactory.getOrCreateLifeStage(GOMEXSI_NAMESPACE + lifeStageName, lifeStageName);
            specimen.setLifeStage(term);
        } catch (NodeFactoryException e) {
            throw new StudyImporterException("failed to map life stage", e);
        }
    }

    private void addPhysiologicalState(Map<String, String> properties, Specimen specimen) throws StudyImporterException {
        try {
            String name = properties.get(Specimen.PHYSIOLOGICAL_STATE_LABEL);
            Term term = nodeFactory.getOrCreatePhysiologicalState(GOMEXSI_NAMESPACE + name, name);
            specimen.setPhysiologicalState(term);
        } catch (NodeFactoryException e) {
            throw new StudyImporterException("failed to map life stage", e);
        }
    }

    private void addBodyPart(Map<String, String> properties, Specimen specimen) throws StudyImporterException {
        try {
            String name = properties.get(Specimen.BODY_PART_LABEL);
            Term term = nodeFactory.getOrCreateBodyPart(GOMEXSI_NAMESPACE + name, name);
            specimen.setBodyPart(term);
        } catch (NodeFactoryException e) {
            throw new StudyImporterException("failed to map body part", e);
        }
    }

    private Double getMandatoryDoubleValue(String locationResource, LabeledCSVParser parser, String label) throws StudyImporterException {
        String value = getMandatoryValue(locationResource, parser, label);
        try {
            return "NA".equals(value) || value == null || value.trim().length() == 0 ? null : Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            throw new StudyImporterException("failed to parse [" + label + "] value [" + value + "] at line [" + parser.getLastLineNumber() + "]", ex);
        }
    }

    private void addSpecimen(String datafile, String scientificNameLabel, ParseEventHandler specimenListener) throws StudyImporterException {
        try {
            LabeledCSVParser parser = parserFactory.createParser(datafile, CharsetConstant.UTF8);
            while (parser.getLine() != null) {
                Map<String, String> properties = new HashMap<String, String>();
                addOptionalProperty(parser, "TOT_WO_FD", STOMACH_COUNT_WITHOUT_FOOD, properties);
                addOptionalProperty(parser, "TOT_W_FD", STOMACH_COUNT_WITH_FOOD, properties);
                addOptionalProperty(parser, "TOT_PRED_STOM_EXAM", STOMACH_COUNT_TOTAL, properties);
                addOptionalProperty(parser, "MN_LEN", Specimen.LENGTH_IN_MM, properties);
                addOptionalProperty(parser, "LIFE_HIST_STAGE", Specimen.LIFE_STAGE_LABEL, properties);
                addOptionalProperty(parser, "PHYSIOLOG_STATE", Specimen.PHYSIOLOGICAL_STATE_LABEL, properties);
                addOptionalProperty(parser, "PREY_PARTS", Specimen.BODY_PART_LABEL, properties);
                addOptionalProperty(parser, "N_CONS", Specimen.TOTAL_COUNT, properties);
                addOptionalProperty(parser, "VOL_CONS", Specimen.TOTAL_VOLUME_IN_ML, properties);
                addOptionalProperty(parser, "FREQ_OCC", Specimen.FREQUENCY_OF_OCCURRENCE, properties);
                properties.put(PropertyAndValueDictionary.NAME, getMandatoryValue(datafile, parser, scientificNameLabel));

                String refId = getMandatoryValue(datafile, parser, "REF_ID");
                String specimenId = getMandatoryValue(datafile, parser, "PRED_ID");
                String predatorUID = refId + specimenId;

                specimenListener.onSpecimen(predatorUID, properties);
            }
        } catch (IOException e) {
            throw new StudyImporterException("failed to open resource [" + datafile + "]", e);
        }
    }

    private void addOptionalProperty(LabeledCSVParser parser, String label, String normalizedName, Map<String, String> properties) {
        String value = parser.getValueByLabel(label);
        value = value == null || "NA".equalsIgnoreCase(value) ? null : value;
        if (value != null) {
            properties.put(normalizedName, value);
        }
    }

    private String getMandatoryValue(String datafile, LabeledCSVParser parser, String label) throws StudyImporterException {
        String value = parser.getValueByLabel(label);
        if (value == null) {
            throw new StudyImporterException("missing mandatory column [" + label + "] in [" + datafile + "]:[" + parser.getLastLineNumber() + "]");
        }
        return "NA".equals(value) ? "" : value;
    }

    public void setSourceCitation(String sourceCitation) {
        this.sourceCitation = sourceCitation;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getSourceCitation() {
        return sourceCitation;
    }
}
