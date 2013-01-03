package org.soframel.jmeter.resultsexporter;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

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

	
	private JCheckBox popupsBeforeTestCheckbox;
	private JButton exportNowButton;
	private JCheckBox fullscreenBeforeExportCheckbox;
	
	public ResultExporterVisualizer(){	
		this.init();
	}
	
	private void init(){
		setLayout(new BorderLayout());
		setBorder(makeBorder());
		
		Box box = Box.createVerticalBox();
		box.add(makeTitlePanel());
		box.setAlignmentX(Component.LEFT_ALIGNMENT);
		box.setAlignmentY(Component.TOP_ALIGNMENT);
		add(box,BorderLayout.NORTH);
		
		Box box1=Box.createHorizontalBox();
		box1.setAlignmentX(Component.LEFT_ALIGNMENT);
		box.add(box1,BorderLayout.WEST);
		JLabel label=new JLabel(ResultExporterListener.getResource("configuration.popupsBeforeTest.label"));
		box1.add(label);		
		popupsBeforeTestCheckbox=new JCheckBox();
		popupsBeforeTestCheckbox.setSelected(false);
		box1.add(popupsBeforeTestCheckbox);		
		
		Box box2=Box.createHorizontalBox();
		box2.setAlignmentX(Component.LEFT_ALIGNMENT);
		box.add(box2,BorderLayout.WEST);
		JLabel labelfull=new JLabel(ResultExporterListener.getResource("configuration.fullscreenBeforeExport.label"));
		box2.add(labelfull);
		fullscreenBeforeExportCheckbox=new JCheckBox();
		fullscreenBeforeExportCheckbox.setSelected(false);
		box2.add(fullscreenBeforeExportCheckbox);
		
		exportNowButton=new JButton(ResultExporterListener.getResource("configuration.exportNowButton.label"));
		exportNowButton.setActionCommand(ResultExporterListener.EXPORTNOW_COMMAND);
		exportNowButton.setAlignmentX(Component.LEFT_ALIGNMENT);
		box.add(exportNowButton,BorderLayout.WEST);
		
		box.add(Box.createVerticalGlue(), BorderLayout.SOUTH);
		add(Box.createVerticalGlue(), BorderLayout.SOUTH);
	}
	
	@Override
	public void configure(TestElement el) {		
		super.configure(el);
		if(el instanceof ResultExporterListener){
			ResultExporterListener listener=(ResultExporterListener) el;
			
			if(exportNowButton.getActionListeners().length==0)
				exportNowButton.addActionListener(listener);
		
			popupsBeforeTestCheckbox.setSelected((listener).isPopupsBeforeTest());
			fullscreenBeforeExportCheckbox.setSelected(listener.isFullscreenBeforeExport());
		}
	}

	@Override
	public void modifyTestElement(TestElement el) {
		super.modifyTestElement(el);
		if(el instanceof ResultExporterListener){
			ResultExporterListener listener=(ResultExporterListener) el;
			listener.setPopupsBeforeTest(popupsBeforeTestCheckbox.isSelected());
			listener.setFullscreenBeforeExport(fullscreenBeforeExportCheckbox.isSelected());
		}
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
