package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class StudyImporterForFishbase extends BaseStudyImporter {
    private static final Log LOG = LogFactory.getLog(StudyImporterForFishbase.class);

    public static final DateTimeFormatter DATE_TIME_FORMATTER = ISODateTimeFormat.dateTimeParser();

    public StudyImporterForFishbase(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public Study importStudy() throws StudyImporterException {
        String studyResource = "fishbase/fooditems.tsv";

        Map<String, Long> predatorSpecimenMap = new HashMap<String, Long>();
        try {
            LabeledCSVParser parser = parserFactory.createParser(studyResource, CharsetConstant.UTF8);
            parser.changeDelimiter('\t');
            while (parser.getLine() != null) {
                int lastLineNumber = parser.getLastLineNumber();
                if (importFilter.shouldImportRecord((long) lastLineNumber)) {
                    Study study = parseStudy(parser);
                    Specimen consumer = parseInteraction(parser, study);
                    associateLocation(parser, consumer);
                }
            }
        } catch (IOException e) {
            throw new StudyImporterException("failed to access resource [" + studyResource + "]");
        }

        return null;
    }

    private void associateLocation(LabeledCSVParser parser, Specimen consumer) throws StudyImporterException {
        parser.getValueByLabel("locality");
        parser.getValueByLabel("countryCode");
        String latitude = StringUtils.replace(parser.getValueByLabel("latitude"), "NULL", "");
        String longitude = StringUtils.replace(parser.getValueByLabel("longitude"), "NULL", "");

        if (StringUtils.isNotBlank(latitude) && StringUtils.isNotBlank(longitude)) {
            try {
                consumer.caughtIn(nodeFactory.getOrCreateLocation(Double.parseDouble(latitude),
                        Double.parseDouble(longitude), null));
            } catch (NodeFactoryException e) {
                throw new StudyImporterException("failed to create location using [" + latitude + "] and [" + longitude + "] on line [" + parser.lastLineNumber() + 1 + "]", e);

            } catch (NumberFormatException e) {
                throw new StudyImporterException("failed to create location using [" + latitude + "] and [" + longitude + "] on line [" + parser.lastLineNumber() + 1 + "]", e);
            }
        }
    }

    private Specimen parseInteraction(LabeledCSVParser parser, Study study) throws StudyImporterException {
        Specimen consumer;
        try {
            String consumerName = StringUtils.join(new String[]{parser.getValueByLabel("consumer genus"),
                    parser.getValueByLabel("consumer species")}, " ");
            consumer = nodeFactory.createSpecimen(consumerName);
            consumer.ate(nodeFactory.createSpecimen(parseFoodName(parser)));
            study.collected(consumer);
        } catch (NodeFactoryException e) {
            throw new StudyImporterException("failed to create specimens on line [" + parser.lastLineNumber() + 1 + "]", e);
        }
        return consumer;
    }

    private Study parseStudy(LabeledCSVParser parser) {
        String author = parser.getValueByLabel("author");
        String year = parser.getValueByLabel("year");
        String title = parser.getValueByLabel("title");
        return nodeFactory.getOrCreateStudy("Fishbase-" + author + year,
                author,
                "",
                "",
                title
                , year
                , "Database export shared by http://fishbase.org in December 2013. For use by Brian Hayden and Jorrit Poelen only.", null);
    }

    private String parseFoodName(LabeledCSVParser parser) {
        String foodName = StringUtils.join(new String[]{parser.getValueByLabel("food item genus"),
                parser.getValueByLabel("food item species")}, " ");
        if (StringUtils.isBlank(foodName) || StringUtils.contains(foodName, "NULL")) {
            foodName = parser.getValueByLabel("food III");
        }
        return foodName;
    }
}
