package org.eol.globi.data.taxon;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class OboParserTest {

    private static final Log LOG = LogFactory.getLog(OboParserTest.class);

    private int counter;

    @Test
    public void oboImport() throws IOException {
        BufferedReader bufferedReader = new OboTaxonReaderFactory().createReader();
        this.counter = 0;

        TaxonImportListener listener = new TaxonImportListener() {

            @Override
            public void addTerm(String name, long id) {
                if (getCounter() % 1000 == 0) {
                    LOG.info("got term with id: [" + id +
                            "], name: [" + name + "]");
                }
                assertThat(name, is(not(nullValue())));
                assertThat(id > 0, is(true));
                count();
            }

            @Override
            public void start() {

            }

            @Override
            public void finish() {

            }
        };

        OboParser oboParser = new OboParser();
        oboParser.parse(bufferedReader, listener);
        assertThat("expected a certain amount of terms", getCounter(), is(oboParser.getExpectedMaxTerms()));
    }


    private void count() {
        counter++;
    }

    public int getCounter() {
        return counter;
    }
}
