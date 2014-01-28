package org.eol.globi.data.taxon;

import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.TaxonNode;

public interface TaxonService {
    TaxonNode getOrCreateTaxon(String name, String externalId, String path) throws NodeFactoryException;

    TaxonNode findTaxon(String taxonName) throws NodeFactoryException;
}
