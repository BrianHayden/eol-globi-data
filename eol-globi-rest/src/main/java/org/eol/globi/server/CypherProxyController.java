package org.eol.globi.server;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.eol.globi.util.ExternalIdUtil;
import org.eol.globi.util.InteractUtil;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;

@Controller
public class CypherProxyController {

    public static final String OBSERVATION_MATCH =
            "MATCH (predatorTaxon)<-[:CLASSIFIED_AS]-(predator)-[:ATE]->(prey)-[:CLASSIFIED_AS]->(preyTaxon)," +
                    "(predator)-[:COLLECTED_AT]->(loc)," +
                    "(predator)<-[collected_rel:COLLECTED]-(study) ";

    public static final String INCLUDE_OBSERVATIONS_TRUE = "includeObservations=true";

    public static final String INTERACTION_PREYS_ON = "preysOn";
    public static final String INTERACTION_PREYED_UPON_BY = "preyedUponBy";
    public static final String DEFAULT_RETURN_LIST = "loc.latitude as latitude," +
            "loc.longitude as longitude," +
            "loc.altitude? as altitude," +
            "study.title," +
            "collected_rel.dateInUnixEpoch? as collection_time_in_unix_epoch," +
            "ID(predator) as tmp_and_unique_specimen_id," +
            "predator.lifeStage? as predator_life_stage," +
            "prey.lifeStage? as prey_life_stage," +
            "predator.bodyPart? as predator_body_part," +
            "prey.bodyPart? as prey_body_part," +
            "predator.physiologicalState? as predator_physiological_state," +
            "prey.physiologicalState? as prey_physiological_state\"";

    public static final String INTERACTION_MATCH = "MATCH predatorTaxon<-[:CLASSIFIED_AS]-predator-[:ATE]->prey-[:CLASSIFIED_AS]->preyTaxon ";

    private void addLocationClausesIfNecessary(HttpServletRequest request, StringBuilder query) {
        query.append(" , predator-[:COLLECTED_AT]->loc ");
        query.append(request == null ? "" : RequestHelper.buildCypherSpatialWhereClause(request.getParameterMap()));
    }

    private String buildParams(String scientificName) {
        String params = "{";

        if (scientificName != null) {
            params += "\"scientificName\":\"" + scientificName + "\"";
        }

        params += "}";
        return params;
    }

