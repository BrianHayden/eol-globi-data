package org.eol.globi.tool;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.data.taxon.TaxonFuzzySearchIndex;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.TaxonNode;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.helpers.collection.MapUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LinkerTaxonIndex {

    public static final String INDEX_TAXON_NAMES_AND_IDS = "taxonPaths";

    public void link(GraphDatabaseService graphDb) {
        Index<Node> taxons = graphDb.index().forNodes("taxons");
        Index<Node> ids = graphDb.index().forNodes(INDEX_TAXON_NAMES_AND_IDS, MapUtil.stringMap(IndexManager.PROVIDER, "lucene", "type", "fulltext"));
        TaxonFuzzySearchIndex fuzzySearchIndex = new TaxonFuzzySearchIndex(graphDb);
        IndexHits<Node> hits = taxons.query("*:*");
        for (Node hit : hits) {
            List<String> externalIds = new ArrayList<String>();
            TaxonNode taxonNode = new TaxonNode(hit);
            collectIds(externalIds, taxonNode);
            addToFuzzyIndex(graphDb, fuzzySearchIndex, hit, taxonNode);
            Iterable<Relationship> rels = hit.getRelationships(Direction.OUTGOING, RelTypes.SAME_AS);
            for (Relationship rel : rels) {
                TaxonNode sameAsTaxon = new TaxonNode(rel.getEndNode());
                collectIds(externalIds, sameAsTaxon);
                addToFuzzyIndex(graphDb, fuzzySearchIndex, hit, sameAsTaxon);
            }
            Transaction tx = graphDb.beginTx();
            try {
                String aggregateIds = StringUtils.join(externalIds, CharsetConstant.SEPARATOR);
                ids.add(hit, PropertyAndValueDictionary.PATH, aggregateIds);
                hit.setProperty(PropertyAndValueDictionary.EXTERNAL_IDS, aggregateIds);
                tx.success();
            } finally {
                tx.finish();
            }
        }
        hits.close();
    }

    protected void addToFuzzyIndex(GraphDatabaseService graphDb, TaxonFuzzySearchIndex fuzzySearchIndex, Node indexNode, TaxonNode taxonNode) {
        Transaction tx = graphDb.beginTx();
        try {
            fuzzySearchIndex.index(indexNode, taxonNode);
            tx.success();
        } finally {
            tx.finish();
        }
    }

    protected void collectIds(List<String> externalIds, TaxonNode taxonNode) {
        String externalId = taxonNode.getExternalId();
        if (StringUtils.isNotBlank(externalId)) {
            externalIds.add(externalId);
        }
        addDelimitedList(externalIds, taxonNode.getPath());
        addDelimitedList(externalIds, taxonNode.getPathIds());
    }

    private void addDelimitedList(List<String> externalIds, String path) {
        String[] pathElements = StringUtils.splitByWholeSeparator(path, CharsetConstant.SEPARATOR);
        if (pathElements != null) {
            externalIds.addAll(Arrays.asList(pathElements));
        }
    }
}
