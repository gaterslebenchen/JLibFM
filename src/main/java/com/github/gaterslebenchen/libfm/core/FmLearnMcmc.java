/*
 * JLibFM
 *
 * Copyright (c) 2017, Jinbo Chen(gaterslebenchen@gmail.com)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the docume
 *    ntation and/or other materials provided with the distribution.
 *  - Neither the name of the <ORGANIZATION> nor the names of its contributors may be used to endorse or promote products derived from this
 *    software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUD
 * ING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN N
 * O EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR C
 * ONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR P
 * ROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 *  TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBI
 *  LITY OF SUCH DAMAGE.
 */
package com.github.gaterslebenchen.libfm.core;

import java.util.Arrays;
import java.util.Properties;

import com.github.gaterslebenchen.libfm.data.DataPointMatrix;
import com.github.gaterslebenchen.libfm.data.DataProvider;
import com.github.gaterslebenchen.libfm.data.EqtermMatrix;
import com.github.gaterslebenchen.libfm.data.SparseEntry;
import com.github.gaterslebenchen.libfm.data.SparseRow;
import com.github.gaterslebenchen.libfm.tools.Debug;
import com.github.gaterslebenchen.libfm.tools.JlibfmRuntimeException;
import com.github.gaterslebenchen.libfm.tools.TaskType;
import com.github.gaterslebenchen.libfm.tools.Util;

public class FmLearnMcmc extends FmLearn {
	public int num_iter;
	public int num_eval_cases;

	public double alpha_0, gamma_0, beta_0, mu_0;
	public double alpha;

	public double w0_mean_0;

	public double[] w_mu, w_lambda;

	public DataPointMatrix v_mu, v_lambda;

	public boolean do_sample; // switch between choosing expected values and
								// drawing from distribution
	public boolean do_multilevel; // use the two-level (hierarchical) model
									// (TRUE) or the one-level (FALSE)
	public int nan_cntr_v, nan_cntr_w, nan_cntr_w0, nan_cntr_alpha, nan_cntr_w_mu, nan_cntr_w_lambda, nan_cntr_v_mu,
			nan_cntr_v_lambda;
	public int inf_cntr_v, inf_cntr_w, inf_cntr_w0, inf_cntr_alpha, inf_cntr_w_mu, inf_cntr_w_lambda, inf_cntr_v_mu,
			inf_cntr_v_lambda;

	double[] cache_for_group_values;

	double[] pred_sum_all;
	double[] pred_sum_all_but5;
	double[] pred_this;

	EqTerm[] cache;
	EqTerm[] cache_test;

	SparseRow empty_data_row; // this is a dummy row for attributes that do not
								// exist in the training data (but in test data)

	public double evaluate(DataProvider data) {
		return Double.NaN;
	}

	public double predict_case(DataProvider data) {
		throw new JlibfmRuntimeException("not supported for MCMC and ALS");
	}

	protected void mcmclearn(DataProvider train, DataProvider test) {
	};

