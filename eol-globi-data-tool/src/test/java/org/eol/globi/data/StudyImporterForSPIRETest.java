package org.eol.globi.data;

import com.hp.hpl.jena.rdf.model.impl.RDFDefaultErrorHandler;
import org.eol.globi.domain.Environment;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.domain.Term;
import org.eol.globi.service.GeoNamesService;
import org.eol.globi.service.GeoNamesServiceImpl;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Relationship;
import org.eol.globi.geo.LatLng;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.IsCollectionContaining.hasItem;

public class StudyImporterForSPIRETest extends GraphDBTestCase {

    @Test
    public void parseIllegalTitle() throws StudyImporterException {
        HashMap<String, String> properties = new HashMap<String, String>();
        StudyImporterForSPIRE.parseTitlesAndAuthors("this is really not supported, and is unformatted", properties);
        assertThat(properties.get(Study.CONTRIBUTOR), is(""));
        assertThat(properties.get(Study.TITLE), is("this is really not su...e9154c16f07ad2470849d90a8a0b9dab"));
        assertThat(properties.get(Study.DESCRIPTION), is("this is really not supported, and is unformatted"));

    }

    @Test
    public void parseAnotherYetYetOtherTitlesAndAuthorsFormat() throws StudyImporterException {
        Map<String, String> properties = new HashMap<String, String>();
        String titlesAndAuthors = "G. A. Knox, Antarctic marine ecosystems. In: Antarctic Ecology, M. W. Holdgate, Ed. (Academic Press, New York, 1970) 1:69-96, from p. 87.";
        StudyImporterForSPIRE.parseTitlesAndAuthors(titlesAndAuthors, properties);
        assertThat(properties.get(Study.DESCRIPTION), is("G. A. Knox, Antarctic marine ecosystems. In: Antarctic Ecology, M. W. Holdgate, Ed. (Academic Press, New York, 1970) 1:69-96, from p. 87."));
        assertThat(properties.get(Study.TITLE), is("Knox, Antarctic marin...984ae066666743823ac7b57da0e01f2d"));
        assertThat(properties.get(Study.CONTRIBUTOR), is(""));
    }

    @Test
    public void parseAnotherYetOtherTitlesAndAuthorsFormat() throws StudyImporterException {
        Map<String, String> properties = new HashMap<String, String>();
        StudyImporterForSPIRE.parseTitlesAndAuthors("B. A. Hawkins and R. D. Goeden, 1984.  Organization of a parasitoid community associated with a complex of galls on Atriplex spp. in southern California.  Ecol. Entomol. 9:271-292, from p. 274.", properties);
        assertThat(properties.get(Study.TITLE), is("Hawkins and Goeden, 1...fcebc21f82937fa4ab9f77a0ecbd62e3"));
        assertThat(properties.get(Study.DESCRIPTION), is("B. A. Hawkins and R. D. Goeden, 1984.  Organization of a parasitoid community associated with a complex of galls on Atriplex spp. in southern California.  Ecol. Entomol. 9:271-292, from p. 274."));
        assertThat(properties.get(Study.CONTRIBUTOR), is(""));
    }

    @Test
    public void parseYetOtherTitlesAndAuthorsFormat() throws StudyImporterException {
        Map<String, String> properties = new HashMap<String, String>();
        StudyImporterForSPIRE.parseTitlesAndAuthors("Townsend, CR, Thompson, RM, McIntosh, AR, Kilroy, C, Edwards, ED, Scarsbrook, MR. 1998.  Disturbance, resource supply and food-web architecture in streams.  Ecology Letters 1:200-209.", properties);
        assertThat(properties.get(Study.TITLE), is("Townsend, CR, Thompso...db61dcc043a135ac2fa8b440e11165e3"));
        assertThat(properties.get(Study.DESCRIPTION), is("Townsend, CR, Thompson, RM, McIntosh, AR, Kilroy, C, Edwards, ED, Scarsbrook, MR. 1998.  Disturbance, resource supply and food-web architecture in streams.  Ecology Letters 1:200-209."));
        assertThat(properties.get(Study.CONTRIBUTOR), is(""));
    }

    @Test
    public void importSingleLink() throws NodeFactoryException {
        assertSingleImport("habitat", "TEST:habitat", "habitat");

    }

