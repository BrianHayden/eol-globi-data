package org.eol.globi.service;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;

public class DOIResolverImpl implements DOIResolver {
    private static final Log LOG = LogFactory.getLog(DOIResolverImpl.class);

    public String findDOIForReference(final String reference) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        HttpPost post = new HttpPost("http://search.crossref.org/links");
        post.setHeader("Content-Type", "application/json");
        StringEntity entity = new StringEntity(mapper.writeValueAsString(new ArrayList<String>() {{
            add(reference);
        }}));
        post.setEntity(entity);

        BasicResponseHandler handler = new BasicResponseHandler();
        String response = new DefaultHttpClient().execute(post, handler);
        JsonNode jsonNode = mapper.readTree(response);
        JsonNode results = jsonNode.get("results");
        String doi = null;
        if (jsonNode.get("query_ok").getValueAsBoolean()) {
            for (JsonNode result : results) {
                if (result.get("match").getValueAsBoolean()) {
                    doi = result.get("doi").getTextValue();
                }
            }
        }
        return doi;
    }

    @Override
    public String findCitationForDOI(String doi) throws IOException {
        String citation = null;
        if (StringUtils.isNotBlank(doi)) {
            try {
            HttpGet request = new HttpGet(doi);
            request.setHeader("Accept", "text/x-bibliography; style=cse");
            citation = new DefaultHttpClient().execute(request, new BasicResponseHandler());
            if (StringUtils.isNotBlank(citation)) {
                citation = citation.replaceFirst("^1\\. ", "");
            }
            } catch (IllegalArgumentException ex) {
                LOG.warn("potientially malformed doi found [" + doi + "]", ex);
            }
        }
        return citation;
    }
}
