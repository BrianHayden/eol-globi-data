package org.eol.globi.data.taxon;

import java.io.IOException;

public interface TaxonLookupService {
    org.eol.globi.domain.Taxon[] lookupTermsByName(String taxonName) throws IOException;

    void destroy();
}
