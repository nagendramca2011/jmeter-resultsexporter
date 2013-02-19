package org.soframel.jmeter.resultsexporter;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.apache.commons.io.FileUtils;
import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.gui.JMeterGUIComponent;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.reporters.ResultCollector;
import org.apache.jmeter.save.SaveGraphicsService;
import org.apache.jmeter.services.FileServer;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.threads.ThreadGroup;
import org.apache.jmeter.visualizers.Printable;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

/**
 * Exports result into a given directory. A pop-up is shown at the end of a test
 * asking if results should be saved. If yes, another pop-up asks for an input
 * directory. The CSV file is copied in this directory, as well as images of all TestListeners 
 * and of the ThreadGroup.  
 * 
 * @author soframel
 * 
 */
public class ResultExporterListener extends ResultCollector implements ActionListener{

	private static AtomicBoolean exporting=new AtomicBoolean(false);
	
	private static final long serialVersionUID = -3408497844654656634L;

	private static final Logger log = LoggingManager.getLoggerForClass(); 
	
	public final static String EXPORTNOW_COMMAND="exportNow";
	private final static String RESOURCES_FILENAME="/resultsexporter.properties";
	private static Properties resources;
	
	public final static String POPUPSBEFORETEST_PROPERY="popupsBeforeTest";
	public final static String DOEXPORT_PROPERY="doExportNow";
	public final static String FULLSCREENBEFOREEXPORT_PROPERTY="fullscreenBeforeExport";
	
	private String exportDirname;
	private String rootFilename;
	
	public ResultExporterListener() {
	}
	
	static{
		loadResources();
	}
	
	public static void loadResources(){
		InputStream in=ResultExporterListener.class.getResourceAsStream(RESOURCES_FILENAME);
		resources=new Properties();
		try {
			resources.load(in);
		} catch (IOException e) {
			log.error("IOException while loading resources file "+RESOURCES_FILENAME+": "+e.getMessage(), e);
		}
	}
	public static String getResource(String key){
		return resources.getProperty(key);
	}

	@Override
	public void testStarted() {
		// delete main file in root dir for each test, because it is reused
		File f = new File(this.getFilename());
		boolean deleted=f.delete();
		if(!deleted)
			log.error("Could not delete output file "+this.getFilename()+": data file may contain too much entries!");
		//TODO: reproduce bug when file was not deleted - it contained too many entries !
		//idea: write the file in a subdirectory, or with a name containing a timestamp, to be sure
		
		//test if configuration should be asked
		if(this.isPopupsBeforeTest())
			this.prepareExport();
		
		super.testStarted();
	}

	@Override
	public void testEnded() {
		super.testEnded();
		this.runExport();
	}
	
	public void runExport(){
		if(!this.isPopupsBeforeTest())
			this.prepareExport();
		if(this.getExportDirname()!=null && !this.getExportDirname().equals(""))
			this.doExport();
	}
	
	public void prepareExport(){
		// show popup to choose if you want to save your tests
		int result = JOptionPane.showConfirmDialog(null,resources.getProperty("askexport.message"), resources.getProperty("askexport.title"),
				JOptionPane.YES_NO_OPTION);
		if (result == JOptionPane.YES_OPTION) {
			log.debug("ResultExporterListener: exporting");

			// Find root directory
			String filename=this.getFilename();			
			filename = FileServer.resolveBaseRelativeName(filename);
			
			log.debug("Filename after resolving=" + filename);
			File rootFile = new File(filename);
			File rootDir = rootFile.getParentFile();

			this.setRootFilename(filename);
			
			////Directory of test
			String inputValue = JOptionPane.showInputDialog(resources.getProperty("testname.message"));
			if (inputValue == null || inputValue.equals("")
					|| inputValue.trim().equals("")) {
				inputValue = "default";
			} else
				inputValue = inputValue.trim();
			File exportDir = new File(rootDir, inputValue);
			
			////if directory exists: ask if we continue
			if(exportDir.exists()){
				int delete = JOptionPane.showConfirmDialog(null,
						resources.getProperty("direxists.message"), resources.getProperty("direxists.title"),
						JOptionPane.YES_NO_OPTION);
				if(delete==JOptionPane.YES_OPTION){
					try {
						FileUtils.deleteDirectory(exportDir);
					} catch (IOException e) {
						log.error("IOException while deleting directory "+exportDir.getAbsolutePath(), e);
					}
				}
				else //do not continue
					return;
			}
			
			this.setExportDirname(exportDir.getPath());
		}
	}
	
