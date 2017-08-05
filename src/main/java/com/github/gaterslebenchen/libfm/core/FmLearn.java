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

import java.util.Properties;

import com.github.gaterslebenchen.libfm.data.DataProvider;
import com.github.gaterslebenchen.libfm.tools.Debug;
import com.github.gaterslebenchen.libfm.tools.JlibfmRuntimeException;
import com.github.gaterslebenchen.libfm.tools.RLog;
import com.github.gaterslebenchen.libfm.tools.TaskType;
import com.github.gaterslebenchen.libfm.tools.Util;

public abstract class FmLearn {
	public DataMetaInfo meta;
	public FmModel fm;
	public double min_target;
	public double max_target;

	public TaskType task; // 0=regression, 1=classification
	
	public DataProvider validation;	
	public RLog log;
	
	public FmLearn()
	{
		task = TaskType.TASK_REGRESSION;
	}
	
	public void init() {
		if (log != null) {
			if (task == TaskType.TASK_REGRESSION) {
				log.addField("rmse", Double.NaN);
				log.addField("mae", Double.NaN);
			} else if (task == TaskType.TASK_CLASSIFICATION) {
				log.addField("accuracy", Double.NaN);
			} else {
				throw new JlibfmRuntimeException("task not supported" + task);
			}
			log.addField("time_pred", Double.NaN);
			log.addField("time_learn", Double.NaN);
			log.addField("time_learn2", Double.NaN);
			log.addField("time_learn4", Double.NaN);
		}
	}
	
	public abstract void learn(DataProvider train, DataProvider test) throws Exception;
	public abstract void predict(DataProvider data, double[] out) throws Exception;
	public abstract void saveModel(Properties properties) throws Exception;
	public abstract void loadModel(Properties properties) throws Exception;
	
	public double evaluate(DataProvider data) throws Exception{
		assert(data.getData() != null);
		if (task == TaskType.TASK_REGRESSION) {
			return evaluate_regression(data);
		} else if (task == TaskType.TASK_CLASSIFICATION) {
			return evaluate_classification(data);
		} else {
			throw new JlibfmRuntimeException("unknown task");
		}
	}
	

	public double predict_case(DataProvider data) {
		return fm.predict(data.getData().getRow());
	}
	
	public void debug() { 
		Debug.println("task=" + task);
		Debug.println("min_target=" + min_target);
		Debug.println("max_target=" + max_target);		
	}
	
	private double evaluate_classification(DataProvider data) throws Exception {
		int num_correct = 0;
		double eval_time = Util.getusertime();
		for (data.getData().begin(); !data.getData().end(); data.getData().next()) {
			double p = predict_case(data);
			if (((p >= 0) && (data.getTarget()[data.getData().getRowIndex()] >= 0)) || ((p < 0) && (data.getTarget()[data.getData().getRowIndex()] < 0))) {
				num_correct++;
			}	
		}	
		eval_time = (Util.getusertime() - eval_time);
		// log the values
		if (log != null) {
			log.log("accuracy", (double) num_correct / (double) data.getData().getNumRows());
			log.log("time_pred", eval_time);
		}

		return (double) num_correct / (double) data.getData().getNumRows();
	}
	
	private double evaluate_regression(DataProvider data) throws Exception {
		double rmse_sum_sqr = 0;
		double mae_sum_abs = 0;
		double eval_time = Util.getusertime();
		for (data.getData().begin(); !data.getData().end(); data.getData().next()) {
			double p = predict_case(data);
			p = Math.min(max_target, p);
			p = Math.max(min_target, p);
			double err = p - data.getTarget()[data.getData().getRowIndex()];
			rmse_sum_sqr += err*err;
			mae_sum_abs += Math.abs(err);
		}	
		eval_time = (Util.getusertime() - eval_time);
		// log the values
		if (log != null) {
			log.log("rmse", Math.sqrt(rmse_sum_sqr/data.getData().getNumRows()));
			log.log("mae", mae_sum_abs/data.getData().getNumRows());
			log.log("time_pred", eval_time);
		}

		return Math.sqrt(rmse_sum_sqr/data.getData().getNumRows());
	}
}
