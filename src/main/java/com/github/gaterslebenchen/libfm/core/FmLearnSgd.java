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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Properties;

import com.github.gaterslebenchen.libfm.data.DataPointMatrix;
import com.github.gaterslebenchen.libfm.data.DataProvider;
import com.github.gaterslebenchen.libfm.data.SparseRow;
import com.github.gaterslebenchen.libfm.tools.Constants;
import com.github.gaterslebenchen.libfm.tools.Debug;
import com.github.gaterslebenchen.libfm.tools.JlibfmRuntimeException;
import com.github.gaterslebenchen.libfm.tools.TaskType;

public class FmLearnSgd extends FmLearn{
	protected double[] sum, sum_sqr;
	public int num_iter;
	public double learn_rate;
	public double[] learn_rates;
	
	public void init() {		
		super.init();	
		learn_rates = new double[3];
		sum = new double[fm.num_factor];
		sum_sqr = new double[fm.num_factor];
	}	
	
	public void learn(DataProvider train, DataProvider test) throws Exception{ 
		Debug.println("learnrate=" + learn_rate);
		Debug.println("learnrates=" + learn_rates[0] + "," + learn_rates[1] + "," + learn_rates[2]);
		Debug.println("#iterations=" + num_iter);
	}
	
	public void SGD(SparseRow x, double multiplier, double[] sum) {
		FmModelSGD.fm_SGD(fm, learn_rate, x, multiplier, sum); 
	}
	
	public void saveModel(Properties properties) throws Exception
	{
		FileOutputStream fos = null;
		DataOutputStream dos = null;	    
	    try {	  
	    	fos = new FileOutputStream(properties.getProperty(Constants.FILENAME));
	        dos = new DataOutputStream(fos);
	        dos.writeBoolean(fm.k0);
	        dos.writeBoolean(fm.k1);
	        dos.writeDouble(fm.w0);
	        dos.writeInt(fm.num_factor);
	        dos.writeInt(fm.num_attribute);
	        dos.writeInt(task.ordinal());
	        dos.writeDouble(max_target);
	        dos.writeDouble(min_target);
	        
	        for(int i=0;i<fm.num_attribute;i++)
	        {
	        	dos.writeDouble(fm.w[i]);
	        }
	        
	        for(int i=0;i<fm.num_factor;i++)
	        {
	        	dos.writeDouble(fm.m_sum[i]);
	        }
	        
	        for(int i=0;i<fm.num_factor;i++)
	        {
	        	dos.writeDouble(fm.m_sum_sqr[i]);
	        }
	        
	        for (int i_1 = 0; i_1 < fm.num_factor; i_1++) {
				for (int i_2 = 0; i_2 < fm.num_attribute; i_2++) {					
					dos.writeDouble(fm.v.get(i_1,i_2));
				}
			}
	        
	        dos.flush();
	    }
	    catch(Exception e) {
	        throw new JlibfmRuntimeException(e);
	    } finally {          
	         if(dos!=null)
	            dos.close();
	         if(fos!=null)
	            fos.close();
	    }
	}
	
	public void loadModel(Properties properties) throws Exception
	{
		InputStream is = null;
	    DataInputStream dis = null;	    
	    try {	  
	    	is = new FileInputStream(properties.getProperty(Constants.FILENAME));          
	        dis = new DataInputStream(is);
	        
	        fm.k0 = dis.readBoolean();
	        fm.k1 = dis.readBoolean();
	        fm.w0 = dis.readDouble();
	        fm.num_factor = dis.readInt();
	        fm.num_attribute = dis.readInt();
	        
	        if(dis.readInt() == 0)
	        {
	        	task = TaskType.TASK_REGRESSION;
	        }
	        else
	        {
	        	task = TaskType.TASK_CLASSIFICATION;
	        }
	       
	        max_target = dis.readDouble();
	        min_target = dis.readDouble();
	        
	        fm.w = new double[fm.num_attribute];
	        
	        for(int i=0;i<fm.num_attribute;i++)
	        {
	        	fm.w[i] = dis.readDouble();
	        }
	        
	        fm.m_sum = new double[fm.num_factor];
	        fm.m_sum_sqr = new double[fm.num_factor];
			
	        for(int i=0;i<fm.num_factor;i++)
	        {
	        	fm.m_sum[i] = dis.readDouble();
	        }
	        
	        for(int i=0;i<fm.num_factor;i++)
	        {
	        	fm.m_sum_sqr[i] = dis.readDouble();
	        }
	        
	        fm.v = new DataPointMatrix(fm.num_factor, fm.num_attribute);
	        
	        for (int i_1 = 0; i_1 < fm.num_factor; i_1++) {
				for (int i_2 = 0; i_2 < fm.num_attribute; i_2++) {		
					fm.v.set(i_1,i_2, dis.readDouble());
				}
			}
	        
	    }
	    catch(Exception e) {
	        throw new JlibfmRuntimeException(e);
	    } finally {          
	         if(dis!=null)
	            dis.close();
	         if(is!=null)
	            is.close();
	    }
	}
	
	public void debug() {
		Debug.println("num_iter=" + num_iter);
		super.debug();			
	}
	
	public void predict(DataProvider data, double[] out) throws Exception{
		assert(data.getData().getNumRows() == out.length);
		for (data.getData().begin(); !data.getData().end(); data.getData().next()) {
			double p = predict_case(data);
			if (task == TaskType.TASK_REGRESSION ) {
				p = Math.min(max_target, p);
				p = Math.max(min_target, p);
			} else if (task == TaskType.TASK_CLASSIFICATION) {
				p = 1.0/(1.0 + Math.exp(-p));
			} else {
				throw new JlibfmRuntimeException("task not supported" + task);
			}
			out[data.getData().getRowIndex()] = p;
		}				
	} 
}
