package org.eol.globi.service;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.eol.globi.domain.Taxon;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class ExternalIdTaxonEnricher extends TaxonEnricher {
    private static final Log LOG = LogFactory.getLog(ExternalIdTaxonEnricher.class);

    public ExternalIdTaxonEnricher(GraphDatabaseService graphDbService) {
        super(graphDbService);
    }

    @Override
    protected void enrichTaxonUsingMatch(String matchString) throws IOException {
        ExecutionEngine engine = new ExecutionEngine(graphDbService);
        List<LSIDLookupService> services = new ArrayList<LSIDLookupService>();
        initServices(services);

        String queryPrefix = "START study = node:studies('*:*') "
                + "MATCH " + matchString
                + "WHERE not(has(taxon.externalId)) ";

        LOG.info("matching [" + matchString + "]...");

        while (enrichWithExternalId(engine, services, queryPrefix) > 0) {

        }
        ;
        shutdownServices(services);
    }

    private int enrichWithExternalId(ExecutionEngine engine, List<LSIDLookupService> services, String queryPrefix) {
        ExecutionResult result = engine.execute(queryPrefix
                + "RETURN taxon LIMIT " + getBatchSize());
        Iterator<Node> taxon = result.columnAs("taxon");
        HashMap<Class, Integer> errorCounts = new HashMap<Class, Integer>();
        int count = 0;
        while (taxon.hasNext()) {
            Node taxonNode = taxon.next();
            try {
                for (LSIDLookupService service : services) {
                    if (enrichedTaxonWithService(errorCounts, taxonNode, service)) {
                        break;
                    }
                }
                count++;
            } catch (LSIDLookupServiceException e) {
                shutdownServices(services);
                initServices(services);
            }
        }
        return count;
    }

    private boolean enrichedTaxonWithService(HashMap<Class, Integer> errorCounts, Node taxonNode, LSIDLookupService service) throws LSIDLookupServiceException {
        Integer errorCount = errorCounts.get(service.getClass());
        if (errorCount != null && errorCount > 10) {
            LOG.error("skipping taxon match against [" + service.getClass().toString() + "], error count [" + errorCount + "] too high.");
        } else {
            String taxonName = (String) taxonNode.getProperty(Taxon.NAME);
            if (taxonName.trim().length() < 2) {
                LOG.warn("not matching invalid [" + taxonName + "]: name is too short");
            } else {
                if (lookupTaxon(errorCounts, taxonNode, service, errorCount, taxonName)) return true;
            }
        }
        return false;
    }

    private boolean lookupTaxon(HashMap<Class, Integer> errorCounts, Node taxonNode, LSIDLookupService service, Integer errorCount, String taxonName) throws LSIDLookupServiceException {
        StopWatch stopwatch = new StopWatch();
        stopwatch.start();
        try {
            String lsid = service.lookupLSIDByTaxonName(taxonName);
            stopwatch.stop();
            String responseTime = "(took " + stopwatch.getTime() + "ms)";
            String msg = "for [" + taxonName + "] with LSID [" + lsid + "] in [" + service.getClass().getSimpleName() + "] " + responseTime;
            if (lsid == null) {
                LOG.info("no match found " + msg);
            } else {
                LOG.info("found match " + msg);
                enrichNode(taxonNode, lsid);
                return true;
            }
        } catch (LSIDLookupServiceException ex) {
            LOG.warn("failed to find a match for [" + taxonName + "] in [" + service.getClass().getSimpleName() + "]", ex);
            if (errorCounts.containsKey(service.getClass()) && errorCount != null) {
                errorCounts.put(service.getClass(), ++errorCount);
            } else {
                errorCounts.put(service.getClass(), 0);
            }
            throw new LSIDLookupServiceException("re-throwing", ex);
        }
        return false;
    }

    private void initServices(List<LSIDLookupService> services) {
        services.add(new EOLService());
        services.add(new WoRMSService());
        //services.add(new ITISService());
        services.add(new LSIDLookupService() {
            @Override
            public String lookupLSIDByTaxonName(String taxonName) throws LSIDLookupServiceException {
                return "no:match";
            }

            @Override
            public void shutdown() {

            }
        });
    }

    private void shutdownServices(List<LSIDLookupService> services) {
        for (LSIDLookupService service : services) {
            service.shutdown();
        }
        services.clear();
    }

    private void enrichNode(Node node, String lsid) {
        Transaction transaction = graphDbService.beginTx();
        try {
            node.setProperty(Taxon.EXTERNAL_ID, lsid);
            transaction.success();
        } finally {
            transaction.finish();
        }
    }

    public int getBatchSize() {
        return 100;
    }
}
