package org.eol.globi.server;

import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;
import static org.mockito.Mockito.when;

public class CypherProxyControllerTest {

    @Test
    public void findPrey() throws IOException, URISyntaxException {
        String list = new CypherProxyController().findDistinctTargetTaxonNames(null, "Homo sapiens", CypherProxyController.INTERACTION_PREYS_ON, null);
        assertThat(list, is(notNullValue()));
    }

    @Test
    public void findPreyAtLocation() throws IOException, URISyntaxException {
        String list = new CypherProxyController().findDistinctTargetTaxonNames(getRequest(), "Homo sapiens", CypherProxyController.INTERACTION_PREYS_ON, null);
        assertThat(list, is(notNullValue()));
    }

    private HttpServletRequest getRequest() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getParameterMap()).thenReturn(new HashMap<String, String[]>() {
            {
                put("lat", new String[]{"18.24829"});
                put("lng", new String[]{"-66.49989"});
            }
        });
        return request;
    }

    @Test
    public void findPreyAtLocationNoLongitude() throws IOException, URISyntaxException {
        String list = new CypherProxyController().findDistinctTargetTaxonNames(null, "Homo sapiens", CypherProxyController.INTERACTION_PREYS_ON, null);
        assertThat(list, is(notNullValue()));
    }

    @Test
    public void findPreyAtLocationNoLatitude() throws IOException, URISyntaxException {
        String list = new CypherProxyController().findDistinctTargetTaxonNames(null, "Homo sapiens", CypherProxyController.INTERACTION_PREYS_ON, null);
        assertThat(list, is(notNullValue()));
    }

    @Test
    public void findPredator() throws IOException, URISyntaxException {
        String list = new CypherProxyController().findDistinctTargetTaxonNames(null, null, CypherProxyController.INTERACTION_PREYS_ON, "Hemiramphus brasiliensis");
        assertThat(list, is(notNullValue()));
    }

    @Test
    public void findTargetsForSource() throws IOException, URISyntaxException {
        String list = new CypherProxyController().findDistinctTargetTaxonNames(null, "Homo sapiens", CypherProxyController.INTERACTION_PREYS_ON, "Hemiramphus brasiliensis");
        assertThat(list, is(notNullValue()));
    }


    @Test
    public void findPredatorObservations() throws IOException, URISyntaxException {
        String list = new CypherProxyController().findObservationsOf(null, "Ariopsis felis", CypherProxyController.INTERACTION_PREYS_ON);
        assertThat(list, is(notNullValue()));
        list = new CypherProxyController().findObservationsOf(null, "Rattus rattus", CypherProxyController.INTERACTION_PREYS_ON);
        assertThat(list, is(notNullValue()));
    }

    @Test
    public void findPreyObservations() throws IOException, URISyntaxException {
        String list = new CypherProxyController().findObservationsOf(null, "Rattus rattus", CypherProxyController.INTERACTION_PREYED_UPON_BY);
        assertThat(list, is(notNullValue()));

        list = new CypherProxyController().findObservationsOf(null, "Ariopsis felis", CypherProxyController.INTERACTION_PREYED_UPON_BY);
        assertThat(list, is(notNullValue()));
    }


    @Test
    public void findPredatorPreyObservations() throws IOException, URISyntaxException {
        String list = new CypherProxyController().findObservationsOf(null, "Rattus rattus", CypherProxyController.INTERACTION_PREYED_UPON_BY, "Homo sapiens");
        assertThat(list, is(notNullValue()));

        list = new CypherProxyController().findObservationsOf(null, "Ariopsis felis", CypherProxyController.INTERACTION_PREYS_ON, "Rattus rattus");
        assertThat(list, is(notNullValue()));
    }

    @Test
    public void findTaxon() throws IOException, URISyntaxException {
        String list = new CypherProxyController().findTaxon("Homo sap");
        assertThat(list, is(notNullValue()));
    }

    @Test
    public void findExternalLinkForTaxonWithName() throws IOException {
        String externalLink = new CypherProxyController().findExternalLinkForTaxonWithName("Homo sapiens");
        assertThat(externalLink, is("{\"url\":\"http://eol.org/pages/327955\"}"));
    }

    @Test
    public void findInteractions() throws IOException {
        String externalLink = new CypherProxyController().findInteractions(getRequest());
        assertThat(externalLink, containsString("ATE"));
    }

    @Test
    public void findShortestPaths() throws IOException {
        String externalLink = new CypherProxyController().findShortestPaths("Homo sapiens", "Rattus rattus");
        assertThat(externalLink, containsString("Rattus rattus"));
    }

}
