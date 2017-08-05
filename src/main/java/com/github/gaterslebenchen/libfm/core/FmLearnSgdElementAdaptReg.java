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

import com.github.gaterslebenchen.libfm.data.DataPointMatrix;
import com.github.gaterslebenchen.libfm.data.DataProvider;
import com.github.gaterslebenchen.libfm.data.SparseRow;
import com.github.gaterslebenchen.libfm.tools.Debug;
import com.github.gaterslebenchen.libfm.tools.TaskType;
import com.github.gaterslebenchen.libfm.tools.Util;

public class FmLearnSgdElementAdaptReg extends FmLearnSgd {
	// regularization parameter
	private double reg_0; // shrinking the bias towards the mean of the bias
	// (which is the bias) is the same as no
	// regularization.
	private double[] reg_w;
	private DataPointMatrix reg_v;
	private double mean_w, var_w;
	private double[] mean_v, var_v;
	// for each parameter there is one gradient to store
	private double[] grad_w;
	private DataPointMatrix grad_v;
	private DataProvider validation;
	// local parameters in the lambda_update step
	private double[] lambda_w_grad;
	private double[] sum_f, sum_f_dash_f;

	public void init() {
		super.init();

		reg_0 = 0;
		reg_w = new double[meta.num_attr_groups];
		reg_v = new DataPointMatrix(meta.num_attr_groups, fm.num_factor);

		mean_v = new double[fm.num_factor];
		var_v = new double[fm.num_factor];

		grad_w = new double[fm.num_attribute];
		grad_v= new DataPointMatrix(fm.num_factor, fm.num_attribute);

		Arrays.fill(grad_w, 0.0);
		grad_v.init(0.0);

		lambda_w_grad = new double[meta.num_attr_groups];
		sum_f = new double[meta.num_attr_groups];
		sum_f_dash_f= new double[meta.num_attr_groups];

		if (log != null) {
			log.addField("rmse_train", Double.NaN);
			log.addField("rmse_val", Double.NaN);

			log.addField("wmean", Double.NaN);
			log.addField("wvar", Double.NaN);
			for (int f = 0; f < fm.num_factor; f++) {
				{
					log.addField("vmean" + f, Double.NaN);
				}
				log.addField("vvar" + f, Double.NaN);
			}
			for (int g = 0; g < meta.num_attr_groups; g++) {
				{
					log.addField("regw[" + g + "]", Double.NaN);
				}
				for (int f = 0; f < fm.num_factor; f++) {
					{
						log.addField("regv[" + g + "," + f + "]", Double.NaN);
					}
				}
			}
		}
	}
	
	public void setValidation(DataProvider validation) {
		this.validation = validation;
	}

	private void sgd_theta_step(SparseRow x, double target) {
		double p = fm.predict(x, sum, sum_sqr);
		double mult = 0;
		if (task == TaskType.TASK_REGRESSION) {
			p = Math.min(max_target, p);
			p = Math.max(min_target, p);
			mult = 2 * (p - target);
		} else if (task == TaskType.TASK_CLASSIFICATION) {
			mult = target * ((1.0 / (1.0 + Math.exp(-target * p))) - 1.0);
		}

		// make the update with my regularization constants:
		if (fm.k0) {
			double w0 = fm.w0;
			double grad_0 = mult;
			w0 -= learn_rate * (grad_0 + 2 * reg_0 * w0);
			fm.w0 = w0;
		}
		if (fm.k1) {
			for (int i = 0; i < x.getSize(); i++) {
				int g = meta.attr_group[x.getData()[i].getId()];
				double w = fm.w[x.getData()[i].getId()];
				grad_w[x.getData()[i].getId()] = mult * x.getData()[i].getValue();
				w -= learn_rate * (grad_w[x.getData()[i].getId()] + 2 * reg_w[g] * w);
				fm.w[x.getData()[i].getId()] = w;
			}
		}
		for (int f = 0; f < fm.num_factor; f++) {
			for (int i = 0; i < x.getSize(); i++) {
				int g = meta.attr_group[x.getData()[i].getId()];
				double v = fm.v.get(f, x.getData()[i].getId());
				grad_v.set(f, x.getData()[i].getId(),
						mult * (x.getData()[i].getValue() * (sum[f] - v * x.getData()[i].getValue()))); 
				v -= learn_rate * (grad_v.get(f, x.getData()[i].getId()) + 2 * reg_v.get(g, f) * v);
				fm.v.set(f, x.getData()[i].getId(), v);
			}
		}
	}

