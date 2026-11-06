package dev.pandabreeder;

import java.util.*;

/** Exact stochastic shortest path on capped brown-copy counts, ignoring growth/cooldowns. */
public final class Optimizer {
    public record Panda(String id, Genetics.Genotype genes) {}
    public record Plan(String first, String second, double brownProbability, double expectedAttempts) {}
    private record Population(int zero, int one, int two) {
        int count(int k) { return k==0?zero:k==1?one:two; }
        Population add(int k) { return new Population(k==0?Math.min(2,zero+1):zero,k==1?Math.min(2,one+1):one,two); }
    }
    private final Map<Population,Double> memo=new HashMap<>();
    private static double transmission(int k) { return 31.0/32*k/2+1.0/256; }
    private double action(Population s,int a,int b) {
        double x=transmission(a),y=transmission(b);
        double[] p={(1-x)*(1-y),x*(1-y)+y*(1-x)};
        double cost=1, self=0;
        for(int k=0;k<2;k++) {
            var next=s.add(k);
            if(next.equals(s)) self+=p[k]; else cost+=p[k]*value(next);
        }
        return cost/(1-self);
    }
    private double value(Population s) {
        if(memo.containsKey(s)) return memo.get(s);
        double best=Double.POSITIVE_INFINITY;
        for(int a=0;a<3;a++) for(int b=a;b<3;b++)
            if(s.count(a)>0 && s.count(b)>(a==b?1:0)) best=Math.min(best,action(s,a,b));
        memo.put(s,best); return best;
    }
    public Plan recommend(List<Panda> pandas) {
        if(pandas.size()<2) return null;
        int[] counts=new int[3]; pandas.forEach(p->counts[p.genes.brownCopies()]++);
        Population s=new Population(Math.min(2,counts[0]),Math.min(2,counts[1]),Math.min(2,counts[2]));
        // Individuals with identical brown-copy counts have identical transition probabilities.
        Panda[] representatives=new Panda[6]; int[] filled=new int[3];
        for(var p:pandas) { int k=p.genes.brownCopies(); if(filled[k]<2) representatives[k*2+filled[k]++]=p; }
        Plan best=null;
        for(int i=0;i<6;i++) for(int j=i+1;j<6;j++) {
            var a=representatives[i]; var b=representatives[j]; if(a==null||b==null) continue;
            int x=a.genes.brownCopies(),y=b.genes.brownCopies(); double cost=action(s,x,y);
            if(best==null || cost<best.expectedAttempts-1e-10) best=new Plan(a.id,b.id,transmission(x)*transmission(y),cost);
        }
        return best;
    }
}