    private void assertSingleImport(String spireHabitat, String envoId, String envoLabel) throws NodeFactoryException {
        StudyImporterForSPIRE studyImporterForSPIRE = createImporter();
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put(Study.TITLE, "the study of men eating dogs");
        properties.put(StudyImporterForSPIRE.PREY_NAME, "dog");
        properties.put(StudyImporterForSPIRE.PREDATOR_NAME, "man");
        properties.put(StudyImporterForSPIRE.LOCALITY_ORIGINAL, "something");
        properties.put(StudyImporterForSPIRE.OF_HABITAT, spireHabitat);
        studyImporterForSPIRE.importTrophicLink(properties);

        TaxonNode dog = nodeFactory.findTaxonByName("dog");
        assertThat(dog, is(notNullValue()));
        TaxonNode man = nodeFactory.findTaxonByName("man");
        assertThat(man, is(notNullValue()));
        Iterable<Relationship> specimenRels = man.getUnderlyingNode().getRelationships(Direction.INCOMING, RelTypes.CLASSIFIED_AS);

        int count = 0;
        for (Relationship specimenRel : specimenRels) {
            count++;
            Specimen specimen = new Specimen(specimenRel.getStartNode());
            assertThat(specimen.getSampleLocation().getLatitude(), is(1.0));
            assertThat(specimen.getSampleLocation().getLongitude(), is(2.0));

            List<Environment> environments = specimen.getSampleLocation().getEnvironments();
            assertThat(environments.size(), is(1));
            Environment environment = environments.get(0);
            assertThat(environment.getExternalId(), is(envoId));
            assertThat(environment.getName(), is(envoLabel));
        }
        assertThat(count, is(1));
    }

    private StudyImporterForSPIRE createImporter() {
        StudyImporterForSPIRE studyImporterForSPIRE = new StudyImporterForSPIRE(null, nodeFactory);
        studyImporterForSPIRE.setGeoNamesService(new GeoNamesService() {

            @Override
            public boolean hasPositionForLocality(String locality) {
                return "something".equals(locality);
            }

            @Override
            public LatLng findPointForLocality(String locality) throws IOException {
                return hasPositionForLocality(locality) ? new LatLng(1.0, 2.0) : null;
            }

        });
        return studyImporterForSPIRE;
    }

    @Test
    public void importStudy() throws IOException, StudyImporterException {
        RDFDefaultErrorHandler.silent = true;
        StudyImporterForSPIRE importer = createImporter();
        TestTrophicLinkListener listener = new TestTrophicLinkListener();
        importer.setTrophicLinkListener(listener);
        importer.importStudy();

        assertGAZMapping(listener);

        GeoNamesServiceImpl geoNamesServiceImpl = new GeoNamesServiceImpl();
        for (String locality : listener.localities) {
            assertThat(geoNamesServiceImpl.hasPositionForLocality(locality), is(true));
        }
        assertThat(listener.getCount(), is(30196));

        assertThat(listener.descriptions, not(hasItem("http://spire.umbc.edu/ontologies/SpireEcoConcepts.owl#")));
        assertThat(listener.titles, not(hasItem("http://spire.umbc.edu/")));
        assertThat(listener.environments, not(hasItem("http://spire.umbc.edu/ontologies/SpireEcoConcepts.owl#")));
        assertThat(listener.publicationYears, hasItem("1996"));
    }