	private double predict_scaled(SparseRow x) {
		double p = 0.0;
		if (fm.k0) {
			p += fm.w0;
		}
		if (fm.k1) {
			for (int i = 0; i < x.getSize(); i++) {
				assert (x.getData()[i].getId() < fm.num_attribute);
				int g = meta.attr_group[x.getData()[i].getId()];
				double w = fm.w[x.getData()[i].getId()];
				double w_dash = w - learn_rate * (grad_w[x.getData()[i].getId()] + 2 * reg_w[g] * w);
				p += w_dash * x.getData()[i].getValue();
			}
		}
		for (int f = 0; f < fm.num_factor; f++) {
			sum[f] = 0.0;
			sum_sqr[f] = 0.0;
			for (int i = 0; i < x.getSize(); i++) {
				int g = meta.attr_group[x.getData()[i].getId()];
				double v = fm.v.get(f, x.getData()[i].getId());
				double v_dash = v - learn_rate * (grad_v.get(f, x.getData()[i].getId()) + 2 * reg_v.get(g, f) * v);
				double d = v_dash * (x.getData()[i].getValue());
				sum[f] = d + sum[f];
				sum_sqr[f] = sum_sqr[f] + d * d;
			}
			p += 0.5 * (sum[f] * sum[f] - sum_sqr[f]);
		}
		return p;
	}

	private void sgd_lambda_step(SparseRow x, final double target) {
		double p = predict_scaled(x);
		double grad_loss = 0;
		if (task == TaskType.TASK_REGRESSION) {
			p = Math.min(max_target, p);
			p = Math.max(min_target, p);
			grad_loss = 2 * (p - target);
		} else if (task == TaskType.TASK_CLASSIFICATION) {
			grad_loss = target * ((1.0 / (1.0 + Math.exp(-target * p))) - 1.0);
		}
		if (fm.k1) {
			Arrays.fill(lambda_w_grad, 0.0);
			for (int i = 0; i < x.getSize(); i++) {
				int g = meta.attr_group[x.getData()[i].getId()];
				lambda_w_grad[g] = 
						lambda_w_grad[g] + (x.getData()[i].getValue()) * (fm.w[x.getData()[i].getId()]);
			}
			for (int g = 0; g < meta.num_attr_groups; g++) {
				lambda_w_grad[g] = -2 * learn_rate * lambda_w_grad[g];
				reg_w[g] = reg_w[g] - (learn_rate * grad_loss * lambda_w_grad[g]);
				reg_w[g] = Math.max(0.0, reg_w[g]);
			}
		}
		for (int f = 0; f < fm.num_factor; f++) {
			double sum_f_dash = 0.0;
			Arrays.fill(sum_f, 0.0);
			Arrays.fill(sum_f_dash_f, 0.0);
			for (int i = 0; i < x.getSize(); i++) {
				int g = meta.attr_group[x.getData()[i].getId()];
				double v = fm.v.get(f, x.getData()[i].getId());
				double v_dash = v - learn_rate * (grad_v.get(f, x.getData()[i].getId()) + 2 * reg_v.get(g, f) * v);

				sum_f_dash += v_dash * x.getData()[i].getValue();
				sum_f[g] = sum_f[g] + (v * x.getData()[i].getValue());
				sum_f_dash_f[g] = 
						sum_f_dash_f[g] + (v_dash * (x.getData()[i].getValue()) * v * (x.getData()[i].getValue()));
			}
			for (int g = 0; g < meta.num_attr_groups; g++) {
				double lambda_v_grad = -2 * learn_rate * (sum_f_dash * sum_f[g] - sum_f_dash_f[g]);
				reg_v.set(g, f, reg_v.get(g, f) - (learn_rate * grad_loss * lambda_v_grad));
				reg_v.set(g, f, Math.max(0.0, reg_v.get(g, f)));
			}
		}
	}