	/**
	 * This function predicts all datasets mentioned in main_data. It stores the
	 * prediction in the e-term.
	 */
	public void predict_data_and_write_to_eterms(DataProvider[] main_data, EqtermMatrix main_cache) throws Exception {
		assert (main_data.length == main_cache.getRow());
		if (main_data.length == 0) {
			return;
		}

		// do this using only the transpose copy of the training data:
		for (int ds = 0; ds < main_cache.getRow(); ds++) {
			DataProvider m_data = main_data[ds];
			EqTerm[] m_cache = main_cache.get(ds);
			for (int i = 0; i < m_data.getRownumber(); i++) {
				m_cache[i].e = 0;
				m_cache[i].q = 0;
			}
		}

		// (1) do the 1/2 sum_f (sum_i v_if x_i)^2 and store it in the e/y-term
		for (int f = 0; f < fm.num_factor; f++) {
			double[] v = fm.v.getArray(f);

			// calculate cache[i].q = sum_i v_if x_i (== q_f-term)
			// Complexity: O(N_z(X^M))
			for (int ds = 0; ds < main_cache.getRow(); ds++) {
				EqTerm[] m_cache = main_cache.get(ds);
				DataProvider m_data = main_data[ds];
				m_data.getTransposedata().begin();
				int row_index;
				SparseRow feature_data;
				for (int i = 0; i < m_data.getTransposedata().getNumRows(); i++) {

					row_index = m_data.getTransposedata().getRowIndex();
					feature_data = m_data.getTransposedata().getRow();
					m_data.getTransposedata().next();

					double v_if = v[row_index];

					for (int i_fd = 0; i_fd < feature_data.getSize(); i_fd++) {
						int train_case_index = feature_data.getData()[i_fd].getId();
						double x_li = feature_data.getData()[i_fd].getValue();
						m_cache[train_case_index].q += v_if * x_li;
					}
				}
			}

			// add 0.5*q^2 to e and set q to zero.
			// O(n*|B|)
			for (int ds = 0; ds < main_cache.getRow(); ds++) {
				EqTerm[] m_cache = main_cache.get(ds);
				DataProvider m_data = main_data[ds];
				for (int c = 0; c < m_data.getRownumber(); c++) {
					double q_all = m_cache[c].q;
					m_cache[c].e += 0.5 * q_all * q_all;
					m_cache[c].q = 0.0;
				}
			}

		}

		// (2) do -1/2 sum_f (sum_i v_if^2 x_i^2) and store it in the q-term
		for (int f = 0; f < fm.num_factor; f++) {
			double[] v = fm.v.getValue()[f];

			// sum up the q^S_f terms in the main-q-cache: 0.5*sum_i (v_if
			// x_i)^2 (== q^S_f-term)
			// Complexity: O(N_z(X^M))
			for (int ds = 0; ds < main_cache.getRow(); ds++) {
				EqTerm[] m_cache = main_cache.get(ds);
				DataProvider m_data = main_data[ds];

				m_data.getTransposedata().begin();
				int row_index;
				SparseRow feature_data;
				for (int i = 0; i < m_data.getTransposedata().getNumRows(); i++) {

					row_index = m_data.getTransposedata().getRowIndex();
					feature_data = m_data.getTransposedata().getRow();
					m_data.getTransposedata().next();

					double v_if = v[row_index];

					for (int i_fd = 0; i_fd < feature_data.getSize(); i_fd++) {
						int train_case_index = feature_data.getData()[i_fd].getId();
						double x_li = feature_data.getData()[i_fd].getValue();
						m_cache[train_case_index].q -= 0.5 * v_if * v_if * x_li * x_li;
					}
				}
			}

		}

		// (3) add the w's to the q-term
		if (fm.k1) {
			for (int ds = 0; ds < main_cache.getRow(); ds++) {
				EqTerm[] m_cache = main_cache.get(ds);
				DataProvider m_data = main_data[ds];

				m_data.getTransposedata().begin();
				int row_index;
				SparseRow feature_data;
				for (int i = 0; i < m_data.getTransposedata().getNumRows(); i++) {

					row_index = m_data.getTransposedata().getRowIndex();
					feature_data = m_data.getTransposedata().getRow();
					m_data.getTransposedata().next();

					double w_i = fm.w[row_index];

					for (int i_fd = 0; i_fd < feature_data.getSize(); i_fd++) {
						int train_case_index = feature_data.getData()[i_fd].getId();
						double x_li = feature_data.getData()[i_fd].getValue();
						m_cache[train_case_index].q += w_i * x_li;
					}
				}
			}
		}
		// (3) merge both for getting the prediction: w0+e(c)+q(c)
		for (int ds = 0; ds < main_cache.getRow(); ds++) {
			EqTerm[] m_cache = main_cache.get(ds);
			DataProvider m_data = main_data[ds];

			for (int c = 0; c < m_data.getRownumber(); c++) {
				double q_all = m_cache[c].q;
				m_cache[c].e = m_cache[c].e + q_all;
				if (fm.k0) {
					m_cache[c].e += fm.w0;
				}

				m_cache[c].q = 0.0;
			}
		}

	}

	public void predict(DataProvider data, double[] out) {
		assert (data.getRownumber() == out.length);
		if (do_sample) {
			assert (data.getRownumber() == pred_sum_all.length);
			for (int i = 0; i < out.length; i++) {
				out[i] = pred_sum_all[i] / num_iter;
			}
		} else {
			assert (data.getRownumber() == pred_this.length);
			for (int i = 0; i < out.length; i++) {
				out[i] = pred_this[i];
			}
		}
		for (int i = 0; i < out.length; i++) {
			if (task == TaskType.TASK_REGRESSION) {
				out[i] = Math.min(max_target, out[i]);
				out[i] = Math.max(min_target, out[i]);
			} else if (task == TaskType.TASK_CLASSIFICATION) {
				out[i] = Math.min(1.0, out[i]);
				out[i] = Math.max(0.0, out[i]);
			} else {
				throw new JlibfmRuntimeException("task not supported" + task);
			}
		}
	}

