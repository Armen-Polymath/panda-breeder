package dev.pandabreeder;

import java.util.*;

/** Exact ordered genotype distribution; independent parental beliefs are explicit inputs. */
public final class Genetics {
    private Genetics() {}
    public enum Gene { NORMAL, LAZY, WORRIED, PLAYFUL, BROWN, WEAK, AGGRESSIVE }
    public record Genotype(Gene main, Gene hidden) {
        public Genotype { Objects.requireNonNull(main); Objects.requireNonNull(hidden); }
        public int brownCopies() { return (main == Gene.BROWN ? 1 : 0) + (hidden == Gene.BROWN ? 1 : 0); }
        public Gene phenotype() { return (main == Gene.BROWN || main == Gene.WEAK) && main != hidden ? Gene.NORMAL : main; }
    }
    public static double randomWeight(Gene gene) {
        return switch (gene) { case NORMAL, WEAK -> 5.0/16; case BROWN -> 2.0/16; default -> 1.0/16; };
    }
    public static Map<Genotype,Double> known(Gene main, Gene hidden) { return Map.of(new Genotype(main, hidden), 1.0); }
    public static Map<Genotype,Double> wildPrior(Gene phenotype) {
        Map<Genotype,Double> map = new LinkedHashMap<>();
        for (Gene a : Gene.values()) for (Gene b : Gene.values()) {
            var g = new Genotype(a,b);
            if (phenotype == null || g.phenotype() == phenotype) map.put(g,randomWeight(a)*randomWeight(b));
        }
        double total = map.values().stream().mapToDouble(Double::doubleValue).sum();
        map.replaceAll((g,p) -> p/total); return Collections.unmodifiableMap(map);
    }
    public static void validate(Map<Genotype,Double> belief) {
        if (belief == null || belief.isEmpty()) throw new IllegalArgumentException("Empty genetic belief");
        double sum=0;
        for (var e : belief.entrySet()) {
            Objects.requireNonNull(e.getKey());
            if (e.getValue()==null || !Double.isFinite(e.getValue()) || e.getValue()<0) throw new IllegalArgumentException("Invalid probability");
            sum+=e.getValue();
        }
        if (Math.abs(sum-1)>1e-9) throw new IllegalArgumentException("Genetic probabilities must sum to one");
    }
    private static double[] gametes(Map<Genotype,Double> belief) {
        validate(belief); double[] p=new double[7];
        belief.forEach((g,w) -> { p[g.main.ordinal()]+=w/2; p[g.hidden.ordinal()]+=w/2; });
        for (Gene g : Gene.values()) p[g.ordinal()]=p[g.ordinal()]*31/32+randomWeight(g)/32;
        return p;
    }
    public static Map<Genotype,Double> offspring(Map<Genotype,Double> a, Map<Genotype,Double> b) {
        double[] x=gametes(a), y=gametes(b); Map<Genotype,Double> result=new LinkedHashMap<>();
        for (Gene m : Gene.values()) for (Gene h : Gene.values()) result.put(new Genotype(m,h),
            (x[m.ordinal()]*y[h.ordinal()]+y[m.ordinal()]*x[h.ordinal()])/2);
        return Collections.unmodifiableMap(result);
    }
    public static double copies(Map<Genotype,Double> belief, int count) {
        return belief.entrySet().stream().filter(e -> e.getKey().brownCopies()==count).mapToDouble(Map.Entry::getValue).sum();
    }
    public static double expectedAttempts(Map<Genotype,Double> offspring) { return 1/copies(offspring,2); }
}
