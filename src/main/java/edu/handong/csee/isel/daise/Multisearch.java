package edu.handong.csee.isel.daise;

import java.io.Serializable;
import java.util.Enumeration;

import weka.classifiers.meta.MultiSearch;
import weka.classifiers.trees.J48;
import weka.core.Instances;
import weka.core.SetupGenerator;
import weka.core.setupgenerator.AbstractParameter;
import weka.core.setupgenerator.MathParameter;
import weka.core.Utils;
import weka.core.converters.ConverterUtils.DataSource;

public class Multisearch {
	
	public static void main(String[] args) throws Exception {
		String arffPath = args[0];
		System.out.println(arffPath);
		
		DataSource source = new DataSource(arffPath);
		Instances Data = source.getDataSet();
		Data.setClassIndex(0);
		System.out.println(Data.classAttribute());
		
		J48 j48 = new J48();

	    // configure generator
	    MathParameter conf = new MathParameter();
	    conf.setProperty("confidenceFactor");
	    conf.setBase(10);
	    conf.setMin(0.05);
	    conf.setMax(0.75);
	    conf.setStep(0.05);
	    conf.setExpression("I");
	    
	    MultiSearch multi = new MultiSearch();
	    multi.setClassifier(j48);
	    SetupGenerator generator = new SetupGenerator();
	    generator.setBaseObject(j48);
	    generator.setParameters(new AbstractParameter[]{
	      conf
	    });

	    // output configuration
	    System.out.println("\nSetupgenerator commandline:\n" + Utils.toCommandLine(generator));

	    // output commandlines
	    System.out.println("\nCommandlines:\n");
	    Enumeration<Serializable> enm = generator.setups();
	    while (enm.hasMoreElements())
	      System.out.println(Utils.toCommandLine(enm.nextElement()));
		
	    multi.buildClassifier(Data);
	}

}
