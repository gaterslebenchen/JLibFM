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
package com.github.gaterslebenchen.libfm.tools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.util.Random;

public class Util {
	public static double sqr(double d) {
		return Math.sqrt(d);
	}
	
	public static double sigmoid(double d)
	{
		return 1.0d/(1.0d+Math.exp(-d));
	}
	
	public static double erf(double x) {
		double t;
		if (x >= 0) {
			t = 1.0 / (1.0 + 0.3275911 * x);
		} else {
			t = 1.0 / (1.0 - 0.3275911 * x);
		}

		double result = 1.0 - (t * (0.254829592 + t * (-0.284496736 + t * (1.421413741 + t * (-1.453152027 + t * 1.061405429)))))*Math.exp(-x*x);
		if (x >= 0) {
			return result;
		} else {
			return -result;
		}
	}
	
	public static byte[] doubletobyte(double d)
	{
		 byte[] bytes = new byte[8];
		 ByteBuffer.wrap(bytes).putDouble(d);
		 return bytes;
	}
	
	public static double cdf_gaussian(double x, double mean, double stdev) {
		return 0.5 + 0.5 * erf(0.707106781 * (x-mean) / stdev);
	}
	
	public static double cdf_gaussian(double x) {
		return 0.5 + 0.5 * erf(0.707106781 * x );
	}
	
	public static double ran_left_tgaussian(double left) {
		// draw a trunctated normal: acceptance region are values larger than <left>
		if (left <= 0.0) { // acceptance probability > 0.5
			return ran_left_tgaussian_naive(left);
		} else {
			// Robert: Simulation of truncated normal variables
			double alpha_star = 0.5*(left + Math.sqrt(left*left + 4.0));

			// draw from translated exponential distr:
			// f(alpha,left) = alpha * exp(-alpha*(z-left)) * I(z>=left)
			double z,d,u;
			do {
				z = ran_exp() / alpha_star + left;
				d = z-alpha_star;
				d = Math.exp(-(d*d)/2);
				u = ran_uniform();
				if (u < d) {
					return z;
				}
			} while (true);
		}
	}
	
	public static String[] tokenize(String s, String delimiter){
		if (s == null) {
			return null;
		}
		int delimiterLength;
		int stringLength = s.length();
		if (delimiter == null || (delimiterLength = delimiter.length()) == 0){
			return new String[] {s};
		}

		// a two pass solution is used because a one pass solution would
		// require the possible resizing and copying of memory structures
		// In the worst case it would have to be resized n times with each
		// resize having a O(n) copy leading to an O(n^2) algorithm.

		int count;
		int start;
		int end;

		// Scan s and count the tokens.
		count = 0;
		start = 0;
		while((end = s.indexOf(delimiter, start)) != -1){
			count++;
			start = end + delimiterLength;
		}
		count++;

		// allocate an array to return the tokens,
		// we now know how big it should be
		String[] result = new String[count];

		// Scan s again, but this time pick out the tokens
		count = 0;
		start = 0;
		while((end = s.indexOf(delimiter, start)) != -1){
			result[count] = (s.substring(start, end));
			count++;
			start = end + delimiterLength;
		}
		end = stringLength;
		result[count] = s.substring(start, end);

		return (result);
	}
	
	public static double ran_left_tgaussian_naive(double left) {
		// draw a trunctated normal: acceptance region are values larger than <left>
		double result;
		do {
			result = ran_gaussian();
		} while (result < left);
		return result;
	}

	
	public static double ran_left_tgaussian(double left, double mean, double stdev) {
		return mean + stdev * ran_left_tgaussian((left-mean)/stdev); 
	}
	
	public static double ran_right_tgaussian(double right) {
		return -ran_left_tgaussian(-right);
	}
	
	public static double ran_right_tgaussian(double right, double mean, double stdev) {
		return mean + stdev * ran_right_tgaussian((right-mean)/stdev); 
	}

	public static double ran_gamma(double alpha) {
		assert(alpha > 0);
		if (alpha < 1.0) {
			double u;
			do {
				u = ran_uniform();
			} while (u == 0.0);
			return ran_gamma(alpha + 1.0) * Math.pow(u, 1.0 / alpha);
		} else {
			// Marsaglia and Tsang: A Simple Method for Generating Gamma Variables
			double d,c,x,v,u;
			d = alpha - 1.0/3.0;
			c = 1.0 / Math.sqrt(9.0 * d);
			do {
				do {
					x = ran_gaussian();
					v = 1.0 + c*x;
				} while (v <= 0.0);
				v = v * v * v;
				u = ran_uniform();
			} while ( 
				(u >= (1.0 - 0.0331 * (x*x) * (x*x)))
				 && (Math.log(u) >= (0.5 * x * x + d * (1.0 - v + Math.log(v))))
				 );
			return d*v;
		}
	}
	
	public static double ran_gamma(double alpha, double beta) {
		return ran_gamma(alpha) / beta;
	}
	
	public static double ran_gaussian() {
		// Joseph L. Leva: A fast normal Random number generator
		double u,v, x, y, Q;
		do {
			do {
				u = ran_uniform();
			} while (u == 0.0); 
			v = 1.7156 * (ran_uniform() - 0.5);
			x = u - 0.449871;
			y = Math.abs(v) + 0.386595;
			Q = x*x + y*(0.19600*y-0.25472*x);
			if (Q < 0.27597) { break; }
		} while ((Q > 0.27846) || ((v*v) > (-4.0*u*u*Math.log(u)))); 
		return v / u;
	}
	
	public static double ran_gaussian(double mean, double stdev) {
		if ((stdev == 0.0) || (Double.isNaN(stdev))) {
			return mean;
		} else {
			return mean + stdev*ran_gaussian();
		}
	}
	
	public static double ran_uniform() {
		Random rn = new Random();
		return rn.nextInt()/((double)Integer.MAX_VALUE + 1);
	}
	
	public static double ran_exp() {
		return -Math.log(1-ran_uniform());
	}
	
	public static boolean ran_bernoulli(double p) {
		return (ran_uniform() < p);
	}
	
	public static double getusertime()
	{
		long t1 = System.currentTimeMillis();
		return ((double)t1)/(1000d);
	}
	
	public static boolean fileexists(String filename)
	{
		File file = new File(filename);
		return file.exists();
	}
	
	public static void init_normal(double[] value, double mean, double stdev) {	
		for (int i_2 = 0; i_2 < value.length; i_2++) {
			value[i_2] = Util.ran_gaussian(mean, stdev);
		}
	}
	
	public static void save(double[] value, String filename) throws Exception {
		BufferedWriter outfile = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(filename)));

		for (int i = 0; i < value.length; i++) {
			outfile.write(Double.toString(value[i]));
			outfile.newLine();
		}
		outfile.flush();
		outfile.close();
	}

}
