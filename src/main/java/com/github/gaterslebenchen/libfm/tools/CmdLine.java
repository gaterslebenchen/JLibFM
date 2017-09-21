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
/**
 * a command line arguments parsing tool
 * 
 * @author Jinbo Chen
 * 
 */
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CmdLine {
	private Map<String,String> value = new HashMap<String,String>();
	private Map<String,String> help = new HashMap<String,String>();
	private static final String delimiter = ",";
	
	public CmdLine(String[] argv)
	{
		
		int i = 0;
		while (i < argv.length) {
			String s = argv[i];
			List<String> parameters = new ArrayList<String>();
			for(char c: s.toCharArray())
			{
				parameters.add(String.valueOf(c));
			}
			if (parseName(parameters)) {
				StringBuffer sb = new StringBuffer();
				for(String parameter:parameters)
				{
					sb.append(parameter);
				}
				if (value.containsKey(sb.toString())) {
					throw new JlibfmRuntimeException("the parameter " + sb.toString() + " is already specified"); 							
				}
				if ((i+1) < argv.length) {
					String snext = argv[i+1];
					List<String> values = new ArrayList<String>();
					for(char c: snext.toCharArray())
					{
						values.add(String.valueOf(c));
					}
					if (! parseName(values)) {
						StringBuffer sbvalue = new StringBuffer();
						for(String value:values)
						{
							sbvalue.append(value);
						}
						value.put(sb.toString(), sbvalue.toString());
						i++;
					} else {
						value.put(sb.toString(), "");
					}
				} else {
					value.put(sb.toString(), "");
				}
			} else {
				throw new JlibfmRuntimeException("cannot parse " + s);
			}
			i++;
		}

	}
	
	public void setValue(String parameter, String valuestr) {
		value.put(parameter, valuestr);
	}
	
	public boolean hasParameter(String parameter) {
		return value.containsKey(parameter);
	}
	
	public String registerParameter(String parameter, String helpstr) {
		help.put(parameter, helpstr);
		return parameter;
	}
	
	public void checkParameters() {
		// make sure there is no parameter specified on the cmdline that is not registered:
		Set<String> keyset = value.keySet();
		for(String key:keyset)
		{
			if(!help.containsKey(key))
			{
				throw  new JlibfmRuntimeException("the parameter " + key + " does not exist");
			}
		}
	}
	
	public String getValue(String parameter) {
		return value.get(parameter);
	}
	
	public String getValue(String parameter, String default_value) {
		if (hasParameter(parameter)) {
			return value.get(parameter);
		} else {
			return default_value;
		}
	}
	
	public double getValue(String parameter, double default_value) {
		if (hasParameter(parameter)) {
			return Double.parseDouble(value.get(parameter));
		} else {
			return default_value;
		}
	}
	
	public int getValue(String parameter, int default_value) {
		if (hasParameter(parameter)) {
			return Integer.parseInt(value.get(parameter));
		} else {
			return default_value;
		}
	}
	
	public String[] getStrValues(String parameter) {
		return Util.tokenize(value.get(parameter), delimiter);
	}
	
	public Integer[] getIntValues(String parameter) {
		Integer[] result = null;
		String[] strresult = Util.tokenize(value.get(parameter), delimiter);
		if(strresult!=null && strresult.length>0)
		{
			result = new Integer[strresult.length];
			for(int i=0;i<strresult.length;i++)
			{
				result[i] = Integer.parseInt(strresult[i]);
			}
		}
		return result;
	}
	
	public Double[] getDblValues(String parameter) {
		Double[] result = null;
		String[] strresult = Util.tokenize(value.get(parameter), delimiter);
		if(strresult!=null && strresult.length>0)
		{
			result = new Double[strresult.length];
			for(int i=0;i<strresult.length;i++)
			{
				result[i] = Double.parseDouble(strresult[i]);
			}
		}
		else
		{
			result = new Double[0];
		}
		return result;
	}
	
	public void printHelp() {
		Set<String> keyset = help.keySet();
		for(String key:keyset)
		{
			System.out.print("-");
			System.out.print(key);
			for(int i=key.length()+1;i<16;i++)
			{
				System.out.print(" ");
			}
			String helpstr = help.get(key);
			if(helpstr.length()>0)
			{
				System.out.println(helpstr); 
			}
		}
	}
	
	private boolean parseName(List<String> s)
	{
		if(s.size()>0 && s.get(0).equals("-"))
		{
			if(s.size()>1 && s.get(1).equals("-"))
			{
				s.remove(0);
				s.remove(0);
			}
			else
			{
				s.remove(0);
			}
			return true;
		}
		else
		{
			return false;
		}
	}
}