	private void add_main_q(DataProvider train, int f) throws Exception {
		// add the q(f)-terms to the main relation q-cache (using only the
		// transpose data)
		double[] v = fm.v.getValue()[f];

		train.getTransposedata().begin();
		int row_index;
		SparseRow feature_data;
		for (int i = 0; i < train.getTransposedata().getNumRows(); i++) {

			row_index = train.getTransposedata().getRowIndex();
			feature_data = train.getTransposedata().getRow();
			train.getTransposedata().next();

			double v_if = v[row_index];
			for (int i_fd = 0; i_fd < feature_data.getSize(); i_fd++) {
				int train_case_index = feature_data.getData()[i_fd].getId();
				double x_li = feature_data.getData()[i_fd].getValue();
				cache[train_case_index].q += v_if * x_li;
			}

		}

	}

	public void draw_all(DataProvider train) throws Exception {
		alpha = draw_alpha(alpha, train.getRownumber());
		if (log != null) {
			log.log("alpha", alpha);
		}

		if (fm.k0) {
			fm.w0 = draw_w0(fm.w0, fm.reg0, train);
		}
		if (fm.k1) {
			draw_w_lambda(fm.w);
			draw_w_mu(fm.w);
			if (log != null) {
				for (int g = 0; g < meta.num_attr_groups; g++) {
					log.log("wmu[" + g + "]", w_mu[g]);
					log.log("wlambda[" + g + "]", w_lambda[g]);
				}
			}

			// draw the w from their posterior
			train.getTransposedata().begin();
			int row_index;
			SparseRow feature_data;
			for (int i = 0; i < train.getTransposedata().getNumRows(); i++) {

				row_index = train.getTransposedata().getRowIndex();
				feature_data = train.getTransposedata().getRow();
				train.getTransposedata().next();

				int g = meta.attr_group[row_index];
				double twvalue = draw_w(fm.w[row_index], w_mu[g], w_lambda[g], feature_data);
				fm.w[row_index] = twvalue;
			}
			// draw w's for which there is no observation in the training data
			for (int i = train.getTransposedata().getNumRows(); i < fm.num_attribute; i++) {
				row_index = i;
				feature_data = empty_data_row;
				int g = meta.attr_group[row_index];
				double twvalue = draw_w(fm.w[row_index], w_mu[g], w_lambda[g], feature_data);
				fm.w[row_index] = twvalue;
			}

		}

		if (fm.num_factor > 0) {
			draw_v_lambda();
			draw_v_mu();
			if (log != null) {
				for (int g = 0; g < meta.num_attr_groups; g++) {
					for (int f = 0; f < fm.num_factor; f++) {
						log.log("vmu[" + g + "," + f + "]", v_mu.get(g, f));
						log.log("vlambda[" + g + "," + f + "]", v_lambda.get(g, f));
					}
				}
			}
		}

		for (int f = 0; f < fm.num_factor; f++) {

			for (int c = 0; c < train.getRownumber(); c++) {
				cache[c].q = 0.0;
			}

			add_main_q(train, f);

			double[] v = fm.v.getValue()[f];

			// draw the thetas from their posterior
			train.getTransposedata().begin();
			int row_index;
			SparseRow feature_data;
			for (int i = 0; i < train.getTransposedata().getNumRows(); i++) {

				row_index = train.getTransposedata().getRowIndex();
				feature_data = train.getTransposedata().getRow();
				train.getTransposedata().next();

				int g = meta.attr_group[row_index];
				double tmpvalue = draw_v(v[row_index], v_mu.get(g, f), v_lambda.get(g, f), feature_data);
				fm.v.set(f, row_index, tmpvalue);
			}
			// draw v's for which there is no observation in the test data
			for (int i = train.getTransposedata().getNumRows(); i < fm.num_attribute; i++) {
				row_index = i;
				feature_data = empty_data_row;
				int g = meta.attr_group[row_index];
				double tmpvalue = draw_v(v[row_index], v_mu.get(g, f), v_lambda.get(g, f), feature_data);
				fm.v.set(f, row_index, tmpvalue);
			}
		}
	}

