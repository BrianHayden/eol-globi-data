package org.eol.globi.service;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.util.HttpUtil;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class GlobalNamesService extends BaseHttpClientService implements TaxonPropertyLookupService {


    private final GlobalNamesSources source;

    public GlobalNamesService() {
        this(GlobalNamesSources.ITIS);
    }

    public GlobalNamesService(GlobalNamesSources source) {
        super();
        this.source = source;
    }

    @Override
    public void lookupPropertiesByName(String name, Map<String, String> properties) throws TaxonPropertyLookupServiceException {
        final List<Taxon> taxa = new ArrayList<Taxon>();
        findTermsForNames(Arrays.asList(name), new TermMatchListener() {
            @Override
            public void foundTermForName(Long id, String name, Taxon taxon) {
                taxa.add(taxon);
            }
        });

        if (taxa.size() > 0) {
            Taxon taxon = taxa.get(0);
            properties.put(PropertyAndValueDictionary.NAME, taxon.getName());
            properties.put(PropertyAndValueDictionary.EXTERNAL_ID, taxon.getExternalId());
            properties.put(PropertyAndValueDictionary.PATH, taxon.getPath());
            properties.put(PropertyAndValueDictionary.RANK, taxon.getRank());
        }

    }

    public void findTermsForNames(List<String> names, TermMatchListener termMatchListener) throws TaxonPropertyLookupServiceException {
        if (names.size() == 0) {
            throw new IllegalArgumentException("need non-empty list of names");
        }

        HttpClient httpClient = HttpUtil.createHttpClient();
        try {
            URI uri = new URI("http", "resolver.globalnames.org", "/name_resolvers.json", "data=" + StringUtils.join(names, "\n") + "&data_source_ids=" + source.getId(), null);
            String result = httpClient.execute(new HttpPost(uri), new BasicResponseHandler());
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(result);
            JsonNode dataList = jsonNode.get("data");
            if (dataList != null && dataList.isArray()) {
                for (JsonNode data : dataList) {
                    JsonNode results = data.get("results");
                    if (results != null && results.isArray()) {
                        for (JsonNode aResult : results) {
                            Taxon taxon = new TaxonImpl();
                            taxon.setName(aResult.get("canonical_form").getValueAsText());
                            taxon.setPath(aResult.get("classification_path").getValueAsText());
                            String[] ranks = aResult.get("classification_path_ranks").getValueAsText().split("\\|");
                            String rank = ranks[ranks.length - 1];
                            taxon.setRank(rank);
                            String externalId = source.getProvider().getIdPrefix() + aResult.get("taxon_id").getValueAsText();
                            taxon.setExternalId(externalId);
                            Long suppliedId = data.has("supplied_id") ? data.get("supplied_id").getLongValue() : null;
                            termMatchListener.foundTermForName(suppliedId, data.get("supplied_name_string").getTextValue(), taxon);
                        }
                    }
                }
            }
        } catch (URISyntaxException e) {
            throw new TaxonPropertyLookupServiceException("Failed to query", e);
        } catch (ClientProtocolException e) {
            throw new TaxonPropertyLookupServiceException("Failed to query", e);
        } catch (IOException e) {
            throw new TaxonPropertyLookupServiceException("Failed to query", e);
        }
    }
}
