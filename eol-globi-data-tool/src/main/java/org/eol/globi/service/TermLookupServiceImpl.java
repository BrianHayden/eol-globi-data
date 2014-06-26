package org.eol.globi.service;

import com.Ostermiller.util.CSVParse;
import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Term;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class TermLookupServiceImpl extends BaseHttpClientService implements TermLookupService {
    private static final Log LOG = LogFactory.getLog(TermLookupServiceImpl.class);

    private Map<String, List<Term>> mapping = null;

    protected abstract List<URI> getMappingURIList();

    protected abstract char getDelimiter();

    @Override
    public List<Term> lookupTermByName(final String name) throws TermLookupServiceException {
        if (mapping == null) {
            buildMapping(getMappingURIList());
        }
        List<Term> terms = mapping.get(name);
        return terms == null ? new ArrayList<Term>() {{
            add(new Term(PropertyAndValueDictionary.NO_MATCH, name));
        }} : terms;
    }

    private void buildMapping(List<URI> uriList) throws TermLookupServiceException {
        mapping = new HashMap<String, List<Term>>();

        for (URI uri : uriList) {
            try {
                String response = IOUtils.toString(uri.toURL());
                CSVParse parser = new CSVParser(new StringReader(response));
                parser.changeDelimiter(getDelimiter());

                if (hasHeader()) {
                    parser = new LabeledCSVParser(parser);
                }
                String[] line;
                while ((line = parser.getLine()) != null) {
                    if (line.length < 4) {
                        LOG.info("line: [" + parser.getLastLineNumber() + "] in [" + uriList + "] contains less than 4 columns");
                    } else {
                        String sourceName = line[1];
                        String targetId = line[2];
                        String targetName = line[3];
                        if (StringUtils.isNotBlank(sourceName)
                                && StringUtils.isNotBlank(targetId)
                                && StringUtils.isNotBlank(targetName)) {
                            List<Term> terms = mapping.get(sourceName);
                            if (terms == null) {
                                terms = new ArrayList<Term>();
                                mapping.put(sourceName, terms);
                            }
                            terms.add(new Term(targetId, targetName));
                        }
                    }
                }
            } catch (IOException e) {
                throw new TermLookupServiceException("failed to retrieve mapping from [" + uriList + "]", e);
            }
        }
    }

    protected abstract boolean hasHeader();

}