	private void update_means() {
		mean_w = 0;
		Arrays.fill(mean_v, 0);
		var_w = 0;
		Arrays.fill(var_v, 0);
		for (int j = 0; j < fm.num_attribute; j++) {
			mean_w += fm.w[j];
			var_w += fm.w[j] * fm.w[j];
			for (int f = 0; f < fm.num_factor; f++) {
				mean_v[f] = mean_v[f] + fm.v.get(f, j);
				var_v[f] = var_v[f] + (fm.v.get(f, j) * fm.v.get(f, j));
			}
		}
		mean_w /= (double) fm.num_attribute;
		var_w = var_w / fm.num_attribute - mean_w * mean_w;
		for (int f = 0; f < fm.num_factor; f++) {
			mean_v[f] = mean_v[f] / (fm.num_attribute);
			var_v[f] = (var_v[f] / fm.num_attribute) - (mean_v[f] * mean_v[f]);
		}

		mean_w = 0;
		for (int f = 0; f < fm.num_factor; f++) {
			mean_v[f] = 0;
		}
	}

	public void learn(DataProvider train, DataProvider test) throws Exception{
		super.learn(train, test);

		Debug.println("Training using self-adaptive-regularization SGD.");
		Debug.println("DON'T FORGET TO SHUFFLE THE ROWS IN TRAINING AND VALIDATION DATA TO GET THE BEST RESULTS.");

		Arrays.fill(fm.w, 0);
		fm.reg0 = 0;
		fm.regw = 0;
		fm.regv = 0;
		
		Arrays.fill(reg_w, 0.0);
		reg_v.init(0.0);

		Debug.println("Using " + train.getData().getNumRows() + " rows for training model parameters and "
				+ validation.getData().getNumRows() + " for training shrinkage.");

		// SGD
		for (int i = 0; i < num_iter; i++) {
			double iteration_time = Util.getusertime();
			train.shuffle();
			// SGD-based learning: both lambda and theta are learned
			update_means();
			validation.getData().begin();
			for (train.getData().begin(); !train.getData().end(); train.getData().next()) {
				sgd_theta_step(train.getData().getRow(), train.getTarget()[train.getData().getRowIndex()]);

				if (i > 0) { // make no lambda steps in the first iteration,
								// because some of the gradients (grad_theta)
								// might not be initialized.
					if (validation.getData().end()) {
						update_means();
						validation.getData().begin();
					}
					sgd_lambda_step(validation.getData().getRow(), validation.getTarget()[validation.getData().getRowIndex()]);
					validation.getData().next();
				}
			}

			// (3) Evaluation
			iteration_time = (Util.getusertime() - iteration_time);

			double rmse_val = evaluate(validation);
			double rmse_train = evaluate(train);
			double rmse_test = evaluate(test);
			Debug.println("#Iter=" + i + "\tTrain=" + rmse_train + "\tTest=" + rmse_test);
			if (log != null) {
				log.log("wmean", mean_w);
				log.log("wvar", var_w);
				for (int f = 0; f < fm.num_factor; f++) {
					log.log("vmean" + f, mean_v[f]);
					log.log("vvar" + f, var_v[f]);
				}
				for (int g = 0; g < meta.num_attr_groups; g++) {
					{
						log.log("regw[" + g + "]", reg_w[g]);
					}
					for (int f = 0; f < fm.num_factor; f++) {
						{
							log.log("regv[" + g + "," + f + "]", reg_v.get(g, f));
						}
					}
				}
				log.log("time_learn", iteration_time);
				log.log("rmse_train", rmse_train);
				log.log("rmse_val", rmse_val);
				log.newLine();
			}
		}
	}
	
	public void debug() {
		Debug.println("method=sgda");
		super.debug();			
	}

}
