package org.eol.globi.data.taxon;

import org.eol.globi.data.FileUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class EOLTaxonReaderFactory implements TaxonReaderFactory {

    public static final String RESOURCE_NAME = "eol/taxon.tab.gz";

    private BufferedReader getFirstReader() throws IOException {
        return FileUtils.getBufferedReaderUTF_8(getClass().getResourceAsStream(RESOURCE_NAME));
    }

    @Override
    public Map<String, BufferedReader> getAllReaders() throws IOException {
        return new HashMap<String, BufferedReader>() {{
            put(RESOURCE_NAME, getFirstReader());
        }};
    }
}
