package org.eol.globi.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class UberonLookupService extends TermLookupServiceImpl {

    @Override
    protected List<URI> getMappingURIList() {
        try {
            return new ArrayList<URI>() {{
                add(getClass().getResource("body-part-mapping.csv").toURI());
                add(getClass().getResource("life-stage-mapping.csv").toURI());
            }};
        } catch (URISyntaxException e) {
            throw new RuntimeException(("failed to configure service for [" + getClass().getResource("body-part-mapping.csv").toString() + "]"));
        }
    }

    @Override
    protected char getDelimiter() {
        return ',';
    }
}
