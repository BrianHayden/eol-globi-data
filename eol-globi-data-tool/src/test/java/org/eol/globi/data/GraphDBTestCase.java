package org.eol.globi.data;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.taxon.CorrectionService;
import org.eol.globi.domain.Taxon;
import org.eol.globi.service.DOIResolver;
import org.eol.globi.service.TaxonPropertyEnricher;
import org.eol.globi.domain.Term;
import org.eol.globi.service.TermLookupService;
import org.eol.globi.service.TermLookupServiceException;
import org.junit.After;
import org.junit.Before;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.test.TestGraphDatabaseFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class GraphDBTestCase {

    private GraphDatabaseService graphDb;

    protected NodeFactory nodeFactory;

    @Before
    public void startGraphDb() throws IOException {
        graphDb = new TestGraphDatabaseFactory().newImpermanentDatabase();
        nodeFactory = new NodeFactory(graphDb, new TaxonPropertyEnricher() {

            @Override
            public void enrich(Taxon taxon) throws IOException {

            }
        });
        nodeFactory.setEnvoLookupService(new TestTermLookupService());
        nodeFactory.setTermLookupService(new TestTermLookupService());
        nodeFactory.setCorrectionService(new CorrectionService() {
            @Override
            public String correct(String taxonName) {
                return taxonName;
            }
        });
        nodeFactory.setDoiResolver(new DOIResolver() {
            @Override
            public String findDOIForReference(String reference) throws IOException {
                return StringUtils.isBlank(reference) ? null : "doi:" + reference;
            }

            @Override
            public String findCitationForDOI(String doi) throws IOException {
                return StringUtils.isBlank(doi) ? null : "citation:" + doi;
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

    private static class TestTermLookupService implements TermLookupService {
        @Override
        public List<Term> lookupTermByName(final String name) throws TermLookupServiceException {
            return new ArrayList<Term>() {{
                add(new Term("TEST:" + name, name));
            }};
        }
    }

}
