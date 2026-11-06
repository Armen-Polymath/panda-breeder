package dev.pandabreeder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;

public final class PandaStore {
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    public final Path file;
    public PandaStore(Path root,String identity) {
        try { file=root.resolve(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(identity.getBytes(StandardCharsets.UTF_8)))+".json"); }
        catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
    public CollectionData load() throws IOException {
        if(!Files.exists(file)) return new CollectionData();
        try { var data=GSON.fromJson(Files.readString(file),CollectionData.class); validate(data); return data; }
        catch(RuntimeException e){throw new IOException("Invalid panda collection; original preserved: "+file,e);}
    }
    public void save(CollectionData data) throws IOException {
        validate(data); Files.createDirectories(file.getParent());
        Path temp=Files.createTempFile(file.getParent(),"pandas-",".tmp");
        try {
            Files.writeString(temp,GSON.toJson(data),StandardCharsets.UTF_8);
            if(Files.exists(file)) Files.copy(file,file.resolveSibling(file.getFileName()+".bak"),StandardCopyOption.REPLACE_EXISTING);
            try {Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException e){Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING);}
        } finally {Files.deleteIfExists(temp);}
    }
    static void validate(CollectionData data) {
        if(data==null||data.schema!=1||data.nextId<1||data.pandas==null)throw new IllegalArgumentException("Unsupported collection schema");
        Set<UUID> uuids=new HashSet<>();Set<String> ids=new HashSet<>();int max=0;
        for(var p:data.pandas) {
            if(p==null||p.uuid==null||!uuids.add(p.uuid)||p.id==null||!p.id.matches("P[0-9]+")||!ids.add(p.id)||p.children==null||p.children.contains(null)) throw new IllegalArgumentException("Invalid or duplicate panda");
            int numeric=Integer.parseInt(p.id.substring(1));if(numeric<1)throw new IllegalArgumentException("Invalid ID");max=Math.max(max,numeric);
            if((p.main==null)!=(p.hidden==null)||!Double.isFinite(p.x)||!Double.isFinite(p.y)||!Double.isFinite(p.z)||p.dimension==null) throw new IllegalArgumentException("Invalid panda fields");
            if(p.uuid.equals(p.parentA)||p.uuid.equals(p.parentB)||(p.parentA!=null&&p.parentA.equals(p.parentB)))throw new IllegalArgumentException("Invalid parents");
            Genetics.validate(p.belief());
        }
        if(data.nextId<=max)throw new IllegalArgumentException("Identifier counter would reuse a panda ID");
        for(var p:data.pandas)checkCycle(data,p,new HashSet<>(),new HashSet<>());
    }
    private static void checkCycle(CollectionData data,CollectionData.Panda p,Set<UUID> path,Set<UUID> done) {
        if(p==null||done.contains(p.uuid))return;
        if(!path.add(p.uuid))throw new IllegalArgumentException("Cyclic pedigree");
        if(p.parentA!=null)checkCycle(data,data.find(p.parentA),path,done);
        if(p.parentB!=null)checkCycle(data,data.find(p.parentB),path,done);
        path.remove(p.uuid);done.add(p.uuid);
    }
}
