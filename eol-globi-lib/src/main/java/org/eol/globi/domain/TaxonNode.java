package org.eol.globi.domain;

import org.neo4j.graphdb.Node;

import static org.eol.globi.domain.PropertyAndValueDictionary.*;

public class TaxonNode extends NamedNode implements Taxon {

    public TaxonNode(Node node) {
        super(node);
    }

    public TaxonNode(Node node, String name) {
        this(node);
        setName(name);
    }

    @Override
    public String getPath() {
        return getUnderlyingNode().hasProperty(PATH) ?
                (String) getUnderlyingNode().getProperty(PATH) : null;
    }

    @Override
    public void setPath(String path) {
        if (path != null) {
            getUnderlyingNode().setProperty(PATH, path);
        }
    }

    @Override
    public String getPathNames() {
        return getUnderlyingNode().hasProperty(PATH_NAMES) ?
                (String) getUnderlyingNode().getProperty(PATH_NAMES) : null;
    }

    @Override
    public void setPathNames(String pathNames) {
        if (pathNames != null) {
            getUnderlyingNode().setProperty(PATH_NAMES, pathNames);
        }
    }

    @Override
    public String getCommonNames() {
        return getUnderlyingNode().hasProperty(COMMON_NAMES) ?
                (String) getUnderlyingNode().getProperty(COMMON_NAMES) : null;
    }

    @Override
    public void setCommonNames(String commonNames) {
        if (commonNames != null) {
            getUnderlyingNode().setProperty(COMMON_NAMES, commonNames);
        }
    }

    @Override
    public String getRank() {
        return getUnderlyingNode().hasProperty(RANK) ?
                (String) getUnderlyingNode().getProperty(RANK) : null;

    }

    @Override
    public void setRank(String rank) {
        if (rank != null) {
            getUnderlyingNode().setProperty(RANK, rank);
        }
    }

    @Override
    public void setPathIds(String pathIds) {
        if (pathIds != null) {
            getUnderlyingNode().setProperty(PATH_IDS, pathIds);
        }
    }

    @Override
    public String getPathIds() {
        return getUnderlyingNode().hasProperty(PATH_IDS) ?
                (String) getUnderlyingNode().getProperty(PATH_IDS) : null;
    }
}
