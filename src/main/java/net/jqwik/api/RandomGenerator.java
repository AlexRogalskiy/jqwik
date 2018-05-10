package net.jqwik.api;

import net.jqwik.properties.arbitraries.*;

import java.util.*;
import java.util.function.*;

public interface RandomGenerator<T> {

	/**
	 * @param random the source of randomness. Injected by jqwik itself.
	 *
	 * @return the next generated value wrapped within the Shrinkable interface. The method must ALWAYS return a next value.
	 */
	Shrinkable<T> next(Random random);

	default <U> RandomGenerator<U> map(Function<T, U> mapper) {
		return random -> RandomGenerator.this.next(random).map(mapper);
	}

	default <U> RandomGenerator<U> flatMap(Function<T, Arbitrary<U>> mapper, int tries) {
		return random -> {
			Shrinkable<T> wrappedShrinkable = this.next(random);
			return new FlatMappedShrinkable<>(wrappedShrinkable, mapper, tries, random.nextLong());
		};
	}

	default RandomGenerator<T> filter(Predicate<T> filterPredicate) {
		return new FilteredGenerator<>(this, filterPredicate);
	}

	default RandomGenerator<T> injectNull(double nullProbability) {
		return random -> {
			if (random.nextDouble() <= nullProbability) return Shrinkable.unshrinkable(null);
			return RandomGenerator.this.next(random);
		};
	}

	default RandomGenerator<T> withEdgeCases(int genSize, List<Shrinkable<T>> samples) {
		if (samples.isEmpty()) {
			return this;
		}

		// TODO: Calculate a smoothly changing ratio
		int baseToEdgeCaseRatio = genSize <= 20 ? 2 //
			: genSize <= 50 ? 5 //
			: genSize <= 1000 ? 10 : 20;

		RandomGenerator<T> samplesGenerator = RandomGenerators.samplesFromShrinkables(samples);
		RandomGenerator<T> baseGenerator = this;
		return random -> {
			if (random.nextInt(baseToEdgeCaseRatio) == 0) {
				return samplesGenerator.next(random);
			} else {
				return baseGenerator.next(random);
			}
		};
	}

	default RandomGenerator<T> withSamples(T... samples) {
		return new WithSamplesGenerator<>(samples, this);
	}

}
