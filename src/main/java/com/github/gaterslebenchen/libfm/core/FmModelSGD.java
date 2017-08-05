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

import com.github.gaterslebenchen.libfm.data.SparseRow;

public class FmModelSGD {
	public static void fm_SGD(FmModel fm, double learn_rate, SparseRow x,
			double multiplier, double[] sum) {
		if (fm.k0) {
			double w0 = fm.w0;
			w0 -= learn_rate * (multiplier + fm.reg0 * w0);
			fm.w0 = w0;
		}
		if (fm.k1) {
			for (int i = 0; i < x.getSize(); i++) {
				double w = fm.w[x.getData()[i].getId()];
				w -= learn_rate
						* (multiplier * x.getData()[i].getValue() + fm.regw * w);
				fm.w[x.getData()[i].getId()] = w;
			}
		}
		for (int f = 0; f < fm.num_factor; f++) {
			for (int i = 0; i < x.getSize(); i++) {
				double v = fm.v.get(f, x.getData()[i].getId());
				double grad = sum[f] * x.getData()[i].getValue() - v
						* x.getData()[i].getValue() * x.getData()[i].getValue();
				v -= learn_rate * (multiplier * grad + fm.regv * v);
				fm.v.set(f, x.getData()[i].getId(), v);
			}
		}
	}

	public static void fm_pairSGD(FmModel fm, double learn_rate, SparseRow x_pos,
			SparseRow x_neg, double multiplier, double[] sum_pos,
			double[] sum_neg, boolean[] grad_visited, double[] grad) {
		if (fm.k0) {
			double w0 = fm.w0;
			w0 -= fm.reg0 * w0; // w0 should always be 0
			fm.w0 = w0;
		}
		if (fm.k1) {
			for (int i = 0; i < x_pos.getSize(); i++) {
				grad[x_pos.getData()[i].getId()] = 0;
				grad_visited[x_pos.getData()[i].getId()] = false;
			}
			for (int i = 0; i < x_neg.getSize(); i++) {
				grad[x_neg.getData()[i].getId()] = 0;
				grad_visited[x_neg.getData()[i].getId()] = false;
			}
			for (int i = 0; i < x_pos.getSize(); i++) {
				grad[x_pos.getData()[i].getId()] = 
						grad[x_pos.getData()[i].getId()]
								+ x_pos.getData()[i].getValue();
			}

			for (int i = 0; i < x_neg.getSize(); i++) {
				grad[x_neg.getData()[i].getId()] = 
						grad[x_neg.getData()[i].getId()]
								- x_neg.getData()[i].getValue();
			}
			for (int i = 0; i < x_pos.getSize(); i++) {
				int attr_id = x_pos.getData()[i].getId();
				if (!grad_visited[attr_id]) {
					double w = fm.w[attr_id];
					w -= learn_rate
							* (multiplier * grad[attr_id] + fm.regw * w);
					grad_visited[attr_id] = true;
					fm.w[attr_id] = w;
				}
			}
			for (int i = 0; i < x_neg.getSize(); i++) {
				int attr_id = x_neg.getData()[i].getId();
				if (!grad_visited[attr_id]) {
					double w = fm.w[attr_id];
					w -= learn_rate
							* (multiplier * grad[attr_id] + fm.regw * w);
					grad_visited[attr_id] =  true;
					fm.w[attr_id] = w;
				}
			}
		}

		for (int f = 0; f < fm.num_factor; f++) {
			for (int i = 0; i < x_pos.getSize(); i++) {
				grad[x_pos.getData()[i].getId()] = 0;
				grad_visited[x_pos.getData()[i].getId()] = false;
			}

			for (int i = 0; i < x_neg.getSize(); i++) {
				grad[x_neg.getData()[i].getId()] = 0;
				grad_visited[x_neg.getData()[i].getId()] = false;
			}

			for (int i = 0; i < x_pos.getSize(); i++) {
				double oldvalue = grad[x_pos.getData()[i].getId()];
				grad[x_pos.getData()[i].getId()] = 
						oldvalue + sum_pos[f]
								* x_pos.getData()[i].getValue()
								- fm.v.get(f, x_pos.getData()[i].getId())
								* x_pos.getData()[i].getValue()
								* x_pos.getData()[i].getValue();
			}

			for (int i = 0; i < x_neg.getSize(); i++) {
				double oldvalue = grad[x_neg.getData()[i].getId()];
				grad[x_neg.getData()[i].getId()] = 
						oldvalue - sum_neg[f]
								* x_neg.getData()[i].getValue()
								- fm.v.get(f, x_neg.getData()[i].getId())
								* x_neg.getData()[i].getValue()
								* x_neg.getData()[i].getValue();
			}
			for (int i = 0; i < x_pos.getSize(); i++) {
				int attr_id = x_pos.getData()[i].getId();
				if (!grad_visited[attr_id]) {
					double v = fm.v.get(f, attr_id);
					v -= learn_rate
							* (multiplier * grad[attr_id] + fm.regv * v);
					grad_visited[attr_id] = true;
					fm.v.set(f, attr_id, v);
				}
			}
			for (int i = 0; i < x_neg.getSize(); i++) {
				int attr_id = x_neg.getData()[i].getId();
				if (!grad_visited[attr_id]) {
					double v = fm.v.get(f, attr_id);
					v -= learn_rate
							* (multiplier * grad[attr_id] + fm.regv * v);
					grad_visited[attr_id] = true;
					fm.v.set(f, attr_id, v);
				}
			}

		}
	}
}
