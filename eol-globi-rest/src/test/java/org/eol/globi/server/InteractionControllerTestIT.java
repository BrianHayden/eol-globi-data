package org.eol.globi.server;

import org.apache.commons.lang3.ArrayUtils;
import org.eol.globi.server.util.ResultFields;
import org.eol.globi.util.CypherQuery;
import org.eol.globi.util.CypherUtil;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;
import static org.mockito.Mockito.when;

public class InteractionControllerTestIT {

    @Test
    public void findPrey() throws IOException, URISyntaxException {
        String list = new CypherQueryExecutor(new InteractionController().findInteractionsNew(null, "Homo sapiens", CypherQueryBuilder.INTERACTION_PREYS_ON, null)).execute(null);
        assertThat(list, containsString("Homo sapiens"));
    }

    @Test
    public void findSupportedInteractionTypes() throws IOException, URISyntaxException {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getParameterMap()).thenReturn(new HashMap<String, String[]>() {
            {
                put("taxon", new String[]{"Phocidae"});
            }
        });
        when(request.getParameter("taxon")).thenReturn("something");
        when(request.getParameter("type")).thenReturn("csv");
        String list = new InteractionController().getInteractionTypes(request);
        assertThat(list, not(containsString("pollinate")));
        assertThat(list, containsString("preysOn"));
    }

    @Test
    public void findSupportedInteractionTypesById() throws IOException, URISyntaxException {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getParameterMap()).thenReturn(new HashMap<String, String[]>() {
            {
                put("taxon", new String[]{"EOL:7666"});
            }
        });
        when(request.getParameter("taxon")).thenReturn("something");
        when(request.getParameter("type")).thenReturn("csv");
        String list = new InteractionController().getInteractionTypes(request);
        assertThat(list, not(containsString("pollinate")));
        assertThat(list, containsString("preysOn"));
    }

    @Test
    public void findSupportedInteractionTypesBees() throws IOException, URISyntaxException {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getParameterMap()).thenReturn(new HashMap<String, String[]>() {
            {
                put("taxon", new String[]{"Apidae"});
            }
        });
        when(request.getParameter("taxon")).thenReturn("something");
        when(request.getParameter("type")).thenReturn("csv");
        String list = new InteractionController().getInteractionTypes(request);
        assertThat(list, containsString("pollinate"));
        assertThat(list, not(containsString("pathogenOf")));
    }

    @Test
    public void findPreyExternalId() throws IOException, URISyntaxException {
        String list = new CypherQueryExecutor(new InteractionController().findInteractionsNew(null, "OTT:770315", CypherQueryBuilder.INTERACTION_PREYS_ON, null)).execute(null);
        assertThat(list, containsString("Homo sapiens"));
    }

    @Test
    public void findThunnusPrey() throws IOException, URISyntaxException {
        // see https://github.com/jhpoelen/eol-globi-data/issues/11
        String list = new CypherQueryExecutor(new InteractionController().findInteractionsNew(null, "Thunnus", CypherQueryBuilder.INTERACTION_PREYS_ON, null)).execute(null);
        assertThat(list, containsString("Thunnus alalunga"));
        assertThat(list, containsString("Thunnus albacares"));
    }

    @Test
    public void findPreyAtLocation() throws IOException, URISyntaxException {
        HttpServletRequest request = getLocationRequest();
        String list = new CypherQueryExecutor(new InteractionController().findInteractionsNew(request, "Homo sapiens", CypherQueryBuilder.INTERACTION_PREYS_ON, null)).execute(request);
        assertThat(list, is(notNullValue()));
    }

    private HttpServletRequest getLocationRequest() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getParameterMap()).thenReturn(new HashMap<String, String[]>() {
            {
                put("lat", new String[]{"18.24829"});
                put("lng", new String[]{"-66.49989"});
            }
        });
        return request;
    }

    private HttpServletRequest getLocationBoxRequest() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getParameterMap()).thenReturn(new HashMap<String, String[]>() {
            {
                put("nw_lat", new String[]{"18.34"});
                put("nw_lng", new String[]{"-66.50"});
                put("se_lat", new String[]{"18.14"});
                put("se_lng", new String[]{"-66.48"});
            }
        });
        return request;
    }


    @Test
    public void findPreyAtLocationNoLongitude() throws IOException, URISyntaxException {
        String list = new CypherQueryExecutor(new InteractionController().findInteractionsNew(null, "Homo sapiens", CypherQueryBuilder.INTERACTION_PREYS_ON, null)).execute(null);
        assertThat(list, is(notNullValue()));
    }

    @Test
    public void findPreyAtLocationNoLatitude() throws IOException, URISyntaxException {
        String list = new CypherQueryExecutor(new InteractionController().findInteractionsNew(null, "Homo sapiens", CypherQueryBuilder.INTERACTION_PREYS_ON, null)).execute(null);
        assertThat(list, is(notNullValue()));
    }

    @Test
    public void findPredator() throws IOException, URISyntaxException {
        String list = CypherUtil.executeRemote(InteractionController.createQuery(null, CypherQueryBuilder.INTERACTION_PREYS_ON, "Hemiramphus brasiliensis", null, CypherQueryBuilder.QueryType.SINGLE_TAXON_DISTINCT));
        assertThat(list, is(notNullValue()));
    }

    @Test
    public void findTargetsForSource() throws IOException, URISyntaxException {
        String list = CypherUtil.executeRemote(InteractionController.createQuery("Homo sapiens", CypherQueryBuilder.INTERACTION_PREYS_ON, "Hemiramphus brasiliensis", null, CypherQueryBuilder.QueryType.SINGLE_TAXON_DISTINCT));
        assertThat(list, is(notNullValue()));
    }


    @Test
    public void findPredatorObservations() throws IOException, URISyntaxException {
        String list = new CypherQueryExecutor(new InteractionController().findInteractionsNew(null, "Ariopsis felis", CypherQueryBuilder.INTERACTION_PREYS_ON, null)).execute(null);
        assertThat(list, is(notNullValue()));
        list = new CypherQueryExecutor(new InteractionController().findInteractionsNew(null, "Rattus rattus", CypherQueryBuilder.INTERACTION_PREYS_ON, null)).execute(null);
        assertThat(list, is(notNullValue()));
    }

    @Test
    public void findPredatorDistinctCSV() throws IOException, URISyntaxException {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getParameter("type")).thenReturn("csv");

        String list = new CypherQueryExecutor(new InteractionController().findInteractionsNew(request, "Ariopsis felis", CypherQueryBuilder.INTERACTION_PREYS_ON, null)).execute(request);
        String[] rows = list.split("\n");
        String[] rows_no_header = ArrayUtils.remove(rows, 0);
        assertThat(rows_no_header.length > 0, is(true));

        for (String row : rows_no_header) {
            String[] columns = row.split("\",");
            assertThat(columns[0], is("\"Ariopsis felis"));
            assertThat(columns.length, is(3));
        }
    }

    @Test
    public void findPredatorObservationsCSV() throws IOException, URISyntaxException {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getParameterMap()).thenReturn(new HashMap<String, String>() {
            {
                put("includeObservations", "true");
                put("type", "csv");
            }
        });
        when(request.getParameter("type")).thenReturn("csv");
        when(request.getParameter("includeObservations")).thenReturn("true");

        String list = new CypherQueryExecutor(new InteractionController().findInteractionsNew(request, "Ariopsis felis", CypherQueryBuilder.INTERACTION_PREYS_ON, null)).execute(request);
        assertThat(list, allOf(containsString("\"latitude\",\"longitude\""), not(containsString(",null,"))));
    }

    @Test
    public void findPredatorObservationsJSONv2() throws IOException, URISyntaxException {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getParameter("type")).thenReturn("json.v2");
        when(request.getParameter("includeObservations")).thenReturn("true");

        String list = new CypherQueryExecutor(new InteractionController().findInteractionsNew(request, "Ariopsis felis", CypherQueryBuilder.INTERACTION_PREYS_ON, null)).execute(request);
        assertThat(list, containsString("\"source\":"));
        assertThat(list, containsString("\"target\":"));
        assertThat(list, containsString("\"type\":\"preysOn\""));
    }

    @Test
    public void findPreyOfJSONv2() throws IOException, URISyntaxException {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getParameter("type")).thenReturn("json.v2");

        String list = new CypherQueryExecutor(new InteractionController().findInteractionsNew(request, "Ariopsis felis", CypherQueryBuilder.INTERACTION_PREYS_ON, null)).execute(request);
        assertThat(list, allOf(containsString("\"source\":"), containsString("\"target\":"), containsString("\"type\":\"preysOn\"")));
    }

    @Test
    public void findPreyObservations() throws IOException, URISyntaxException {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getParameter("includeObservations")).thenReturn("true");


        String list = new CypherQueryExecutor(new InteractionController().findInteractionsNew(request, "Rattus rattus", CypherQueryBuilder.INTERACTION_PREYED_UPON_BY, null)).execute(request);
        assertThat(list, is(notNullValue()));

        list = new CypherQueryExecutor(new InteractionController().findInteractionsNew(request, "Ariopsis felis", CypherQueryBuilder.INTERACTION_PREYED_UPON_BY, null)).execute(request);
        assertThat(list, is(notNullValue()));
    }


    @Test
    public void findPredatorPreyObservations() throws IOException, URISyntaxException {
        String list = new CypherQueryExecutor(new InteractionController().findInteractionsNew(null, "Rattus rattus", CypherQueryBuilder.INTERACTION_PREYED_UPON_BY, "Homo sapiens")).execute(null);
        assertThat(list, is(notNullValue()));

        list = new CypherQueryExecutor(new InteractionController().findInteractionsNew(null, "Ariopsis felis", CypherQueryBuilder.INTERACTION_PREYS_ON, "Rattus rattus")).execute(null);
        assertThat(list, is(notNullValue()));
    }

    @Test
    public void findInteractions() throws IOException {
        HttpServletRequest request = getLocationRequest();
        CypherQuery query = new InteractionController().findInteractionsNew(request);
        String externalLink = new CypherQueryExecutor(query.getQuery(), query.getParams()).execute(request);
        assertThat(externalLink, containsString("ate"));
        assertThat(externalLink, containsString(ResultFields.SOURCE_TAXON_PATH));
        assertThat(externalLink, containsString(ResultFields.TARGET_TAXON_PATH));
    }

    @Test
    public void findInteractionsBox() throws IOException {
        HttpServletRequest request = getLocationBoxRequest();
        CypherQuery query = new InteractionController().findInteractionsNew(request);
        String externalLink = new CypherQueryExecutor(query.getQuery(), query.getParams()).execute(request);
        assertThat(externalLink, containsString("ate"));
        assertThat(externalLink, containsString(ResultFields.SOURCE_TAXON_PATH));
        assertThat(externalLink, containsString(ResultFields.TARGET_TAXON_PATH));
    }


}
