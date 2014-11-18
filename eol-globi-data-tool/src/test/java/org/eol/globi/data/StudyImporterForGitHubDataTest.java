package org.eol.globi.data;

import org.eol.globi.domain.Study;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.*;
import static org.junit.internal.matchers.IsCollectionContaining.hasItem;

public class StudyImporterForGitHubDataTest extends GraphDBTestCase {

    @Test
    public void importAll() throws StudyImporterException, NodeFactoryException {
        StudyImporter importer = new StudyImporterForGitHubData(new ParserFactoryImpl(), nodeFactory);
        importer.importStudy();

        List<Study> allStudies = NodeFactory.findAllStudies(getGraphDb());
        List<String> refs = new ArrayList<String>();
        List<String> DOIs = new ArrayList<String>();
        for (Study study : allStudies) {
            DOIs.add(study.getDOI());
            refs.add(study.getCitation());
        }

        assertThat(refs, hasItem("Gittenberger, A., Gittenberger, E. (2011). Cryptic, adaptive radiation of endoparasitic snails: sibling species of Leptoconchus (Gastropoda: Coralliophilidae) in corals. Org Divers Evol, 11(1), 21–41. doi:10.1007/s13127-011-0039-1"));
        assertThat(DOIs, hasItem("doi:10.1007/s13127-011-0039-1"));
        assertThat(DOIs, hasItem("doi:10.3354/meps09511"));

        assertThat(nodeFactory.findTaxonByName("Leptoconchus incycloseris"), is(notNullValue()));
        assertThat(nodeFactory.findTaxonByName("Sandalolitha dentata"), is(notNullValue()));
    }

}