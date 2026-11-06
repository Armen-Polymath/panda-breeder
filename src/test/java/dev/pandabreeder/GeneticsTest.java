package dev.pandabreeder;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static dev.pandabreeder.Genetics.*;
import static dev.pandabreeder.Genetics.Gene.*;
class GeneticsTest {
    @Test void brownCross() { assertEquals(Math.pow(249.0/256,2),copies(offspring(known(BROWN,BROWN),known(BROWN,BROWN)),2),1e-14); }
    @Test void carrierCross() { assertEquals(Math.pow(125.0/256,2),copies(offspring(known(NORMAL,BROWN),known(BROWN,NORMAL)),2),1e-14); }
    @Test void carrierNoncarrier() { assertEquals(125.0/65536,copies(offspring(known(NORMAL,BROWN),known(NORMAL,NORMAL)),2),1e-14); }
    @Test void mutationFromNoBrown() { assertEquals(1.0/65536,copies(offspring(known(NORMAL,NORMAL),known(LAZY,WEAK)),2),1e-14); }
    @Test void allGenotypesNormalizedAndSymmetric() {
        for (Gene a:Gene.values()) for (Gene b:Gene.values()) {
            var x=offspring(known(a,b),known(WEAK,BROWN)); validate(x);
            assertEquals(x,offspring(known(WEAK,BROWN),known(a,b)));
            assertEquals(1,copies(x,0)+copies(x,1)+copies(x,2),1e-12);
        }
    }
    @Test void uncertaintyIsMixture() {
        var prior=wildPrior(null); validate(prior);
        assertEquals(1.0/64,copies(offspring(prior,prior),2),1e-14);
        assertTrue(wildPrior(NORMAL).keySet().stream().allMatch(g->g.phenotype()==NORMAL));
        assertThrows(IllegalArgumentException.class,()->validate(java.util.Map.of(new Genotype(BROWN,BROWN),Double.NaN)));
    }
}
