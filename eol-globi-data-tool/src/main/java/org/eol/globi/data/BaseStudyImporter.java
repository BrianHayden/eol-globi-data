package org.eol.globi.data;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.Study;
import org.eol.globi.service.GeoNamesService;
import org.eol.globi.service.GeoNamesServiceImpl;

public abstract class BaseStudyImporter extends BaseImporter implements StudyImporter {
    private final Log LOG = LogFactory.getLog(BaseStudyImporter.class);
    protected ParserFactory parserFactory;
    protected ImportFilter importFilter = new ImportFilter() {
        @Override
        public boolean shouldImportRecord(Long recordNumber) {
            return true;
        }
    };

    private GeoNamesService geoNamesService = new GeoNamesServiceImpl();

    private ImportLogger importLogger = new ImportLogger() {
        @Override
        public void warn(Study study, String message) {
            LOG.warn("import logger says: [" + message + "]");
        }

        @Override
        public void info(Study study, String message) {
            LOG.info("import logger says: [" + message + "]");
        }

        @Override
        public void severe(Study study, String message) {
            LOG.error("import logger says: [" + message + "]");
        }
    };

    public BaseStudyImporter(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(nodeFactory);
        this.parserFactory = parserFactory;
    }

    @Override
    public void setFilter(ImportFilter importFilter) {
        this.importFilter = importFilter;
    }

    @Override
    public void setLogger(ImportLogger importLogger) {
        this.importLogger = importLogger;
    }

    public ImportLogger getLogger() {
        return this.importLogger;
    }

    public void setGeoNamesService(GeoNamesService geoNamesService) {
        this.geoNamesService = geoNamesService;
    }

    public GeoNamesService getGeoNamesService() {
        return geoNamesService;
    }
}
