package org.eol.globi.data;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.data.taxon.TaxonService;
import org.eol.globi.domain.Environment;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Season;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.domain.Term;
import org.eol.globi.geo.Ecoregion2;
import org.eol.globi.geo.EcoregionFinder2;
import org.eol.globi.geo.EcoregionFinderException2;
import org.eol.globi.service.DOIResolver;
import org.eol.globi.service.EnvoLookupService;
import org.eol.globi.service.TermLookupService;
import org.eol.globi.service.TermLookupServiceException;
import org.eol.globi.service.UberonLookupService;
import org.eol.globi.util.NodeUtil;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.index.lucene.QueryContext;
import org.neo4j.index.lucene.ValueContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class NodeFactory {

    private static final Log LOG = LogFactory.getLog(NodeFactory.class);
    public static final org.eol.globi.domain.Term NO_MATCH_TERM = new org.eol.globi.domain.Term(PropertyAndValueDictionary.NO_MATCH, PropertyAndValueDictionary.NO_MATCH);

    private GraphDatabaseService graphDb;
    private final Index<Node> studies;
    private final Index<Node> seasons;
    private final Index<Node> locations;
    private final Index<Node> environments;
    private final Index<Node> ecoregions;
    private final Index<Node> ecoregionSuggestions;
    private final Index<Node> ecoregionPaths;

    private TermLookupService termLookupService;
    private TermLookupService envoLookupService;

    private DOIResolver doiResolver;
    private EcoregionFinder2 ecoregionFinder2;
    private TaxonService taxonService;

    public NodeFactory(GraphDatabaseService graphDb, TaxonService taxonService) {
        this.graphDb = graphDb;

        this.termLookupService = new UberonLookupService();
        this.envoLookupService = new EnvoLookupService();
        this.studies = graphDb.index().forNodes("studies");
        this.seasons = graphDb.index().forNodes("seasons");
        this.locations = graphDb.index().forNodes("locations");
        this.environments = graphDb.index().forNodes("environments");

        this.ecoregions = graphDb.index().forNodes("ecoregions");
        this.ecoregionPaths = graphDb.index().forNodes("ecoregionPath", MapUtil.stringMap(IndexManager.PROVIDER, "lucene", "type", "fulltext"));
        this.ecoregionSuggestions = graphDb.index().forNodes("ecoregionSuggestions");

        this.taxonService = taxonService;

    }

    public static List<Study> findAllStudies(GraphDatabaseService graphService) {
        List<Study> studies = new ArrayList<Study>();
        Index<Node> studyIndex = graphService.index().forNodes("studies");
        IndexHits<Node> hits = studyIndex.query("title", "*");
        for (Node hit : hits) {
            studies.add(new Study(hit));
        }
        return studies;
    }

    public GraphDatabaseService getGraphDb() {
        return graphDb;
    }

    public TaxonNode findTaxon(String taxonName) throws NodeFactoryException {
        return findTaxonOfType(taxonName);
    }

    public TaxonNode findTaxonOfType(String taxonName) throws NodeFactoryException {
        return getTaxonService().findTaxonByName(taxonName);
    }

    public TaxonNode getOrCreateTaxon(String name) throws NodeFactoryException {
        return getOrCreateTaxon(name, null, null);
    }

    public TaxonNode getOrCreateTaxon(String name, String externalId, String path) throws NodeFactoryException {
        return getTaxonService().getOrCreateTaxon(name, externalId, path);
    }

    public Location findLocation(Double latitude, Double longitude, Double altitude) {
        QueryContext queryOrQueryObject = QueryContext.numericRange(Location.LATITUDE, latitude, latitude);
        IndexHits<Node> matchingLocations = locations.query(queryOrQueryObject);
        Node matchingLocation = null;
        for (Node node : matchingLocations) {
            Double foundLongitude = (Double) node.getProperty(Location.LONGITUDE);

            boolean altitudeMatches = false;
            if (node.hasProperty(Location.ALTITUDE)) {
                Double foundAltitude = (Double) node.getProperty(Location.ALTITUDE);
                altitudeMatches = altitude != null && altitude.equals(foundAltitude);
            } else if (null == altitude) {
                // explicit null value matches
                altitudeMatches = true;
            }

            if (longitude.equals(foundLongitude) && altitudeMatches) {
                matchingLocation = node;
                break;
            }

        }
        matchingLocations.close();
        return matchingLocation == null ? null : new Location(matchingLocation);
    }

    public Season createSeason(String seasonNameLower) {
        Transaction transaction = graphDb.beginTx();
        Season season;
        try {
            Node node = graphDb.createNode();
            season = new Season(node, seasonNameLower);
            seasons.add(node, Season.TITLE, seasonNameLower);
            transaction.success();
        } finally {
            transaction.finish();
        }
        return season;
    }

    private Location createLocation(Double latitude, Double longitude, Double altitude) {
        Transaction transaction = graphDb.beginTx();
        Location location;
        try {
            Node node = graphDb.createNode();
            location = new Location(node, latitude, longitude, altitude);
            locations.add(node, Location.LATITUDE, ValueContext.numeric(latitude));
            locations.add(node, Location.LONGITUDE, ValueContext.numeric(longitude));
            if (altitude != null) {
                locations.add(node, Location.ALTITUDE, ValueContext.numeric(altitude));
            }
            transaction.success();
        } finally {
            transaction.finish();
        }
        return location;
    }

    public Specimen createSpecimen(String taxonName) throws NodeFactoryException {
        return createSpecimen(taxonName, null);
    }

    public Specimen createSpecimen(String taxonName, String taxonExternalId) throws NodeFactoryException {
        TaxonNode taxon = getOrCreateTaxon(taxonName, taxonExternalId, null);
        Specimen specimen = createSpecimen(taxon);
        if (taxonName.contains("eggs")) {
            specimen.setLifeStage(new Term("egg", "some id"));
        }
        specimen.setOriginalTaxonDescription(taxonName, taxonExternalId);
        return specimen;
    }


    private Specimen createSpecimen(TaxonNode taxon) {
        Transaction transaction = graphDb.beginTx();
        Specimen specimen;
        try {
            specimen = new Specimen(graphDb.createNode(), null);
            if (taxon != null) {
                specimen.classifyAs(taxon);
            }
            transaction.success();
        } finally {
            transaction.finish();
        }
        return specimen;
    }


    public Study createStudy(String title) {
        return createStudy(title, null, null, null, null, null, null, null);
    }

    private Study createStudy(String title, String contributor, String institution, String period, String description, String publicationYear, String source, String doi) {
        Transaction transaction = graphDb.beginTx();
        Study study;
        try {
            Node node = graphDb.createNode();
            study = new Study(node, title);
            study.setSource(source);
            study.setContributor(contributor);
            study.setInstitution(institution);
            study.setPeriod(period);
            study.setDescription(description);
            study.setPublicationYear(publicationYear);
            if (doiResolver != null) {
                try {
                    if (StringUtils.isBlank(doi)) {
                        doi = findDOIUsingReference(contributor, description, publicationYear);
                    }

                    if (StringUtils.isNotBlank(doi)) {
                        study.setDOI(doi);
                        study.setCitation(doiResolver.findCitationForDOI(doi));
                    }
                } catch (IOException e) {
                    LOG.warn("failed to lookup doi for [" + title + "]");
                }
            }
            studies.add(node, Study.TITLE, title);
            transaction.success();
        } finally {
            transaction.finish();
        }

        return study;
    }

    private String findDOIUsingReference(String contributor, String description, String publicationYear) throws IOException {
        String reference = StringUtils.join(new String[] {contributor, publicationYear, description}, " ");
        return doiResolver.findDOIForReference(reference);
    }

    @Deprecated
    public Study getOrCreateStudy(String title, String contributor, String institution, String period, String description, String publicationYear, String source) {
        return getOrCreateStudy(title, contributor, institution, period, description, publicationYear, source, null);
    }

    @Deprecated
    public Study getOrCreateStudy(String title, String contributor, String institution, String period, String description, String publicationYear, String source, String doi) {
        Study study = findStudy(title);
        if (null == study) {
            study = createStudy(title, contributor, institution, period, description, publicationYear, source, doi);
        }
        return study;
    }

    public Study getOrCreateStudy(String title, String source, String doi) {
        return getOrCreateStudy(title, null, null, null, null, null, source, doi);
    }

    public Study findStudy(String title) {
        Node foundStudyNode = studies.get(Study.TITLE, title).getSingle();
        return foundStudyNode == null ? null : new Study(foundStudyNode);
    }

    public Season findSeason(String seasonName) {
        IndexHits<Node> nodeIndexHits = seasons.get(Season.TITLE, seasonName);
        Node seasonHit = nodeIndexHits.getSingle();
        nodeIndexHits.close();
        return seasonHit == null ? null : new Season(seasonHit);
    }

    public Location getOrCreateLocation(Double latitude, Double longitude, Double altitude) throws NodeFactoryException {
        Location location = null;
        if (latitude != null && longitude != null) {
            validate(latitude, longitude);
            location = findLocation(latitude, longitude, altitude);
            if (null == location) {
                location = createLocation(latitude, longitude, altitude);
                try {
                    enrichLocationWithEcoRegions(location);
                } catch (NodeFactoryException e) {
                    LOG.error("failed to create eco region for location (" + location.getLatitude() + ", " + location.getLongitude() + ")");
                }
            }
        }
        return location;
    }

    private void validate(Double latitude, Double longitude) throws NodeFactoryException {

        if (!LocationUtil.isValidLatitude(latitude)) {
            throw new NodeFactoryException("found invalid latitude [" + latitude + "]");
        }
        if (!LocationUtil.isValidLongitude(longitude)) {
            throw new NodeFactoryException("found invalid longitude [" + longitude + "]");
        }
    }

    public void setUnixEpochProperty(Relationship rel, Date date) {
        if (date != null) {
            Transaction tx = rel.getGraphDatabase().beginTx();
            try {
                rel.setProperty(Specimen.DATE_IN_UNIX_EPOCH, date.getTime());
                tx.success();
            } finally {
                tx.finish();
            }
        }
    }

    public Date getUnixEpochProperty(Relationship rel) {
        Date date = null;
        if (rel != null) {
            if (rel.hasProperty(Specimen.DATE_IN_UNIX_EPOCH)) {
                Long unixEpoch = (Long) rel.getProperty(Specimen.DATE_IN_UNIX_EPOCH);
                date = new Date(unixEpoch);
            }

        }
        return date;
    }

    public List<Environment> getOrCreateEnvironments(Location location, String externalId, String name) throws NodeFactoryException {
        List<org.eol.globi.domain.Term> terms;
        try {
            terms = envoLookupService.lookupTermByName(name);
            if (terms.size() == 0) {
                terms.add(new org.eol.globi.domain.Term(externalId, name));
            }
        } catch (TermLookupServiceException e) {
            throw new NodeFactoryException("failed to lookup environment [" + name + "]");
        }

        return addEnvironmentToLocation(location, terms);
    }

    public List<Environment> addEnvironmentToLocation(Location location, List<Term> terms) {
        List<Environment> normalizedEnvironments = new ArrayList<Environment>();
        for (Term term : terms) {
            Environment environment = findEnvironment(term.getName());
            if (environment == null) {
                Transaction transaction = graphDb.beginTx();
                try {
                    environment = new Environment(graphDb.createNode(), term.getId(), term.getName());
                    environments.add(environment.getUnderlyingNode(), PropertyAndValueDictionary.NAME, term.getName());
                    transaction.success();
                } finally {
                    transaction.finish();
                }
            }
            location.addEnvironment(environment);
            normalizedEnvironments.add(environment);
        }
        return normalizedEnvironments;
    }

    private List<Ecoregion2> getEcoRegions(Node locationNode) {
        Iterable<Relationship> relationships = locationNode.getRelationships(RelTypes.IN_ECO_REGION, Direction.OUTGOING);
        List<Ecoregion2> ecoregion2s = null;
        for (Relationship relationship : relationships) {
            Node ecoregionNode = relationship.getEndNode();
            Ecoregion2 ecoregion2 = new Ecoregion2();
            ecoregion2.setGeometry(NodeUtil.getPropertyStringValueOrNull(ecoregionNode, "geometry"));
            ecoregion2.setName(NodeUtil.getPropertyStringValueOrNull(ecoregionNode, PropertyAndValueDictionary.NAME));
            ecoregion2.setId(NodeUtil.getPropertyStringValueOrNull(ecoregionNode, PropertyAndValueDictionary.EXTERNAL_ID));
            ecoregion2.setPath(NodeUtil.getPropertyStringValueOrNull(ecoregionNode, "path"));
            if (ecoregion2s == null) {
                ecoregion2s = new ArrayList<Ecoregion2>();
            }
            ecoregion2s.add(ecoregion2);
        }
        return ecoregion2s;
    }

    public List<Ecoregion2> enrichLocationWithEcoRegions(Location location) throws NodeFactoryException {
        List<Ecoregion2> associatedEcoregion2s = getEcoRegions(location.getUnderlyingNode());
        return associatedEcoregion2s == null ? associateWithEcoRegions(location) : associatedEcoregion2s;
    }

    private List<Ecoregion2> associateWithEcoRegions(Location location) throws NodeFactoryException {
        List<Ecoregion2> associatedEcoregion2s = new ArrayList<Ecoregion2>();
        try {
            EcoregionFinder2 finder = getEcoregionFinder2();
            if (finder != null) {
                Collection<Ecoregion2> ecoregion2s = finder.findEcoregion(location.getLatitude(), location.getLongitude());
                for (Ecoregion2 ecoregion2 : ecoregion2s) {
                    associateLocationWithEcoRegion(location, ecoregion2);
                    associatedEcoregion2s.add(ecoregion2);
                }
            }
        } catch (EcoregionFinderException2 e) {
            throw new NodeFactoryException("problem finding eco region for location (lat,lng):(" + location.getLatitude() + "," + location.getLongitude() + ")");
        }
        return associatedEcoregion2s;
    }

    private void associateLocationWithEcoRegion(Location location, Ecoregion2 ecoregion2) {
        Node ecoregionNode = findEcoRegion(ecoregion2);
        Transaction tx = graphDb.beginTx();
        try {
            if (ecoregionNode == null) {
                ecoregionNode = addAndIndexEcoRegion(ecoregion2);
            }
            location.getUnderlyingNode().createRelationshipTo(ecoregionNode, RelTypes.IN_ECO_REGION);
            tx.success();
        } finally {
            tx.finish();
        }
    }

    private Node findEcoRegion(Ecoregion2 ecoregion2) {
        String query = "name:\"" + ecoregion2.getName() + "\"";
        IndexHits<Node> hits = this.ecoregions.query(query);
        try {
            return hits.hasNext() ? hits.next() : null;
        } finally {
            hits.close();
        }
    }

    private Node addAndIndexEcoRegion(Ecoregion2 ecoregion2) {
        Node node = graphDb.createNode();
        node.setProperty(PropertyAndValueDictionary.NAME, ecoregion2.getName());
        node.setProperty(PropertyAndValueDictionary.EXTERNAL_ID, ecoregion2.getId());
        node.setProperty("path", ecoregion2.getPath());
        node.setProperty("geometry", ecoregion2.getGeometry());
        ecoregions.add(node, PropertyAndValueDictionary.NAME, ecoregion2.getName());
        ecoregionPaths.add(node, "path", ecoregion2.getPath());
        ecoregionSuggestions.add(node, PropertyAndValueDictionary.NAME, StringUtils.lowerCase(ecoregion2.getName()));
        if (StringUtils.isNotBlank(ecoregion2.getPath())) {
            for (String part : ecoregion2.getPath().split(CharsetConstant.SEPARATOR)) {
                ecoregionSuggestions.add(node, PropertyAndValueDictionary.NAME, StringUtils.lowerCase(part));
            }
        }
        return node;
    }

    protected Environment findEnvironment(String name) {
        String query = "name:\"" + name + "\"";
        IndexHits<Node> matches = environments.query(query);
        Environment firstMatchingEnvironment = null;
        if (matches.hasNext()) {
            firstMatchingEnvironment = new Environment(matches.next());
        }
        matches.close();
        return firstMatchingEnvironment;
    }

    public org.eol.globi.domain.Term getOrCreateBodyPart(String externalId, String name) throws NodeFactoryException {
        return matchTerm(externalId, name);
    }

    public org.eol.globi.domain.Term getOrCreatePhysiologicalState(String externalId, String name) throws NodeFactoryException {
        return matchTerm(externalId, name);
    }

    public org.eol.globi.domain.Term getOrCreateLifeStage(String externalId, String name) throws NodeFactoryException {
        return matchTerm(externalId, name);
    }

    private org.eol.globi.domain.Term matchTerm(String externalId, String name) throws NodeFactoryException {
        try {
            List<org.eol.globi.domain.Term> terms = getTermLookupService().lookupTermByName(name);
            return terms.size() == 0 ? NO_MATCH_TERM : terms.get(0);
        } catch (TermLookupServiceException e) {
            throw new NodeFactoryException("failed to lookup term [" + externalId + "]:[" + name + "]");
        }
    }

    public TermLookupService getTermLookupService() {
        return termLookupService;
    }

    public void setEnvoLookupService(TermLookupService envoLookupService) {
        this.envoLookupService = envoLookupService;
    }

    public void setTermLookupService(TermLookupService termLookupService) {
        this.termLookupService = termLookupService;
    }

    public void setDoiResolver(DOIResolver doiResolver) {
        this.doiResolver = doiResolver;
    }

    public void setEcoregionFinder2(EcoregionFinder2 ecoregionFinder2) {
        this.ecoregionFinder2 = ecoregionFinder2;
    }

    public EcoregionFinder2 getEcoregionFinder2() {
        return ecoregionFinder2;
    }

    public IndexHits<Node> findCloseMatchesForEcoregion(String ecoregionName) {
        return NodeUtil.query(ecoregionName, PropertyAndValueDictionary.NAME, ecoregions);
    }

    public IndexHits<Node> findCloseMatchesForEcoregionPath(String ecoregionPath) {
        return NodeUtil.query(ecoregionPath, PropertyAndValueDictionary.PATH, ecoregionPaths);
    }

    public IndexHits<Node> suggestEcoregionByName(String wholeOrPartialEcoregionNameOrPath) {
        return ecoregionSuggestions.query("name:\"" + wholeOrPartialEcoregionNameOrPath + "\"");
    }

    public void setTaxonService(TaxonService taxonService) {
        this.taxonService = taxonService;
    }

    public TaxonService getTaxonService() {
        return taxonService;
    }
}

