package org.eol.globi.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.NodeBacked;
import org.eol.globi.domain.Taxon;

public abstract class BaseExternalIdService extends BaseHttpClientService implements TaxonPropertyLookupService {
    private static final Log LOG = LogFactory.getLog(BaseHttpClientService.class);

    @Override
    public String lookupPropertyValueByTaxonName(String taxonName, String propertyName) throws TaxonPropertyLookupServiceException {
        String propertyValue = null;
        if (NodeBacked.EXTERNAL_ID.equals(propertyName)) {
            if (taxonName.trim().length() < 2) {
                LOG.warn("taxon name [" + taxonName + "] too short");
            } else {
                try {
                    propertyValue = lookupLSIDByTaxonName(taxonName);
                } catch (TaxonPropertyLookupServiceException e) {
                    shutdown();
                    throw e;
                }
            }
        } else if (Taxon.PATH.equals(propertyName)) {
            try {
                String lsid = lookupLSIDByTaxonName(taxonName);
                if (lsid != null) {
                    propertyValue = lookupTaxonPathByLSID(lsid);
                }
            } catch (TaxonPropertyLookupServiceException e) {
                shutdown();
                throw e;
            }
        }
        return propertyValue;
    }

    @Override
    public boolean canLookupProperty(String propertyName) {
        return NodeBacked.EXTERNAL_ID.equals(propertyName);
    }

    public abstract String lookupLSIDByTaxonName(String taxonName) throws TaxonPropertyLookupServiceException;

    public abstract String lookupTaxonPathByLSID(String lsid) throws TaxonPropertyLookupServiceException;

}
