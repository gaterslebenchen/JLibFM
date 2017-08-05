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

import com.github.gaterslebenchen.libfm.data.DataProvider;
import com.github.gaterslebenchen.libfm.data.EqtermMatrix;
import com.github.gaterslebenchen.libfm.data.MetricsResult;
import com.github.gaterslebenchen.libfm.tools.Debug;
import com.github.gaterslebenchen.libfm.tools.JlibfmRuntimeException;
import com.github.gaterslebenchen.libfm.tools.TaskType;
import com.github.gaterslebenchen.libfm.tools.Util;

public class FmLearnMcmcSimultaneous extends FmLearnMcmc {

    public void mcmclearn(DataProvider train, DataProvider test) {
        int num_complete_iter = 0;

        // make a collection of datasets that are predicted jointly
        int num_data = 2;
        DataProvider[] main_data = new DataProvider[num_data];
        EqtermMatrix main_cache = new EqtermMatrix(num_data,cache_test.length);
        main_data[0] = train;
        main_data[1] = test;

        main_cache.set(0, cache);
        main_cache.set(1, cache_test);


        try
        {
            predict_data_and_write_to_eterms(main_data, main_cache);
        }
        catch(Exception e)
        {
        	throw new JlibfmRuntimeException(e);
        }


        if (task == TaskType.TASK_REGRESSION) {
            // remove the target from each prediction, because: e(c) := \hat{y}(c) - target(c)
            for (int c = 0; c < train.getRownumber(); c++) {
                cache[c].e = cache[c].e - train.getTarget()[c];
            }
        } else if (task == TaskType.TASK_CLASSIFICATION) {
            // for Classification: remove from e not the target but a sampled value from a truncated normal
            // for initializing, they are not sampled but initialized with meaningful values:
            // -1 for the negative class and +1 for the positive class (actually these are the values that are already in the target and thus, we can do the same as for regression; but note that other initialization strategies would need other techniques here:
            for (int c = 0; c < train.getRownumber(); c++) {
                cache[c].e = cache[c].e - train.getTarget()[c];
            }

        } else {
        	throw new JlibfmRuntimeException("task not supported" + task);
        }



        for (int i = num_complete_iter; i < num_iter; i++) {
            double iteration_time = Util.getusertime();
            nan_cntr_w0 = 0;
            inf_cntr_w0 = 0;
            nan_cntr_w = 0;
            inf_cntr_w = 0;
            nan_cntr_v = 0;
            inf_cntr_v = 0;
            nan_cntr_alpha = 0;
            inf_cntr_alpha = 0;
            nan_cntr_w_mu = 0;
            inf_cntr_w_mu = 0;
            nan_cntr_w_lambda = 0;
            inf_cntr_w_lambda = 0;
            nan_cntr_v_mu = 0;
            inf_cntr_v_mu = 0;
            nan_cntr_v_lambda = 0;
            inf_cntr_v_lambda = 0;

            try
            {
                draw_all(train);
            }
            catch(Exception e)
            {
            	throw new JlibfmRuntimeException(e);
            }



            if ((nan_cntr_alpha > 0) || (inf_cntr_alpha > 0)) {
            	Debug.println("#nans in alpha:\t" + nan_cntr_alpha + "\t#inf_in_alpha:\t" + inf_cntr_alpha);
            }
            if ((nan_cntr_w0 > 0) || (inf_cntr_w0 > 0)) {
            	Debug.println("#nans in w0:\t" + nan_cntr_w0 + "\t#inf_in_w0:\t" + inf_cntr_w0);
            }
            if ((nan_cntr_w > 0) || (inf_cntr_w > 0)) {
            	Debug.println("#nans in w:\t" + nan_cntr_w + "\t#inf_in_w:\t" + inf_cntr_w);
            }
            if ((nan_cntr_v > 0) || (inf_cntr_v > 0)) {
            	Debug.println("#nans in v:\t" + nan_cntr_v + "\t#inf_in_v:\t" + inf_cntr_v);
            }
            if ((nan_cntr_w_mu > 0) || (inf_cntr_w_mu > 0)) {
            	Debug.println("#nans in w_mu:\t" + nan_cntr_w_mu + "\t#inf_in_w_mu:\t" + inf_cntr_w_mu);
            }
            if ((nan_cntr_w_lambda > 0) || (inf_cntr_w_lambda > 0)) {
            	Debug.println("#nans in w_lambda:\t" + nan_cntr_w_lambda + "\t#inf_in_w_lambda:\t" + inf_cntr_w_lambda);
            }
            if ((nan_cntr_v_mu > 0) || (inf_cntr_v_mu > 0)) {
            	Debug.println("#nans in v_mu:\t" + nan_cntr_v_mu + "\t#inf_in_v_mu:\t" + inf_cntr_v_mu);
            }
            if ((nan_cntr_v_lambda > 0) || (inf_cntr_v_lambda > 0)) {
            	Debug.println("#nans in v_lambda:\t" + nan_cntr_v_lambda + "\t#inf_in_v_lambda:\t" + inf_cntr_v_lambda);
            }



            // predict test and train
            try
            {
                predict_data_and_write_to_eterms(main_data, main_cache);
            }
            catch(Exception e)
            {
            	throw new JlibfmRuntimeException(e);
            }

            // (prediction of train is not necessary but it increases numerical stability)

            iteration_time = (Util.getusertime() - iteration_time);

            if (log != null) {
                log.log("time_learn", iteration_time);
            }



            double acc_train = 0.0;
            double rmse_train = 0.0;
            if (task == TaskType.TASK_REGRESSION) {
                // evaluate test and store it
                for (int c = 0; c < test.getRownumber(); c++) {
                    double p = cache_test[c].e;
                    pred_this[c] = p;
                    p = Math.min(max_target, p);
                    p = Math.max(min_target, p);
                    pred_sum_all[c] = pred_sum_all[c]+p;
                    if (i >= 5) {
                        pred_sum_all_but5[c] = pred_sum_all_but5[c] + p;
                    }
                }

                // Evaluate the training dataset and update the e-terms
                for (int c = 0; c < train.getRownumber(); c++) {
                    double p = cache[c].e;
                    p = Math.min(max_target, p);
                    p = Math.max(min_target, p);
                    double err = p - train.getTarget()[c];
                    rmse_train += err*err;
                    cache[c].e = cache[c].e - train.getTarget()[c];
                }
                rmse_train = Math.sqrt(rmse_train/train.getRownumber());
            } else if (task == TaskType.TASK_CLASSIFICATION) {
                // evaluate test and store it
                for (int c = 0; c < test.getRownumber(); c++) {
                    double p = cache_test[c].e;
                    p = Util.cdf_gaussian(p);
                    pred_this[c] = p;
                    pred_sum_all[c] = pred_sum_all[c]+p;
                    if (i >= 5) {
                        pred_sum_all_but5[c] = pred_sum_all_but5[c] + p;
                    }
                }

                // Evaluate the training dataset and update the e-terms
                int tmp_acc_train = 0;
                for (int c = 0; c < train.getRownumber(); c++) {
                    double p = cache[c].e;
                    p = Util.cdf_gaussian(p);
                    if (((p >= 0.5) && (train.getTarget()[c] > 0.0)) || ((p < 0.5) && (train.getTarget()[c] < 0.0))) {
                        tmp_acc_train++;
                    }

                    double sampled_target;
                    if (train.getTarget()[c] >= 0.0) {
                        if (do_sample) {
                            sampled_target = Util.ran_left_tgaussian(0.0, cache[c].e, 1.0);
                        } else {
                            // the target is the expected value of the truncated normal
                            double mu = cache[c].e;
                            double phi_minus_mu = Math.exp(-mu*mu/2.0) / Math.sqrt(3.141*2);
                            double Phi_minus_mu = Util.cdf_gaussian(-mu);
                            sampled_target = mu + phi_minus_mu / (1-Phi_minus_mu);
                        }
                    } else {
                        if (do_sample) {
                            sampled_target = Util.ran_right_tgaussian(0.0, cache[c].e, 1.0);
                        } else {
                            // the target is the expected value of the truncated normal
                            double mu = cache[c].e;
                            double phi_minus_mu = Math.exp(-mu*mu/2.0) / Math.sqrt(3.141*2);
                            double Phi_minus_mu = Util.cdf_gaussian(-mu);
                            sampled_target = mu - phi_minus_mu / Phi_minus_mu;
                        }
                    }
                    cache[c].e = cache[c].e - sampled_target;
                }
                acc_train = (double) tmp_acc_train / train.getRownumber();

            } else {
            	throw new JlibfmRuntimeException("task not supported" + task);
            }


            // Evaluate the test data sets
            if (task == TaskType.TASK_REGRESSION) {
                MetricsResult test_this = new MetricsResult();
                MetricsResult test_all = new MetricsResult();
                MetricsResult test_all_but5 = new MetricsResult();
                mcmcevaluate(pred_this, test.getTarget(), 1.0, test_this, num_eval_cases);
                mcmcevaluate(pred_sum_all, test.getTarget(), 1.0/(i+1), test_all, num_eval_cases);
                mcmcevaluate(pred_sum_all_but5, test.getTarget(), 1.0/(i-5+1), test_all_but5, num_eval_cases);

                Debug.println("#Iter=" + i + "\tTrain=" + rmse_train + "\tTest=" + test_all.getRmse());

                if (log != null) {
                    log.log("rmse", test_all.getRmse());
                    log.log("mae", test_all.getMae());
                    log.log("rmse_mcmc_this", test_this.getRmse());
                    log.log("rmse_mcmc_all", test_all.getRmse());
                    log.log("rmse_mcmc_all_but5", test_all_but5.getRmse());

                    if (num_eval_cases < test.getTarget().length) {
                        MetricsResult test2_this = new MetricsResult();
                        MetricsResult test2_all = new MetricsResult();
                        mcmcevaluate(pred_this, test.getTarget(), 1.0, test2_this, num_eval_cases, test.getTarget().length);
                        mcmcevaluate(pred_sum_all, test.getTarget(), 1.0/(i+1), test2_all, num_eval_cases, test.getTarget().length);
                    }
                    try {
                        log.newLine();
                    } catch (Exception e) {
                    	throw new JlibfmRuntimeException(e);
                    }
                }
            } else if (task == TaskType.TASK_CLASSIFICATION) {
                MetricsResult acc_test_this = new MetricsResult();
                MetricsResult acc_test_all = new MetricsResult();
                MetricsResult test_all_but5 = new MetricsResult();
                mcmcevaluate_class(pred_this, test.getTarget(), 1.0, acc_test_this, num_eval_cases);
                mcmcevaluate_class(pred_sum_all, test.getTarget(), 1.0/(i+1), acc_test_all, num_eval_cases);
                mcmcevaluate_class(pred_sum_all_but5, test.getTarget(), 1.0/(i-5+1), test_all_but5, num_eval_cases);

                Debug.println("#Iter=" + i + "\tTrain=" + acc_train + "\tTest=" + acc_test_all.getMae() + "\tTest(ll)=" + acc_test_all.getRmse() );

                if (log != null) {
                    log.log("accuracy", acc_test_all.getMae());
                    log.log("acc_mcmc_this", acc_test_this.getMae());
                    log.log("acc_mcmc_all", acc_test_all.getMae());
                    log.log("acc_mcmc_all_but5", test_all_but5.getMae());
                    log.log("ll_mcmc_this", acc_test_this.getRmse());
                    log.log("ll_mcmc_all", acc_test_all.getRmse());
                    log.log("ll_mcmc_all_but5", test_all_but5.getRmse());

                    if (num_eval_cases < test.getTarget().length) {
                        MetricsResult acc_test2_this = new MetricsResult();
                        MetricsResult acc_test2_all = new MetricsResult();
                        mcmcevaluate_class(pred_this, test.getTarget(), 1.0, acc_test2_this, num_eval_cases, test.getTarget().length);
                        mcmcevaluate_class(pred_sum_all, test.getTarget(), 1.0/(i+1), acc_test2_all, num_eval_cases, test.getTarget().length);
                    }
                    try {
                        log.newLine();
                    } catch (Exception e) {
                    	throw new JlibfmRuntimeException(e);
                    }
                }

            } else {
            	throw new JlibfmRuntimeException("task not supported" + task);
            }
        }
    }

