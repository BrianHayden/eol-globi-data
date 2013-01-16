package org.trophic.graph.data.taxon;

import org.junit.Test;
import org.trophic.graph.data.taxon.TaxonNameNormalizer;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class TaxonNameNormalizerTest {

    @Test
    public void cleanName() {
        TaxonNameNormalizer normalizer = new TaxonNameNormalizer();
        assertThat(normalizer.normalize("Blbua blas "), is("Blbua blas"));
        assertThat(normalizer.normalize("Blbua  blas  "), is("Blbua blas"));
        assertThat(normalizer.normalize("Blbua  blas (akjhaskdjhf)"), is("Blbua blas"));
        assertThat(normalizer.normalize("  Blbua  blas (akjhaskdjhf)"), is("Blbua blas"));
        assertThat(normalizer.normalize("Aegathoa oculata¬†"), is("Aegathoa oculata"));
        assertThat(normalizer.normalize("Aegathoa oculata*"), is("Aegathoa oculata"));
        assertThat(normalizer.normalize("Aegathoa oculata?"), is("Aegathoa oculata"));
        assertThat(normalizer.normalize("Aegathoa oculata sp."), is("Aegathoa oculata"));
        assertThat(normalizer.normalize("Aegathoa oculata spp"), is("Aegathoa oculata"));
        assertThat(normalizer.normalize("Aegathoa oculata spp."), is("Aegathoa oculata"));
        assertThat(normalizer.normalize("Taraxacum leptaleum"), is("Taraxacum leptaleum"));
        assertThat(normalizer.normalize("NA"), is("NomenNescio"));
        assertThat(normalizer.normalize("NE"), is("NomenNescio"));
        assertThat(normalizer.normalize("'Loranthus'"), is("Loranthus"));
        assertThat(normalizer.normalize("Leptochela cf bermudensis"), is("Leptochela bermudensis"));
    }
}