	private double draw_alpha(double alpha, int num_train_total) {
		if (!do_multilevel) {
			alpha = alpha_0;
			return alpha;
		}
		double alpha_n = alpha_0 + num_train_total;
		double gamma_n = gamma_0;
		for (int i = 0; i < num_train_total; i++) {
			gamma_n += cache[i].e * cache[i].e;
		}
		double alpha_old = alpha;
		alpha = Util.ran_gamma(alpha_n / 2.0, gamma_n / 2.0);

		// check for out of bounds values
		if (Double.isNaN(alpha)) {
			nan_cntr_alpha++;
			alpha = alpha_old;
			assert (!Double.isNaN(alpha_old));
			assert (!Double.isNaN(alpha));
			return alpha;
		}

		if (Double.isInfinite(alpha)) {
			inf_cntr_alpha++;
			alpha = alpha_old;
			assert (!Double.isInfinite(alpha_old));
			assert (!Double.isInfinite(alpha));
			return alpha;
		}
		return alpha;
	}

	// Find the optimal value for the global bias (0-way interaction)
	private double draw_w0(double w0, double reg, DataProvider train) {
		// h = 1
		// h^2 = 1
		// \sum e*h = \sum e
		// \sum h^2 = \sum 1
		double w0_sigma_sqr;
		double w0_mean = 0;
		for (int i = 0; i < train.getRownumber(); i++) {
			w0_mean += (cache[i].e - w0);
		}
		w0_sigma_sqr = 1.0 / (reg + alpha * train.getRownumber());
		w0_mean = -w0_sigma_sqr * (alpha * w0_mean - w0_mean_0 * reg);
		// update w0
		double w0_old = w0;

		if (do_sample) {
			w0 = Util.ran_gaussian(w0_mean, Math.sqrt(w0_sigma_sqr));
		} else {
			w0 = w0_mean;
		}

		// check for out of bounds values
		if (Double.isNaN(w0)) {
			nan_cntr_w0++;
			w0 = w0_old;
			assert (!Double.isNaN(w0_old));
			assert (!Double.isNaN(w0));
			return w0;
		}

		if (Double.isInfinite(w0)) {
			inf_cntr_w0++;
			w0 = w0_old;
			assert (!Double.isInfinite(w0_old));
			assert (!Double.isInfinite(w0));
			return w0;
		}
		// update error
		for (int i = 0; i < train.getRownumber(); i++) {
			cache[i].e -= (w0_old - w0);
		}
		return w0;
	}

	// Find the optimal value for the 1-way interaction w
	private double draw_w(double w, double w_mu, double w_lambda, SparseRow feature_data) {
		double w_sigma_sqr = 0;
		double w_mean = 0;
		for (int i_fd = 0; i_fd < feature_data.getSize(); i_fd++) {
			int train_case_index = feature_data.getData()[i_fd].getId();
			double x_li = feature_data.getData()[i_fd].getValue();
			w_mean += x_li * (cache[train_case_index].e - w * x_li);
			w_sigma_sqr += x_li * x_li;
		}
		w_sigma_sqr = 1.0 / (w_lambda + alpha * w_sigma_sqr);
		w_mean = -w_sigma_sqr * (alpha * w_mean - w_mu * w_lambda);

		// update w:
		double w_old = w;

		if (Double.isNaN(w_sigma_sqr) || Double.isInfinite(w_sigma_sqr)) {
			w = 0.0;
		} else {
			if (do_sample) {
				w = Util.ran_gaussian(w_mean, Math.sqrt(w_sigma_sqr));
			} else {
				w = w_mean;
			}
		}

		// check for out of bounds values
		if (Double.isNaN(w)) {
			nan_cntr_w++;
			w = w_old;
			assert (!Double.isNaN(w_old));
			assert (!Double.isNaN(w));
			return w;
		}
		if (Double.isInfinite(w)) {
			inf_cntr_w++;
			w = w_old;
			assert (!Double.isInfinite(w_old));
			assert (!Double.isInfinite(w));
			return w;
		}
		// update error:
		for (int i_fd = 0; i_fd < feature_data.getSize(); i_fd++) {
			int train_case_index = feature_data.getData()[i_fd].getId();
			double x_li = feature_data.getData()[i_fd].getValue();
			double h = x_li;
			cache[train_case_index].e -= h * (w_old - w);
		}
		return w;
	}