    private void assertGAZMapping(TestTrophicLinkListener listener) {
        Map<String, Term> gazMap = new HashMap<String, Term>() {{
            put("Country: New Zealand;   State: Otago;   Locality: Catlins, Craggy Tor catchment", new Term("GAZ:00146864", "The Catlins"));
            put("Country: Scotland", new Term("GAZ:00002639", "Scotland"));
            put("Country: USA;   State: Georgia", new Term("GAZ:00002611", "State of Georgia"));
            put("Country: USA;   State: Iowa", new Term("GAZ:00004438", "State of Iowa"));
            put("Country: Southern Ocean", new Term("GAZ:00000373", "Southern Ocean"));
            put("Country: USA", new Term("GAZ:00002459", "United States of America"));
            put("Country: USA;   State: Iowa;   Locality: Mississippi River", new Term("GAZ:00004438", "State of Iowa"));
            put("Country: Japan", new Term("GAZ:00002747", "Japan"));
            put("Country: Malaysia;   Locality: W. Malaysia", new Term("GAZ:00003902", "Malaysia"));
            put("Country: Chile;   Locality: central Chile", new Term("GAZ:00002825", "Chile"));
            put("Country: USA;   State: New Mexico;   Locality: Aden Crater", new Term("GAZ:00004427", "State of New Mexico"));
            put("Country: USA;   State: Alaska;   Locality: Torch Bay", new Term("GAZ:00002521", "State of Alaska"));
            put("Country: USA;   State: Pennsylvania", new Term("GAZ:00002542", " Commonwealth of Pennsylvania"));
            put("Country: Costa Rica", new Term("GAZ:00002901", "Costa Rica"));
            put("Country: Pacific", new Term("GAZ:00000360", "Pacific Ocean"));
            put("Country: USA;   State: California;   Locality: Cabrillo Point", new Term("GAZ:00002461", "State of California"));
            put("Country: USA;   State: Texas", new Term("GAZ:00002580", "State of Texas"));
            put("Country: Portugal", new Term("GAZ:00004125", "Autonomous Region (Portugal)"));
            put("Country: USA;   Locality: Northeastern US contintental shelf", new Term("GAZ:00002459", "United States of America"));
            put("Country: Sri Lanka", new Term("GAZ:00003924", "Sri Lanka"));
            put("Country: USA;   State: Maine;   Locality: Troy", new Term("GAZ:00002602", "State of Maine"));
            put("Country: New Zealand", new Term("GAZ:00000469", "New Zealand"));
            put("Country: USA;   State: Maine;   Locality: Gulf of Maine", new Term("GAZ:00002876", "Gulf of Maine"));
            put("Country: New Zealand;   State: Otago;   Locality: Dempster's Stream, Taieri River, 3 O'Clock catchment", new Term("GAZ:00004767", "Otago Region"));
            put("Country: Panama;   Locality: Gatun Lake", new Term("GAZ:00002898", "Lake Gatun"));
            put("Country: USA;   State: Maryland;   Locality: Chesapeake Bay", new Term("GAZ:00002604", "Chesapeake Bay"));
            put("Country: India;   Locality: Cochin", new Term("GAZ:00002839", "India"));
            put("Country: Ethiopia;   Locality: Lake Abaya", new Term("GAZ:00041560", "Lake Abaya"));
            put("Country: unknown;   State: Black Sea", new Term("GAZ:00008171", "Black Sea"));
            put("Country: St. Martin;   Locality: Caribbean", new Term("GAZ:00044587", "Saint-Martin Island"));
            put("Country: USA;   State: Yellowstone", new Term("GAZ:00002534", "Yellowstone National Park"));
            put("Country: Scotland;   Locality: Loch Leven", new Term("GAZ:00002639", "Scotland"));
            put("Country: New Zealand;   State: Otago;   Locality: Sutton Stream, Taieri River, Sutton catchment", new Term("GAZ:00004767", "Otago Region"));
            put("Country: USA;   State: Alaska;   Locality: Barrow", new Term("GAZ:00198344", "City Of Barrow"));
            put("Country: Malawi;   Locality: Lake Nyasa", new Term("GAZ:00000058", "Lake Malawi"));
            put("Country: USA;   State: Alaska;   Locality: Aleutian Islands", new Term("GAZ:00005858", "Aleutian Islands"));
            put("Country: USA;   State: California;   Locality: Southern California", new Term("GAZ:00168979", "Southern California"));
            put("Country: Canada;   State: Manitoba", new Term("GAZ:00002571", "Province of Manitoba"));
            put("Country: USA;   State: Maine", new Term("GAZ:00002602", "State Of Maine"));
            put("Country: Polynesia", new Term("GAZ:00005861", "Polynesia"));
            put("Country: South Africa", new Term("GAZ:00000553", "South Africa"));
            put("Country: New Zealand;   State: Otago;   Locality: Berwick, Meggatburn", new Term("GAZ:00004767", "Otago Region"));
            put("Country: New Zealand;   State: Otago;   Locality: Venlaw, Mimihau catchment", new Term("GAZ:00004767", "Otago Region"));
            put("Country: USA;   State: Montana", new Term("GAZ:00002606", "State of Montana"));
            put("Country: UK;   State: Yorkshire;   Locality: Aire,  Nidd & Wharfe Rivers", new Term("GAZ:00003688", "Yorkshire and the Humber"));
            put("Country: Hong Kong", new Term("GAZ:00003203", "Hong Kong"));
            put("Country: Pacific;   State: Bay of Panama", new Term("GAZ:00047280", "Panama Bay"));
            put("Country: Netherlands;   State: Wadden Sea;   Locality: Ems estuary", new Term("GAZ:00008137", "Wadden See"));
            put("Country: New Zealand;   State: Otago;   Locality: North Col, Silver catchment", new Term("GAZ:00004767", "Otago Region"));
            put("Country: USA;   State: North Carolina", new Term("GAZ:00002520", "State of North Carolina"));
            put("Country: USA;   State: Washington", new Term("GAZ:00002553", "State of Washington"));
            put("Country: USA;   State: Alaska", new Term("GAZ:00002521", "State of Alaska"));
            put("Country: USA;   State: Hawaii", new Term("GAZ:00003939", "State of Hawaii"));
            put("Country: Uganda;   Locality: Lake George", new Term("GAZ:00001102", "Uganda"));
            put("Country: Costa Rica;   State: Guanacaste", new Term("GAZ:00003210", "Guanacaste Province"));
            put("Country: USA;   State: Massachusetts;   Locality: Cape Ann", new Term("GAZ:00002537", "Commonwealth of Massachusetts"));
            put("Country: USA;   State: Maine;   Locality: Martins", new Term("GAZ:00002602", "State of Maine"));
            put("Country: USA;   State: New York", new Term("GAZ:00002514", "State of New York"));
            put("Country: General;   Locality: General", new Term("GAZ:00000448", "geographic location"));
            put("Country: New Zealand;   State: Otago;   Locality: Stony, Sutton catchment", new Term("GAZ:00004767", "Otago Region"));
            put("Country: Tibet", new Term("GAZ:00004219", "Tibet Autonomous Region"));
            put("Country: USA;   State: Texas;   Locality: Franklin Mtns", new Term("GAZ:00002580", "State of Texas"));
            put("Country: Russia", new Term("GAZ:00002721", "Russia"));
            put("Country: New Zealand;   State: Otago;   Locality: Broad, Lee catchment", new Term("GAZ:00004767", "Otago Region"));
            put("Country: Africa;   Locality: Lake McIlwaine", new Term("GAZ:00016177", "Lake Chivero"));
            put("Country: England;   State: River Medway", new Term("GAZ:00046238", "River Medway"));
            put("Country: South Africa;   Locality: Southwest coast", new Term("GAZ:00001094", "Republic of South Africa"));
            put("Country: USA;   State: Kentucky", new Term("GAZ:00004440", "Commonwealth of Kentucky"));
            put("Country: USA;   State: Washington;   Locality: Cape Flattery", new Term("GAZ:00049988", "Cape Flattery"));
            put("Country: USA;   State: New Jersey", new Term("GAZ:00002557", "State of New Jersey"));
            put("Country: India;   Locality: Rajasthan Desert", new Term("GAZ:00002839", "India"));
            put("Country: England", new Term("GAZ:00002641", "England"));
            put("Country: Austria;   Locality: Hafner Lake", new Term("GAZ:00002942", "Austria"));
            put("Country: USA;   State:  NE USA", new Term("GAZ:00002459", "United States of America"));
            put("Country: England;   Locality: Sheffield", new Term("GAZ:00004871", "City of Sheffield"));
            put("Country: Uganda", new Term("GAZ:00001102", "Uganda"));
            put("Country: USA;   State:  California;   Locality: Monterey Bay", new Term("GAZ:00002509", "Monterey Bay"));
            put("Country: Germany", new Term("GAZ:00002646", "Germany"));
            put("Country: England;   Locality: Skipwith Pond", new Term("GAZ:00002641", "England"));
            put("Country: USA;   State: Wisconsin;   Locality: Little Rock Lake", new Term("GAZ:00002586", "State of Wisconsin"));
            put("Country: USA;   State: California;   Locality: Coachella Valley", new Term("GAZ:00002461", "State of California"));
            put("Country: Arctic", new Term("GAZ:00000323", "Arctic Ocean"));
            put("Country: USA;   State: Michigan", new Term("GAZ:00003152", "State of Michigan"));
            put("Country: Mexico;   State: Guerrero", new Term("GAZ:00010927", "State of Guerrero"));
            put("Country: Norway;   State: Spitsbergen", new Term("GAZ:00005397", "Spitzbergen"));
            put("Country: USA;   State: Kentucky;   Locality: Station 1", new Term("GAZ:00004440", "Commonwealth of Kentucky"));
            put("Country: New Zealand;   State: Otago;   Locality: Kye Burn", new Term("GAZ:00004767", "Otago Region"));
            put("Country: New Zealand;   State: Otago;   Locality: Little Kye, Kye Burn catchment", new Term("GAZ:00004767", "Otago Region"));
            put("Country: USA;   State: North Carolina;   Locality: Pamlico", new Term("GAZ:00002520", "State of North Carolina"));
            put("Country: Antarctic", new Term("GAZ:00000462", "Antarctica"));
            put("Country: USA;   State: Arizona", new Term("GAZ:00002518", "State of Arizona"));
            put("Country: England;   Locality: Lancaster", new Term("GAZ:04000224", "City of Lancaster"));
            put("Country: USA;   State: Florida;   Locality: Everglades", new Term("GAZ:00082878", "Everglades"));
            put("Country: Barbados", new Term("GAZ:00001251", "Barbados"));
            put("Country: USA;   State: New York;   Locality: Bridge Brook", new Term("GAZ:00002514", "State of New York"));
            put("Country: England;   Locality: Oxshott Heath", new Term("GAZ:00002641", "England"));
            put("Country: New Zealand;   State: Otago;   Locality: Blackrock, Lee catchment", new Term("GAZ:00004767", "Otago Region"));
            put("Country: Canada;   State: Ontario", new Term("GAZ:00002563", "Province of Ontario"));
            put("Country: Puerto Rico;   Locality: El Verde", new Term("GAZ:00006935", "Commonwealth of Puerto Rico"));
            put("Country: Quebec", new Term("GAZ:00002569", "Province of Quebec"));
            put("Country: Ireland", new Term("GAZ:00002943", "Republic of Ireland"));
            put("Country: Wales;   Locality: Dee River", new Term("GAZ:00007857", "River Dee [Wales]"));
            put("Country: Marshall Islands", new Term("GAZ:00006470", "Republic of the Marshall Islands"));
            put("Country: New Zealand;   State: South Island;   Locality: Canton Creek, Taieri River, Lee catchment", new Term("GAZ:00004764", "South Island"));
            put("Country: Seychelles", new Term("GAZ:00006922", "The Seychelles"));
            put("Country: Namibia;   Locality: Namib Desert", new Term("GAZ:00007516", "Namib Desert"));
            put("Country: USA;   State: Rhode Island", new Term("GAZ:00002531", "State of Rhode Island"));
            put("Country: USA;   State: Idaho-Utah;   Locality: Deep Creek", new Term("GAZ:00000448", "geographic location"));
            put("Country: Malawi", new Term("GAZ:00001105", "Malawi"));
            put("Country: Malaysia", new Term("GAZ:00003902", "GAZ:00003902"));
            put("Country: Europe;   State: Central Europe", new Term("GAZ:00000464", "Europe"));
            put("Country: USA;   State: Florida", new Term("GAZ:00002888", "State of Florida"));
            put("Country: Norway;   State: Oppland;   Locality: Ovre Heimdalsvatn Lake", new Term("GAZ:00005244", "Oppland County"));
            put("Country: Austria;   Locality: Vorderer Finstertaler Lake", new Term("GAZ:00002942", "Austria"));
            put("Country: Canada;   Locality: high Arctic", new Term("GAZ:00002560", "Canada"));
            put("Country: unknown", new Term("GAZ:00000448", "geographic location"));
            put("Country: Peru", new Term("GAZ:00002932", "Peru"));
            put("Country: USA;   State: New England", new Term("GAZ:00006323", "New England Division"));
            put("Country: Great Britain", new Term("GAZ:00002637", "United Kingdom"));
            put("Country: New Zealand;   State: Otago;   Locality: German, Kye Burn catchment", new Term("GAZ:00004767", "Otago Region"));
            put("Country: USA;   State: Colorado", new Term("GAZ:00006254", "State of Colorado"));
            put("Country: USA;   State: Texas;   Locality: Hueco Tanks", new Term("GAZ:00002580", "State of Texas"));
            put("Country: Canada;   State: Ontario;   Locality: Mad River", new Term("GAZ:00002563", "Province of Ontario"));
            put("Country: Wales;   Locality: River Rheidol", new Term("GAZ:00002640", "Wales"));
            put("Country: Costa Rica;   State: de Osa", new Term("GAZ:00002901", "Costa Rica"));
            put("Country: Finland", new Term("GAZ:00002937", "Finland"));
            put("Country: Africa;   Locality: Crocodile Creek,  Lake Nyasa", new Term("GAZ:00000058", "Lake Malawi"));
            put("Country: USA;   State: Florida;   Locality: South Florida", new Term("GAZ:00004412", "Southern Florida"));
            put("Country: USA;   State: Illinois", new Term("GAZ:00003142", "State of Illinois"));
            put("Country: Puerto Rico;   Locality: Puerto Rico-Virgin Islands shelf", new Term("GAZ:00002822", "Puerto Rico"));
            put("Country: England;   Locality: River Thames", new Term("GAZ:00007824", "River Thames"));
            put("Country: Madagascar", new Term("GAZ:00006934", "Madagascar"));
            put("Country: USA;   State: New Mexico;   Locality: White Sands", new Term("GAZ:00004427", "State of New Mexico"));
            put("Country: England;   Locality: River Cam", new Term("GAZ:00002641", "England"));
            put("Country: Australia", new Term("GAZ:00000463", "Australia"));
            put("Country: USA;   State: North Carolina;   Locality: Coweeta", new Term("GAZ:00002520", "State of North Carolina"));
            put("Country: Scotland;   Locality: Ythan estuary", new Term("GAZ:00002639", "Scotland"));
            put("Country: Wales;   Locality: River Clydach", new Term("GAZ:00052132", "South Wales"));
            put("Country: USA;   State: Texas;   Locality: Hueco Mountains", new Term("GAZ:00002580", "State of Texas"));
            put("Country: Wales", new Term("GAZ:00002640", "Wales"));
            put("Country: USA;   State: Arizona;   Locality: Sonora Desert", new Term("GAZ:00006847", "Sonoran Desert"));
            put("Country: England;   Locality: Silwood Park", new Term("GAZ:00052254", "Silwood Park"));
            put("Country: Austria;   Locality: Neusiedler Lake", new Term("GAZ:00002942", "Austria"));
            put("Country: New Zealand;   State: Otago;   Locality: Narrowdale catchment", new Term("GAZ:00004767", "Otago Region"));
            put("Country: USA;   State: California", new Term("GAZ:00002461", "State of California"));
            put("Country: England;   State: Oxfordshire;   Locality: Wytham Wood", new Term("GAZ:00052249", "Wytham Woods"));
            put("Country: USA;   State: Michigan;   Locality: Tuesday Lake", new Term("GAZ:00003152", "State of Michigan"));
            put("Country: USA;   State: Alabama", new Term("GAZ:00006881", "State of Alabama"));
            put("Country: New Zealand;   State: Otago;   Locality: Healy Stream, Taieri River, Kye Burn catchment", new Term("GAZ:00004767", "Otago Region"));
            put("Country: USA;   State: New York;   Locality: Long Island", new Term("GAZ:00002584", "Long Island"));
            put("Country: Venezuela", new Term("GAZ:00002931", "Venezuela"));
            put("Country: New Zealand;   State: Otago;   Locality: Akatore, Akatore catchment", new Term("GAZ:00004767", "Otago Region"));
        }};

        int gazHit = 0;
        for (String locality : listener.localities) {
            if (gazMap.containsKey(locality) && gazMap.get(locality).getId().startsWith("GAZ:")) {
                gazHit++;
            } else {
                System.out.println("put(\"" + locality + "\", new Term(\"externalid\", \"name\"));");
            }
        }
        assertThat(gazHit, is(listener.localities.size()));
    }


