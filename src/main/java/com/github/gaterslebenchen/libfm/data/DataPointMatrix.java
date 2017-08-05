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
package com.github.gaterslebenchen.libfm.data;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import com.github.gaterslebenchen.libfm.tools.Util;

public class DataPointMatrix {
	private double[][] value = null;
	private List<String> col_names = null;
	private int dim1, dim2;

	public double get(int x, int y) {
		return value[x][y];
	}
	
	public void set(int x, int y, double val) {
		value[x][y] = val;
	}

	public double[] getArray(int x) {
		return value[x];
	}

	public DataPointMatrix(int p_dim1, int p_dim2) {
		dim1 = 0;
		dim2 = 0;
		value = null;
		setSize(p_dim1, p_dim2);
	}

	public DataPointMatrix() {
		dim1 = 0;
		dim2 = 0;
		value = null;
	}

	public void assign(DataPointMatrix v) {
		if ((v.dim1 != dim1) || (v.dim2 != dim2)) {
			setSize(v.dim1, v.dim2);
		}
		for (int i = 0; i < dim1; i++) {
			for (int j = 0; j < dim2; j++) {
				value[i][j] = v.value[i][j];
			}
		}
	}

	public void init(double v) {
		for (int i = 0; i < dim1; i++) {
			for (int i2 = 0; i2 < dim2; i2++) {
				value[i][i2] = v;
			}
		}
	}

	public void setSize(int p_dim1, int p_dim2) {
		if ((p_dim1 == dim1) && (p_dim2 == dim2)) {
			return;
		}
		dim1 = p_dim1;
		dim2 = p_dim2;
		value = new double[dim1][dim2];
		col_names = new ArrayList<String>();
		for (int i = 1; i < dim2; i++) {
			col_names.add("");
		}
	}

	public double[][] getValue() {
		return value;
	}

	public void setValue(double[][] value) {
		this.value = value;
	}

	public void save(String filename) throws Exception {
		save(filename, false);
	}

	public void save(String filename, boolean has_header) throws Exception {
		BufferedWriter outfile = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(filename)));

		if (has_header) {
			for (int i_2 = 0; i_2 < dim2; i_2++) {
				if (i_2 > 0) {
					outfile.write("\t");
				}
				outfile.write(col_names.get(i_2));
			}
			outfile.newLine();
		}
		for (int i_1 = 0; i_1 < dim1; i_1++) {
			for (int i_2 = 0; i_2 < dim2; i_2++) {
				if (i_2 > 0) {
					outfile.write("\t");
				}
				outfile.write(Double.toString(value[i_1][i_2]));
			}
			outfile.newLine();
		}
		outfile.flush();
		outfile.close();
	}
	
	public void load(String filename)  throws Exception{			
		Reader fr = new FileReader(filename);
		BufferedReader br = new BufferedReader(fr);
		String firstline;
		int i_1 = 0;
		while (br.ready()) {
			firstline = br.readLine();
			String[] arr = Util.tokenize(firstline, "\t");
			for(int j=0;j<arr.length;j++)
			{
				value[i_1][j] = Double.parseDouble(arr[j]);
			}
			i_1++;
		}
		br.close();
		fr.close();		
	} 	
	
	public void init(double mean, double stdev) {	
		for (int i_1 = 0; i_1 < dim1; i_1++) {
			for (int i_2 = 0; i_2 < dim2; i_2++) {
				value[i_1][i_2] = Util.ran_gaussian(mean, stdev);
			}
		}
	}
	
	public void init_column(double mean, double stdev, int column) {	
		for (int i_1 = 0; i_1 < dim1; i_1++) {
			value[i_1][column] = Util.ran_gaussian(mean, stdev);
		}
	}
}
