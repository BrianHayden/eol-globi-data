package org.trophic.graph.data;

import org.apache.commons.lang3.StringUtils;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.helpers.collection.ClosableIterable;
import org.trophic.graph.domain.*;
import org.trophic.graph.repository.TaxonRepository;

import static org.trophic.graph.domain.RelTypes.PART_OF;

public class TaxonFactory {

    private TaxonRepository taxonRepository;
    private GraphDatabaseService graphDb;
    private Index<Node> studies;

    public TaxonFactory(GraphDatabaseService graphDb, TaxonRepository taxonRepository) {
        this.graphDb = graphDb;
        this.taxonRepository = taxonRepository;
        this.studies = graphDb.index().forNodes("studies");
        
    }

    public Taxon create(String speciesName2, Family family) throws TaxonFactoryException {
        String cleanedSpeciesName = createName(speciesName2);
        String[] split = cleanedSpeciesName.split(" ");
        Taxon taxon = null;

        if (split.length > 1) {
            String firstPart = split[0];
            if (cleanedSpeciesName.contains("sp.") || cleanedSpeciesName.contains("spp.")) {
                taxon = createFamilyOrGenus(family, firstPart);
            } else {
                if (isFamilyName(firstPart)) {
                    taxon = createFamily(firstPart);
                } else {
                    Genus genus = createGenus(firstPart);
                    if (family != null) {
                        genus.createRelationshipTo(family, PART_OF);
                    }
                    Species species = createSpecies(genus, cleanedSpeciesName);
                    taxon = species;
                }
            }
        } else if (split.length == 1) {
            taxon = createFamilyOrGenus(family, split[0]);
        }
        return taxon;
    }

    private String createName(String speciesName2) {
        return speciesName2.replaceAll("\\(.*\\)", "");
    }

    private Taxon createFamilyOrGenus(Family family, String firstPart) throws TaxonFactoryException {
        Taxon taxon;
        if (isFamilyName(firstPart)) {
            taxon = createFamily(firstPart);
        } else {
            Genus genus = createGenus(firstPart);
            if (family != null) {
                genus.createRelationshipTo(family, PART_OF);
            }
            taxon = genus;
        }
        return taxon;
    }

    private boolean isFamilyName(String firstPart) {
        return firstPart.endsWith("ae");
    }

    public Species createSpecies(Genus genus, String speciesName) throws TaxonFactoryException {
        Species species = (Species) findTaxonOfClass(speciesName, Species.class);
        if (species == null) {
            Transaction transaction = graphDb.beginTx();
            try {
                species = new Species(graphDb.createNode());
                species.setName(speciesName);
                transaction.success();
            } finally {
                transaction.finish();
            }
        }
        if (null != genus) {
            species.createRelationshipTo(genus, PART_OF);
        }
        return species;
    }

    public Genus createGenus(String genusName) throws TaxonFactoryException {
        Genus genus = (Genus) findTaxonOfClass(genusName, Genus.class);
        if (genus == null) {
            Transaction transaction = graphDb.beginTx();
            try {
                genus = new Genus(graphDb.createNode(), genusName);
                transaction.success();
            } finally {
                transaction.finish();
            }

        }
        return genus;
    }

    public Family createFamily(final String familyName) throws TaxonFactoryException {
        Family family = null;
        if (familyName != null) {
            String trimmedFamilyName = StringUtils.trim(familyName);
            Taxon foundFamily = findTaxonOfClass(trimmedFamilyName, Family.class);
            if (foundFamily == null) {
                Transaction transaction = graphDb.beginTx();
                try {
                    family = new Family(graphDb.createNode(), trimmedFamilyName);
                    transaction.success();
                } finally {
                    transaction.finish();
                }
            } else {
                family = (Family) foundFamily;
            }
        }
        return family;
    }

    private Taxon findTaxonOfClass(String taxonName, Class expectedClass) throws TaxonFactoryException {
        ClosableIterable<Taxon> taxons = taxonRepository.findAllByPropertyValue("name", taxonName);
        Taxon taxon = null;
        if (taxons != null && taxons.iterator().hasNext()) {
            Taxon first = taxons.iterator().next();
            if (taxons.iterator().hasNext()) {
                Taxon second = taxons.iterator().next();
                throw new TaxonFactoryException("found taxon with duplicate name: [" + first.getName() + first.getClass().getSimpleName() + "] and [" + second.getName() + second.getClass().getSimpleName() + "]");
            }
            taxon = (first != null &&
                    first.getClass().equals(expectedClass)) ? first : null;
        }
        return taxon;
    }

    public TaxonRepository getTaxonRepository() {
        return taxonRepository;
    }

    public void setTaxonRepository(TaxonRepository taxonRepository) {
        this.taxonRepository = taxonRepository;
    }

    public Season createSeason(String seasonNameLower) {
        Transaction transaction = graphDb.beginTx();
        Season season;
        try {
            season = new Season(graphDb.createNode(), seasonNameLower);
            transaction.success();
        } finally {
            transaction.finish();
        }
        return season;
    }

    public Location createLocation(Double latitude, Double longitude, Double altitude) {
        Transaction transaction = graphDb.beginTx();
        Location location;
        try {
            location = new Location(graphDb.createNode(), latitude, longitude, altitude);
            transaction.success();
        } finally {
            transaction.finish();
        }
        return location;
    }

    public Specimen createSpecimen() {
        Transaction transaction = graphDb.beginTx();
        Specimen specimen;
        try {
            specimen = new Specimen(graphDb.createNode());
            transaction.success();
        } finally {
            transaction.finish();
        }
        return specimen;
    }

    public Study createStudy(String title) {
        Transaction transaction = graphDb.beginTx();
        Study study;
        try {
            Node node = graphDb.createNode();
            study = new Study(node, title);
            studies.add(node, "title", title);
            transaction.success();
        } finally {
            transaction.finish();
        }

        return study;
    }

    public Study findStudy(String title) {
        return new Study(studies.get("title", title).getSingle());
    }
}
