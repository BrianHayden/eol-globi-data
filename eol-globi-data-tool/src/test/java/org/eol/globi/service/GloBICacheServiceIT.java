package org.eol.globi.service;

import org.eol.globi.domain.PropertyAndValueDictionary;
import org.junit.Test;

import java.util.HashMap;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.StringContains.containsString;

public class GloBICacheServiceIT {

    @Test
    public void ariopsisFelis() throws TaxonPropertyLookupServiceException {
        HashMap<String, String> properties = new HashMap<String, String>();
        new GloBICacheService().lookupPropertiesByName("Ariopsis felis", properties);
        assertThat(properties.get(PropertyAndValueDictionary.NAME), is("Ariopsis felis"));
        assertThat(properties.get(PropertyAndValueDictionary.PATH), containsString("Animalia"));
        assertThat(properties.get(PropertyAndValueDictionary.COMMON_NAMES), containsString("hardhead catfish"));
        assertThat(properties.get(PropertyAndValueDictionary.EXTERNAL_ID), containsString(":"));
    }
}
