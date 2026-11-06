# Vanilla 1.21.11 verification

Verified against the local Loom-decompiled Minecraft 1.21.11 source using Yarn 1.21.11+build.3 (not a wiki approximation). Copyrighted game sources are not redistributed.

`PandaEntity` registers `MAIN_GENE` and `HIDDEN_GENE` as tracked bytes. `initDataTracker` initializes both to normal; `getMainGene` and `getHiddenGene` read them. Entity metadata synchronization sends non-default tracked values at spawn and dirty values on changes. A default normal value is valid, not evidence of a missing gene. Both genes are available on an ordinary vanilla client; no custom packet, server mod, or genetics accessor Mixin is necessary.

`initGenes(mother, father)` chooses parent order with a fair boolean, then chooses one of each parent's two genes with another fair boolean. Each offspring locus independently mutates with probability 1/32. Mutation replaces the gene using `Gene.createRandom`, even if the replacement equals the inherited gene. Its 16 equiprobable outcomes are lazy (0), worried (1), playful (2), aggressive (4), weak (3,5,6,7,8), brown (9,10), and normal (11–15).

Brown and weak are recessive: a recessive main gene expresses only if the hidden gene matches; otherwise the phenotype is normal. Other main genes express regardless of the hidden gene.

The engine enumerates all 49 ordered genotypes. Each parent's transmitted marginal is `31/32 × inherited marginal + 1/32 × random replacement distribution`. The offspring distribution averages the two parent orders. This gives exact probabilities up to floating point rounding.

Sanity cases: brown/brown × brown/brown brown probability = (249/256)^2; two one-copy carriers = (125/256)^2; one carrier and zero-copy parent = 125/65536; two zero-copy parents = 1/65536. `1/p` is the mean attempts for a fixed pair with independent repeated outcomes, not an adaptive-strategy estimate.

Uncertainty inputs are normalized genotype distributions. `wildPrior` assumes independently generated wild genes, optionally conditioned on phenotype; it must not be presented as a measured genotype or as an appropriate prior for arbitrary bred pandas. Independent parent beliefs do not represent pedigree-induced correlations. Standard synchronized observations take precedence. Servers that modify genetics or falsify metadata are outside the exact vanilla model.

Mapping reference: https://maven.fabricmc.net/docs/yarn-1.21.11+build.3/net/minecraft/entity/passive/PandaEntity.html
