package net.jqwik.engine.properties.shrinking;

import java.util.*;
import java.util.function.*;

import org.assertj.core.api.*;

import net.jqwik.api.*;
import net.jqwik.api.lifecycle.*;
import net.jqwik.engine.*;
import net.jqwik.engine.properties.*;

import static net.jqwik.api.ShrinkingTestHelper.*;
import static net.jqwik.api.Tuple.*;

@PropertyDefaults(tries = 20, afterFailure = AfterFailureMode.RANDOM_SEED)
class LazyOfArbitraryShrinkingTests {

	@Property
	void shrinkToOtherSuppliers(@ForAll Random random) {
		Arbitrary<Integer> arbitrary =
			Arbitraries.lazyOf(
				() -> Arbitraries.integers().between(1, 10),
				() -> Arbitraries.integers().between(1, 20).filter(i -> i > 10),
				() -> Arbitraries.integers().between(1, 30).filter(i -> i > 20),
				() -> Arbitraries.integers().between(1, 40).filter(i -> i > 30)
			);
		Integer value = shrinkToMinimal(arbitrary, random);
		Assertions.assertThat(value).isEqualTo(1);
	}

	@Property
	void oneStep(@ForAll Random random) {
		Arbitrary<Integer> arbitrary =
			Arbitraries.lazyOf(Arbitraries::integers);
		Integer value = shrinkToMinimal(arbitrary, random);
		Assertions.assertThat(value).isEqualTo(0);
	}

	@Disabled
	@Property(seed = "-4759270301851429536")
	void severalStepsToList(@ForAll Random random) {
		Arbitrary<List<Integer>> arbitrary = listOfInteger();
		TestingFalsifier<List<Integer>> falsifier = integers -> integers.size() < 2;
		List<Integer> shrunkValue = falsifyThenShrink(arbitrary, random, falsifier);

		Assertions.assertThat(shrunkValue).isEqualTo(Arrays.asList(1, 1));
	}

	@Provide
	Arbitrary<List<Integer>> listOfInteger() {
		return Arbitraries.lazyOf(
			() -> Arbitraries.integers().between(1, 5).list().ofSize(1),
			() -> Combinators.combine(listOfInteger(), listOfInteger()).as((l1, l2) -> {
				ArrayList<Integer> newList = new ArrayList<>(l1);
				newList.addAll(l2);
				return newList;
			})
		);
	}

	@Disabled
	@Property(seed = "-1585665271736274201")
	void severalStepsToList_withReversedOrderOfSuppliers(@ForAll Random random) {
		Arbitrary<List<Integer>> arbitrary = listOfIntegerReversedLazy();
		TestingFalsifier<List<Integer>> falsifier = integers -> integers.size() < 2;
		List<Integer> shrunkValue = falsifyThenShrink(arbitrary, random, falsifier);

		Assertions.assertThat(shrunkValue).isEqualTo(Arrays.asList(1, 1));
	}

	@Provide
	Arbitrary<List<Integer>> listOfIntegerReversedLazy() {
		return Arbitraries.lazyOf(
			() -> Combinators.combine(listOfIntegerReversedLazy(), listOfIntegerReversedLazy()).as((l1, l2) -> {
				ArrayList<Integer> newList = new ArrayList<>(l1);
				newList.addAll(l2);
				return newList;
			}),
			() -> Arbitraries.integers().between(1, 5).list().ofSize(1)
		);
	}

	@Property
	void withDuplicateSuppliers(@ForAll Random random) {
		Arbitrary<List<Integer>> arbitrary = listOfIntegerWithDuplicateSuppliers();
		List<Integer> shrunkValue = falsifyThenShrink(arbitrary, random, alwaysFalsify());;
		Assertions.assertThat(shrunkValue).isEqualTo(Collections.emptyList());
	}

