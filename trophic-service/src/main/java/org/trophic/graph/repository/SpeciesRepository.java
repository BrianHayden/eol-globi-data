package org.trophic.graph.repository;

import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.data.neo4j.repository.NamedIndexRepository;
import org.trophic.graph.domain.Species;

public interface SpeciesRepository extends GraphRepository<Species>,
		NamedIndexRepository<Species> {
}