	// Find the optimal value for the 2-way interaction parameter v
	private double draw_v(double v, double v_mu, double v_lambda, SparseRow feature_data) {
		double v_sigma_sqr = 0;
		double v_mean = 0;
		// v_sigma_sqr = \sum h^2 (always)
		// v_mean = \sum h*e (for non_internlock_interactions)
		for (int i_fd = 0; i_fd < feature_data.getSize(); i_fd++) {
			int train_case_index = feature_data.getData()[i_fd].getId();
			double x_li = feature_data.getData()[i_fd].getValue();
			EqTerm cache_li = cache[train_case_index];
			double h = x_li * (cache_li.q - x_li * v);
			v_mean += h * cache_li.e;
			v_sigma_sqr += h * h;
		}
		v_mean -= v * v_sigma_sqr;
		v_sigma_sqr = 1.0 / (v_lambda + alpha * v_sigma_sqr);
		v_mean = -v_sigma_sqr * (alpha * v_mean - v_mu * v_lambda);

		// update v:
		double v_old = v;

		if (Double.isNaN(v_sigma_sqr) || Double.isInfinite(v_sigma_sqr)) {
			v = 0.0;
		} else {
			if (do_sample) {
				v = Util.ran_gaussian(v_mean, Math.sqrt(v_sigma_sqr));
			} else {
				v = v_mean;
			}
		}

		// check for out of bounds values
		if (Double.isNaN(v)) {
			nan_cntr_v++;
			v = v_old;
			assert (!Double.isNaN(v_old));
			assert (!Double.isNaN(v));
			return v;
		}
		if (Double.isInfinite(v)) {
			inf_cntr_v++;
			v = v_old;
			assert (!Double.isInfinite(v_old));
			assert (!Double.isInfinite(v));
			return v;
		}

		// update error and q:
		for (int i_fd = 0; i_fd < feature_data.getSize(); i_fd++) {
			int train_case_index = feature_data.getData()[i_fd].getId();
			double x_li = feature_data.getData()[i_fd].getValue();
			EqTerm cache_li = cache[train_case_index];
			double h = x_li * (cache_li.q - x_li * v_old);
			cache_li.q -= x_li * (v_old - v);
			cache_li.e -= h * (v_old - v);
		}
		return v;
	}

	private void draw_w_mu(double[] w) {
		if (!do_multilevel) {
			Arrays.fill(w_mu, mu_0);
			return;
		}
		double[] w_mu_mean = cache_for_group_values;
		Arrays.fill(w_mu_mean, 0.0);
		for (int i = 0; i < fm.num_attribute; i++) {
			int g = meta.attr_group[i];
			w_mu_mean[g] = w_mu_mean[g] + w[i];
		}
		for (int g = 0; g < meta.num_attr_groups; g++) {
			w_mu_mean[g] = (w_mu_mean[g] + beta_0 * mu_0) / (meta.num_attr_per_group[g] + beta_0);
			double w_mu_sigma_sqr = 1.0 / ((meta.num_attr_per_group[g] + beta_0) * w_lambda[g]);
			double w_mu_old = w_mu[g];
			if (do_sample) {
				w_mu[g] = Util.ran_gaussian(w_mu_mean[g], Math.sqrt(w_mu_sigma_sqr));
			} else {
				w_mu[g] = w_mu_mean[g];
			}

			// check for out of bounds values
			if (Double.isNaN(w_mu[g])) {
				nan_cntr_w_mu++;
				w_mu[g] = w_mu_old;
				assert (!Double.isNaN(w_mu_old));
				assert (!Double.isNaN(w_mu[g]));
				return;
			}
			if (Double.isInfinite(w_mu[g])) {
				inf_cntr_w_mu++;
				w_mu[g] = w_mu_old;
				assert (!Double.isInfinite(w_mu_old));
				assert (!Double.isInfinite(w_mu[g]));
				return;
			}
		}
	}

