package org.eol.globi.data.taxon;

import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.service.NameSuggestor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ManualSuggestor implements NameSuggestor {
    private Map<String, String> corrections;

    @Override
    public String suggest(final String name) {
        if (!isInitialized()) {
            doInit();
        }
        String suggestedReplacement = corrections.get(name);
        return StringUtils.isBlank(suggestedReplacement) ? name : suggestedReplacement;
    }

    private void doInit() {
        try {
            InputStream resourceAsStream = getClass().getResourceAsStream("/org/eol/globi/service/taxon-name-mapping.csv");
            BufferedReader is = org.eol.globi.data.FileUtils.getUncompressedBufferedReader(resourceAsStream, CharsetConstant.UTF8);
            LabeledCSVParser labeledCSVParser = new LabeledCSVParser(new CSVParser(is));
            String[] line;

            corrections = new HashMap<String, String>();
            while ((line = labeledCSVParser.getLine()) != null) {
                if (line.length > 1) {
                    String original = line[0];
                    String correction = line[1];
                    if (StringUtils.isBlank(correction) || correction.trim().length() < 2) {
                        throw new RuntimeException("found invalid blank or single character conversion for [" + original + "], on line [" + labeledCSVParser.lastLineNumber() + 1 + "]");
                    }

                    String existingCorrection = corrections.get(original);
                    if (StringUtils.isNotBlank(existingCorrection)) {
                        if (StringUtils.equals(existingCorrection, correction)) {
                            throw new RuntimeException("found duplicated mapping for term [" + original + "]. Please remove line [" + (labeledCSVParser.lastLineNumber() + 1) + "]");
                        } else {
                            throw new RuntimeException("term [" + original + "] already mapped. Please revisit line [" + (labeledCSVParser.lastLineNumber() + 1) + "]");
                        }
                    }
                    corrections.put(original, correction);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("failed to initialize taxon name normalizer", e);
        }
    }

    private boolean isInitialized() {
        return corrections != null;
    }
}
