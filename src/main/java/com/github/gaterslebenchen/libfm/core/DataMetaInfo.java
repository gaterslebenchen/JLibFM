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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Reader;
import java.util.Arrays;

public class DataMetaInfo {
	public int[] attr_group; // attribute_id -> group_id
	public int num_attr_groups;
	public int[] num_attr_per_group;
	
	public DataMetaInfo(int num_attributes) {
		attr_group = new int[num_attributes];
		Arrays.fill(attr_group, 0);
		num_attr_groups = 1;
		num_attr_per_group = new int[num_attr_groups];
		num_attr_per_group[0] = num_attributes;
	}
	
	public void loadGroupsFromFile(String filename) throws Exception {
		load(filename);
		num_attr_groups = 0;
		for (int i = 0; i < attr_group.length; i++) {
			num_attr_groups = Math.max(num_attr_groups, attr_group[i]+1);
		}
		num_attr_per_group = new int[num_attr_groups];
		Arrays.fill(num_attr_per_group, 0);
		for (int i = 0; i < attr_group.length; i++) {		
			num_attr_per_group[attr_group[i]] = num_attr_per_group[attr_group[i]]+1;
		}
	}
	
	public void debug() {
		System.out.println("#attr=" + attr_group.length + "\t#groups=" + num_attr_groups);
		for (int g = 0; g < num_attr_groups; g++) {
			System.out.println("#attr_in_group[" + g + "]=" + num_attr_per_group[g]);
		}
	}
	
	private void load(String filename) throws Exception {
		Reader fr = new FileReader(filename);
		BufferedReader br = new BufferedReader(fr);
		String firstline;
		int i = 0;
		while (br.ready()) {
			firstline = br.readLine();
			attr_group[i] = Integer.parseInt(firstline);
		}
		br.close();
		fr.close();
	}
}
