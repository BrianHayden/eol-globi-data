package org.eol.globi.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.eol.globi.domain.TaxonImage;
import org.eol.globi.domain.TaxonomyProvider;

import java.io.IOException;

public class EOLTaxonImageService extends BaseHttpClientService {
    private static final Log LOG = LogFactory.getLog(EOLTaxonImageService.class);

    public TaxonImage lookupImageForExternalId(String externalId) throws IOException {
        TaxonImage image = null;
        if (externalId == null) {
            LOG.warn("cannot lookup image for null externalId");
        } else if (externalId.startsWith(TaxonomyProvider.ID_PREFIX_EOL)) {
            image = lookupImageURLs(TaxonomyProvider.EOL, externalId.replace(TaxonomyProvider.ID_PREFIX_EOL, ""));
        } else if (externalId.startsWith(TaxonomyProvider.ID_PREFIX_WORMS)) {
            image = lookupImageURLs(TaxonomyProvider.WORMS, externalId.replace(TaxonomyProvider.ID_PREFIX_WORMS, ""));
        } else if (externalId.startsWith(TaxonomyProvider.ID_PREFIX_ITIS)) {
            image = lookupImageURLs(TaxonomyProvider.ITIS, externalId.replace(TaxonomyProvider.ID_PREFIX_ITIS, ""));
        }
        return image;
    }

    public TaxonImage lookupImageURLs(TaxonomyProvider provider, String taxonId) throws IOException {
        TaxonImage taxonImage = null;
        String eolPageId = null;
        String eolProviderId = "627";

        if (TaxonomyProvider.ITIS.equals(provider)) {
            eolProviderId = "903";
        } else if (TaxonomyProvider.NCBI.equals(provider)) {
            eolProviderId = "759";
        } else if (TaxonomyProvider.WORMS.equals(provider)) {
            eolProviderId = "123";
        } else if (TaxonomyProvider.EOL.equals(provider)) {
            // no need to lookup, because the page id is already in the taxon id
            eolPageId = taxonId.replace(TaxonomyProvider.ID_PREFIX_EOL, "");
        } else {
            throw new UnsupportedOperationException("unsupported taxonomy provider [" + provider + "]");
        }

        if (eolPageId == null) {
            eolPageId = lookupEOLPageId(taxonId, eolPageId, eolProviderId);
        }

        HttpResponse response;
        String responseString;

        String imageObjectId = null;

        if (null != eolPageId) {
            imageObjectId = getImageObjectId(eolPageId, imageObjectId);

            if (null != imageObjectId) {
                String pageUrlString = "http://eol.org/api/data_objects/1.0/" + imageObjectId + ".json";
                response = getHttpClient().execute(new HttpGet(pageUrlString));
                responseString = EntityUtils.toString(response.getEntity());
                if (200 == response.getStatusLine().getStatusCode()) {
                    taxonImage = new TaxonImage();
                    taxonImage.setEOLPageId(eolPageId);
                    enrichTaxonWithImageInfo(taxonImage, responseString);
                }
            }
        }
        return taxonImage;
    }

    private String getImageObjectId(String eolPageId, String imageObjectId) throws IOException {
        HttpResponse response;
        String responseString;
        String pageUrlString = "http://eol.org/api/pages/1.0/" + eolPageId + ".json?common_names=0&details=0&images=1&videos=0&text=0";
        response = getHttpClient().execute(new HttpGet(pageUrlString));
        responseString = EntityUtils.toString(response.getEntity());
        if (200 == response.getStatusLine().getStatusCode()) {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode array = mapper.readTree(responseString);
            JsonNode dataObjects = array.findValue("dataObjects");
            if (dataObjects != null && dataObjects.size() > 0) {
                JsonNode dataObject = dataObjects.get(0);
                if (dataObject.has("identifier")) {
                    imageObjectId = dataObject.get("identifier").getValueAsText();
                }
            }

        }
        return imageObjectId;
    }

    protected void enrichTaxonWithImageInfo(TaxonImage taxonImage, String responseString) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode array = mapper.readTree(responseString);
        JsonNode eolMediaURL = array.findValue("eolMediaURL");
        if (eolMediaURL != null) {
            taxonImage.setImageURL(eolMediaURL.getValueAsText());
        }

        JsonNode eolThumbnailURL = array.findValue("eolThumbnailURL");
        if (eolThumbnailURL != null) {
            taxonImage.setThumbnailURL(eolThumbnailURL.getValueAsText());
        }
    }

    private String lookupEOLPageId(String taxonId, String eolPageId, String eolProviderId) throws IOException {
        String urlString = "http://eol.org/api/search_by_provider/1.0/" + taxonId + ".json?hierarchy_id=" + eolProviderId;
        HttpGet get = new HttpGet(urlString);
        HttpResponse response = getHttpClient().execute(get);

        String responseString = EntityUtils.toString(response.getEntity());

        if (200 == response.getStatusLine().getStatusCode()) {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(responseString);
            if (jsonNode.isArray()) {
                ArrayNode arrayNode = (ArrayNode) jsonNode;
                if (arrayNode.size() > 0) {
                    JsonNode firstNode = arrayNode.get(0);
                    JsonNode eol_page_id = firstNode.get("eol_page_id");
                    eolPageId = eol_page_id.getValueAsText();
                }
            }
        }
        return eolPageId;
    }

}
