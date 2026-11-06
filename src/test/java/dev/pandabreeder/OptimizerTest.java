package dev.pandabreeder;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static dev.pandabreeder.Genetics.Gene.*;
class OptimizerTest {
    Optimizer.Panda panda(String id, Genetics.Gene a, Genetics.Gene b) { return new Optimizer.Panda(id,new Genetics.Genotype(a,b)); }
    @Test void futureCarriersReduceExpectedAttempts() {
        var plan=new Optimizer().recommend(List.of(panda("a",NORMAL,NORMAL),panda("b",NORMAL,NORMAL)));
        assertTrue(plan.expectedAttempts()<65536); assertTrue(plan.expectedAttempts()>1);
        assertEquals(1.0/65536,plan.brownProbability(),1e-14);
    }
    @Test void brownPairIsFixedPairOptimal() {
        var plan=new Optimizer().recommend(List.of(panda("a",BROWN,BROWN),panda("b",BROWN,BROWN),panda("c",NORMAL,NORMAL)));
        assertEquals("a",plan.first()); assertEquals("b",plan.second());
        assertEquals(1/plan.brownProbability(),plan.expectedAttempts(),1e-10);
    }
    @Test void noSelfBreedingAndEmptyPool() {
        assertNull(new Optimizer().recommend(List.of(panda("a",BROWN,BROWN))));
        var plan=new Optimizer().recommend(List.of(panda("a",NORMAL,BROWN),panda("b",NORMAL,NORMAL)));
        assertNotEquals(plan.first(),plan.second());
        assertTrue(plan.expectedAttempts()<1/plan.brownProbability());
    }
}