    @RequestMapping(value = "/interactionTypes", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public String getInteractionTypes(HttpServletRequest request) throws IOException {
        return "[ \"" + INTERACTION_PREYS_ON + "\"," + INTERACTION_PREYED_UPON_BY + "\"]";
    }

    @RequestMapping(value = "/interaction", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public String findInteractions(HttpServletRequest request) throws IOException {
        StringBuilder query = new StringBuilder();
        query.append("{\"query\":\"START loc" + " = node:locations('*:*') " +
                "MATCH predatorTaxon<-[:CLASSIFIED_AS]-predator-[interactionType:" + InteractUtil.allInteractionsCypherClause() + "]->prey-[:CLASSIFIED_AS]->preyTaxon ");
        addLocationClausesIfNecessary(request, query);
        query.append("RETURN predatorTaxon.externalId, predatorTaxon.name as predatorName, type(interactionType), preyTaxon.externalId, preyTaxon.name as preyTaxon\", " +
                "\"params\":" + buildParams(null) + "}");
        return execute(query.toString());
    }

    @RequestMapping(value = "/taxon/{sourceTaxonName}/{interactionType}", method = RequestMethod.GET, produces = "application/json", headers = "content-type=*/*")
    @ResponseBody
    public String findPreyOf(HttpServletRequest request,
                             @PathVariable("sourceTaxonName") String sourceTaxonName,
                             @PathVariable("interactionType") String interactionType) throws IOException {
        return findDistinctTargetTaxonNames(request, sourceTaxonName, interactionType, null);
    }


    @RequestMapping(value = "/taxon/{sourceTaxonName}/{interactionType}/{targetTaxonName}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public String findDistinctTargetTaxonNames(HttpServletRequest request,
                                               @PathVariable("sourceTaxonName") String sourceTaxonName,
                                               @PathVariable("interactionType") String interactionType,
                                               @PathVariable("targetTaxonName") String targetTaxonName) throws IOException {

        StringBuilder query = new StringBuilder();
        if (INTERACTION_PREYS_ON.equals(interactionType)) {
            query.append("{\"query\":\"START ").append(getTaxonSelector(sourceTaxonName, targetTaxonName))
                    .append(" ")
                    .append(INTERACTION_MATCH);
                    addLocationClausesIfNecessary(request, query);
                    query.append("RETURN distinct(preyTaxon.name) as preyName\", ")
                    .append("\"params\":{")
                    .append(getParams(sourceTaxonName, targetTaxonName))
                    .append("}}");
        } else if (INTERACTION_PREYED_UPON_BY.equals(interactionType)) {
            // "preyedUponBy is inverted interaction of "preysOn"
            query.append("{\"query\":\"START ").append(getTaxonSelector(targetTaxonName, sourceTaxonName))
                    .append(" ")
                    .append(INTERACTION_MATCH);
                    addLocationClausesIfNecessary(request, query);
                    query.append("RETURN distinct(predatorTaxon.name) as preyName\", ")
                    .append("\"params\":{")
                    .append(getParams(targetTaxonName, sourceTaxonName))
                    .append("}}");
        }

        if (query.length() == 0) {
            throw new IllegalArgumentException("unsupported interaction type [" + interactionType + "]");
        }

        return execute(query.toString());
    }

    @RequestMapping(value = "/findTaxon/{taxonName}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public String findTaxon(@PathVariable("taxonName") String taxonName) throws IOException {
        String query = "{\"query\":\"START taxon = node:taxons('*:*') " +
                "WHERE taxon.name =~ '" + taxonName + ".*'" +
                "RETURN distinct(taxon.name) " +
                "LIMIT 15\"}";
        return execute(query);
    }

    @RequestMapping(value = "/taxon/{sourceTaxonName}/{interactionType}/{targetTaxonName}", params = {INCLUDE_OBSERVATIONS_TRUE}, method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public String findObservationsOf(HttpServletRequest request,
                                     @PathVariable("sourceTaxonName") String sourceTaxonName,
                                     @PathVariable("interactionType") String interactionType,
                                     @PathVariable("targetTaxonName") String targetTaxonName)
            throws IOException {
        String query = null;

        if (INTERACTION_PREYS_ON.equals(interactionType)) {
            query = "{\"query\":\"START " + getTaxonSelector(sourceTaxonName, targetTaxonName) +
                    OBSERVATION_MATCH +
                    getSpatialWhereClause(request) +
                    " RETURN preyTaxon.name as preyName, " +
                    DEFAULT_RETURN_LIST +
                    ", \"params\": { " + getParams(sourceTaxonName, targetTaxonName) + " } }";
        } else if (INTERACTION_PREYED_UPON_BY.equals(interactionType)) {
            // note that "preyedUponBy" is interpreted as an inverted "preysOn" relationship
            query = "{\"query\":\"START " + getTaxonSelector(targetTaxonName, sourceTaxonName) +
                    OBSERVATION_MATCH +
                    getSpatialWhereClause(request) +
                    " RETURN predatorTaxon.name as predatorName, " +
                    DEFAULT_RETURN_LIST +
                    ", \"params\": { " + getParams(targetTaxonName, sourceTaxonName) + " } }";
        }
        if (query == null) {
            throw new IllegalArgumentException("unsupported interaction type [" + interactionType + "]");
        }

        return execute(query);
    }

    private String getParams(String sourceTaxonName, String targetTaxonName) {
        final String sourceTaxonNameParam = "\"predatorName\" : \"" + sourceTaxonName + "\"";
        final String targetTaxonNameParam = "\"preyName\" : \"" + targetTaxonName + "\"";
        StringBuilder builder = new StringBuilder();
        if (sourceTaxonName != null) {
            builder.append(sourceTaxonNameParam);
        }
        if (targetTaxonName != null) {
            if (sourceTaxonName != null) {
                builder.append(", ");
            }

            builder.append(targetTaxonNameParam);
        }

        return builder.toString();
    }

    private String getTaxonSelector(String sourceTaxonName, String targetTaxonName) {
        final String sourceTaxonSelector = "predatorTaxon = node:taxons(name={predatorName})";
        final String targetTaxonSelector = "preyTaxon = node:taxons(name={preyName})";
        StringBuilder builder = new StringBuilder();
        if (sourceTaxonName != null) {
            builder.append(sourceTaxonSelector);
        }
        if (targetTaxonName != null) {
            if (sourceTaxonName != null) {
                builder.append(", ");
            }
            builder.append(targetTaxonSelector);
        }

        return builder.toString();
    }


    @RequestMapping(value = "/taxon/{sourceTaxonName}/{interactionType}", params = {INCLUDE_OBSERVATIONS_TRUE}, method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public String findObservationsOf(HttpServletRequest request,
                                     @PathVariable("sourceTaxonName") String sourceTaxonName,
                                     @PathVariable("interactionType") String interactionType) throws IOException {
        return findObservationsOf(request, sourceTaxonName, interactionType, null);
    }


    private String getSpatialWhereClause(HttpServletRequest request) {
        return request == null ? "" : RequestHelper.buildCypherSpatialWhereClause(request.getParameterMap());
    }

    @RequestMapping(value = "/locations", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @Cacheable(value = "locationCache")
    public String locations() throws IOException {
        return execute("{\"query\":\"START loc = node:locations('*:*') RETURN loc.latitude, loc.longitude\"}");
    }


    @RequestMapping(value = "/contributors", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @Cacheable(value = "contributorCache")
    public String contributors() throws IOException {
        String query = "{\"query\":\"START study=node:studies('*:*')" +
                " MATCH study-[:COLLECTED]->sourceSpecimen-[interact:" + InteractUtil.allInteractionsCypherClause() + "]->prey-[:CLASSIFIED_AS]->targetTaxon, sourceSpecimen-[:CLASSIFIED_AS]->sourceTaxon " +
                " RETURN study.institution, study.period, study.description, study.contributor, count(interact), count(distinct(sourceTaxon)), count(distinct(targetTaxon))\", \"params\": { } }";
        return execute(query);
    }


    private String execute(String query) throws IOException {
        org.apache.http.client.HttpClient httpclient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost("http://46.4.36.142:7474/db/data/cypher");
        HttpClient.addJsonHeaders(httpPost);
        httpPost.setEntity(new StringEntity(query));
        BasicResponseHandler responseHandler = new BasicResponseHandler();
        return httpclient.execute(httpPost, responseHandler);
    }


    @RequestMapping(value = "/findExternalUrlForTaxon/{taxonName}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public String findExternalLinkForTaxonWithName(@PathVariable("taxonName") String taxonName) throws IOException {
        String result = findExternalIdForTaxon(taxonName);

        String url = null;
        for (Map.Entry<String, String> stringStringEntry : ExternalIdUtil.getURLPrefixMap().entrySet()) {
            url = getUrl(result, stringStringEntry.getKey(), stringStringEntry.getValue());
            if (url != null) {
                break;
            }

        }
        return buildJsonUrl(url);
    }

    @RequestMapping(value = "/findExternalIdForTaxon/{taxonName}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public String findExternalIdForTaxon(String taxonName) throws IOException {
        String query = "{\"query\":\"START taxon = node:taxons(name={taxonName}) " +
                " RETURN taxon.externalId as externalId\"" +
                ", \"params\": { \"taxonName\" : \"" + taxonName + "\" } }";

        return execute(query);
    }

    private String buildJsonUrl(String url) {
        return url == null ? "{}" : "{\"url\":\"" + url + "\"}";
    }

    @RequestMapping(value = "/findExternalUrlForExternalId/{externalId}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public String findExternalLinkForExternalId(@PathVariable("externalId") String externalId) {
        return buildJsonUrl(ExternalIdUtil.infoURLForExternalId(externalId));
    }

    @RequestMapping(value = "/shortestPathsBetweenTaxon/{startTaxon}/andTaxon/{endTaxon}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public String findShortestPaths(@PathVariable("startTaxon") String startTaxon, @PathVariable("endTaxon") String endTaxon) throws IOException {
        String query = "{\"query\":\"START startNode = node:taxons(name={startTaxon}),endNode = node:taxons(name={endTaxon}) " +
                "MATCH p = allShortestPaths(startNode-[:" + InteractUtil.allInteractionsCypherClause() + "|CLASSIFIED_AS*..100]-endNode) " +
                "RETURN extract(n in (filter(x in nodes(p) : has(x.name))) : " +
                "coalesce(n.name?)) as shortestPaths " +
                "LIMIT 10\",\"params\":{\"startTaxon\": \"" + startTaxon + "\", \"endTaxon\":\"" + endTaxon + "\"}}";
        return execute(query);
    }

    private String getUrl(String result, String externalIdPrefix, String urlPrefix) {
        String url = "";
        if (result.contains(externalIdPrefix)) {
            String[] split = result.split(externalIdPrefix);
            if (split.length > 1) {
                String[] externalIdParts = split[1].split("\"");
                if (externalIdParts.length > 1) {
                    url = urlPrefix + externalIdParts[0];
                }
            }
        }
        return url;
    }

}