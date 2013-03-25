package org.eol.globi.data;

import org.eol.globi.data.taxon.TaxonTerm;
import org.eol.globi.domain.Taxon;
import org.eol.globi.service.TaxonPropertyEnricher;
import org.junit.After;
import org.junit.Before;
import org.neo4j.graphdb.GraphDatabaseService;
import org.eol.globi.data.taxon.TaxonLookupService;

import java.io.IOException;

public abstract class GraphDBTestCase {

    private GraphDatabaseService graphDb;

    protected NodeFactory nodeFactory;

    @Before
    public void startGraphDb() throws IOException {
        graphDb = new org.neo4j.test.ImpermanentGraphDatabase();
        nodeFactory = new NodeFactory(graphDb, new TaxonPropertyEnricher() {

            @Override
            public void enrich(Taxon taxon) throws IOException {

            }
        });
    }

    @After
    public void shutdownGraphDb() {
        graphDb.shutdown();
    }

    protected GraphDatabaseService getGraphDb() {
        return graphDb;
    }

}
