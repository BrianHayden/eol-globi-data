package org.eol.globi.service;

import org.eol.globi.data.taxon.EOLTaxonParser;
import org.eol.globi.data.taxon.SingleResourceTaxonReaderFactory;
import org.eol.globi.data.taxon.TaxonTerm;
import org.eol.globi.data.taxon.TaxonomyImporter;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.TaxonomyProvider;

public class EOLOfflineService extends OfflineService {

    @Override
    protected TaxonomyImporter createTaxonomyImporter() {
        return new TaxonomyImporter(new EOLTaxonParser(), new SingleResourceTaxonReaderFactory("eol/taxon.tab.gz"));
    }

    @Override
    protected String getValueForPropertyName(String propertyName, TaxonTerm term) {
        String value = null;
        if (PropertyAndValueDictionary.EXTERNAL_ID.equals(propertyName)) {
            value = TaxonomyProvider.ID_PREFIX_EOL + term.getId();
        }
        return value;
    }

}
