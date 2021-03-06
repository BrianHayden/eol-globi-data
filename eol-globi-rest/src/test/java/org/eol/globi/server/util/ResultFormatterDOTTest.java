package org.eol.globi.server.util;

import org.eol.globi.server.util.ResultFormatterDOT;
import org.eol.globi.server.util.ResultFormattingException;
import org.hamcrest.core.Is;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class ResultFormatterDOTTest {

    @Test
    public void idTest() {
        assertThat(ResultFormatterDOT.getSafeLabel("EOL:123"), Is.is("EOL_123"));
        assertThat(ResultFormatterDOT.getSafeLabel("EOL//123"), Is.is("EOL__123"));
    }

    @Test
    public void formatter() throws ResultFormattingException {
        String dots = new ResultFormatterDOT().format("{\n" +
                "  \"columns\" : [ \"source_taxon_name\", \"interaction_type\", \"target_taxon_name\" ],\n" +
                "  \"data\" : [ [ \"Vireo olivaceus\", \"preysOn\", [ \"Diptera\", \"Auchenorrhyncha\", \"Coleoptera\", \"Lepidoptera\", \"Arachnida\" ] ], [ \"Colaptes auratus\", \"preysOn\", [ \"Hymenoptera\" ] ], [ \"Anseriformes\", \"preysOn\", [ \"Cyperaceae\", \"Tracheophyta\", \"marine invertebrates\" ] ], [ \"Geositta\", \"preysOn\", [ \"Insecta\", \"Coelopidae\", \"Diptera\", \"Talitridae\" ] ], [ \"Patagioenas squamosa\", \"preysOn\", [ \"fruit\" ] ], [ \"Ardea cinerea\", \"preysOn\", [ \"Anguilla anguilla\", \"Pollachius virens\", \"Ammodytes tobianus\", \"Zoarces viviparus\", \"Pholis gunnellus\", \"Myoxocephalus scorpius\", \"Pomatoschistus microps\", \"Crangon crangon\", \"Salmo trutta\" ] ], [ \"Setophaga petechia\", \"preysOn\", [ \"Araneae\", \"Insecta\", \"Orthoptera\", \"fruit and seeds\", \"Hemiptera\", \"Diptera\", \"Lepidoptera\", \"Formicidae\", \"Hymenoptera\", \"Coleoptera\", \"Auchenorrhyncha\" ] ] ] }");
        assertThat(dots, is(notNullValue()));
    }


}