	/**
	 * Export all the test results
	 */
	private void doExport() {			
		log.debug("ResultExporterListener: exportAll called");
		log.debug("ResultExporter: name="+this.getName());

		if(exporting.get())
			log.warn("Not exporting because an export operation is currenty in progress");
		else{
			exporting.set(true);	
			
			try{
				//change cursor
				GuiPackage.getInstance().getMainFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				
				//get configured directory and rootFile
				String rootFilename=this.getRootFilename();
				File rootFile=new File(rootFilename);
				String exportDirname=this.getExportDirname();
				File exportDir=new File(exportDirname);
				
				////create dir
				if (!exportDir.exists())
					exportDir.mkdirs();			
	
				//// copy main CSV/XML file
				File mainFile = new File(exportDir, rootFile.getName());
				try {
					FileUtils.copyFile(rootFile, mainFile);
				} catch (IOException e) {
					log.error("An IOException occured while copying main file: "+ e.getMessage(), e);
				}
	
				////Find other plugins and export images
				//extend window to take screenshots in fullscreen
				if(this.getPropertyAsBoolean(FULLSCREENBEFOREEXPORT_PROPERTY))
					GuiPackage.getInstance().getMainFrame().setExtendedState(JFrame.MAXIMIZED_BOTH);
	
				//Find ThreadGroup from tree
				JTree tree=GuiPackage.getInstance().getMainFrame().getTree();	
				JMeterTreeNode tgNode=this.getThreadGroupTreeNode(tree);
			
				//open once every TestElement			
				this.showAllTestElements(exportDir, tree, tgNode);
				
				//export images		
				log.info("exporting nodes' images");
				this.exportAllTestElementsAsImages(exportDir, tgNode);
				
				////Confirmation of end of processing
				JOptionPane.showMessageDialog(null, resources.getProperty("exportfinished.message"), resources.getProperty("exportfinished.title"), JOptionPane.INFORMATION_MESSAGE);
			
			}finally{
				exporting.set(false);
				GuiPackage.getInstance().getMainFrame().setCursor(Cursor.getDefaultCursor());
			}
		}
	}
	
	/**
	 * Display once every TestElement
	 * @param folder
	 * @param tree
	 * @param node
	 */
	private void showAllTestElements(File folder, JTree tree, JMeterTreeNode node){
		if(node.getTestElement().isEnabled()){
			if(node.getTestElement() instanceof TestElement
					&& !node.getTestElement().getClass().equals(this.getClass())){ 	
					
				//select element
				TreeNode[] pathNodes=node.getPath();
				TreePath path = new TreePath(pathNodes);				
				tree.scrollPathToVisible(path);
				tree.setSelectionPath(path);
				
				//wait until component is shown
				JMeterGUIComponent component = GuiPackage.getInstance().getGui(node.getTestElement());
				JComponent jcomp=(JComponent) component;
				int maxIterations=20;
				int nbIteration=0;
				while(!jcomp.isShowing() && nbIteration<maxIterations){
					log.info("Waiting for component to show: "+node.getTestElement().getName());
					try {
						Thread.sleep(100);
						nbIteration++;
					} catch (InterruptedException e) {
						log.error("InterruptedException while waiting in showAllTestElements: "+e.getMessage(), e);
					}
				}
				if(nbIteration==maxIterations)
					log.warn("Waited for component until maximum wait time, screenshot will probably be empty: "+node.getTestElement().getName());
			}
			
			//process children
			Enumeration<JMeterTreeNode> children=node.children();
			while(children.hasMoreElements()){
				JMeterTreeNode child=children.nextElement();
				this.showAllTestElements(folder, tree, child);
			}
		
		}
	}

	/**
	 * Export all TestElements as images
	 * @param folder
	 * @param node
	 */
	private void exportAllTestElementsAsImages(File folder, JMeterTreeNode node){
		if(node.getTestElement().isEnabled()){
		
			if(node.getTestElement() instanceof TestElement
					&& !node.getTestElement().getClass().equals(this.getClass())){			
				this.exportTestElementAsImage(folder, (TestElement)node.getTestElement());
			}
			
			//process children
			Enumeration<JMeterTreeNode> children=node.children();
			while(children.hasMoreElements()){
				JMeterTreeNode child=children.nextElement();
				this.exportAllTestElementsAsImages(folder, child);
			}
		
		}
	}

	/**
	 * Find the tree node from the current thread group (containing this ResultExporter)
	 * @param tree
	 * @return
	 */
	private JMeterTreeNode getThreadGroupTreeNode(JTree tree){		
		TreeModel model=tree.getModel();
		JMeterTreeNode root=(JMeterTreeNode) model.getRoot();
		
		JMeterTreeNode resultExporterNode=this.getResultExporterTreeNode(root);
		return this.getContainingThreadGroup(resultExporterNode);
	}
	
