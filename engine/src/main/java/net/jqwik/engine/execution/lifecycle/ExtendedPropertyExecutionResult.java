package net.jqwik.engine.execution.lifecycle;

import net.jqwik.api.*;
import net.jqwik.api.lifecycle.*;
import net.jqwik.engine.execution.*;

public interface ExtendedPropertyExecutionResult extends PropertyExecutionResult {

	boolean isExtended();

	default String randomSeed() {
		return generationInfo().randomSeed().orElse(null);
	}

	GenerationInfo generationInfo();

	GenerationMode generation();

	EdgeCasesExecutionResult edgeCases();

	class EdgeCasesExecutionResult {

		private final EdgeCasesMode mode;
		private final int total;
		private final int tried;

		public EdgeCasesExecutionResult(EdgeCasesMode mode, int total, int tried) {
			this.mode = mode;
			this.total = total;
			this.tried = tried;
		}

		public EdgeCasesMode mode() {
			return mode;
		}

		public int total() {
			return total;
		}

		public int tried() {
			return tried;
		}
	}

}
