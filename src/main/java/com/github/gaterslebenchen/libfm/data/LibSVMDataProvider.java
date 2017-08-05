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
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import com.github.gaterslebenchen.libfm.tools.Constants;
import com.github.gaterslebenchen.libfm.tools.Debug;
import com.github.gaterslebenchen.libfm.tools.Util;

public class LibSVMDataProvider implements DataProvider {
	private double[] target;
	private int featurenum = 0;
	private int valuenum = 0;
	private int rownum = 0;
	private double mintarget;
	private double maxtarget;
	private LargeSparseMatrix sparsetransposematrix;
	private LargeSparseMatrix sparsematrix;

	@Override
	public void load(Properties properties, boolean loadttransposedata) throws Exception{
		sparsematrix = new LargeSparseDoubleMatrixMem();
		SparseRowVector datacontainer = new SparseRowVector();
		sparsematrix.data = datacontainer;

		mintarget += Double.MAX_VALUE;
		maxtarget -= Double.MAX_VALUE;
		
		boolean has_feature = false;
		String filename = properties.getProperty(Constants.FILENAME);

		// (1) determine the number of rows and the maximum feature_id
		try {
			Reader fr = new FileReader(filename);
			BufferedReader br = new BufferedReader(fr);
			String line;
			double _value;
			int _feature;
			while (br.ready()) {
				line = br.readLine().trim();
				if (line.length() == 0 || line.startsWith("#")) {
					// skip empty rows
					continue;
				}
				String[] arr = Util.tokenize(line, " ");
				if (arr.length > 1) {
					_value = Double.parseDouble(arr[0]);
					mintarget = Math.min(_value, mintarget);
					maxtarget = Math.max(_value, maxtarget);
					rownum++;
					for (int i = 1; i < arr.length; i++) {
						int position = arr[i].indexOf(":");
						if (position > 0) {
							_feature = Integer.parseInt(arr[i].substring(0, position));
							featurenum = Math.max(_feature, featurenum);
							has_feature = true;
							valuenum++;
						} else {
							break;
						}
					}
				}
			}
			br.close();
			fr.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if (has_feature) {	
			featurenum++; // number of feature is bigger (by one) than the largest value
		}
		
		Debug.println("num_rows=" + rownum + "\tnum_values=" + valuenum + "\tnum_features=" + featurenum + "\tmin_target=" + mintarget + "\tmax_target=" + maxtarget);
		
		datacontainer.setSize(rownum);
		target = new double[rownum];
		
		sparsematrix.num_cols = featurenum;
		sparsematrix.num_values = valuenum;
		
		// (2) read the data
		Reader fr = new FileReader(filename);
		BufferedReader br = new BufferedReader(fr);
		try
		{
			String line;
			double tmpvalue;
			int tmpfeatureid;
			int rowid = 0;
			
			while (br.ready()) {
				line = br.readLine().trim();
				if(line.length()==0 || line.startsWith("#"))
				{
					 // skip empty rows
					continue;
				}
				String[] arr = Util.tokenize(line, " ");
				if(arr.length>1)
				{
					tmpvalue = Double.parseDouble(arr[0]);
					target[rowid] =  tmpvalue;
					
					List<SparseEntry> datalist = new ArrayList<SparseEntry>();
					
					for(int i=1;i<arr.length;i++)
					{
						int ipos = arr[i].indexOf(":");
						if(ipos>0)
						{
							tmpfeatureid = Integer.parseInt(arr[i].substring(0, ipos));
							datalist.add(new SparseEntry(tmpfeatureid,Double.parseDouble(arr[i].substring(ipos+1))));
						}
						else
						{
							break;
						}
					}
					datacontainer.value[rowid] = new SparseRow(datalist.toArray(new SparseEntry[datalist.size()]));
					rowid++;
				}
			}	
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if(br!=null)
			{
				br.close();
			}
		}
		
		if (loadttransposedata) {createtransposedata();}
	}
	
	private void createtransposedata() {
		// for creating transpose data, the data has to be memory-data because we use random access
		SparseRowVector datacontainer = sparsematrix.data;

		sparsetransposematrix = new LargeSparseDoubleMatrixMem();

		// make transpose copy of training data
		SparseRowVector datatcontainer = new SparseRowVector(featurenum);
		sparsetransposematrix.data = datatcontainer;
			
		// find dimensionality of matrix
		int[] num_values_per_column = new int[featurenum];
		int num_values = 0;
		for (int i = 0; i < datacontainer.dim; i++) {
			for (int j = 0; j < datacontainer.get(i).getSize(); j++) {
				num_values_per_column[datacontainer.get(i).getData()[j].getId()] = 1 + num_values_per_column[datacontainer.get(i).getData()[j].getId()];
				num_values++;
			}
		}	


		sparsetransposematrix.num_cols = datacontainer.dim;
		sparsetransposematrix.num_values = num_values;

		// create data structure for values			
		
		for (int i = 0; i < datatcontainer.dim; i++) {
			datatcontainer.value[i] = new SparseRow(new SparseEntry[num_values_per_column[i]]);
		} 
		// write the data into the transpose matrix
		Arrays.fill(num_values_per_column, 0);
		// num_values per column now contains the pointer on the first empty field
		for (int i = 0; i < datacontainer.dim; i++) {
			for (int j = 0; j < datacontainer.get(i).getSize(); j++) {
				int f_id = datacontainer.get(i).getData()[j].getId();
				int cntr = num_values_per_column[f_id];
				datatcontainer.get(f_id).getData()[cntr] = new SparseEntry(i,datacontainer.get(i).getData()[j].getValue());
				num_values_per_column[f_id] = num_values_per_column[f_id]+1;
			}
		}
	}
	
	
	public void shuffle()
	{
		int n = getData().getNumRows();
        for (int i = 0; i < n; i++) {
            int r = (int) (Math.random() * (i + 1));
            SparseRow swapobj = getData().getRow(r);
            double swaptarget = getTarget()[r];
            getData().setRow(r, getData().getRow(i));
            getData().setRow(i, swapobj);
            getTarget()[r] = getTarget()[i];
            getTarget()[i] = swaptarget;
        }
	}

	@Override
	public double getMaxtarget() {
		return maxtarget;
	}

	@Override
	public double getMintarget() {
		return mintarget;
	}

	@Override
	public double[] getTarget() {
		return target;
	}

	@Override
	public int getFeaturenumber() {
		return featurenum;
	}

	@Override
	public int getRownumber() {
		return rownum;
	}

	@Override
	public int getValuenumber() {
		return valuenum;
	}

	@Override
	public LargeSparseMatrix getData() {
		return sparsematrix;
	}

	@Override
	public LargeSparseMatrix getTransposedata() {
		return sparsetransposematrix;
	}
}
