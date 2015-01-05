package org.eol.globi.service;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.eol.globi.domain.TaxonomyProvider;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class WoRMSService extends BasePropertyEnricherService {
    public static final String RESPONSE_PREFIX = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><SOAP-ENV:Envelope SOAP-ENV:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:SOAP-ENC=\"http://schemas.xmlsoap.org/soap/encoding/\"><SOAP-ENV:Body><ns1:getAphiaIDResponse xmlns:ns1=\"http://tempuri.org/\"><return xsi:type=\"xsd:int\">";
    public static final String RESPONSE_SUFFIX = "</return></ns1:getAphiaIDResponse></SOAP-ENV:Body></SOAP-ENV:Envelope>";

    @Override
    public String lookupIdByName(String taxonName) throws PropertyEnricherException {
        String response = getResponse("getAphiaID", "scientificname", taxonName);
        String id = null;
        if (response.startsWith(RESPONSE_PREFIX) && response.endsWith(RESPONSE_SUFFIX)) {
            String trimmed = response.replace(RESPONSE_PREFIX, "");
            trimmed = trimmed.replace(RESPONSE_SUFFIX, "");
            try {
                Long aphiaId = Long.parseLong(trimmed);
                id = TaxonomyProvider.ID_PREFIX_WORMS + aphiaId;
            } catch (NumberFormatException ex) {
                //ignore
            }
        }
        return id;
    }

    private String getResponse(String methodName, String paramName, String paramValue) throws PropertyEnricherException {
        HttpPost post = new HttpPost("http://www.marinespecies.org/aphia.php?p=soap");
        post.setHeader("SOAPAction", "http://tempuri.org/getAphiaID");
        post.setHeader("Content-Type", "text/xml;charset=utf-8");
        String requestBody = "<?xml version=\"1.0\" ?>";
        requestBody += "<soap:Envelope ";
        requestBody += "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ";
        requestBody += "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" ";
        requestBody += "xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">";
        requestBody += "<soap:Body>";
        requestBody += "<" + methodName + " xmlns=\"http://tempuri.org/\">";
        requestBody = requestBody + "<" + paramName + ">" + paramValue + "</" + paramName + ">";
        requestBody = requestBody + "<marine_only>false</marine_only>";
        requestBody += "</" + methodName + "></soap:Body></soap:Envelope>";

        InputStreamEntity catchEntity;
        try {
            catchEntity = new InputStreamEntity(new ByteArrayInputStream(requestBody.getBytes("UTF-8")), requestBody.getBytes().length);
        } catch (UnsupportedEncodingException e) {
            throw new PropertyEnricherException("problem creating request body for [" + post.getURI().toString() + "]", e);
        }
        post.setEntity(catchEntity);

        BasicResponseHandler responseHandler = new BasicResponseHandler();
        String response;
        try {
            response = execute(post, responseHandler);
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to connect to [" + post.getURI().toString() + "]", e);
        }
        return response;
    }

    @Override
    public String lookupTaxonPathById(String id) throws PropertyEnricherException {
        String path = null;
        if (StringUtils.startsWith(id, TaxonomyProvider.ID_PREFIX_WORMS)) {
            String response = getResponse("getAphiaClassificationByID", "AphiaID", id.replace(TaxonomyProvider.ID_PREFIX_WORMS, ""));
            path = ServiceUtil.extractPath(response, "scientificname");
        }

        return StringUtils.isBlank(path) ? null : path;
    }
}
