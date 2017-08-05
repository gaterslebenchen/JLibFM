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

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * Logging into R compatible files
 */
public class RLog {
	private PrintWriter out = null;
	private List<String> header = new ArrayList<String>();
	private Map<String,Double> default_valuemap = new HashMap<String,Double>();
	private Map<String,Double> valuemap = new HashMap<String,Double>();
	
	public RLog(String filepath) throws Exception
	{
		out = new PrintWriter(new BufferedOutputStream(new FileOutputStream(filepath)));
	}
	
	public void log(String field, double d) {
		valuemap.put(field, d);
	}
	
	public void init() throws Exception{
		if (out != null) {
			for (int i = 0; i < header.size(); i++) {
				out.append(header.get(i));
				if (i < (header.size()-1)) {
					out.append("\t");
				} else {
					out.println();
				}
			}			
			out.flush();
		}
		for (int i = 0; i < header.size(); i++) {
			valuemap.put(header.get(i), default_valuemap.get(header.get(i)));	
		}
	}
	
	public void addField(String field_name, double def) {
		int ifind = header.indexOf(field_name);
		if (ifind!=-1) {
			throw new JlibfmRuntimeException("the field " + field_name + " already exists");
		}
		header.add(field_name);
		default_valuemap.put(field_name, def);
	}
	
	public void newLine() throws Exception{
		if (out != null) {
			for (int i = 0; i < header.size(); i++) {
				out.append(Double.toString(valuemap.get(header.get(i))));
				if (i < (header.size()-1)) {
					out.append("\t");
				} else {
					out.println();
				}
			}
			out.flush();
			valuemap.clear();
			for (int i = 0; i < header.size(); i++) {
				valuemap.put(header.get(i), default_valuemap.get(header.get(i)));	
			}
		}
	}
	
	
}
