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

import com.github.gaterslebenchen.libfm.tools.Util;

public class SparseRowVector {
	public int dim;
	public SparseRow[] value = null;

	public SparseRowVector() {
		dim = 0;
	}

	public SparseRowVector(int p_dim) {
		dim = 0;
		setSize(p_dim);
	}

	public SparseRow get(int x) {
		return value[x];
	}
	
	public void set(int x,SparseRow v) {
		value[x] = v;
	}

	public void setSize(int p_dim) {
		if (p_dim == dim) {
			return;
		}
		if (value != null) {
			value = null;
		}
		dim = p_dim;
		value = new SparseRow[dim];
	}

	public void init(SparseRow v) {
		for (int i = 0; i < dim; i++) {
			value[i] = v;
		}
	}

	public void assign(SparseRow[] v) {
		if (v.length != dim) {
			setSize(v.length);
		}
		for (int i = 0; i < dim; i++) {
			value[i] = v[i];
		}
	}

	public void assign(SparseRowVector v) {
		if (v.dim != dim) {
			setSize(v.dim);
		}
		for (int i = 0; i < dim; i++) {
			value[i] = v.value[i];
		}
	}

	public void save(String filename) throws Exception {
		BufferedWriter outfile = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(filename)));

		for (int i = 0; i < dim; i++) {
			StringBuilder sb = new StringBuilder();
			sb.append(value[i].getSize()+" ");
			for(int j=0;j<value[i].getSize();j++)
			{
				sb.append(value[i].getData()[j].getId()+" "+value[i].getData()[j].getValue()+" ");
			}
			outfile.write(sb.toString());
			outfile.newLine();
		}
		outfile.flush();
		outfile.close();
	}

	public void load(String filename) throws Exception {
		Reader fr = new FileReader(filename);
		BufferedReader br = new BufferedReader(fr);
		String firstline;
		int i = 0;
		while (br.ready()) {
			firstline = br.readLine();
			String[] arr = Util.tokenize(firstline, " ");
			SparseEntry[] data = new SparseEntry[Integer.parseInt(arr[0])];
			for(int j=0;j<Integer.parseInt(arr[0]);j++)
			{
				data[j] = new SparseEntry(Integer.parseInt(arr[2*j+1]),Double.parseDouble(arr[2*j+2]));
			}
			value[i] = new SparseRow(data);
		}
		br.close();
		fr.close();
	}
}
