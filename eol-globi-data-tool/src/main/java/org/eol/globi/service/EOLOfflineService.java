package org.eol.globi.service;

import org.eol.globi.data.taxon.EOLTaxonParser;
import org.eol.globi.data.taxon.SingleResourceTaxonReaderFactory;
import org.eol.globi.data.taxon.TaxonTerm;
import org.eol.globi.data.taxon.TaxonomyImporter;
import org.eol.globi.domain.Taxon;

public class EOLOfflineService extends OfflineService {

    @Override
    protected TaxonomyImporter createTaxonomyImporter() {
        return new TaxonomyImporter(new EOLTaxonParser(), new SingleResourceTaxonReaderFactory("eol/taxon.tab.gz"));
    }

    @Override
    public boolean canLookupProperty(String propertyName) {
        return Taxon.EXTERNAL_ID.equals(propertyName);
    }

    @Override
    protected String getValueForPropertyName(String propertyName, TaxonTerm term) {
        String value = null;
        if (Taxon.EXTERNAL_ID.equals(propertyName)) {
            value = "EOL:" + term.getId();
        }
        return value;
    }
}
