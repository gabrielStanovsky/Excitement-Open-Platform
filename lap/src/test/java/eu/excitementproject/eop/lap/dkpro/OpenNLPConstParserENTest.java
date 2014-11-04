package eu.excitementproject.eop.lap.dkpro;

import static org.junit.Assert.*;

import org.apache.uima.jcas.JCas;
import org.junit.Test;

import eu.excitementproject.eop.lap.LAPAccess;
import eu.excitementproject.eop.lap.LAPException;
import eu.excitementproject.eop.lap.PlatformCASProber;
import eu.excitementproject.eop.lap.dkpro.OpenNLPConstParserEN;

public class OpenNLPConstParserENTest {

	@Test
	public void test() {
		
		LAPAccess lap = null; 
		JCas aJCas = null; 

		// Generating a Single CAS 
		try {
			lap = new OpenNLPConstParserEN(); 
			
			// one of the LAPAccess interface: that generates single TH CAS. 
			aJCas = lap.generateSingleTHPairCAS("Bush used his weekly radio address to try to build support for his plan to allow workers to divert part of their Social Security payroll taxes into private investment accounts", "Mr. Bush is proposing that workers be allowed to divert their payroll taxes into private accounts."); 

			PlatformCASProber.probeCas(aJCas, null); 
			
			// If you want to check the result of the parser, please 
			// uncomment the following and check the newly generated file. 
			// (will be at /lap/openNLP_parse_test.casdump.txt). 
			//PlatformCASProber.dumpJCasToTextFile(aJCas, "openNLP_parse_test.casdump.txt"); 
			

		}
		catch(LAPException e)
		{
			fail(e.getMessage()); 
		}

//		// process TE data format, and produce XMI files.
//		// Let's process English RTE3 data (formatted as RTE5+) as an example. 
//		File input = new File("./src/test/resources/small.xml"); // this only holds the first 3 of them.. generate 3 XMIs (first 3 of t.xml) 
//		//File input = new File("./src/test/resources/t.xml");  // this is full, and will generate 800 XMIs (serialized CASes)
//		File outputDir = new File("./target/"); 
//		try {
//			lap.processRawInputFormat(input, outputDir); // outputDir will have those XMIs
//		} catch (LAPException e)
//		{
//			fail(e.getMessage()); 
//		}

		// Now time to open up the XMI files. 
		// PlatformCASPRober also provides a probe method 
		// for XMI files: probeXmi() --- this does the same thing 
		// of probeCas(), but on XMI. 
//		File testXmi = new File("./target/3.xmi"); // you can pick and probe any XMI..  
//		try {
//			PlatformCASProber.probeXmi(testXmi, System.out);
//		} catch (LAPException e) {
//			fail(e.getMessage()); 
//		} 

	}

}
