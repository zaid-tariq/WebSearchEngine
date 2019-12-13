package com.example.main.backend.pagerank;

import org.la4j.Vector;
import org.la4j.matrix.SparseMatrix;
import org.la4j.vector.DenseVector;

public class PageRank {

	private DenseVector stationaryDistribution;
	private SparseMatrix transitionMatrix;
	private double terminationCriteria;
	private double randomJumpProbability;

	private PageRank(Builder builder) {
		this.randomJumpProbability = builder.randomJumpProbability;
		this.terminationCriteria = builder.terminationCriteria;
		this.transitionMatrix = builder.transitionMatrix;
		this.stationaryDistribution = computeWithPowerIterationMethod(this.transitionMatrix, randomJumpProbability,
				terminationCriteria);
	}

	public DenseVector getStationaryDistribution() {
		return stationaryDistribution;
	}

	public SparseMatrix getTransitionMatrix() {
		return transitionMatrix;
	}

	public double getTerminationCriteria() {
		return terminationCriteria;
	}

	public double getRandomJumpProbability() {
		return randomJumpProbability;
	}

	private DenseVector computeWithPowerIterationMethod(SparseMatrix transitionMatrix, double randomJumpProbability,
			double terminationCriteria) {
		SparseMatrix p = (SparseMatrix) transitionMatrix.multiply(1 - randomJumpProbability).add(SparseMatrix
				.constant(transitionMatrix.rows(), transitionMatrix.columns(), ((double) 1) / transitionMatrix.rows())
				.multiply(transitionMatrix));
		DenseVector v = DenseVector.constant(p.rows(), ((double) 1) / p.rows());
		DenseVector b = DenseVector.constant(p.rows(), Double.POSITIVE_INFINITY);
		Vector a = null;
		
		while (v.subtract(b).sum() >= terminationCriteria) {
			b = v;
			v = (DenseVector) v.multiply(transitionMatrix);
		}
		return v;
	}

	public static final class Builder {

		private SparseMatrix transitionMatrix;
		private double randomJumpProbability = -1;
		private double terminationCriteria = -1;

		public Builder() {

		}

		public Builder withTransitionMatrix(SparseMatrix m) {
			this.transitionMatrix = m;
			return this;
		}

		public Builder withRandomJumpProability(double p) {
			this.randomJumpProbability = p;
			return this;
		}

		public Builder withTerminationCriteria(double value) {
			this.terminationCriteria = value;
			return this;
		}

		public PageRank build() {
			if (this.transitionMatrix == null) {
				throw new IllegalArgumentException("Transition matrix not set");
			}
			if (this.randomJumpProbability == -1) {
				throw new IllegalArgumentException("Random jump probability not set");
			}
			if (this.terminationCriteria == -1) {
				throw new IllegalArgumentException("Termination criteria not set");
			}
			return new PageRank(this);
		}
	}
}