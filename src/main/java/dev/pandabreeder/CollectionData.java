package dev.pandabreeder;

import java.util.*;

public final class CollectionData {
    public int schema=1, nextId=1;
    public boolean overlays=true, autoBabies=false;
    public List<Panda> pandas=new ArrayList<>();
    public static final class Panda {
        public UUID uuid;
        public String id, dimension, personality;
        public Genetics.Gene main, hidden;
        public double[] uncertainty;
        public String evidence="Synchronized";
        public double x,y,z;
        public boolean baby, dead;
        public long lastSeen;
        public UUID parentA,parentB;
        public List<UUID> children=new ArrayList<>();
        public Genetics.Genotype genotype() { return main==null||hidden==null?null:new Genetics.Genotype(main,hidden); }
        public Map<Genetics.Genotype,Double> belief() {
            if(genotype()!=null) return Genetics.known(main,hidden);
            if(uncertainty==null) return Genetics.wildPrior(null);
            if(uncertainty.length!=49) throw new IllegalArgumentException("Invalid belief length");
            var result=new LinkedHashMap<Genetics.Genotype,Double>(); int i=0;
            for(var a:Genetics.Gene.values()) for(var b:Genetics.Gene.values()) result.put(new Genetics.Genotype(a,b),uncertainty[i++]);
            Genetics.validate(result); return result;
        }
    }
    public Panda find(UUID uuid) { return pandas.stream().filter(p->p.uuid.equals(uuid)).findFirst().orElse(null); }
    public void parentage(Panda child,Panda a,Panda b) {
        if(child==null||a==null||b==null||a==b||a==child||b==child) throw new IllegalArgumentException("Choose three different pandas");
        if(isAncestor(child.uuid,a,new HashSet<>())||isAncestor(child.uuid,b,new HashSet<>())) throw new IllegalArgumentException("Parentage would form a cycle");
        for(var p:pandas) p.children.remove(child.uuid);
        child.parentA=a.uuid; child.parentB=b.uuid;
        a.children.add(child.uuid); b.children.add(child.uuid);
        if(child.genotype()==null) {
            var distribution=Genetics.offspring(a.belief(),b.belief());
            child.uncertainty=distribution.values().stream().mapToDouble(Double::doubleValue).toArray();
            child.evidence="Inferred from manually assigned parents";
        }
    }
    private boolean isAncestor(UUID ancestor,Panda p,Set<UUID> visited) {
        if(p==null||!visited.add(p.uuid)) return false;
        return ancestor.equals(p.parentA)||ancestor.equals(p.parentB)||isAncestor(ancestor,findNullable(p.parentA),visited)||isAncestor(ancestor,findNullable(p.parentB),visited);
    }
    private Panda findNullable(UUID id) { return id==null?null:find(id); }
    public void remove(Panda p) {
        pandas.remove(p);
        // UUID references remain as historical parentage, even after unregistering a panda.
    }
}