	@Provide
	Arbitrary<List<Integer>> listOfIntegerWithDuplicateSuppliers() {
		return Arbitraries.lazyOf(
			() -> Arbitraries.just(new ArrayList<>()),
			() -> Arbitraries.just(new ArrayList<>()),
			() -> Arbitraries.integers().between(1, 5).list().ofSize(1),
			() -> Arbitraries.integers().between(1, 5).list().ofSize(1),
			() -> Combinators.combine(listOfIntegerWithDuplicateSuppliers(), listOfIntegerWithDuplicateSuppliers()).as((l1, l2) -> {
				ArrayList<Integer> newList = new ArrayList<>(l1);
				newList.addAll(l2);
				return newList;
			}),
			() -> Combinators.combine(listOfIntegerWithDuplicateSuppliers(), listOfIntegerWithDuplicateSuppliers()).as((l1, l2) -> {
				ArrayList<Integer> newList = new ArrayList<>(l1);
				newList.addAll(l2);
				return newList;
			})
		);
	}

	@Group
	class Calculator {

		/**
		 * Not all shrinking attempts reach the shortest possible expression of 5 nodes
		 * Moreover shrinking results are usually small (5 - 10 nodes) but
		 * are sometimes very large (200 nodes and more)
		 */
		@Property(tries = 1000, seed="3404249936767611181") // This seed produces the desired result
		@ExpectFailure(checkResult = ShrinkToSmallExpression.class)
		// @Report(Reporting.FALSIFIED)
		void shrinkExpressionTree(@ForAll("expression") Object expression) {
			Assume.that(divSubterms(expression));
			evaluate(expression);
		}

		private class ShrinkToSmallExpression implements Consumer<PropertyExecutionResult> {
			@Override
			public void accept(PropertyExecutionResult propertyExecutionResult) {
				List<Object> actual = propertyExecutionResult.falsifiedParameters().get();
				// The best shrinker should shrink to just 5 nodes
				Assertions.assertThat(countNodes(actual.get(0))).isLessThanOrEqualTo(5);
			}

			private int countNodes(Object expression) {
				if (expression instanceof Integer) {
					return 1;
				};
				@SuppressWarnings("rawtypes")
				Tuple3 tupleExpression = (Tuple3) expression;
				return 1 + countNodes(tupleExpression.get2()) + countNodes(tupleExpression.get3());
			}
		}

		private boolean divSubterms(final Object expression) {
			if (expression instanceof Integer) {
				return true;
			}
			@SuppressWarnings("rawtypes")
			Tuple3 tupleExpression = (Tuple3) expression;
			if (tupleExpression.get1().equals("/") && tupleExpression.get3().equals(0)) {
				return false;
			}
			return divSubterms(tupleExpression.get2()) && divSubterms(tupleExpression.get3());
		}

		@Provide
		Arbitrary<Object> expression() {
			return Arbitraries.lazyOf(
				Arbitraries::integers,
				Arbitraries::integers,
				Arbitraries::integers,
				() -> Combinators.combine(getLazy(), getLazy())
									   .as((e1, e2) -> of("+", e1, e2)),
				() -> Combinators.combine(getLazy(), getLazy())
									   .as((e1, e2) -> of("/", e1, e2))
			);
		}

		private Arbitrary<Object> getLazy() {
			return Arbitraries.lazy(this::expression);
		}

		int evaluate(Object expression) {
			if (expression instanceof Integer) {
				return (int) expression;
			}
			@SuppressWarnings("rawtypes")
			Tuple3 tupleExpression = (Tuple3) expression;
			if (tupleExpression.get1().equals("+")) {
				return evaluate(tupleExpression.get2()) + evaluate(tupleExpression.get3());
			}
			if (tupleExpression.get1().equals("/")) {
				return evaluate(tupleExpression.get2()) / evaluate(tupleExpression.get3());
			}
			throw new IllegalArgumentException(String.format("%s is not a valid expression", expression));
		}

	}

}
