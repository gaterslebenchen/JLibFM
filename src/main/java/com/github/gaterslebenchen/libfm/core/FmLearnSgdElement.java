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
import com.github.gaterslebenchen.libfm.tools.Debug;
import com.github.gaterslebenchen.libfm.tools.JlibfmRuntimeException;
import com.github.gaterslebenchen.libfm.tools.TaskType;
import com.github.gaterslebenchen.libfm.tools.Util;

public class FmLearnSgdElement extends FmLearnSgd {
	
	public void init() {
		super.init();
		if (log != null) {
			log.addField("rmse_train", Double.NaN);
		}
	}
	
	public void learn(DataProvider train, DataProvider test)  throws Exception{
		super.learn(train, test);

		Debug.println("SGD: DON'T FORGET TO SHUFFLE THE ROWS IN TRAINING DATA TO GET THE BEST RESULTS."); 
		// SGD
		for (int i = 0; i < num_iter; i++) {
			try
			{
				double iteration_time = Util.getusertime();
				train.shuffle();
				for (train.getData().begin(); !train.getData().end(); train.getData().next()) {
					double p = fm.predict(train.getData().getRow(), sum, sum_sqr);
					double mult = 0;
					if (task == TaskType.TASK_REGRESSION) {
						p = Math.min(max_target, p);
						p = Math.max(min_target, p);
						mult = -(train.getTarget()[train.getData().getRowIndex()]-p);
					} else if (task == TaskType.TASK_CLASSIFICATION) {
						mult = -train.getTarget()[train.getData().getRowIndex()]*(1.0-1.0/(1.0+Math.exp(-train.getTarget()[train.getData().getRowIndex()]*p)));
					}				
					SGD(train.getData().getRow(), mult, sum);					
				}				
				iteration_time = (Util.getusertime() - iteration_time);
				double rmse_train = evaluate(train);
				double rmse_test = evaluate(test);
				Debug.println("#Iter=" + i + "\tTrain=" + rmse_train + "\tTest=" + rmse_test);
				if (log != null) {
					log.log("rmse_train", rmse_train);
					log.log("time_learn", iteration_time);
					log.newLine();
				}
			}
			catch(Exception e)
			{
				throw new JlibfmRuntimeException(e);
			}
		}		
	}
}
