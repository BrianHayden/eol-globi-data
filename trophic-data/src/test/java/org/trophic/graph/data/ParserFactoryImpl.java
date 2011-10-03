package org.trophic.graph.data;

import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.LabeledCSVParser;

import java.io.IOException;
import java.util.zip.GZIPInputStream;

public class ParserFactoryImpl implements ParserFactory {

    public LabeledCSVParser createParser() throws IOException {
        return new LabeledCSVParser(new CSVParser(new GZIPInputStream(getClass().getResourceAsStream("mississippiAlabamaFishDiet.csv.gz"))));
    }

}
