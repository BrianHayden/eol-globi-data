package org.trophic.graph.client;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.trophic.graph.data.*;
import org.trophic.graph.db.GraphService;
import org.trophic.graph.domain.Study;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;

public class TrophicImporter {

    public static void main(final String[] commandLineArguments) throws StudyImporterException {
        new TrophicImporter().startImportStop(commandLineArguments);
    }

    public void startImportStop(String[] commandLineArguments) throws StudyImporterException {
        final GraphDatabaseService graphService = GraphService.getGraphService();
        importStudies(graphService);
        int count = 0;
        for (Node node : graphService.getAllNodes()) {
            System.out.print(count + ":{");
            for (String key : node.getPropertyKeys()) {
                System.out.println(key + "=" + node.getProperty(key));
            }
            System.out.println("}");
            count++;
        }
        graphService.shutdown();
    }

    public void importStudies(GraphDatabaseService graphService) throws StudyImporterException {
        ArrayList<StudyLibrary.Study> studies = new ArrayList<StudyLibrary.Study>();
        StudyLibrary.Study[] availableStudies = StudyLibrary.Study.values();
        studies.addAll(Arrays.asList(availableStudies));

        ArrayList<Study> importedStudies = new ArrayList<Study>();

        for (StudyLibrary.Study study : studies) {
            StudyImporter studyImporter = createStudyImporter(graphService, study);
            System.out.println("study [" + study + "] importing ...");
            importedStudies.add(studyImporter.importStudy());
            System.out.println("study [" + study + "]");
        }

        try {
            Writer writer = new FileWriter("./export.csv", false);
            for (Study importedStudy : importedStudies) {
                boolean includeHeader = importedStudies.indexOf(importedStudy) == 0;
                new StudyExporterImpl().exportStudy(importedStudy, writer, includeHeader);
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            throw new StudyImporterException("failed to export result to csv file", e);
        }

    }

    private StudyImporter createStudyImporter(GraphDatabaseService graphService, StudyLibrary.Study study) throws StudyImporterException {
        NodeFactory factory = new NodeFactory(graphService);
        ParserFactory parserFactory = new ParserFactoryImpl();
        return new StudyImporterFactory(parserFactory, factory).createImporterForStudy(study);
    }

}