package dev.pandabreeder;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
class CollectionDataTest {
    CollectionData.Panda add(CollectionData data) { var p=new CollectionData.Panda(); p.uuid=UUID.randomUUID(); p.id="P"+data.nextId++; data.pandas.add(p); return p; }
    @Test void parentageIsExplicitAndCycleSafe() {
        var d=new CollectionData(); var a=add(d); var b=add(d); var c=add(d);
        d.parentage(c,a,b); assertEquals(a.uuid,c.parentA); assertTrue(a.children.contains(c.uuid));
        Genetics.validate(c.belief()); assertTrue(c.evidence.startsWith("Inferred"));
        assertThrows(IllegalArgumentException.class,()->d.parentage(a,c,b));
        assertThrows(IllegalArgumentException.class,()->d.parentage(c,a,a));
        d.parentage(c,a,b); assertEquals(1,a.children.size());
        d.remove(a); assertEquals(a.uuid,c.parentA);
    }
}
