package org.eol.globi.service;

import org.eol.globi.data.CharsetConstant;
import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactory;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.Taxon;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.StringContains.containsString;

public class TaxonPropertyEnricherImplIT extends GraphDBTestCase {

    private TaxonPropertyEnricher enricher;

    @Before
    public void start() {
        enricher = TaxonPropertyEnricherFactory.createTaxonEnricher(getGraphDb());
        nodeFactory = new NodeFactory(getGraphDb(), enricher);
    }


    @Test
    public void enrichTwoTaxons() throws NodeFactoryException, IOException {
        Taxon taxon = nodeFactory.getOrCreateTaxon("Homo sapiens");
        assertThat(taxon.getExternalId(), is("EOL:327955"));
        assertThat(taxon.getPath(), is(not("no:match")));


        taxon = nodeFactory.getOrCreateTaxon("Ariopsis felis");
        assertThat(taxon.getExternalId(), is("EOL:223038"));
        assertThat(taxon.getPath(), is(not("no:match")));

        Taxon sameTaxon = nodeFactory.getOrCreateTaxon("Ariopsis felis");
        assertThat(taxon.getNodeID(), is(sameTaxon.getNodeID()));

        taxon = nodeFactory.getOrCreateTaxon("Pitar fulminatus");
        assertThat(taxon.getExternalId(), is("EOL:448962"));
        assertThat(taxon.getPath(), is(not("no:match")));
    }

    @Test
    // see https://github.com/jhpoelen/eol-globi-data/issues/12
    public void foraminifera() throws IOException, NodeFactoryException {
        Taxon taxon = nodeFactory.getOrCreateTaxon("Foraminifera");
        assertThat(taxon.getExternalId(), is("EOL:4888"));
        assertThat(taxon.getName(), is("Foraminifera"));
        assertThat(taxon.getPath(), containsString(CharsetConstant.SEPARATOR + "Foraminifera"));
    }
}
