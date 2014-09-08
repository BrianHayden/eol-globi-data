package org.eol.globi.service;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.impl.client.BasicResponseHandler;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.util.HttpUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GlobalNamesService extends BaseHttpClientService implements PropertyEnricher {


    private final GlobalNamesSources source;

    public GlobalNamesService() {
        this(GlobalNamesSources.ITIS);
    }

    public GlobalNamesService(GlobalNamesSources source) {
        super();
        this.source = source;
    }

    @Override
    public Map<String, String> enrich(Map<String, String> properties) throws PropertyEnricherException {
        Map<String, String> enrichedProperties = new HashMap<String, String>();
        final List<Taxon> taxa = new ArrayList<Taxon>();
        findTermsForNames(Arrays.asList(properties.get(PropertyAndValueDictionary.NAME)), new TermMatchListener() {
            @Override
            public void foundTaxonForName(Long id, String name, Taxon taxon) {
                taxa.add(taxon);
            }
        });

        if (taxa.size() > 0) {
            Taxon taxon = taxa.get(0);
            enrichedProperties.put(PropertyAndValueDictionary.NAME, taxon.getName());
            enrichedProperties.put(PropertyAndValueDictionary.EXTERNAL_ID, taxon.getExternalId());
            enrichedProperties.put(PropertyAndValueDictionary.PATH, taxon.getPath());
            enrichedProperties.put(PropertyAndValueDictionary.PATH_NAMES, taxon.getPathNames());
            enrichedProperties.put(PropertyAndValueDictionary.RANK, taxon.getRank());
        }
        return Collections.unmodifiableMap(enrichedProperties);
    }

    public void findTermsForNames(List<String> names, TermMatchListener termMatchListener) throws PropertyEnricherException {
        if (names.size() == 0) {
            throw new IllegalArgumentException("need non-empty list of names");
        }

        try {
            String result = queryForNames(names);
            parseResult(termMatchListener, result);
        } catch (ClientProtocolException e) {
            throw new PropertyEnricherException("Failed to query", e);
        } catch (IOException e) {
            throw new PropertyEnricherException("Failed to query", e);
        } catch (URISyntaxException e) {
            throw new PropertyEnricherException("Failed to query", e);
        }

    }

    protected String queryForNames(List<String> names) throws URISyntaxException, IOException {
        HttpClient httpClient = HttpUtil.createHttpClient();
        URI uri = new URI("http", "resolver.globalnames.org", "/name_resolvers.json", "best_match_only=true&data_source_ids=" + source.getId(), null);
        HttpPost post = new HttpPost(uri);

        MultipartEntity entity = new MultipartEntity();
        InputStream is = IOUtils.toInputStream(StringUtils.join(names, "\n"), "UTF-8");
        entity.addPart("file", new InputStreamBody(is, "file"));
        post.setEntity(entity);

        return httpClient.execute(post, new BasicResponseHandler());
    }

    protected void parseResult(TermMatchListener termMatchListener, String result) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(result);
        JsonNode dataList = jsonNode.get("data");
        if (dataList != null && dataList.isArray()) {
            for (JsonNode data : dataList) {
                JsonNode results = data.get("results");
                if (results != null && results.isArray()) {
                    for (JsonNode aResult : results) {
                        Taxon taxon = new TaxonImpl();
                        if (aResult.has("classification_path") && aResult.has("classification_path_ranks")) {
                            parseResult(termMatchListener, data, aResult, taxon);
                        }
                    }
                }
            }
        }
    }

    protected void parseResult(TermMatchListener termMatchListener, JsonNode data, JsonNode aResult, Taxon taxon) {
        String classificationPath = aResult.get("classification_path").getValueAsText();
        taxon.setPath(classificationPath);
        String pathRanks = aResult.get("classification_path_ranks").getValueAsText();
        taxon.setPathNames(pathRanks);
        String[] ranks = pathRanks.split("\\|");
        if (ranks.length > 0) {
            String rank = ranks[ranks.length - 1];
            taxon.setRank(rank);
        }
        String[] taxonNames = classificationPath.split("\\|");
        if (ranks.length > 0 && taxonNames.length > 0) {
            String taxonName = taxonNames[taxonNames.length - 1];
            taxon.setName(taxonName);
        } else {
            taxon.setName(aResult.get("canonical_form").getValueAsText());
        }

        String taxonIdLabel = aResult.has("current_taxon_id") ? "current_taxon_id" : "taxon_id";
        String taxonIdValue = aResult.get(taxonIdLabel).getValueAsText();
        // see https://github.com/GlobalNamesArchitecture/gni/issues/35
        if (!StringUtils.startsWith(taxonIdValue, "gn:")) {
            String externalId = source.getProvider().getIdPrefix() + taxonIdValue;
            taxon.setExternalId(externalId);
            Long suppliedId = data.has("supplied_id") ? data.get("supplied_id").getValueAsLong() : null;
            JsonNode supplied_name_string = data.get("supplied_name_string");

            if (aResult.has("match_type") && aResult.get("match_type").getIntValue() < 3) {
                termMatchListener.foundTaxonForName(suppliedId, supplied_name_string.getTextValue(), taxon);
            }
        }
    }

    public GlobalNamesSources getSource() {
        return source;
    }
}