    private static class TestTrophicLinkListener implements TrophicLinkListener {
        public int getCount() {
            return count;
        }

        private int count = 0;
        Set<String> localities = new HashSet<String>();
        Set<String> descriptions = new HashSet<String>();
        Set<String> titles = new HashSet<String>();
        List<String> environments = new ArrayList<String>();
        List<String> publicationYears = new ArrayList<String>();

        @Override
        public void newLink(Map<String, String> properties) {
            if (properties.containsKey(StudyImporterForSPIRE.LOCALITY_ORIGINAL)) {
                localities.add(properties.get(StudyImporterForSPIRE.LOCALITY_ORIGINAL));
            }

            if (properties.containsKey(Study.DESCRIPTION)) {
                descriptions.add(properties.get(Study.DESCRIPTION));
            }
            if (properties.containsKey(Study.TITLE)) {
                titles.add(properties.get(Study.TITLE));
            }

            if (properties.containsKey(Study.TITLE)) {
                titles.add(properties.get(Study.TITLE));
            }
            if (properties.containsKey(StudyImporterForSPIRE.OF_HABITAT)) {
                environments.add(properties.get(StudyImporterForSPIRE.OF_HABITAT));
            }
            if (properties.containsKey(Study.PUBLICATION_YEAR)) {
                publicationYears.add(properties.get(Study.PUBLICATION_YEAR));
            }
            count++;
        }
    }


}
