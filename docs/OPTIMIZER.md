# Breeding optimizer model

The objective is the expected number of future breeding attempts until the **next brown offspring**. Existing brown pandas can breed; the objective remains producing another brown panda.

Only the number of brown alleles (0, 1, 2) matters for this objective. Other genes have the same probability of mutating to brown. A parent with k copies transmits brown with probability `q(k) = 31k/64 + 1/256`. A pair's offspring-copy probabilities are `(1-q(a))(1-q(b))`, `q(a)(1-q(b)) + q(b)(1-q(a))`, and `q(a)q(b)`.

Population states contain counts of each copy category, capped at two. A third identical breeder adds no benefit when cooldown, survival risk, capacity, and elapsed time are excluded. Self-pairing is forbidden. A newborn with zero or one copies is retained; a two-copy offspring ends the process. Retaining offspring is free and cannot worsen the optimal value.

Every non-self transition increases a capped count. The implementation solves these states recursively, eliminates self-transitions algebraically, and minimizes over legal pairs:

`V(s) = min_action [1 + sum_{t != s, nonterminal} P(t | s, action) V(t)] / [1 - P(s | s, action)]`.

There are at most 27 abstract states. At most two representatives of each class are evaluated to choose a concrete pair. This is an exact stochastic shortest-path solution **within the stated model**, not a claim about optimal elapsed gameplay time. It includes mutation, future carriers, and the current known adult population. Compared with repeating a fixed pair (`1 / immediateBrownProbability`), adaptive breeding can require substantially fewer attempts.

Assumptions: vanilla mechanics, independent RNG, offspring survive and can eventually mature, unlimited space/bamboo, no travel or cooldown costs, breeders remain available. Loaded adults with synchronized genes enter recommendations. Babies enter when observed mature; missing and observed-dead pandas are excluded. Uncertain records can be used in the calculator but are excluded from this optimizer; it does not solve partially observed pedigree planning.

The client recomputes after registration, removal, relevant gene/age changes, or loaded-population changes. One scan of loaded entities per second refreshes records. Rendering does not run the search.
