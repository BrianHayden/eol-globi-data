package org.eol.globi.data.taxon;

import org.eol.globi.domain.Taxon;

public interface TaxonImportListener {
    void addTerm(Taxon term);

    void addTerm(String name, Taxon term);

    void start();

    void finish();
}
