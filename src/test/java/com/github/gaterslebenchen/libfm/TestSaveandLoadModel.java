package com.github.gaterslebenchen.libfm;

import java.io.File;
import java.util.Arrays;
import java.util.Properties;

import com.github.gaterslebenchen.libfm.core.FmLearn;
import com.github.gaterslebenchen.libfm.core.FmLearnSgd;
import com.github.gaterslebenchen.libfm.core.FmLearnSgdElement;
import com.github.gaterslebenchen.libfm.core.FmModel;
import com.github.gaterslebenchen.libfm.data.DataProvider;
import com.github.gaterslebenchen.libfm.data.LibSVMDataProvider;
import com.github.gaterslebenchen.libfm.tools.Constants;
import com.github.gaterslebenchen.libfm.tools.TaskType;

import junit.framework.Assert;
import junit.framework.TestCase;

public class TestSaveandLoadModel extends TestCase{	
	private  double[] pred;
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		FmLearn fml;
		FmModel fm;
		fml = new FmLearnSgdElement();
		fm = new FmModel();
		
		fml.fm = fm;
		
		DataProvider train = new LibSVMDataProvider();
	    Properties trainproperties = new Properties();
	    trainproperties.put(Constants.FILENAME, "ratings_train.libfm");
	    train.load(trainproperties,false);
	    
	    DataProvider test = new LibSVMDataProvider();
        
        Properties testproperties = new Properties();
        testproperties.put(Constants.FILENAME, "ratings_test.libfm");
        test.load(testproperties,false);
        
        int num_all_attribute = Math.max(train.getFeaturenumber(), test.getFeaturenumber());
        
        fm.num_attribute = num_all_attribute;
        fm.initstdev = 0.1;
       
        fm.k0 = true;
        fm.k1 = true;
        fm.num_factor = 8;
       
        fm.init();
        
        ((FmLearnSgd)fml).num_iter = 100;
        
        fml.max_target = train.getMaxtarget();
        fml.min_target = train.getMintarget();
        fml.task = TaskType.TASK_REGRESSION;
        
        fm.reg0 = 0.0;
        fm.regw = 0.0;
        fm.regv = 0.1;
        
        ((FmLearnSgd)fml).learn_rate = 0.01;
        
        ((FmLearnSgd)fml).init();
        Arrays.fill(((FmLearnSgd)fml).learn_rates, ((FmLearnSgd)fml).learn_rate);
        
        fml.learn(train, test);
        
        Properties modelproperties = new Properties();
        modelproperties.put(Constants.FILENAME, "model.bin");
        
        pred = new double[test.getRownumber()];
        fml.predict(test, pred);
        
        fml.saveModel(modelproperties);
	}

	@Override
	protected void tearDown() throws Exception {
		File binfile = new File("model.bin");
		if(binfile.exists())
		{
			binfile.delete();
		}
		super.tearDown();
	}
	
	public void testModel()
	{
		try
		{
			FmLearn fml = new FmLearnSgdElement();
			FmModel fm = new FmModel();
			
			fml.fm = fm;
			
			Properties modelproperties = new Properties();
	        modelproperties.put(Constants.FILENAME, "model.bin");
	        
			fml.loadModel(modelproperties);

			DataProvider test = new LibSVMDataProvider();

			Properties testproperties = new Properties();
			testproperties.put(Constants.FILENAME, "ratings_test.libfm");
			
			test.load(testproperties,false);
			
			double[] pred_byload = new double[test.getRownumber()];
	        fml.predict(test, pred_byload); 
	        
	        assertTrue(Arrays.equals(pred, pred_byload));
		}
		catch(Exception e)
		{
			Assert.fail("Test failed : " + e.getMessage());
		}
	}
}
