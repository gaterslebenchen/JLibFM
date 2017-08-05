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
package com.github.gaterslebenchen.libfm;

import com.github.gaterslebenchen.libfm.core.*;
import com.github.gaterslebenchen.libfm.data.DataProvider;
import com.github.gaterslebenchen.libfm.data.LibSVMDataProvider;
import com.github.gaterslebenchen.libfm.tools.CmdLine;
import com.github.gaterslebenchen.libfm.tools.Constants;
import com.github.gaterslebenchen.libfm.tools.Debug;
import com.github.gaterslebenchen.libfm.tools.JlibfmRuntimeException;
import com.github.gaterslebenchen.libfm.tools.RLog;
import com.github.gaterslebenchen.libfm.tools.TaskType;
import com.github.gaterslebenchen.libfm.tools.Util;

import java.util.Arrays;
import java.util.Properties;

public class Main {
	public static void main(String[] args) throws Exception {
		CmdLine cmdline = new CmdLine(args);

		System.out.println("----------------------------------------------------------------------------");
		System.out.println("JLibFM");
		System.out.println("A Java implementation of libFM: Factorization Machine Library(http://www.libfm.org/)");
		System.out.println("Author: Jinbo Chen, gaterslebenchen@gmail.com");
		System.out.println("----------------------------------------------------------------------------");

		String param_task = cmdline.registerParameter("task", "r=regression, c=binary classification [MANDATORY]");
		String param_meta_file = cmdline.registerParameter("meta", "filename for meta information about data set");
		String param_train_file = cmdline.registerParameter("train", "filename for training data [MANDATORY]");
		String param_test_file = cmdline.registerParameter("test", "filename for test data [MANDATORY]");
		String param_val_file = cmdline.registerParameter("validation", "filename for validation data (only for SGDA)");
		String param_out = cmdline.registerParameter("out", "filename for output");

		String param_dim = cmdline.registerParameter("dim",
				"'k0,k1,k2': k0=use bias, k1=use 1-way interactions, k2=dim of 2-way interactions; default=1,1,8");
		String param_regular = cmdline.registerParameter("regular",
				"'r0,r1,r2' for SGD and ALS: r0=bias regularization, r1=1-way regularization, r2=2-way regularization");
		String param_init_stdev = cmdline.registerParameter("init_stdev",
				"stdev for initialization of 2-way factors; default=0.1");
		String param_num_iter = cmdline.registerParameter("iter", "number of iterations; default=100");
		String param_learn_rate = cmdline.registerParameter("learn_rate", "learn_rate for SGD; default=0.1");

		String param_method = cmdline.registerParameter("method",
				"learning method (SGD, SGDA, ALS, MCMC); default=MCMC");

		String param_verbosity = cmdline.registerParameter("verbosity", "how much infos to print; default=0");
		String param_r_log = cmdline.registerParameter("rlog",
				"write measurements within iterations to a file; default=''");
		String param_help = cmdline.registerParameter("help", "this screen");

		String param_do_sampling = "do_sampling";
		String param_do_multilevel = "do_multilevel";
		String param_num_eval_cases = "num_eval_cases";

		if (cmdline.hasParameter(param_help) || (args.length <= 1)) {
			cmdline.printHelp();
			return;
		}
		cmdline.checkParameters();

		if (!cmdline.hasParameter(param_method)) {
			cmdline.setValue(param_method, "mcmc");
		}
		if (!cmdline.hasParameter(param_init_stdev)) {
			cmdline.setValue(param_init_stdev, "0.1");
		}
		if (!cmdline.hasParameter(param_dim)) {
			cmdline.setValue(param_dim, "1,1,8");
		}

		if (cmdline.getValue(param_method).equals("als")) { // als is an mcmc
															// without sampling
															// and
															// hyperparameter
															// inference
			cmdline.setValue(param_method, "mcmc");
			if (!cmdline.hasParameter(param_do_sampling)) {
				cmdline.setValue(param_do_sampling, "0");
			}
			if (!cmdline.hasParameter(param_do_multilevel)) {
				cmdline.setValue(param_do_multilevel, "0");
			}
		}

		// (1) Load the data
		System.out.println("Loading train...\t");

		DataProvider train = new LibSVMDataProvider();
		Properties trainproperties = new Properties();
		trainproperties.put(Constants.FILENAME, cmdline.getValue(param_train_file));

		// no transpose data for sgd, sgda
		train.load(trainproperties,
				!(cmdline.getValue(param_method).equals("sgd") || cmdline.getValue(param_method).equals("sgda")));

		System.out.println("Loading test... \t");
		DataProvider test = new LibSVMDataProvider();

		Properties testproperties = new Properties();
		testproperties.put(Constants.FILENAME, cmdline.getValue(param_test_file));

		// no transpose data for sgd, sgda

		test.load(testproperties,
				!(cmdline.getValue(param_method).equals("sgd") || cmdline.getValue(param_method).equals("sgda")));

		DataProvider validation = null;
		if (cmdline.hasParameter(param_val_file)) {
			if (!cmdline.getValue(param_method).equals("sgda")) {
				System.out.println("WARNING: Validation data is only used for SGDA. The data is ignored.");
			} else {
				System.out.println("Loading validation set...\t");
				validation = new LibSVMDataProvider();
				Properties validproperties = new Properties();
				validproperties.put(Constants.FILENAME, cmdline.getValue(param_val_file));

				// no transpose data for sgd, sgda
				validation.load(validproperties, !(!cmdline.getValue(param_method).equals("sgd")
						|| !cmdline.getValue(param_method).equals("sgda")));
			}
		}

		// (1.3) Load meta data
		System.out.println("Loading meta data...\t");

		// (main table)
		int num_all_attribute = Math.max(train.getFeaturenumber(), test.getFeaturenumber());
		if (validation != null) {
			num_all_attribute = Math.max(num_all_attribute, validation.getFeaturenumber());
		}

		DataMetaInfo meta = new DataMetaInfo(num_all_attribute);
		if (cmdline.hasParameter(param_meta_file)) {
			meta.loadGroupsFromFile(cmdline.getValue(param_meta_file));
		}
		if (cmdline.getValue(param_verbosity, 0) > 0) {
			meta.debug();
			Debug.openConsole();
		}

		// (2) Setup the factorization machine
		FmModel fm = new FmModel();
		{
			fm.num_attribute = num_all_attribute;
			fm.initstdev = cmdline.getValue(param_init_stdev, 0.1);
			// set the number of dimensions in the factorization
			{
				Integer[] dim = cmdline.getIntValues(param_dim);
				assert (dim.length == 3);
				fm.k0 = dim[0] != 0;
				fm.k1 = dim[1] != 0;
				fm.num_factor = dim[2];
			}
			fm.init();

		}

		// (3) Setup the learning method:
		FmLearn fml = null;

		if (cmdline.getValue(param_method).equals("sgd")) {
			fml = new FmLearnSgdElement();
			((FmLearnSgd) fml).num_iter = cmdline.getValue(param_num_iter, 100);

		} else if (cmdline.getValue(param_method).equals("sgda")) {
			assert (validation != null);
			fml = new FmLearnSgdElementAdaptReg();
			((FmLearnSgd) fml).num_iter = cmdline.getValue(param_num_iter, 100);
			((FmLearnSgdElementAdaptReg) fml).setValidation(validation);
		} else if (cmdline.getValue(param_method).equals("mcmc")) {
			Util.init_normal(fm.w, fm.initmean, fm.initstdev);
			fml = new FmLearnMcmcSimultaneous();
			fml.validation = validation;
			((FmLearnMcmc) fml).num_iter = cmdline.getValue(param_num_iter, 100);
			((FmLearnMcmc) fml).num_eval_cases = cmdline.getValue(param_num_eval_cases, test.getRownumber());

			((FmLearnMcmc) fml).do_sample = (cmdline.getValue(param_do_sampling, 1) > 0);
			((FmLearnMcmc) fml).do_multilevel = (cmdline.getValue(param_do_multilevel, 1) > 0);
		} else {
			throw new JlibfmRuntimeException("unknown method");
		}
		fml.fm = fm;
		fml.max_target = train.getMaxtarget();
		fml.min_target = train.getMintarget();
		fml.meta = meta;
		if (cmdline.getValue(param_task).equals("r")) {
			fml.task = TaskType.TASK_REGRESSION;
		} else if (cmdline.getValue(param_task).equals("c")) {
			fml.task = TaskType.TASK_CLASSIFICATION;
			for (int i = 0; i < train.getTarget().length; i++) {
				if (train.getTarget()[i] <= 0.0) {
					train.getTarget()[i] = -1.0;
				} else {
					train.getTarget()[i] = 1.0;
				}
			}
			for (int i = 0; i < test.getTarget().length; i++) {
				if (test.getTarget()[i] <= 0.0) {
					test.getTarget()[i] = -1.0;
				} else {
					test.getTarget()[i] = 1.0;
				}
			}
			if (validation != null) {
				for (int i = 0; i < validation.getTarget().length; i++) {
					if (validation.getTarget()[i] <= 0.0) {
						validation.getTarget()[i] = -1.0;
					} else {
						validation.getTarget()[i] = 1.0;
					}
				}
			}
		} else {
			throw new JlibfmRuntimeException("unknown task");
		}

		// (4) init the logging
		RLog rlog = null;
		if (cmdline.hasParameter(param_r_log)) {
			String r_log_str = cmdline.getValue(param_r_log);

			System.out.println("logging to " + r_log_str);
			rlog = new RLog(r_log_str);
		}

		fml.log = rlog;
		fml.init();
		if (cmdline.getValue(param_method).equals("mcmc")) {
			// set the regularization; for als and mcmc this can be individual
			// per group
			{
				Double[] reg = cmdline.getDblValues(param_regular);
				assert ((reg.length == 0) || (reg.length == 1) || (reg.length == 3)
						|| (reg.length == (1 + meta.num_attr_groups * 2)));
				if (reg.length == 0) {
					fm.reg0 = 0.0;
					fm.regw = 0.0;
					fm.regv = 0.0;
					Arrays.fill(((FmLearnMcmc) fml).w_lambda, fm.regw);
					((FmLearnMcmc) fml).v_lambda.init(fm.regv);
				} else if (reg.length == 1) {
					fm.reg0 = reg[0];
					fm.regw = reg[0];
					fm.regv = reg[0];
					Arrays.fill(((FmLearnMcmc) fml).w_lambda, fm.regw);
					((FmLearnMcmc) fml).v_lambda.init(fm.regv);
				} else if (reg.length == 3) {
					fm.reg0 = reg[0];
					fm.regw = reg[1];
					fm.regv = reg[2];
					Arrays.fill(((FmLearnMcmc) fml).w_lambda, fm.regw);
					((FmLearnMcmc) fml).v_lambda.init(fm.regv);
				} else {
					fm.reg0 = reg[0];
					fm.regw = 0.0;
					fm.regv = 0.0;
					int j = 1;
					for (int g = 0; g < meta.num_attr_groups; g++) {
						((FmLearnMcmc) fml).w_lambda[g] = reg[j];
						j++;
					}
					for (int g = 0; g < meta.num_attr_groups; g++) {
						for (int f = 0; f < fm.num_factor; f++) {
							((FmLearnMcmc) fml).v_lambda.set(g, f, reg[j]);
						}
						j++;
					}
				}

			}
		} else {
			// set the regularization; for standard SGD, groups are not
			// supported
			{
				Double[] reg = cmdline.getDblValues(param_regular);
				assert ((reg.length == 0) || (reg.length == 1) || (reg.length == 3));
				if (reg.length == 0) {
					fm.reg0 = 0.0;
					fm.regw = 0.0;
					fm.regv = 0.0;
				} else if (reg.length == 1) {
					fm.reg0 = reg[0];
					fm.regw = reg[0];
					fm.regv = reg[0];
				} else {
					fm.reg0 = reg[0];
					fm.regw = reg[1];
					fm.regv = reg[2];
				}
			}
		}

		if (fml instanceof FmLearnSgd) {
			FmLearnSgd fmlsgd = (FmLearnSgd) (fml);
			if (fmlsgd != null) {
				// set the learning rates (individual per layer)
				{
					Double[] lr = cmdline.getDblValues(param_learn_rate);
					assert ((lr.length == 1) || (lr.length == 3));
					if (lr.length == 1) {
						fmlsgd.learn_rate = lr[0];
						Arrays.fill(fmlsgd.learn_rates, lr[0]);
					} else {
						fmlsgd.learn_rate = 0;
						fmlsgd.learn_rates[0] = lr[0];
						fmlsgd.learn_rates[1] = lr[1];
						fmlsgd.learn_rates[2] = lr[2];
					}
				}
			}
		}

		if (rlog != null) {
			rlog.init();
		}

		if (cmdline.getValue(param_verbosity, 0) > 0) {
			fm.debug();
			fml.debug();
		}

		// () learn
		fml.learn(train, test);

		// () Prediction at the end (not for mcmc and als)
		if (!cmdline.getValue(param_method).equals("mcmc")) {
			System.out.println("Final\t" + "Train=" + fml.evaluate(train) + "\tTest=" + fml.evaluate(test));
		}

		// () Save prediction
		if (cmdline.hasParameter(param_out)) {
			double[] pred = new double[test.getRownumber()];
			fml.predict(test, pred);
			Util.save(pred, cmdline.getValue(param_out));
		}

	}
}
