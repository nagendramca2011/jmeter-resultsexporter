package org.soframel.jmeter.resultsexporter;

import java.awt.BorderLayout;

import javax.swing.Box;

import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.visualizers.gui.AbstractVisualizer;

/**
 * Vizualizer for the ResultExporter listener
 * @author soframel
 *
 */
public class ResultExporterVisualizer extends AbstractVisualizer{

	/**
	 * 
	 */
	private static final long serialVersionUID = -8828414474135550223L;

	public ResultExporterVisualizer(){	
		this.init();
	}
	
	private void init(){
		setLayout(new BorderLayout());
		setBorder(makeBorder());
		
		Box box = Box.createVerticalBox();
		box.add(makeTitlePanel());
		add(box,BorderLayout.NORTH);	
	}
	
	@Override
	public void configure(TestElement el) {		
		super.configure(el);	
	}

	@Override
	public void modifyTestElement(TestElement el) {
		super.modifyTestElement(el);
	}
	
	@Override
	public TestElement createTestElement() {
		TestElement el=new ResultExporterListener();
		this.modifyTestElement(el);
		return el;
	}

	public void add(SampleResult arg0) {
		
	}

	public void clearData() {
		
	}

	@Override
	public String getStaticLabel() {
		return "Results Exporter";
	}

	public String getLabelResource() {
		//use static label
		return null;
	}

}