	private void draw_w_lambda(double[] w) {
		if (!do_multilevel) {
			return;
		}

		double[] w_lambda_gamma = cache_for_group_values;
		for (int g = 0; g < meta.num_attr_groups; g++) {
			w_lambda_gamma[g] = beta_0 * (w_mu[g] - mu_0) * (w_mu[g] - mu_0) + gamma_0;
		}
		for (int i = 0; i < fm.num_attribute; i++) {
			int g = meta.attr_group[i];
			w_lambda_gamma[g] = w_lambda_gamma[g] + (w[i] - w_mu[g]) * (w[i] - w_mu[g]);
		}
		for (int g = 0; g < meta.num_attr_groups; g++) {
			double w_lambda_alpha = alpha_0 + meta.num_attr_per_group[g] + 1;
			double w_lambda_old = w_lambda[g];
			if (do_sample) {
				w_lambda[g] = Util.ran_gamma(w_lambda_alpha / 2.0, w_lambda_gamma[g] / 2.0);
			} else {
				w_lambda[g] = w_lambda_alpha / w_lambda_gamma[g];
			}
			// check for out of bounds values
			if (Double.isNaN(w_lambda[g])) {
				nan_cntr_w_lambda++;
				w_lambda[g] = w_lambda_old;
				assert (!Double.isNaN(w_lambda_old));
				assert (!Double.isNaN(w_lambda[g]));
				return;
			}
			if (Double.isInfinite(w_lambda[g])) {
				inf_cntr_w_lambda++;
				w_lambda[g] = w_lambda_old;
				assert (!Double.isInfinite(w_lambda_old));
				assert (!Double.isInfinite(w_lambda[g]));
				return;
			}
		}
	}

	private void draw_v_mu() {
		if (!do_multilevel) {
			v_mu.init(mu_0);
			return;
		}

		double[] v_mu_mean = cache_for_group_values;
		for (int f = 0; f < fm.num_factor; f++) {
			Arrays.fill(v_mu_mean, 0.0);
			for (int i = 0; i < fm.num_attribute; i++) {
				int g = meta.attr_group[i];
				v_mu_mean[g] = v_mu_mean[g] + fm.v.get(f, i);
			}
			for (int g = 0; g < meta.num_attr_groups; g++) {
				v_mu_mean[g] = (v_mu_mean[g] + beta_0 * mu_0) / (meta.num_attr_per_group[g] + beta_0);
				double v_mu_sigma_sqr = 1.0 / ((meta.num_attr_per_group[g] + beta_0) * v_lambda.get(g, f));
				double v_mu_old = v_mu.get(g, f);
				if (do_sample) {
					v_mu.set(g, f, Util.ran_gaussian(v_mu_mean[g], Math.sqrt(v_mu_sigma_sqr)));
				} else {
					v_mu.set(g, f, v_mu_mean[g]);
				}
				if (Double.isNaN(v_mu.get(g, f))) {
					nan_cntr_v_mu++;
					v_mu.set(g, f, v_mu_old);
					assert (!Double.isNaN(v_mu_old));
					assert (!Double.isNaN(v_mu.get(g, f)));
					return;
				}
				if (Double.isInfinite(v_mu.get(g, f))) {
					inf_cntr_v_mu++;
					v_mu.set(g, f, v_mu_old);
					assert (!Double.isInfinite(v_mu_old));
					assert (!Double.isInfinite(v_mu.get(g, f)));
					return;
				}
			}
		}
	}