	/**
	 * Find the TreeNode for this ResultExporterListener
	 * @param node
	 * @return
	 */
	public JMeterTreeNode getResultExporterTreeNode(JMeterTreeNode node){
		JMeterTreeNode foundNode=null;		

		if(node.getTestElement() instanceof ResultExporterListener 
				&& node.isEnabled()
				&& node.getName().equals(this.getName())
				){
			log.debug("ResultExporter found");
			foundNode=node;	
		}

		if(foundNode==null){
			//search in children				
			Enumeration<JMeterTreeNode> children=node.children();
			while(children.hasMoreElements() && foundNode==null){
				JMeterTreeNode child=children.nextElement();
				foundNode=this.getResultExporterTreeNode(child);
			}			
		}
		return foundNode;
	}	
	
	/**
	 * Find the first ThreadGroup container of this node's TestElement
	 * @param node
	 * @return
	 */
	public JMeterTreeNode getContainingThreadGroup(JMeterTreeNode node){
		JMeterTreeNode containingTG=null;
		
		JMeterTreeNode parent=(JMeterTreeNode) node.getParent();
		if(parent.getTestElement() instanceof ThreadGroup)
			containingTG=parent;
		
		if(containingTG==null && parent!=null && !parent.equals(node)){
			containingTG=this.getContainingThreadGroup(parent);
		}
		
		return containingTG;
	}
	
	/**
	 * export a single TestElement as an image
	 * @param folder
	 * @param testEl
	 */
	private void exportTestElementAsImage(File folder, TestElement testEl) {
		JMeterGUIComponent component = GuiPackage.getInstance().getGui(testEl);
		if (component instanceof Printable) {
			JComponent comp = ((Printable) component).getPrintableComponent();				
			String name = testEl.getName();			
			
			component.configure(testEl);
			
			//change name to be a possible file name
			name=ResultExporterListener.transformNameIntoFilename(name);
			name=name+ SaveGraphicsService.PNG_EXTENSION;
			File f = new File(folder, name);				
			log.info("Saving file "+f.getAbsolutePath());			
			this.saveJComponent(f,SaveGraphicsService.PNG, comp);			
		}
	}

	/**
	 * parse and transforms slashes and other characters that are not authorized in file names
	 * @param name
	 * @return a filename (without extension)
	 */
	public static String transformNameIntoFilename(String name){
		String result=name;
		if(name!=null){		
			result=name.replace("\\", "_");
			result=result.replace("/", "_");
			result=result.replace(".", "_");
			result=result.replace("=", "_");
			if(result.length()>256)
				result=result.substring(0, 253);
		}
		
		if(result==null || result.equals(""))
			result="default";
		
		return result;
	}
	
	/**
	 * Saving of a JComponent
	 * @param f
	 * @param type
	 * @param component
	 */
	private void saveJComponent(File f, int type, JComponent component) {		
		GuiPackage.getInstance().getMainFrame().setMainPanel(component);
		
		Dimension size = component.getSize();
		int width=size.width;
        int height=size.height; 
		
        log.debug("Found width="+width+", height="+height);
        if(width==0 || height==0){
        	width=800;
        	height=600;
        	log.info("Component widht or height were zero: setting to default=800*600");
        	component.setSize(width, height);        	
        }        
        	
        //export
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED);
        Graphics2D grp = image.createGraphics();
        if(grp==null)
        	log.error("Could not save image in file "+f.getName()+": Graphics is null");
        else{        	
        	
        	if(SwingUtilities.getWindowAncestor(component)==null){        	
        		log.error("Could not export image of component because parent window is null: "+f.getName()+": perhaps close logging window?");   		        		
        	}
        	else{
        		component.paint(grp);
	        
		        //save image to file
		        try {
					ImageIO.write(image, "PNG", f);
				} catch (IOException e) {
					log.error("IOException while saving image: "+f.getAbsolutePath(), e);
				}        
        	}
        }
    }

	public void actionPerformed(ActionEvent event) {
		if(EXPORTNOW_COMMAND.equals(event.getActionCommand())){
			this.runExport();
		}	
	}

	////// getters/setters for normal fields
	
	public void setRootFilename(String filename){
		this.rootFilename=filename;
	}
	public String getRootFilename(){
		String p=rootFilename;
		return p;
	}
	public void setExportDirname(String dirname){
		this.exportDirname=dirname;
	}
	public String getExportDirname(){
		String p=this.exportDirname;
		return p;		
	}
	
	///// getters/setters for properties
	
	public boolean isPopupsBeforeTest() {
		return this.getPropertyAsBoolean(POPUPSBEFORETEST_PROPERY);
	}

	public void setPopupsBeforeTest(boolean popupsBeforeTest) {
		this.setProperty(POPUPSBEFORETEST_PROPERY, popupsBeforeTest);
	}

	public boolean isFullscreenBeforeExport() {
		return this.getPropertyAsBoolean(FULLSCREENBEFOREEXPORT_PROPERTY);
	}

	public void setFullscreenBeforeExport(boolean fullscreenBeforeExport) {
		this.setProperty(FULLSCREENBEFOREEXPORT_PROPERTY, fullscreenBeforeExport);
	}
	
}
