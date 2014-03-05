package org.eol.globi.service;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

import static org.eol.globi.domain.TaxonomyProvider.ID_PREFIX_EOL;
import static org.eol.globi.domain.TaxonomyProvider.ID_PREFIX_WIKIPEDIA;
import static org.eol.globi.domain.TaxonomyProvider.WIKIPEDIA;

public class FunctionalGroupService extends BaseTaxonIdService {

    public static final String PHYTOPLANKTON = ID_PREFIX_EOL + "19662463";
    public static final String PLANKTON = ID_PREFIX_WIKIPEDIA + "Plankton";
    public static final String ZOOPLANKTON = ID_PREFIX_EOL + "19662459";
    public static final String INVERTEBRATA = ID_PREFIX_WIKIPEDIA + "Invertebrata";
    public static final String BACTERIOPLANKTON = ID_PREFIX_WIKIPEDIA + "Bacterioplankton";
    public static final String ALGAE = ID_PREFIX_EOL + "37577397";
    public static final String MACROALGAE = WIKIPEDIA + "Macroalgae";

    private Map<String, String> mapping = new HashMap<String, String>() {{
        put("phytoplankton", PHYTOPLANKTON);
        put("zooplankton", ZOOPLANKTON);
        put("bacterioplankton", BACTERIOPLANKTON);
        put("plankton", PLANKTON);
        put("invertebrata", INVERTEBRATA);
        put("algae", ALGAE);
        put("macroalgae", MACROALGAE);
    }};

    private Map<String, String> pathLookup = new HashMap<String, String>() {{
        put(PHYTOPLANKTON, "plankton | phytoplankton");
        put(ZOOPLANKTON, "plankton | zooplankton | Animalia");
        put(BACTERIOPLANKTON, "plankton | bacterioplankton | Bacteria");
        put(PLANKTON, "plankton");
        put(INVERTEBRATA, "invertebrata");
        put(ALGAE, "algae");
        put(MACROALGAE, "algae | macroalgae");
    }};

    public String lookupIdByName(String taxonName) throws TaxonPropertyLookupServiceException {
        String id = null;
        String lowerCaseName = StringUtils.lowerCase(taxonName);
        if (StringUtils.isNotBlank(lowerCaseName)) {
            id = mapping.get(lowerCaseName);
        }
        return id;
    }

    @Override
    public String lookupTaxonPathById(String id) throws TaxonPropertyLookupServiceException {
        return pathLookup.get(id);
    }
}
