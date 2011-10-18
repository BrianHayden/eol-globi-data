package org.trophic.graph.data;

import org.neo4j.helpers.collection.ClosableIterable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.trophic.graph.domain.Family;
import org.trophic.graph.domain.Genus;
import org.trophic.graph.domain.Species;
import org.trophic.graph.domain.Taxon;
import org.trophic.graph.repository.TaxonRepository;

@Component
public class TaxonFactory {

    @Autowired
    TaxonRepository taxonRepository;

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
                    Species species = createSpecies(createGenus(family, firstPart), cleanedSpeciesName);
                    species.persist();
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
            taxon = createGenus(family, firstPart);
        }
        return taxon;
    }

    private boolean isFamilyName(String firstPart) {
        return firstPart.endsWith("ae");
    }

    private Species createSpecies(Genus genus, String speciesName) throws TaxonFactoryException {
        Species species = (Species) findTaxonOfClass(speciesName, Species.class);
        if (species == null) {
            species = new Species();
            species.setName(speciesName);
            species.persist();
        }
        species.partOf(genus);
        return species;
    }

    private Genus createGenus(Family family, String genusName) throws TaxonFactoryException {


        Genus genus = (Genus) findTaxonOfClass(genusName, Genus.class);
        if (genus == null) {
            genus = new Genus(genusName).persist();
        }
        genus.partOf(family);
        return genus;
    }

    public Family createFamily(final String familyName) throws TaxonFactoryException {
        Family family = null;
        if (familyName != null) {
            String trimmedFamilyName = StringUtils.trimWhitespace(familyName);
            Taxon foundFamily = findTaxonOfClass(trimmedFamilyName, Family.class);
            if (foundFamily == null) {
                family = new Family(trimmedFamilyName).persist();
            } else {
                family = (Family) foundFamily;
            }
        }
        return family;
    }

    private Taxon findTaxonOfClass(String taxonName, Class expectedClass) throws TaxonFactoryException {
        ClosableIterable<Taxon> taxons = taxonRepository.findAllByPropertyValue("name", taxonName);
        Taxon taxon = null;
        if (taxons.iterator().hasNext()) {
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

}
