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
import org.eol.globi.domain.TaxonImage;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.data.taxon.OboParser;

import java.io.IOException;
import java.util.Iterator;

public class TaxonImageEnricher extends TaxonEnricher {
    private static final Log LOG = LogFactory.getLog(TaxonImageEnricher.class);

    public TaxonImageEnricher(GraphDatabaseService graphDbService) {
        super(graphDbService);
    }

    @Override
    protected void enrichTaxonUsingMatch(String matchString) throws IOException {
        ExecutionEngine engine = new ExecutionEngine(graphDbService);
        String queryPrefix = "START study = node:studies('*:*') "
                + "MATCH " + matchString
                + "WHERE has(taxon.externalId) and not(has(taxon.imageURL)) ";

        LOG.info("matching [" + matchString + "]...");

        ExecutionResult result = engine.execute(queryPrefix
                + "RETURN count(distinct taxon) as totalTaxons");
        Iterator<Long> totalAffectedTaxons = result.columnAs("totalTaxons");
        Long totalTaxons = totalAffectedTaxons.next();

        while (enrichWithImageURLs(engine, queryPrefix, totalTaxons) > 0) {
            // ignore
        }

    }

    private int enrichWithImageURLs(ExecutionEngine engine, String queryPrefix, Long totalTaxons) {
        ExecutionResult result2 = engine.execute(queryPrefix
                + "RETURN distinct taxon LIMIT " + getBatchSize());
        Iterator<Node> taxon = result2.columnAs("taxon");


        EOLTaxonImageService service = new EOLTaxonImageService();
        int count = 0;
        while (taxon.hasNext()) {
            Node taxonNode = taxon.next();
            count++;
            if (count % 10 == 0) {
                LOG.info("Attempted to enrich taxon [" + count + "] out of [" + totalTaxons + "] with images");
            }
            String taxonName = (String) taxonNode.getProperty(Taxon.NAME);
            String externalId = (String) taxonNode.getProperty(Taxon.EXTERNAL_ID);
            StopWatch stopwatch = new StopWatch();
            stopwatch.start();
            try {
                TaxonomyProvider taxonomyProvider = lookupProvider(externalId);
                if (taxonomyProvider != null) {
                    int lastColon = externalId.lastIndexOf(":");
                    TaxonImage taxonImage = service.lookupImageURLs(taxonomyProvider, externalId.substring(lastColon + 1, externalId.length()));
                    stopwatch.stop();
                    String responseTime = "(took " + stopwatch.getTime() + "ms)";
                    String msg = "for [" + taxonName + "] with externalId [" + externalId + "] in [" + service.getClass().getSimpleName() + "] " + responseTime;
                    if (taxonImage == null) {
                        LOG.info("no match found " + msg);
                    } else {
                        LOG.info("found match " + msg);
                        enrichNode(taxonNode, taxonImage);
                    }
                }

            } catch (IOException ex) {
                LOG.warn("failed to find a match for [" + taxonName + "] in [" + service.getClass().getSimpleName() + "]", ex);
            }
        }
        return count;
    }

    private TaxonomyProvider lookupProvider(String externalId) {
        TaxonomyProvider taxonomyProvider = null;
        if (externalId.startsWith(WoRMSService.URN_LSID_PREFIX)) {
            taxonomyProvider = TaxonomyProvider.WORMS;
        } else if (externalId.startsWith(ITISService.URN_LSID_PREFIX)) {
            taxonomyProvider = TaxonomyProvider.ITIS;
        } else if (externalId.startsWith(OboParser.URN_LSID_PREFIX)) {
            taxonomyProvider = TaxonomyProvider.NCBI;
        } else if (externalId.startsWith(EOLTaxonImageService.EOL_LSID_PREFIX)) {
            taxonomyProvider = TaxonomyProvider.EOL;
        }
        return taxonomyProvider;
    }

    private void enrichNode(Node node, TaxonImage taxonImage) {
        Transaction transaction = graphDbService.beginTx();
        try {
            String imageUrl = taxonImage.getImageURL() == null ? "" : taxonImage.getImageURL();
            node.setProperty(Taxon.IMAGE_URL, imageUrl);
            String thumbnailUrl = taxonImage.getThumbnailURL() == null ? "" : taxonImage.getThumbnailURL();
            node.setProperty(Taxon.THUMBNAIL_URL, thumbnailUrl);

            if (taxonImage.getEOLPageId() != null) {
                node.setProperty(Taxon.EXTERNAL_ID, EOLTaxonImageService.EOL_LSID_PREFIX + taxonImage.getEOLPageId());
            }
            transaction.success();
        } finally {
            transaction.finish();
        }
    }

    public int getBatchSize() {
        return 100;
    }
}
