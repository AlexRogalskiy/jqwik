package net.jqwik.time.internal.properties.arbitraries.valueRanges;

import java.time.*;

public class LocalDateBetween extends ValueRange<LocalDate> {

	@Override
	protected void exceptionCheck(Parameter parameter) {
		if (parameter.getMin() != null && parameter.getMin().getYear() <= 0) {
			throw new IllegalArgumentException("Minimum year in a date must be > 0");
		}
		if (parameter.getMax() != null && parameter.getMax().getYear() <= 0) {
			throw new IllegalArgumentException("Minimum year in a date must be > 0");
		}
		if (parameter.getMin() != null && parameter.getMax() != null && parameter.getMin().isAfter(parameter.getMax())) {
			throw new IllegalArgumentException("Minimum date must not be after maximum date");
		}
	}

	public void setYearBetween(YearBetween yearBetween) {
		LocalDate min = LocalDate.of(yearBetween.getMin().getValue(), 1, 1);
		LocalDate max = LocalDate.of(yearBetween.getMax().getValue(), 12, 31);
		set(min, max);
	}

}