	private void draw_v_lambda() {
		if (!do_multilevel) {
			return;
		}

		double[] v_lambda_gamma = cache_for_group_values;
		for (int f = 0; f < fm.num_factor; f++) {
			for (int g = 0; g < meta.num_attr_groups; g++) {
				v_lambda_gamma[g] = beta_0 * (v_mu.get(g, f) - mu_0) * (v_mu.get(g, f) - mu_0) + gamma_0;
			}
			for (int i = 0; i < fm.num_attribute; i++) {
				int g = meta.attr_group[i];
				v_lambda_gamma[g] = v_lambda_gamma[g]
						+ (fm.v.get(f, i) - v_mu.get(g, f)) * (fm.v.get(f, i) - v_mu.get(g, f));
			}
			for (int g = 0; g < meta.num_attr_groups; g++) {
				double v_lambda_alpha = alpha_0 + meta.num_attr_per_group[g] + 1;
				double v_lambda_old = v_lambda.get(g, f);
				if (do_sample) {
					v_lambda.set(g, f, Util.ran_gamma(v_lambda_alpha / 2.0, v_lambda_gamma[g] / 2.0));
				} else {
					v_lambda.set(g, f, v_lambda_alpha / v_lambda_gamma[g]);
				}
				if (Double.isNaN(v_lambda.get(g, f))) {
					nan_cntr_v_lambda++;
					v_lambda.set(g, f, v_lambda_old);
					assert (!Double.isNaN(v_lambda_old));
					assert (!Double.isNaN(v_lambda.get(g, f)));
					return;
				}
				if (Double.isInfinite(v_lambda.get(g, f))) {
					inf_cntr_v_lambda++;
					v_lambda.set(g, f, v_lambda_old);
					assert (!Double.isInfinite(v_lambda_old));
					assert (!Double.isInfinite(v_lambda.get(g, f)));
					return;
				}
			}
		}
	}

	public void init() {
		super.init();

		cache_for_group_values = new double[meta.num_attr_groups];

		empty_data_row = new SparseRow(new SparseEntry[0]);

		alpha_0 = 1.0;
		gamma_0 = 1.0;
		beta_0 = 1.0;
		mu_0 = 0.0;

		alpha = 1;

		w0_mean_0 = 0.0;
		w_mu = new double[meta.num_attr_groups];
		w_lambda = new double[meta.num_attr_groups];
		Arrays.fill(w_mu, 0.0);
		Arrays.fill(w_lambda, 0.0);

		v_mu = new DataPointMatrix(meta.num_attr_groups, fm.num_factor);
		v_lambda = new DataPointMatrix(meta.num_attr_groups, fm.num_factor);
		v_mu.init(0.0);
		v_lambda.init(0.0);

		if (log != null) {
			log.addField("alpha", Double.NaN);
			if (task == TaskType.TASK_REGRESSION) {
				log.addField("rmse_mcmc_this", Double.NaN);
				log.addField("rmse_mcmc_all", Double.NaN);
				log.addField("rmse_mcmc_all_but5", Double.NaN);
			} else if (task == TaskType.TASK_CLASSIFICATION) {
				log.addField("acc_mcmc_this", Double.NaN);
				log.addField("acc_mcmc_all", Double.NaN);
				log.addField("acc_mcmc_all_but5", Double.NaN);
				log.addField("ll_mcmc_this", Double.NaN);
				log.addField("ll_mcmc_all", Double.NaN);
				log.addField("ll_mcmc_all_but5", Double.NaN);
			}

			for (int g = 0; g < meta.num_attr_groups; g++) {
				log.addField("wmu[" + g + "]", Double.NaN);
				log.addField("wlambda[" + g + "]", Double.NaN);
				for (int f = 0; f < fm.num_factor; f++) {
					log.addField("vmu[" + g + "," + f + "]", Double.NaN);
					log.addField("vlambda[" + g + "," + f + "]", Double.NaN);
				}
			}
		}
	}

	public void saveModel(Properties properties) throws Exception {
		throw new JlibfmRuntimeException("not supported for MCMC and ALS");
	}

	public void loadModel(Properties properties) throws Exception {
		throw new JlibfmRuntimeException("not supported for MCMC and ALS");
	}

	public void learn(DataProvider train, DataProvider test) {
		pred_sum_all = new double[test.getRownumber()];
		pred_sum_all_but5 = new double[test.getRownumber()];
		pred_this = new double[test.getRownumber()];
		Arrays.fill(pred_sum_all, 0.0);
		Arrays.fill(pred_sum_all_but5, 0.0);
		Arrays.fill(pred_this, 0.0);

		cache = new EqTerm[train.getRownumber()];
		for (int i = 0; i < cache.length; i++) {
			cache[i] = new EqTerm();
		}
		cache_test = new EqTerm[test.getRownumber()];
		for (int i = 0; i < cache_test.length; i++) {
			cache_test[i] = new EqTerm();
		}

		mcmclearn(train, test);
	}

	public void debug() {
		super.debug();
		Debug.println("do_multilevel=" + do_multilevel);
		Debug.println("do_sampling=" + do_sample);
		Debug.println("num_eval_cases=" + num_eval_cases);
	}
}
