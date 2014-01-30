package org.eol.globi.server;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.util.InteractUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CypherQueryBuilder {
    public static final String DEFAULT_LIMIT_CLAUSE = "LIMIT 512";
    public static final String SOURCE_TAXON_HTTP_PARAM_NAME = "sourceTaxon";
    public static final String TARGET_TAXON_HTTP_PARAM_NAME = "targetTaxon";
    public static final String OBSERVATION_MATCH =
            "MATCH (sourceTaxon)<-[:CLASSIFIED_AS]-(sourceSpecimen)-[:ATE]->(targetSpecimen)-[:CLASSIFIED_AS]->(targetTaxon)," +
                    "(sourceSpecimen)-[:COLLECTED_AT]->(loc)," +
                    "(sourceSpecimen)<-[collected_rel:COLLECTED]-(study) ";
    public static final String INTERACTION_MATCH = "MATCH sourceTaxon<-[:CLASSIFIED_AS]-sourceSpecimen-[:ATE]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon ";
    public static final String INTERACTION_PREYS_ON = "preysOn";
    public static final String INTERACTION_PREYED_UPON_BY = "preyedUponBy";
    static final Map<String, String> EMPTY_PARAMS = new HashMap<String, String>();

    public static void addLocationClausesIfNecessary(StringBuilder query, Map parameterMap) {
        query.append(" , sourceSpecimen-[:COLLECTED_AT]->loc ");
        query.append(parameterMap == null ? "" : RequestHelper.buildCypherSpatialWhereClause(parameterMap));
    }

    public static void addTaxonStartClausesIfNecessary(StringBuilder query, Map parameterMap) {
        if (parameterMap.containsKey(SOURCE_TAXON_HTTP_PARAM_NAME)) {
            String luceneQuery = buildLuceneQuery(parameterMap.get(SOURCE_TAXON_HTTP_PARAM_NAME));
            query.append(", sourceTaxon = node:taxonpaths(\'" + luceneQuery + "\')");
        }
        if (parameterMap.containsKey(TARGET_TAXON_HTTP_PARAM_NAME)) {
            String luceneQuery = buildLuceneQuery(parameterMap.get(TARGET_TAXON_HTTP_PARAM_NAME));
            query.append(", targetTaxon = node:taxonpaths(\'" + luceneQuery + "\')");
        }
    }

    public static String buildLuceneQuery(Object paramObject) {
        List<String> taxonNames = new ArrayList<String>();
        if (paramObject instanceof String[]) {
            String[] names = (String[]) paramObject;
            for (String name : names) {
                taxonNames.add(lucenePathQuery(name));
            }
        } else if (paramObject instanceof String) {
            taxonNames.add(lucenePathQuery((String) paramObject));
        }

        return StringUtils.join(taxonNames, " OR ");
    }

    public static String lucenePathQuery(String targetTaxonName) {
        return "path:\\\"" + targetTaxonName + "\\\"";
    }

    public static CypherQuery interactionObservations(String sourceTaxonName, String interactionType, String targetTaxonName, Map parameterMap) {
        Map<String, String> queryParams;
        StringBuilder query = new StringBuilder();
        boolean isInvertedInteraction = INTERACTION_PREYED_UPON_BY.equals(interactionType);

        String predatorPrefix = isInvertedInteraction ? ResultFields.PREFIX_TARGET_SPECIMEN : ResultFields.PREFIX_SOURCE_SPECIMEN;
        String preyPrefix = isInvertedInteraction ? ResultFields.PREFIX_SOURCE_SPECIMEN : ResultFields.PREFIX_TARGET_SPECIMEN;

        final StringBuilder returnClause = new StringBuilder();
        returnClause.append("loc." + Location.LATITUDE + " as ").append(ResultFields.LATITUDE)
                .append(",loc." + Location.LONGITUDE + " as ").append(ResultFields.LONGITUDE)
                .append(",loc." + Location.ALTITUDE + "? as ").append(ResultFields.ALTITUDE)
                .append(",study." + Study.TITLE + " as ").append(ResultFields.STUDY_TITLE)
                .append(",collected_rel.dateInUnixEpoch? as ").append(ResultFields.COLLECTION_TIME_IN_UNIX_EPOCH)
                .append(",ID(sourceSpecimen) as tmp_and_unique_")
                .append(predatorPrefix).append("_id,")
                .append("ID(targetSpecimen) as tmp_and_unique_")
                .append(preyPrefix).append("_id,")
                .append("sourceSpecimen." + Specimen.LIFE_STAGE_LABEL + "? as ").append(predatorPrefix).append(ResultFields.SUFFIX_LIFE_STAGE).append(",")
                .append("targetSpecimen." + Specimen.LIFE_STAGE_LABEL + "? as ").append(preyPrefix).append(ResultFields.SUFFIX_LIFE_STAGE).append(",")
                .append("sourceSpecimen." + Specimen.BODY_PART_LABEL + "? as ").append(predatorPrefix).append(ResultFields.SUFFIX_BODY_PART).append(",")
                .append("targetSpecimen." + Specimen.BODY_PART_LABEL + "? as ").append(preyPrefix).append(ResultFields.SUFFIX_BODY_PART).append(",")
                .append("sourceSpecimen." + Specimen.PHYSIOLOGICAL_STATE_LABEL + "? as ").append(predatorPrefix).append(ResultFields.SUFFIX_PHYSIOLOGICAL_STATE).append(",")
                .append("targetSpecimen." + Specimen.PHYSIOLOGICAL_STATE_LABEL + "? as ").append(preyPrefix).append(ResultFields.SUFFIX_PHYSIOLOGICAL_STATE).append(",")
                .append("targetSpecimen." + Specimen.TOTAL_COUNT + "? as ").append(preyPrefix).append("_total_count").append(",")
                .append("targetSpecimen." + Specimen.TOTAL_VOLUME_IN_ML + "? as ").append(preyPrefix).append("_total_volume_ml").append(",")
                .append("targetSpecimen." + Specimen.FREQUENCY_OF_OCCURRENCE + "? as ").append(preyPrefix).append("_frequency_of_occurrence");

        if (INTERACTION_PREYS_ON.equals(interactionType)) {
            query.append("START ").append(getTaxonSelector(sourceTaxonName, targetTaxonName)).append(" ")
                    .append(OBSERVATION_MATCH)
                    .append(getSpatialWhereClause(parameterMap))
                    .append(" RETURN ")
                    .append("sourceTaxon.name as ").append(ResultFields.SOURCE_TAXON_NAME)
                    .append(",'").append(interactionType).append("' as ").append(ResultFields.INTERACTION_TYPE)
                    .append(",targetTaxon.name as ").append(ResultFields.TARGET_TAXON_NAME).append(", ")
                    .append(returnClause);
            queryParams = getParams(sourceTaxonName, targetTaxonName);
        } else if (isInvertedInteraction) {
            // note that "preyedUponBy" is interpreted as an inverted "preysOn" relationship
            query.append("START ").append(getTaxonSelector(targetTaxonName, sourceTaxonName)).append(" ")
                    .append(OBSERVATION_MATCH)
                    .append(getSpatialWhereClause(parameterMap))
                    .append(" RETURN ")
                    .append("targetTaxon.name as ").append(ResultFields.SOURCE_TAXON_NAME)
                    .append(",'").append(interactionType).append("' as ").append(ResultFields.INTERACTION_TYPE)
                    .append(",sourceTaxon.name as ").append(ResultFields.TARGET_TAXON_NAME).append(", ")
                    .append(returnClause);
            queryParams = getParams(targetTaxonName, sourceTaxonName);
        } else {
            throw new IllegalArgumentException("unsupported interaction type [" + interactionType + "]");
        }

        return new CypherQuery(query.toString(), queryParams);
    }

    static Map<String, String> getParams(String sourceTaxonName, String targetTaxonName) {
        Map<String, String> paramMap = new HashMap<String, String>();
        if (sourceTaxonName != null) {
            paramMap.put(ResultFields.SOURCE_TAXON_NAME, lucenePathQuery(sourceTaxonName));
        }

        if (targetTaxonName != null) {
            paramMap.put(ResultFields.TARGET_TAXON_NAME, lucenePathQuery(targetTaxonName));
        }
        return paramMap;
    }

    static String getTaxonSelector(String sourceTaxonName, String targetTaxonName) {
        StringBuilder builder = new StringBuilder();
        if (sourceTaxonName != null) {
            final String sourceTaxonSelector = "sourceTaxon = " + getTaxonPathSelector(ResultFields.SOURCE_TAXON_NAME);
            builder.append(sourceTaxonSelector);
        }
        if (targetTaxonName != null) {
            if (sourceTaxonName != null) {
                builder.append(", ");
            }
            final String targetTaxonSelector = "targetTaxon = " + getTaxonPathSelector(ResultFields.TARGET_TAXON_NAME);
            builder.append(targetTaxonSelector);
        }

        return builder.toString();
    }

    private static String getTaxonPathSelector(String taxonParamName) {
        return "node:taxonpaths({" + taxonParamName + "})";
    }

    private static String getSpatialWhereClause(Map parameterMap) {
        return parameterMap == null ? "" : RequestHelper.buildCypherSpatialWhereClause(parameterMap);
    }

    public static CypherQuery shortestPathQuery(final String startTaxon, final String endTaxon) {
        String query = "START startNode = node:taxons(name={startTaxon}),endNode = node:taxons(name={endTaxon}) " +
                "MATCH p = allShortestPaths(startNode-[:" + InteractUtil.allInteractionsCypherClause() + "|CLASSIFIED_AS*..100]-endNode) " +
                "RETURN extract(n in (filter(x in nodes(p) : has(x.name))) : " +
                "coalesce(n.name?)) as shortestPaths " +
                "LIMIT 10";


        HashMap<String, String> params = new HashMap<String, String>() {{
            put("startTaxon", startTaxon);
            put("endTaxon", endTaxon);
        }};

        return new CypherQuery(query, params);
    }

    public static CypherQuery externalIdForStudy(final String studyTitle) {
        String query = "START study = node:studies(title={studyTitle}) " +
                " RETURN study.externalId? as study";

        HashMap<String, String> params = new HashMap<String, String>() {{
            put("studyTitle", studyTitle);
        }};

        return new CypherQuery(query, params);
    }

    public static CypherQuery externalIdForTaxon(final String taxonName) {
        String query = "START taxon = node:taxons(name={taxonName}) " +
                " RETURN taxon.externalId? as externalId";

        HashMap<String, String> taxonName1 = new HashMap<String, String>() {{
            put("taxonName", taxonName);
        }};

        return new CypherQuery(query, taxonName1);
    }

    public static CypherQuery stats(final String source) {
        String whereClause = StringUtils.isBlank(source) ? "" : " WHERE study.source = {source}";
        Map<String, String> params = StringUtils.isBlank(source) ? EMPTY_PARAMS : new HashMap<String, String>() {{
            put("source", source);
        }};
        String cypherQuery = "START study=node:studies('*:*')" +
                " MATCH study-[:COLLECTED]->sourceSpecimen-[interact:" + InteractUtil.allInteractionsCypherClause() + "]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon, sourceSpecimen-[:CLASSIFIED_AS]->sourceTaxon " +
                whereClause +
                " RETURN count(distinct(study)) as `number of studies`, count(interact) as `number of interactions`, count(distinct(sourceTaxon)) as `number of distinct source taxa (e.g. predators)`, count(distinct(targetTaxon)) as `number of distinct target taxa (e.g. prey)`, count(distinct(study.source)) as `number of distinct study sources`";

        return new CypherQuery(cypherQuery, params);
    }

    public static CypherQuery references(final String source) {
        String whereClause = StringUtils.isBlank(source) ? "" : " WHERE study.source = {source}";
        Map<String, String> params = StringUtils.isBlank(source) ? EMPTY_PARAMS : new HashMap<String, String>() {{
            put("source", source);
        }};
        String cypherQuery = "START study=node:studies('*:*')" +
                " MATCH study-[:COLLECTED]->sourceSpecimen-[interact:" + InteractUtil.allInteractionsCypherClause() + "]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon, sourceSpecimen-[:CLASSIFIED_AS]->sourceTaxon " +
                whereClause +
                " RETURN study.institution?, study.period?, study.description, study.contributor?, count(interact), count(distinct(sourceTaxon)), count(distinct(targetTaxon)), study.title, study.citation?, study.doi?, study.source";

        return new CypherQuery(cypherQuery, params);
    }

    public static CypherQuery locations() {
        String query = "START loc = node:locations('*:*') RETURN loc.latitude, loc.longitude";
        return new CypherQuery(query);
    }

    public static CypherQuery findTaxon(String taxonName) {
        String query = "START taxon = node:taxons('*:*') " +
                "WHERE taxon.name =~ '" + taxonName + ".*'" +
                "RETURN distinct(taxon.name) " +
                "LIMIT 15";
        return new CypherQuery(query);
    }

    public static CypherQuery distinctInteractions(String sourceTaxonName, String interactionType, String targetTaxonName, Map parameterMap) {
        StringBuilder query = new StringBuilder();
        Map<String, String> params = EMPTY_PARAMS;
        if (INTERACTION_PREYS_ON.equals(interactionType)) {
            query.append("START ").append(getTaxonSelector(sourceTaxonName, targetTaxonName))
                    .append(" ")
                    .append(INTERACTION_MATCH);
            addLocationClausesIfNecessary(query, parameterMap);
            query.append("RETURN sourceTaxon.name as " + ResultFields.SOURCE_TAXON_NAME + ", '" + interactionType + "' as " + ResultFields.INTERACTION_TYPE + ", collect(distinct(targetTaxon.name)) as " + ResultFields.TARGET_TAXON_NAME);
            params = getParams(sourceTaxonName, targetTaxonName);
        } else if (INTERACTION_PREYED_UPON_BY.equals(interactionType)) {
            // "preyedUponBy is inverted interaction of "preysOn"
            query.append("START ").append(getTaxonSelector(targetTaxonName, sourceTaxonName))
                    .append(" ")
                    .append(INTERACTION_MATCH);
            addLocationClausesIfNecessary(query, parameterMap);
            query.append("RETURN targetTaxon.name as " + ResultFields.SOURCE_TAXON_NAME + ", '" + interactionType + "' as " + ResultFields.INTERACTION_TYPE + ", collect(distinct(sourceTaxon.name)) as " + ResultFields.TARGET_TAXON_NAME);
            params = getParams(targetTaxonName, sourceTaxonName);
        }

        if (query.length() == 0) {
            throw new IllegalArgumentException("unsupported interaction type [" + interactionType + "]");
        }

        return new CypherQuery(query.toString(), params);
    }

    public static CypherQuery sourcesQuery() {
        String cypherQuery = "START study=node:studies('*:*')" +
                " RETURN distinct(study.source)";
        return new CypherQuery(cypherQuery, EMPTY_PARAMS);
    }

    public CypherQuery buildInteractionQuery(Map parameterMap) {
        StringBuilder query = new StringBuilder();
        query.append("START loc = node:locations('*:*') ");
        addTaxonStartClausesIfNecessary(query, parameterMap);

        query.append(" MATCH sourceTaxon<-[:CLASSIFIED_AS]-sourceSpecimen-[interactionType:")
                .append(InteractUtil.allInteractionsCypherClause())
                .append("]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon ");
        addLocationClausesIfNecessary(query, parameterMap);

        query.append("RETURN sourceTaxon.externalId? as ").append(ResultFields.SOURCE_TAXON_EXTERNAL_ID)
                .append(",sourceTaxon.name as ").append(ResultFields.SOURCE_TAXON_NAME)
                .append(",sourceTaxon.path? as ").append(ResultFields.SOURCE_TAXON_PATH)
                .append(",type(interactionType) as ").append(ResultFields.INTERACTION_TYPE)
                .append(",targetTaxon.externalId? as ").append(ResultFields.TARGET_TAXON_EXTERNAL_ID)
                .append(",targetTaxon.name as ").append(ResultFields.TARGET_TAXON_NAME)
                .append(",targetTaxon.path? as ").append(ResultFields.TARGET_TAXON_PATH);

        // fix quick before introducing smarter way to chunk the results
        query.append(" ");
        query.append(DEFAULT_LIMIT_CLAUSE);
        return new CypherQuery(query.toString());
    }


}
