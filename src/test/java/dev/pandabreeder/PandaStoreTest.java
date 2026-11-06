package dev.pandabreeder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class PandaStoreTest {
    @TempDir Path root;
    CollectionData sample() {
        var d=new CollectionData();var p=new CollectionData.Panda();p.id="P01";p.uuid=UUID.randomUUID();p.dimension="minecraft:overworld";
        p.main=Genetics.Gene.NORMAL;p.hidden=Genetics.Gene.BROWN;d.pandas.add(p);d.nextId=2;return d;
    }
    @Test void roundTripAndBackup() throws Exception {
        var store=new PandaStore(root,"server/world");var d=sample();store.save(d);var loaded=store.load();
        assertEquals(d.pandas.getFirst().uuid,loaded.pandas.getFirst().uuid);assertEquals(d.pandas.getFirst().belief(),loaded.pandas.getFirst().belief());
        loaded.overlays=false;store.save(loaded);assertFalse(store.load().overlays);assertTrue(Files.exists(Path.of(store.file+".bak")));
    }
    @Test void corruptFileNeverOverwrittenByLoad() throws Exception {
        var store=new PandaStore(root,"server");Files.writeString(store.file,"{broken");assertThrows(java.io.IOException.class,store::load);assertEquals("{broken",Files.readString(store.file));
    }
    @Test void isolationAndCounterValidation() throws Exception {
        assertNotEquals(new PandaStore(root,"server/a").file,new PandaStore(root,"server/b").file);
        var d=sample();d.nextId=1;assertThrows(IllegalArgumentException.class,()->new PandaStore(root,"a").save(d));
    }
    @Test void uncertainGenotypesPersist() throws Exception {
        var d=sample();var p=d.pandas.getFirst();p.main=p.hidden=null;p.uncertainty=Genetics.wildPrior(null).values().stream().mapToDouble(Double::doubleValue).toArray();
        var store=new PandaStore(root,"unknown");store.save(d);assertEquals(p.belief(),store.load().pandas.getFirst().belief());
    }
}
