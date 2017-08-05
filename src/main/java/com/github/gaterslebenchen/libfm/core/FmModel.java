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
import com.github.gaterslebenchen.libfm.data.SparseRow;
import com.github.gaterslebenchen.libfm.tools.Debug;

public class FmModel {
	public double[] m_sum, m_sum_sqr;
	public double w0;
	public double[] w;
	public DataPointMatrix v;
	public int num_attribute;
	public boolean k0, k1;
	public int num_factor;
	
	public double reg0;
	public double regw, regv;
	
	public double initstdev;
	public double initmean;
	
	public FmModel()
	{
		num_factor = 0;
		initmean = 0;
		initstdev = 0.01;
		reg0 = 0.0;
		regw = 0.0;
		regv = 0.0; 
		k0 = true;
		k1 = true;
	}
	
	public 	void debug()
	{
		Debug.println("num_attributes=" + num_attribute);
		Debug.println("use w0=" + k0);
		Debug.println("use w1=" + k1);
		Debug.println("dim v =" + num_factor);
		Debug.println("reg_w0=" + reg0);
		Debug.println("reg_w=" + regw);
		Debug.println("reg_v=" + regv); 
		Debug.println("init ~ N(" + initmean + "," + initstdev + ")");
	}
	
	public void init()
	{
		w0 = 0;
		w = new double[num_attribute];
		v = new DataPointMatrix(num_factor, num_attribute);
		Arrays.fill(w, 0);
		v.init(initmean, initstdev);
		m_sum = new double[num_factor];
		m_sum_sqr = new double[num_factor];
	}
	
	public double predict(SparseRow x)
	{
		return predict(x, m_sum, m_sum_sqr);
	}
	
	public double predict(SparseRow x, double[] sum, double[] sum_sqr)
	{
		double result = 0;
		if (k0) {	
			result += w0;
		}
		if (k1) {
			for (int i = 0; i < x.getSize(); i++) {
				result += w[x.getData()[i].getId()] * x.getData()[i].getValue();
			}
		}
		for (int f = 0; f < num_factor; f++) {
			sum[f] = 0;
			sum_sqr[f] = 0;
			for (int i = 0; i < x.getSize(); i++) {
				double d = v.get(f,x.getData()[i].getId()) * x.getData()[i].getValue();
				sum[f] = sum[f]+d;
				sum_sqr[f] = sum_sqr[f]+d*d;
			}
			result += 0.5 * (sum[f]*sum[f] - sum_sqr[f]);
		}

		return result;
	}
	
}