    private void mcmcevaluate(double[] pred, double[] target, double normalizer, MetricsResult doubleHolder, int from_case, int to_case) {
        assert(pred.length == target.length);
        double rmse = 0;
        double mae = 0;
        int num_cases = 0;
        for (int c = Math.max( 0, from_case); c < Math.min(pred.length, to_case); c++) {
            double p = pred[c] * normalizer;
            p = Math.min(max_target, p);
            p = Math.max(min_target, p);
            double err = p - target[c];
            rmse += err*err;
            mae += Math.abs(err);
            num_cases++;
        }

        doubleHolder.setRmse(Math.sqrt(rmse/num_cases));
        doubleHolder.setMae(mae/num_cases);
    }

    private void mcmcevaluate(double[] pred, double[] target, double normalizer, MetricsResult doubleHolder, int num_eval_cases) {
        mcmcevaluate(pred, target, normalizer, doubleHolder, 0, num_eval_cases);
    }

    private void mcmcevaluate_class(double[] pred, double[] target, double normalizer, MetricsResult doubleHolder, int from_case, int to_case) {
        double loglikelihood = 0.0;
        int accuracy = 0;
        int num_cases = 0;
        for (int c = Math.max((int) 0, from_case); c < Math.min((int)pred.length, to_case); c++) {
            double p = pred[c] * normalizer;
            if (((p >= 0.5) && (target[c] > 0.0)) || ((p < 0.5) && (target[c] < 0.0))) {
                accuracy++;
            }
            double m = (target[c]+1.0)*0.5;
            double pll = p;
            if (pll > 0.99) { pll = 0.99; }
            if (pll < 0.01) { pll = 0.01; }
            loglikelihood -= m*Math.log10(pll) + (1-m)*Math.log10(1-pll);
            num_cases++;
        }

        doubleHolder.setRmse(loglikelihood/num_cases);
        doubleHolder.setMae((double) accuracy / num_cases);
    }

    private void mcmcevaluate_class(double[] pred, double[] target, double normalizer, MetricsResult doubleHolder, int num_eval_cases) {
        mcmcevaluate_class(pred, target, normalizer, doubleHolder, 0, num_eval_cases);
    }
}
