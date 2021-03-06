package de.tud.plt.r43ples.merging.control;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.log4j.Logger;


import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.update.UpdateAction;

import de.tud.plt.r43ples.exception.InternalErrorException;
import de.tud.plt.r43ples.management.GitRepositoryState;
import de.tud.plt.r43ples.management.MergeQueryTypeEnum;
import de.tud.plt.r43ples.management.RebaseQueryTypeEnum;
import de.tud.plt.r43ples.management.ResolutionState;
import de.tud.plt.r43ples.management.RevisionManagement;
import de.tud.plt.r43ples.management.SDDTripleStateEnum;
import de.tud.plt.r43ples.merging.management.BranchManagement;
import de.tud.plt.r43ples.merging.management.ProcessManagement;
import de.tud.plt.r43ples.merging.management.ReportManagement;
import de.tud.plt.r43ples.merging.management.StrategyManagement;
import de.tud.plt.r43ples.merging.model.structure.CommitModel;
import de.tud.plt.r43ples.merging.model.structure.Difference;
import de.tud.plt.r43ples.merging.model.structure.DifferenceGroup;
import de.tud.plt.r43ples.merging.model.structure.DifferenceModel;
import de.tud.plt.r43ples.merging.model.structure.HighLevelChangeModel;
import de.tud.plt.r43ples.merging.model.structure.HighLevelChangeRenaming;
import de.tud.plt.r43ples.merging.model.structure.HighLevelChangeTableModel;
import de.tud.plt.r43ples.merging.model.structure.HighLevelChangeTableRow;
import de.tud.plt.r43ples.merging.model.structure.IndividualModel;
import de.tud.plt.r43ples.merging.model.structure.IndividualStructure;
import de.tud.plt.r43ples.merging.model.structure.ReportResult;
import de.tud.plt.r43ples.merging.model.structure.ReportTableRow;
import de.tud.plt.r43ples.merging.model.structure.TableEntrySemanticEnrichmentAllIndividuals;
import de.tud.plt.r43ples.merging.model.structure.TableModel;
import de.tud.plt.r43ples.merging.model.structure.TableRow;
import de.tud.plt.r43ples.merging.model.structure.TreeNode;
import de.tud.plt.r43ples.merging.model.structure.Triple;
import de.tud.plt.r43ples.webservice.Endpoint;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;

import javax.ws.rs.core.Response;


public class MergingControl {
	private static Logger logger = Logger.getLogger(MergingControl.class);
	private DifferenceModel differenceModel = new DifferenceModel();
	private List<TreeNode> treeList = new ArrayList<TreeNode>();
	private TableModel tableModel = new TableModel();
	
	
	private HighLevelChangeModel highLevelChangeModel = new HighLevelChangeModel();
	
	/**read the HighLevelChangeModel and create the highLevelChangeTableModel*/
	private HighLevelChangeTableModel highLevelChangeTableModel = new HighLevelChangeTableModel();
	
	/** The individual model of branch A. **/
	private IndividualModel individualModelBranchA;
	/** The individual model of branch B. **/
	private IndividualModel individualModelBranchB;	
	/** The properties array list. **/
	private ArrayList<String> propertyList;
	/** Merg Query Model. **/
	private CommitModel commitModel;
	
	/** The revision number of the branch A. **/
	private String revisionNumberBranchA;
	/** The revision number of the branch B. **/
	private String revisionNumberBranchB;
	
	/** The report result. **/
	private ReportResult reportResult;
	
	private boolean isRebase = false;
	
	/**The RebaseControl for the Rebae Merging**/
	private RebaseControl rebaseControl = null;
	
	
	//set and get the rebaseControl in MergingControl
	public void setRebaseControl(){
		this.rebaseControl = new RebaseControl(this);
	}
	
	public RebaseControl getRebaseControl(){
		return rebaseControl;
	}
	
	
	/**show merging start view*/
	public static String getMenuHtmlOutput() throws TemplateException, IOException {
		List<String> graphList = RevisionManagement.getRevisedGraphs();	
	    StringWriter sw = new StringWriter();
	    
	    //freemarker template engine
	    freemarker.template.Template temp = null; 
		String name = "merging.ftl";
		try {  
            // create the configuration of the template  
            Configuration cfg = new Configuration();  
            // set the path of the template 
            cfg.setClassForTemplateLoading(MergingControl.class, "/templates");
            // get the template page with this name
            temp = cfg.getTemplate(name);  
        } catch (IOException e) {  
            e.printStackTrace();  
        }  
	    
	    Map<String, Object> scope = new HashMap<String, Object>();
	    scope.put("merging_active", true);
		scope.put("graphList", graphList);
		
		scope.put("version", Endpoint.class.getPackage().getImplementationVersion() );
		scope.put("gitCommit", GitRepositoryState.getGitRepositoryState().commitIdAbbrev);
		scope.put("gitBranch", GitRepositoryState.getGitRepositoryState().branch);
		
		temp.process(scope,sw);	
		return sw.toString();
	}
	
	
	/**show triple merging view*/
	public String getViewHtmlOutput() throws TemplateException, IOException {	
		Map<String, Object> scope = new HashMap<String, Object>();
		StringWriter sw = new StringWriter();
		freemarker.template.Template temp = null; 
		String name = "mergingView3.ftl";
		try {  
            Configuration cfg = new Configuration();  
            cfg.setClassForTemplateLoading(MergingControl.class, "/templates");
            temp = cfg.getTemplate(name);  
        } catch (IOException e) {  
            e.printStackTrace();  
        }  
		
		/**conList fuer conflict triple
		 * diffList fuer deference triple*/
		List<TreeNode> conList = new ArrayList<TreeNode>();
		List<TreeNode> diffList = new ArrayList<TreeNode>();
		Iterator<TreeNode> itG = treeList.iterator();
	 	
		/**create conList and diffList*/
	 	String conStatus = "0";
	 	while(itG.hasNext()){
	 		TreeNode node = itG.next();
	 		
	 		if(node.status == true){
	 			conStatus = "1";
		 		conList.add(node);
	 		}else{
	 			diffList.add(node);
	 		}	 		
	 	}
	 	
	 	if(isRebase){
	 		logger.info("commitGraphname: " + rebaseControl.getCommitModel().getGraphName());
		 	scope.put("graphName", rebaseControl.getCommitModel().getGraphName());
		 	scope.put("clientName", rebaseControl.getCommitModel().getUser());
	 	}else{
	 		logger.info("commitGraphname: " + commitModel.getGraphName());
		 	scope.put("graphName", commitModel.getGraphName());	
		 	scope.put("clientName", commitModel.getUser());
	 	}
	 	
	 	scope.put("isRebase", isRebase);
	 	scope.put("tableRowList", tableModel.getTripleRowList());
	 		
		scope.put("conList",conList);
		scope.put("diffList",diffList);
		scope.put("conStatus", conStatus);
		scope.put("propertyList", propertyList);	
		
		scope.put("version", Endpoint.class.getPackage().getImplementationVersion() );
		scope.put("gitCommit", GitRepositoryState.getGitRepositoryState().commitIdAbbrev);
		scope.put("gitBranch", GitRepositoryState.getGitRepositoryState().branch);
		
		temp.process(scope,sw);		
		return sw.toString();		
	}
	
	
	/**get the new graph after der three way merging, with difference tree und triple table*/
	public String getUpdatedViewHtmlOutput() throws TemplateException, IOException, ConfigurationException, InternalErrorException {	
		Map<String, Object> scope = new HashMap<String, Object>();
		StringWriter sw = new StringWriter();
		freemarker.template.Template temp = null; 
		String name = "mergingView3.ftl";
		try {  
            Configuration cfg = new Configuration();  
            cfg.setClassForTemplateLoading(MergingControl.class, "/templates");
            temp = cfg.getTemplate(name);  
        } catch (IOException e) {  
            e.printStackTrace();  
        } 
		
		/** updated tree structure, table structure, property list and individual model*/
		ProcessManagement.createDifferenceTree(differenceModel, treeList);
		
		ProcessManagement.createTableModel(differenceModel, tableModel);
		
		logger.info("updated tableModel fertig!");

		
		// Create the individual models of both branches
		individualModelBranchA = ProcessManagement.createIndividualModelOfRevision(commitModel.getGraphName(), commitModel.getBranch1(), differenceModel);
		
		individualModelBranchB = ProcessManagement.createIndividualModelOfRevision(commitModel.getGraphName(), commitModel.getBranch2(), differenceModel);
		
		
		// Create the property list of revisions
		propertyList = ProcessManagement.getPropertiesOfRevision(commitModel.getGraphName(), commitModel.getBranch1(), commitModel.getBranch2());
		
		
		/**conList fuer conflict triple
		 * diffList fuer deference triple*/
		List<TreeNode> conList = new ArrayList<TreeNode>();
		List<TreeNode> diffList = new ArrayList<TreeNode>();
		Iterator<TreeNode> itG = treeList.iterator();
	 	
		/**create conList and diffList*/
	 	String conStatus = "0";
	 	while(itG.hasNext()){
	 		TreeNode node = itG.next();
	 		
	 		if(node.status == true){
	 			conStatus = "1";
		 		conList.add(node);
	 		}else{
	 			diffList.add(node);
	 		}	 		
	 	}
	 	
	 	scope.put("isRebase",isRebase);
	 	scope.put("tableRowList", tableModel.getTripleRowList());
	 	scope.put("graphName", commitModel.getGraphName());	
	 	scope.put("clientName", commitModel.getUser());
		scope.put("conList",conList);
		scope.put("diffList",diffList);
		scope.put("conStatus", conStatus);
		scope.put("propertyList", propertyList);	
		
		scope.put("version", Endpoint.class.getPackage().getImplementationVersion() );
		scope.put("gitCommit", GitRepositoryState.getGitRepositoryState().commitIdAbbrev);
		scope.put("gitBranch", GitRepositoryState.getGitRepositoryState().branch);
		
		temp.process(scope,sw);
		logger.info("updated view fertig!");
		return sw.toString();		
	}
	
	
	/**get the new graph after three way merging , with old graph and new graph , ohne difference tree and triple table*/
	
	public String getThreeWayReportView(String graphName) throws TemplateException, IOException{
		Map<String, Object> scope = new HashMap<String, Object>();
		StringWriter sw = new StringWriter();
		freemarker.template.Template temp = null; 
		String name = "mergingResultView.ftl";
		try {  
            Configuration cfg = new Configuration();  
            cfg.setClassForTemplateLoading(MergingControl.class, "/templates");
            temp = cfg.getTemplate(name);  
        } catch (IOException e) {  
            e.printStackTrace();  
        }  
		
		if(graphName == null) {
			graphName = commitModel.getGraphName();
		}
		
		scope.put("graphName", graphName);
		scope.put("clientName", commitModel.getUser());
		scope.put("commit", commitModel);
		scope.put("version", Endpoint.class.getPackage().getImplementationVersion() );
		scope.put("gitCommit", GitRepositoryState.getGitRepositoryState().commitIdAbbrev);
		scope.put("gitBranch", GitRepositoryState.getGitRepositoryState().branch);
		
		temp.process(scope,sw);		
		return sw.toString();	
	}
	
	
	/**
	 * get branch information through the graphName
	 * @param graph : name of graph
	 * return branch name*/
	public static String getBranchInformation(String graph) throws IOException {
		List<String> branchList = BranchManagement.getAllBranchNamesOfGraph(graph);
		StringBuilder branchInformation = new StringBuilder();
		for(String branchName:branchList){
			branchInformation.append("<option value="+"\""+branchName+"\""+">"+branchName+"</option>");
		}
		return branchInformation.toString();
	}
	

	
	public void getMergeProcess(Response response, String graphName, String branchNameA, String branchNameB, String format) throws IOException, ConfigurationException, InternalErrorException{
		if (isRebase) {
			
			ProcessManagement.readDifferenceModel(response.getEntity().toString(), differenceModel, format);
			
			
			ProcessManagement.createDifferenceTree(differenceModel, treeList);
			
			ProcessManagement.createTableModel(differenceModel, tableModel);
			
			
			// Save the current revision numbers
			revisionNumberBranchA = RevisionManagement.getRevisionNumber(graphName, branchNameA);
			revisionNumberBranchB = RevisionManagement.getRevisionNumber(graphName, branchNameB);
			
			//create and initialization reportResult
			
			reportResult = ReportManagement.initialReportResult(differenceModel);
			
			// Create the individual models of both branches
			individualModelBranchA = ProcessManagement.createIndividualModelOfRevision(graphName, branchNameA, differenceModel);
			logger.info("Individual Model A Test : " + individualModelBranchA.getIndividualStructures().keySet().toString());
			Iterator<Entry<String, IndividualStructure>> itEnt = individualModelBranchA.getIndividualStructures().entrySet().iterator();
			while(itEnt.hasNext()){
				Entry<String,IndividualStructure> entryInd = itEnt.next();
				logger.info("Individual Sturcture Uri Test" + entryInd.getValue().getIndividualUri());
				logger.info("Individual Sturcture Triples Test" + entryInd.getValue().getTriples().keySet().toString());

				
			}

			individualModelBranchB = ProcessManagement.createIndividualModelOfRevision(graphName, branchNameB, differenceModel);
			logger.info("Individual Model B Test : " + individualModelBranchB.getIndividualStructures().keySet().toString());

			
			
			// Create the property list of revisions
			propertyList = ProcessManagement.getPropertiesOfRevision(graphName, branchNameA, branchNameB);
			
			Iterator<String> pit = propertyList.iterator();
			while(pit.hasNext()){
				logger.info("propertyList Test : " + pit.next().toString());
			}

		}else{
			
			//ob diese satz richt ist oder nicht?
			if (response.getStatusInfo() == Response.Status.CONFLICT){
				logger.info("Merge query produced conflicts.");
				
				ProcessManagement.readDifferenceModel(response.getEntity().toString(), differenceModel,"text/html");
				
				
				ProcessManagement.createDifferenceTree(differenceModel, treeList);
				
				ProcessManagement.createTableModel(differenceModel, tableModel);
				
				
				// Save the current revision numbers
				revisionNumberBranchA = RevisionManagement.getRevisionNumber(graphName, branchNameA);
				revisionNumberBranchB = RevisionManagement.getRevisionNumber(graphName, branchNameB);
				
				//create and initialization reportResult
				
				reportResult = ReportManagement.initialReportResult(differenceModel);
				
				// Create the individual models of both branches
				individualModelBranchA = ProcessManagement.createIndividualModelOfRevision(graphName, branchNameA, differenceModel);
				logger.info("Individual Model A Test : " + individualModelBranchA.getIndividualStructures().keySet().toString());
				Iterator<Entry<String, IndividualStructure>> itEnt = individualModelBranchA.getIndividualStructures().entrySet().iterator();
				while(itEnt.hasNext()){
					Entry<String,IndividualStructure> entryInd = itEnt.next();
					logger.info("Individual Sturcture Uri Test" + entryInd.getValue().getIndividualUri());
					logger.info("Individual Sturcture Triples Test" + entryInd.getValue().getTriples().keySet().toString());

					
				}

				individualModelBranchB = ProcessManagement.createIndividualModelOfRevision(graphName, branchNameB, differenceModel);
				logger.info("Individual Model B Test : " + individualModelBranchB.getIndividualStructures().keySet().toString());

				
				
				// Create the property list of revisions
				propertyList = ProcessManagement.getPropertiesOfRevision(graphName, branchNameA, branchNameB);
				
				Iterator<String> pit = propertyList.iterator();
				while(pit.hasNext()){
					logger.info("propertyList Test : " + pit.next().toString());
				}
				
				
				
			} else if (response.getStatusInfo() == Response.Status.CREATED){
				logger.info("Merge query produced no conflicts. Merged revision was created.");
				
			} else {
				// error occurred 
				throw new InternalErrorException("Error in response : " + response);
			}				
		}	
	}
	
	/**show individual view of the merging */	
	public String getIndividualView() throws TemplateException, IOException{
		Map<String, Object> scope = new HashMap<String, Object>();
		StringWriter sw = new StringWriter();
		freemarker.template.Template temp = null; 
		String name = "individualView.ftl";
		try {  
            Configuration cfg = new Configuration();  
            cfg.setClassForTemplateLoading(MergingControl.class, "/templates");
            temp = cfg.getTemplate(name);  
        } catch (IOException e) {  
            e.printStackTrace();  
        }  
		
		
		if(isRebase){
	 		logger.info("commitGraphname: " + rebaseControl.getCommitModel().getGraphName());
		 	scope.put("graphName", rebaseControl.getCommitModel().getGraphName());	
		 	scope.put("clientName", rebaseControl.getCommitModel().getUser());
	 	}else{
	 		logger.info("commitGraphname: " + commitModel.getGraphName());
		 	scope.put("graphName", commitModel.getGraphName());	 
		 	scope.put("clientName", commitModel.getUser());
	 	}
		
		
		scope.put("individualTableList", createTableModelSemanticEnrichmentAllIndividualsList());
		
		scope.put("version", Endpoint.class.getPackage().getImplementationVersion() );
		scope.put("gitCommit", GitRepositoryState.getGitRepositoryState().commitIdAbbrev);
		scope.put("gitBranch", GitRepositoryState.getGitRepositoryState().branch);
		
		temp.process(scope,sw);		
		return sw.toString();	
		
	}
	/**
	 * @param individualA individual of Branch A
	 * @param individualB individual of Branch B
	 * return response of updated triple table by individual 
	 * */
	public String getIndividualFilter(String individualA , String individualB) throws ConfigurationException, TemplateException, IOException {
		//updated tableModel
		ProcessManagement.createTableModel(differenceModel, tableModel);
		
		List<TableRow> updatedTripleRowList = ProcessManagement.createIndividualTableList(individualA, 
				individualB, individualModelBranchA, individualModelBranchB, tableModel);
		
		//test Table Row list
		Iterator<TableRow> ite = updatedTripleRowList.iterator();
		while(ite.hasNext()){
			TableRow t = ite.next();
			logger.info("updated table list : "+ t.getSubject() +"--"+ t.getConflicting());
		}
		
		Map<String, Object> scope = new HashMap<String, Object>();
		StringWriter sw = new StringWriter();
		freemarker.template.Template temp = null; 
		String name = "individualFilterTable.ftl";
		try {  
            Configuration cfg = new Configuration();  
            cfg.setClassForTemplateLoading(MergingControl.class, "/templates");
            temp = cfg.getTemplate(name);  
        } catch (IOException e) {  
            e.printStackTrace();  
        }  
		
		if(isRebase){
	 		logger.info("commitGraphname: " + rebaseControl.getCommitModel().getGraphName());
		 	scope.put("graphName", rebaseControl.getCommitModel().getGraphName());	
		 	scope.put("clientName", rebaseControl.getCommitModel().getUser());
	 	}else{
	 		logger.info("commitGraphname: " + commitModel.getGraphName());
		 	scope.put("graphName", commitModel.getGraphName());	
		 	scope.put("clientName", commitModel.getUser());
	 	}
		
		scope.put("updatedTripleRowList", updatedTripleRowList);
		
		scope.put("version", Endpoint.class.getPackage().getImplementationVersion() );
		scope.put("gitCommit", GitRepositoryState.getGitRepositoryState().commitIdAbbrev);
		scope.put("gitBranch", GitRepositoryState.getGitRepositoryState().branch);
		
		temp.process(scope,sw);
		
		return sw.toString();	
		
		
	}
	
	/**getHighLevel View
	 * @throws IOException 
	 * @throws TemplateException 
	 * @throws ConfigurationException */
	public String getHighLevelView() throws TemplateException, IOException, ConfigurationException {
		
		//get high level change model
		ProcessManagement.createHighLevelChangeRenamingModel(highLevelChangeModel, differenceModel);
		
		//test the highlevelChangeTableModel
		ProcessManagement.createHighLevelChangeTableModel(highLevelChangeModel, highLevelChangeTableModel);
		
		List<HighLevelChangeTableRow> highLevelRowList = highLevelChangeTableModel.getTripleRowList();
		
		Map<String, Object> scope = new HashMap<String, Object>();
		StringWriter sw = new StringWriter();
		freemarker.template.Template temp = null; 
		String name = "highLevelView.ftl";
		try {  
            Configuration cfg = new Configuration();  
            cfg.setClassForTemplateLoading(MergingControl.class, "/templates");
            temp = cfg.getTemplate(name);  
        } catch (IOException e) {  
            e.printStackTrace();  
        }  
		
		if(isRebase){
	 		logger.info("commitGraphname: " + rebaseControl.getCommitModel().getGraphName());
		 	scope.put("graphName", rebaseControl.getCommitModel().getGraphName());
		 	scope.put("clientName", rebaseControl.getCommitModel().getUser());
	 	}else{
	 		logger.info("commitGraphname: " + commitModel.getGraphName());
		 	scope.put("graphName", commitModel.getGraphName());	 
		 	scope.put("clientName", commitModel.getUser());
	 	}
		
		scope.put("highLevelRowList", highLevelRowList);
		
		scope.put("version", Endpoint.class.getPackage().getImplementationVersion() );
		scope.put("gitCommit", GitRepositoryState.getGitRepositoryState().commitIdAbbrev);
		scope.put("gitBranch", GitRepositoryState.getGitRepositoryState().branch);
		
		temp.process(scope,sw);
		
		return sw.toString();		
		
	}
	
	/**@param properties :  property list of Filter by property
	 * return response of updated triple table
	 * @throws ConfigurationException */
	
	public String updateTripleTable(String properties) throws TemplateException, IOException, ConfigurationException{
		
		Map<String, Object> scope = new HashMap<String, Object>();
		StringWriter sw = new StringWriter();
		freemarker.template.Template temp = null; 
		String name = "tripleTable.ftl";
		
		try {  
            Configuration cfg = new Configuration();  
            cfg.setClassForTemplateLoading(MergingControl.class, "/templates");
            temp = cfg.getTemplate(name);  
        } catch (IOException e) {  
            e.printStackTrace();  
        } 
		
		//updated tableModel	 	
		ProcessManagement.createTableModel(differenceModel, tableModel);
		
		String[] propertyArray = properties.split(",");
		List<TableRow> TripleRowList = tableModel.getTripleRowList();
		List<TableRow> updatedTripleRowList = new ArrayList<TableRow>();
		for(String property: propertyArray) {
		
			Iterator<TableRow> itu = TripleRowList.iterator();
			while(itu.hasNext()){
				TableRow tableRow = itu.next();
				if(tableRow.getPredicate().equals(property)) {
					updatedTripleRowList.add(tableRow);
				}
			}					
		}
		
		if(isRebase){
	 		logger.info("rebasecommitGraphname: " + rebaseControl.getCommitModel().getGraphName());
		 	scope.put("graphName", rebaseControl.getCommitModel().getGraphName());	
		 	scope.put("clientName", rebaseControl.getCommitModel().getUser());
	 	}else{
	 		logger.info("mergingcommitGraphname: " + commitModel.getGraphName());
		 	scope.put("graphName", commitModel.getGraphName());	 
		 	scope.put("clientName", commitModel.getUser());
	 	}
		
		scope.put("tableRowList", updatedTripleRowList);
		
		scope.put("version", Endpoint.class.getPackage().getImplementationVersion() );
		scope.put("gitCommit", GitRepositoryState.getGitRepositoryState().commitIdAbbrev);
		scope.put("gitBranch", GitRepositoryState.getGitRepositoryState().branch);
		
		temp.process(scope,sw);	
		
		return sw.toString();	
		
	}
	
	/**@param triples :  triple list of Difference Tree by checkbox select
	 * return response of updated triple table
	 * @throws ConfigurationException */
	
	public String updateTripleTableByTree(String triples) throws TemplateException, IOException, ConfigurationException{
		
		Map<String, Object> scope = new HashMap<String, Object>();
		StringWriter sw = new StringWriter();
		freemarker.template.Template temp = null; 
		String name = "tripleTable.ftl";
		
		try {  
            Configuration cfg = new Configuration();  
            cfg.setClassForTemplateLoading(MergingControl.class, "/templates");
            temp = cfg.getTemplate(name);  
        } catch (IOException e) {  
            e.printStackTrace();  
        } 
		
		//updated tableModel	 	
		ProcessManagement.createTableModel(differenceModel, tableModel);
		
		String[] tripleArray = triples.split(",");
		List<TableRow> TripleRowList = tableModel.getTripleRowList();
		List<TableRow> updatedTripleRowList = new ArrayList<TableRow>();
		for(String triple: tripleArray) {
			
			Iterator<TableRow> itu = TripleRowList.iterator();
			while(itu.hasNext()){
				TableRow tableRow = itu.next();
				//get the triple in table ant transform to string 
				Triple tableTriple = tableRow.getTriple();
				String subject = ProcessManagement.convertTripleStringToPrefixTripleString(ProcessManagement.getSubject(tableTriple));
				String predicate = ProcessManagement.convertTripleStringToPrefixTripleString(ProcessManagement.getPredicate(tableTriple));
				String object = ProcessManagement.convertTripleStringToPrefixTripleString(ProcessManagement.getObject(tableTriple));
				
				StringBuilder TripleBuilder = new StringBuilder();
				String prefixTriple = TripleBuilder.append(subject).append(" ").append(predicate).append(" ").append(object).toString();
				logger.info("tree triple: " + "--" + prefixTriple + "--" +triple.trim()+"--");
				
				logger.info("tree boolean"+ (prefixTriple).equals(triple.trim()));
				if((prefixTriple.trim()).equals(triple.trim())) {
					updatedTripleRowList.add(tableRow);
				}
				
				
			}					
		}
		
		if(isRebase){
	 		logger.info("commitGraphname: " + rebaseControl.getCommitModel().getGraphName());
		 	scope.put("graphName", rebaseControl.getCommitModel().getGraphName());	
		 	scope.put("clientName", rebaseControl.getCommitModel().getUser());
	 	}else{
	 		logger.info("commitGraphname: " + commitModel.getGraphName());
		 	scope.put("graphName", commitModel.getGraphName());	
		 	scope.put("clientName", commitModel.getUser());
	 	}
		
		logger.info("tree table list: "+ updatedTripleRowList.size());
		scope.put("tableRowList", updatedTripleRowList);
		
		scope.put("version", Endpoint.class.getPackage().getImplementationVersion() );
		scope.put("gitCommit", GitRepositoryState.getGitRepositoryState().commitIdAbbrev);
		scope.put("gitBranch", GitRepositoryState.getGitRepositoryState().branch);
		
		temp.process(scope,sw);	
		
		return sw.toString();	
		
	}
	
	/**get triple view after changed view
	 * @throws ConfigurationException */
	public String getTripleView() throws TemplateException, IOException, ConfigurationException{
		Map<String, Object> scope = new HashMap<String, Object>();
		StringWriter sw = new StringWriter();
		freemarker.template.Template temp = null; 
		String name = "tripleView.ftl";
		try {  
            Configuration cfg = new Configuration();  
            cfg.setClassForTemplateLoading(MergingControl.class, "/templates");
            temp = cfg.getTemplate(name);  
        } catch (IOException e) {  
            e.printStackTrace();  
        }  
		
		//updated tableModel	 	
		ProcessManagement.createTableModel(differenceModel, tableModel);	
		
		if(isRebase){
	 		logger.info("commitGraphname: " + rebaseControl.getCommitModel().getGraphName());
		 	scope.put("graphName", rebaseControl.getCommitModel().getGraphName());	
		 	scope.put("clientName", rebaseControl.getCommitModel().getUser());
	 	}else{
	 		logger.info("commitGraphname: " + commitModel.getGraphName());
		 	scope.put("graphName", commitModel.getGraphName());	 
		 	scope.put("clientName", commitModel.getUser());
	 	}
		
		scope.put("tableRowList", tableModel.getTripleRowList());
		
		scope.put("version", Endpoint.class.getPackage().getImplementationVersion() );
		scope.put("gitCommit", GitRepositoryState.getGitRepositoryState().commitIdAbbrev);
		scope.put("gitBranch", GitRepositoryState.getGitRepositoryState().branch);
		
		temp.process(scope,sw);		
		return sw.toString();	
		
	}
	
	/**
	 * ##########################################################################################################################################################################
	 * ##########################################################################################################################################################################
	 * ##                                                                                                                                                                      ##
	 * ## approve Process : interactive approved process through ajax                                                                                                                              ##
	 * ##                                                                                                                                                                      ##
	 * ##########################################################################################################################################################################
	 * ##########################################################################################################################################################################
	 */
	
	/**
	 * approve id nummer of Triple in Triple Table 
	 * Difference Model will changed though ajax
	 * updated reportResult by decrementCounterDifferenceResolutionChanged() and incrementCounterDifferenceResolutionChanged()
	 * @param id: to approved Triple id 
	 * @param isChecked : status of approved Triple*/
	
	public void approveToDifferenceModel(String id, String isChecked){
		
		// Count for Conflict;
		int count = 0;
		Triple checkedTriple = tableModel.getManuellTriple().get(id);
		SDDTripleStateEnum tripleState;
		if (isChecked.equals("1")){
			tripleState = SDDTripleStateEnum.ADDED;
		}else {
			tripleState = SDDTripleStateEnum.DELETED;
		}
		
		Iterator<Entry<String, DifferenceGroup>> iterDM = differenceModel.getDifferenceGroups().entrySet().iterator();
		while(iterDM.hasNext()) {
			Entry<String, DifferenceGroup> entryDG = (Entry<String, DifferenceGroup>) iterDM.next();
			DifferenceGroup differ = (DifferenceGroup) entryDG.getValue();
			boolean conflicting = differ.isConflicting();
			
			SDDTripleStateEnum automaticState = differ.getAutomaticResolutionState();
			Iterator<Entry<String, Difference>> iterDIF = differ.getDifferences().entrySet().iterator();
			while(iterDIF.hasNext()){
				Entry<String, Difference> entryDF = iterDIF.next();
				//get triple
				String tripleString = entryDF.getKey();
				
				logger.info("tripleString : "+ tripleString);
				Difference difference = entryDF.getValue();
				ResolutionState resolutionState = difference.getResolutionState();

				
				if (ProcessManagement.tripleToString(checkedTriple).equals(tripleString)){
					
					if(resolutionState == ResolutionState.RESOLVED){
						if(!tripleState.equals(automaticState) && !conflicting) {
							reportResult.decrementCounterDifferencesResolutionChanged();
							difference.setTripleResolutionState(automaticState);
						}
						
						difference.setResolutionState(ResolutionState.DIFFERENCE);
						if (conflicting) {
							difference.setResolutionState(ResolutionState.CONFLICT);
							if(!tripleState.equals(automaticState)){
								difference.setTripleResolutionState(automaticState);
							}
						}
					}else{
						
						if(resolutionState == ResolutionState.DIFFERENCE && (!tripleState.equals(automaticState))) {
							reportResult.incrementCounterDifferencesResolutionChanged();
							difference.setTripleResolutionState(tripleState);
						}else if (resolutionState == ResolutionState.CONFLICT && (!tripleState.equals(automaticState))){
							difference.setTripleResolutionState(tripleState);
						}
						
						difference.setResolutionState(ResolutionState.RESOLVED);
					}
					
				}
				
				if(difference.getResolutionState().equals(ResolutionState.CONFLICT)){
					count ++;
				}
			}
		}
		
		
		reportResult.setConflictsNotApproved(count);
		
		logger.info("reportresult : "+ reportResult.getConflictsNotApproved());
		
		//test ReportResult:
		logger.info("reportresult count: "+ reportResult.getConflictsNotApproved() + "--" + reportResult.getDifferencesResolutionChanged());
		// only for test approved difference model
		Iterator<Entry<String, DifferenceGroup>> iterD = differenceModel.getDifferenceGroups().entrySet().iterator();
		while(iterD.hasNext()) {
			Entry<String, DifferenceGroup> entryDG = (Entry<String, DifferenceGroup>) iterD.next();
			DifferenceGroup differ = (DifferenceGroup) entryDG.getValue();
			Iterator<Entry<String, Difference>> iterDIF = differ.getDifferences().entrySet().iterator();
			while(iterDIF.hasNext()){
				Entry<String, Difference> entryDF = iterDIF.next();
				//get triple
				String tripleString = entryDF.getKey();
				Difference difference = entryDF.getValue();
				
				logger.info("approved difference model: " + tripleString + difference.getTripleResolutionState() + difference.getResolutionState().toString());
			}
		}	
		
	}
	
	/**
	 * approve id nummer of highLevelRow in high level Table 
	 * Difference Model will changed though ajax
	 * updated reportResult by decrementCounterDifferenceResolutionChanged() and incrementCounterDifferenceResolutionChanged()
	 * @param id: to approved Triple id 
	 * @param isChecked : status of approved Triple*/
	
	public void approveHighLevelToDifferenceModel(String id, String isChecked){
		
		// Count for Conflict;
		int count = 0;
		HighLevelChangeTableRow tableRow = highLevelChangeTableModel.getManuellTriple().get(id);
//		String isRenaming = tableRow.getIsRenaming();
		String isResolved = tableRow.getIsResolved();
		HighLevelChangeRenaming changeRenaming = tableRow.getHighLevelChangeRenaming();
		Difference additionDifference = changeRenaming.getAdditionDifference();
		Difference deletionDifference = changeRenaming.getDeletionDifference();
		
		SDDTripleStateEnum additionDifferenceSDDState;
		SDDTripleStateEnum deletionDifferenceSDDState;
		
		//result approved 
		if(isResolved.equals("no")) {
			if (isChecked.equals("1")) {
				// Rename - yes
				additionDifferenceSDDState = SDDTripleStateEnum.ADDED;
				deletionDifferenceSDDState = SDDTripleStateEnum.DELETED;
				
				
				
			} else {
				// Rename - no
				
				additionDifferenceSDDState = SDDTripleStateEnum.DELETED;
				deletionDifferenceSDDState = SDDTripleStateEnum.ADDED;
	
			}
			
			Iterator<Entry<String, DifferenceGroup>> iterDM = differenceModel.getDifferenceGroups().entrySet().iterator();
			while(iterDM.hasNext()) {
				Entry<String, DifferenceGroup> entryDG = (Entry<String, DifferenceGroup>) iterDM.next();
				DifferenceGroup differ = (DifferenceGroup) entryDG.getValue();
//				boolean conflicting = differ.isConflicting();
				
				SDDTripleStateEnum automaticState = differ.getAutomaticResolutionState();
				Iterator<Entry<String, Difference>> iterDIF = differ.getDifferences().entrySet().iterator();
				while(iterDIF.hasNext()){					
					Entry<String, Difference> entryDF = iterDIF.next();
					Difference difference = entryDF.getValue();
					ResolutionState resolutionState = difference.getResolutionState();
					
					if(difference.equals(additionDifference)){
						
						if(resolutionState == ResolutionState.DIFFERENCE && (!additionDifferenceSDDState.equals(automaticState))){
							reportResult.incrementCounterDifferencesResolutionChanged();						
						}
						
						difference.setTripleResolutionState(additionDifferenceSDDState);
						difference.setResolutionState(ResolutionState.RESOLVED);			
						
					}
					if(difference.equals(deletionDifference)){
						
						if(resolutionState == ResolutionState.DIFFERENCE && (!deletionDifferenceSDDState.equals(automaticState))){
							reportResult.incrementCounterDifferencesResolutionChanged();						
						}
						
						difference.setTripleResolutionState(deletionDifferenceSDDState);
						difference.setResolutionState(ResolutionState.RESOLVED);			
						
					}
								
				}
			}
			
			tableRow.setIsResolved("yes");
		}else{
			Iterator<Entry<String, DifferenceGroup>> iterDM = differenceModel.getDifferenceGroups().entrySet().iterator();
			while(iterDM.hasNext()) {
				Entry<String, DifferenceGroup> entryDG = (Entry<String, DifferenceGroup>) iterDM.next();
				DifferenceGroup differ = (DifferenceGroup) entryDG.getValue();
				boolean conflicting = differ.isConflicting();
				
				SDDTripleStateEnum automaticState = differ.getAutomaticResolutionState();
				Iterator<Entry<String, Difference>> iterDIF = differ.getDifferences().entrySet().iterator();
				while(iterDIF.hasNext()){					
					Entry<String, Difference> entryDF = iterDIF.next();
					Difference difference = entryDF.getValue();
					
					SDDTripleStateEnum differenceSDDState = difference.getTripleResolutionState();
//					ResolutionState resolutionState = difference.getResolutionState();
					
					if(difference.equals(additionDifference)){
						
						if(!differenceSDDState.equals(automaticState) && !conflicting){
							reportResult.decrementCounterDifferencesResolutionChanged();
							difference.setTripleResolutionState(automaticState);						
						}
						difference.setResolutionState(ResolutionState.DIFFERENCE);
						if (conflicting) {
							difference.setResolutionState(ResolutionState.CONFLICT);
							if(!differenceSDDState.equals(automaticState)){
								difference.setTripleResolutionState(automaticState);
							}
						}					
						
					}
					if(difference.equals(deletionDifference)){
						
						if(!differenceSDDState.equals(automaticState) && !conflicting){
							reportResult.decrementCounterDifferencesResolutionChanged();
							difference.setTripleResolutionState(automaticState);						
						}
						difference.setResolutionState(ResolutionState.DIFFERENCE);
						if (conflicting) {
							difference.setResolutionState(ResolutionState.CONFLICT);
							if(!differenceSDDState.equals(automaticState)){
								difference.setTripleResolutionState(automaticState);
							}
						}	
					}	
					
				}
			}
			tableRow.setIsResolved("no");
		}
		
		//else fertig
		
		
		Iterator<Entry<String, DifferenceGroup>> iterDM = differenceModel.getDifferenceGroups().entrySet().iterator();
		while(iterDM.hasNext()) {
			Entry<String, DifferenceGroup> entryDG = (Entry<String, DifferenceGroup>) iterDM.next();
			DifferenceGroup differ = (DifferenceGroup) entryDG.getValue();
			
			Iterator<Entry<String, Difference>> iterDIF = differ.getDifferences().entrySet().iterator();
			while(iterDIF.hasNext()){
				Entry<String, Difference> entryDF = iterDIF.next();
				//get triple
				String tripleString = entryDF.getKey();
				
				logger.info("tripleString : "+ tripleString);
				Difference difference = entryDF.getValue();
				
				if(difference.getResolutionState().equals(ResolutionState.CONFLICT)){
					count ++;
				}
			}
		}
		
		
		reportResult.setConflictsNotApproved(count);
		
		logger.info("reportresult : "+ reportResult.getConflictsNotApproved());
		
		//test ReportResult:
		logger.info("reportresult count: "+ reportResult.getConflictsNotApproved() + "--" + reportResult.getDifferencesResolutionChanged());
		// only for test approved difference model
		Iterator<Entry<String, DifferenceGroup>> iterD = differenceModel.getDifferenceGroups().entrySet().iterator();
		while(iterD.hasNext()) {
			Entry<String, DifferenceGroup> entryDG = (Entry<String, DifferenceGroup>) iterD.next();
			DifferenceGroup differ = (DifferenceGroup) entryDG.getValue();
			Iterator<Entry<String, Difference>> iterDIF = differ.getDifferences().entrySet().iterator();
			while(iterDIF.hasNext()){
				Entry<String, Difference> entryDF = iterDIF.next();
				//get triple
				String tripleString = entryDF.getKey();
				Difference difference = entryDF.getValue();
				
				logger.info("high level approved difference model: " + tripleString + difference.getTripleResolutionState() + difference.getResolutionState().toString());
			}
		}	
		
	}
	
	/**
	 * ##########################################################################################################################################################################
	 * ##########################################################################################################################################################################
	 * ##                                                                                                                                                                      ##
	 * ## create Report Process : get report result                                                                                                                             ##
	 * ##                                                                                                                                                                      ##
	 * ##########################################################################################################################################################################
	 * ##########################################################################################################################################################################
	 * @throws IOException 
	 * @throws TemplateException 
	 * @throws ConfigurationException 
	 */
	
	public String createReportProcess() throws TemplateException, IOException, ConfigurationException {
		Map<String, Object> scope = new HashMap<String, Object>();
		StringWriter sw = new StringWriter();
		freemarker.template.Template temp = null; 
		String name = "reportView.ftl";
		try {  
            Configuration cfg = new Configuration();  
            cfg.setClassForTemplateLoading(MergingControl.class, "/templates");
            temp = cfg.getTemplate(name);  
        } catch (IOException e) {  
            e.printStackTrace();  
        }  
		
		
		List<ReportTableRow>  reportTableRowList = ReportManagement.createReportTableRowList(differenceModel) ;
		
		String report = null;
		if(reportResult.getConflictsNotApproved() > 0){
			report = "1";
		}else {
			report = "0";
		}
		
		if(isRebase) {
			scope.put("commit", rebaseControl.getCommitModel());
		}else{
			scope.put("commit", commitModel);
		}
		
		if(isRebase){
	 		logger.info("commitGraphname: " + rebaseControl.getCommitModel().getGraphName());
		 	scope.put("graphName", rebaseControl.getCommitModel().getGraphName());
		 	scope.put("clientName", rebaseControl.getCommitModel().getUser());
	 	}else{
	 		logger.info("commitGraphname: " + commitModel.getGraphName());
		 	scope.put("graphName", commitModel.getGraphName());	 
		 	scope.put("clientName", commitModel.getUser());
	 	}
		
		// three way merging
		scope.put("isRebase", false);
		
		scope.put("report", report);
		scope.put("reportTableRowList", reportTableRowList);
		
		scope.put("version", Endpoint.class.getPackage().getImplementationVersion() );
		scope.put("gitCommit", GitRepositoryState.getGitRepositoryState().commitIdAbbrev);
		scope.put("gitBranch", GitRepositoryState.getGitRepositoryState().branch);
		
		temp.process(scope,sw);		
		return sw.toString();	
		
		
	}
	
	/**
	 * ##########################################################################################################################################################################
	 * ##########################################################################################################################################################################
	 * ##                                                                                                                                                                      ##
	 * ## create Rebase Report Process : get report result                                                                                                                             ##
	 * ##                                                                                                                                                                      ##
	 * ##########################################################################################################################################################################
	 * ##########################################################################################################################################################################
	 * @throws IOException 
	 * @throws TemplateException 
	 * @throws ConfigurationException 
	 */
	
	public String createRebaseReportProcess() throws TemplateException, IOException, ConfigurationException {
		Map<String, Object> scope = new HashMap<String, Object>();
		StringWriter sw = new StringWriter();
		freemarker.template.Template temp = null; 
		String name = "reportView.ftl";
		try {  
            Configuration cfg = new Configuration();  
            cfg.setClassForTemplateLoading(MergingControl.class, "/templates");
            temp = cfg.getTemplate(name);  
        } catch (IOException e) {  
            e.printStackTrace();  
        }  
		
		
		List<ReportTableRow>  reportTableRowList = ReportManagement.createReportTableRowList(differenceModel) ;
		
		String report = null;
		if(reportResult.getConflictsNotApproved() > 0){
			report = "1";
		}else {
			report = "0";
		}
		
		
		scope.put("graphName", rebaseControl.getCommitModel().getGraphName());	
		scope.put("clientName", rebaseControl.getCommitModel().getUser());
	 	
		scope.put("isRebase", true);
		scope.put("commit", rebaseControl.getCommitModel());
		
		scope.put("report", report);
		scope.put("reportTableRowList", reportTableRowList);
		
		scope.put("version", Endpoint.class.getPackage().getImplementationVersion() );
		scope.put("gitCommit", GitRepositoryState.getGitRepositoryState().commitIdAbbrev);
		scope.put("gitBranch", GitRepositoryState.getGitRepositoryState().branch);
		
		temp.process(scope,sw);		
		return sw.toString();	
		
		
	}
	
	
	/**
	 * ##########################################################################################################################################################################
	 * ##########################################################################################################################################################################
	 * ##                                                                                                                                                                      ##
	 * ## push Process : get push Result                                                                                                                              ##
	 * ##                                                                                                                                                                      ##
	 * ##########################################################################################################################################################################
	 * ##########################################################################################################################################################################
	 */
	/**
	 * check the reportResult and get the new mergeQuery.
	 * die difference model is always updated through Ajax
	 * @throws InternalErrorException 
	 * 
	 * @throws IOException 
	 */
	
	public String updateMergeQueryNew () throws IOException, InternalErrorException {
		
		if (reportResult != null) {
			if (reportResult.getConflictsNotApproved() == 0){
				
				// Get the definitions
				String user;
				String message;
				String graphName;
				String sdd;
				
				if(isRebase) {
					user = rebaseControl.getCommitModel().getUser();
					message = rebaseControl.getCommitModel().getMessage();
					graphName = rebaseControl.getCommitModel().getGraphName();
					sdd = rebaseControl.getCommitModel().getSddName();
				}else{
					user = commitModel.getUser();
					message = commitModel.getMessage();
					graphName = commitModel.getGraphName();
					sdd = commitModel.getSddName();
				}
				
				
				String mergeQuery = null;
				if (reportResult.getDifferencesResolutionChanged() > 0) {
					
					//create Triples
					// Get the whole dataset
					Model wholeContentModel = ProcessManagement.getWholeContentOfRevision(graphName, revisionNumberBranchB);
					logger.debug("Whole model as N-Triples: \n" + ProcessManagement.writeJenaModelToNTriplesString(wholeContentModel));
					
					logger.info("whole model: " + ProcessManagement.writeJenaModelToNTriplesString(wholeContentModel));

					// Update dataset with local data
					ArrayList<String> list = ProcessManagement.getAllTriplesDividedIntoInsertAndDelete(differenceModel, wholeContentModel);
					
					logger.debug("INSERT: \n" + list.get(0));
					logger.debug("DELETE: \n" + list.get(1));
					
					logger.info("insert Triple: "+list.get(0));
					logger.info("delete Triple: "+list.get(1));

					
					String updateQueryInsert = String.format(
							  "INSERT DATA { %n"
							+ "	%s %n"
							+ "}", list.get(0));
					UpdateAction.parseExecute(updateQueryInsert, wholeContentModel);
					
					String updateQueryDelete = String.format(
							  "DELETE DATA { %n"
							+ " %s %n"
							+ "}", list.get(1));
					UpdateAction.parseExecute(updateQueryDelete, wholeContentModel);
					
					String triples = ProcessManagement.writeJenaModelToNTriplesString(wholeContentModel);
					logger.debug("Updated model as N-Triples: \n" + triples); 
					
					logger.info("updated whole model: "+ triples);
					// Execute MERGE MANUAL query
					if(isRebase){
						mergeQuery = StrategyManagement.createRebaseQuery(graphName, sdd, user, message, revisionNumberBranchA, revisionNumberBranchB, RebaseQueryTypeEnum.MANUAL, triples);
					}else {
						mergeQuery = ProcessManagement.createMergeQuery(graphName, sdd, user, message, MergeQueryTypeEnum.MANUAL, revisionNumberBranchA, revisionNumberBranchB, triples);
					}
					
					logger.info("manualmergeQuery:"+mergeQuery);
					
					
				}else{
					
					String triples = ProcessManagement.getTriplesOfMergeWithQuery(differenceModel);
					// Execute MERGE WITH query
					if(isRebase) {
						mergeQuery = StrategyManagement.createRebaseQuery(graphName, sdd, user, message, revisionNumberBranchA, revisionNumberBranchB, RebaseQueryTypeEnum.WITH, triples);
					}else{
						mergeQuery = ProcessManagement.createMergeQuery(graphName, sdd, user, message, MergeQueryTypeEnum.WITH, revisionNumberBranchA, revisionNumberBranchB, triples);
					}
					
					logger.info("withmergeQuery:"+mergeQuery);
				}
				
				return mergeQuery;
				
			}else {
				logger.info("there is still conflict");
				return "there is still conflict";
			}
		}
		
		logger.info("reportResult is null");
		return "reportResult is null";
		
	}
	
	/**updated the difference model in rebase control */
	public void transformDifferenceModelToRebase(){
		rebaseControl.updateRebaseDifferenceModel(differenceModel);
	}
	
		
	
	/**
	 * Push the changes to the remote repository.
	 * @param triplesId id of triples, what als added in end version selected
	 * @throws InternalErrorException 
	 * 
	 * @throws IOException 
	 */
	
	public String updateMergeQuery (String triplesId) throws IOException, InternalErrorException {
		//update DifferenceModel by triples id 
		updateDifferenceModel(triplesId);
		String user = commitModel.getUser();
		String message = commitModel.getMessage();
		String graphName = commitModel.getGraphName();
		String sdd = commitModel.getSddName();
		
		//create Triples
		Model wholeContentModel = ProcessManagement.getWholeContentOfRevision(graphName, revisionNumberBranchB);
		logger.debug("Whole model as N-Triples: \n" + ProcessManagement.writeJenaModelToNTriplesString(wholeContentModel));
		
		logger.info("whole model: " + ProcessManagement.writeJenaModelToNTriplesString(wholeContentModel));

		// Update dataset with local data
		ArrayList<String> list = ProcessManagement.getAllTriplesDividedIntoInsertAndDelete(differenceModel, wholeContentModel);
		
		logger.debug("INSERT: \n" + list.get(0));
		logger.debug("DELETE: \n" + list.get(1));
		
		logger.info("insert Triple: "+list.get(0));
		logger.info("delete Triple: "+list.get(1));

		
		String updateQueryInsert = String.format(
				  "INSERT DATA { %n"
				+ "	%s %n"
				+ "}", list.get(0));
		UpdateAction.parseExecute(updateQueryInsert, wholeContentModel);
		
		String updateQueryDelete = String.format(
				  "DELETE DATA { %n"
				+ " %s %n"
				+ "}", list.get(1));
		UpdateAction.parseExecute(updateQueryDelete, wholeContentModel);
		
		String triples = ProcessManagement.writeJenaModelToNTriplesString(wholeContentModel);
		logger.debug("Updated model as N-Triples: \n" + triples); 
		
		logger.info("updated whole model: "+ triples);
		
		String mergeQuery = ProcessManagement.createMergeQuery(graphName, sdd, user, message, MergeQueryTypeEnum.MANUAL, revisionNumberBranchA, revisionNumberBranchB, triples);
		logger.info("UpdatedmergeQuery:"+mergeQuery);
		
		return mergeQuery;
		
	}
	
	
	/**create Commit Model and save in the mergingControl
	 * @param graphName
	 * @param sdd model name
	 * @param client name 
	 * @param messsage of client
	 * @param name of branch 1
	 * @param name of branch 2
	 * @param name of merging strategy 
	 * @param type of merging */
	public void createCommitModel(String graphName, String sddName, String user, String message, String branch1, String branch2, String strategy,String type){
		commitModel = new CommitModel(graphName, sddName, user, message, branch1, branch2, strategy,type);
	}
	
	/**get the commit model*/
	public CommitModel getCommitModel(){
		return commitModel;
	}
	
	
	/** update difference model nach checkebox in triple table*/
	
	public void updateDifferenceModel(String triplesId) {
		String[] idArray = triplesId.split(",");
			
		Iterator<Entry<String, DifferenceGroup>> iterDM = differenceModel.getDifferenceGroups().entrySet().iterator();
		while(iterDM.hasNext()) {
			Entry<String, DifferenceGroup> entryDG = (Entry<String, DifferenceGroup>) iterDM.next();
			DifferenceGroup differ = (DifferenceGroup) entryDG.getValue();
			Iterator<Entry<String, Difference>> iterDIF = differ.getDifferences().entrySet().iterator();
			while(iterDIF.hasNext()){
				Entry<String, Difference> entryDF = iterDIF.next();
				//get triple
				String tripleString = entryDF.getKey();
				
				logger.info("tripleString : "+ tripleString);
				Difference difference = entryDF.getValue();
				
				difference.setTripleResolutionState(SDDTripleStateEnum.DELETED);
				
				logger.info("test:" + difference.getTripleResolutionState().toString());
				for (String id : idArray) {
					logger.info("tripleId: "+ id);
					Triple checkedTriple = tableModel.getManuellTriple().get(id);
					logger.info("checked triple: "+ ProcessManagement.tripleToString(checkedTriple));
					if (ProcessManagement.tripleToString(checkedTriple).equals(tripleString)){
						difference.setTripleResolutionState(SDDTripleStateEnum.ADDED);
						logger.info("test:" + difference.getTripleResolutionState().toString());
					}				
				}

				
			}
		}
 			
		// only for test updated difference model
		Iterator<Entry<String, DifferenceGroup>> iterD = differenceModel.getDifferenceGroups().entrySet().iterator();
		while(iterD.hasNext()) {
			Entry<String, DifferenceGroup> entryDG = (Entry<String, DifferenceGroup>) iterD.next();
			DifferenceGroup differ = (DifferenceGroup) entryDG.getValue();
			Iterator<Entry<String, Difference>> iterDIF = differ.getDifferences().entrySet().iterator();
			while(iterDIF.hasNext()){
				Entry<String, Difference> entryDF = iterDIF.next();
				//get triple
				String tripleString = entryDF.getKey();
				Difference difference = entryDF.getValue();
				
				logger.info("updated difference model: " + tripleString + difference.getTripleResolutionState() + difference.getResolutionState().toString());
			}
		}		
		
	}
	
	
	
	
	/**
	 * ##########################################################################################################################################################################
	 * ##########################################################################################################################################################################
	 * ##                                                                                                                                                                      ##
	 * ## Semantic enrichment - individuals.                                                                                                                                   ##
	 * ##                                                                                                                                                                      ##
	 * ##########################################################################################################################################################################
	 * ##########################################################################################################################################################################
	 */
	
	
	/**
	 * Create the semantic enrichment List of all individuals.
	 */
	public List<TableEntrySemanticEnrichmentAllIndividuals> createTableModelSemanticEnrichmentAllIndividualsList() {
		List<TableEntrySemanticEnrichmentAllIndividuals> individualTableList = new ArrayList<TableEntrySemanticEnrichmentAllIndividuals>();
		
		if(individualModelBranchA == null || individualModelBranchB == null) {
			return individualTableList ;
		}

		// Get key sets
		ArrayList<String> keySetIndividualModelBranchA = new ArrayList<String>(individualModelBranchA.getIndividualStructures().keySet());
		ArrayList<String> keySetIndividualModelBranchB = new ArrayList<String>(individualModelBranchB.getIndividualStructures().keySet());
		
		// Iterate over all individual URIs of branch A
		@SuppressWarnings("unchecked")
		Iterator<String> iteKeySetIndividualModelBranchA = ((ArrayList<String>) keySetIndividualModelBranchA.clone()).iterator();
		while (iteKeySetIndividualModelBranchA.hasNext()) {
			String currentKeyBranchA = iteKeySetIndividualModelBranchA.next();
			
			// Add all individual URIs to table model which are in both branches
			if (keySetIndividualModelBranchB.contains(currentKeyBranchA)) {
				TableEntrySemanticEnrichmentAllIndividuals tableEntry = new TableEntrySemanticEnrichmentAllIndividuals(individualModelBranchA.getIndividualStructures().get(currentKeyBranchA), individualModelBranchB.getIndividualStructures().get(currentKeyBranchA), new Object[]{currentKeyBranchA, currentKeyBranchA});
				
				individualTableList.add(tableEntry);
				// Remove key from branch A key set copy
				keySetIndividualModelBranchA.remove(currentKeyBranchA);
				// Remove key from branch B key set copy
				keySetIndividualModelBranchB.remove(currentKeyBranchA);
			}
		}
		
		// Iterate over all individual URIs of branch A (will only contain the individuals which are not in B)
		Iterator<String> iteKeySetIndividualModelBranchAOnly = keySetIndividualModelBranchA.iterator();
		while (iteKeySetIndividualModelBranchAOnly.hasNext()) {
			String currentKeyBranchA = iteKeySetIndividualModelBranchAOnly.next();
			TableEntrySemanticEnrichmentAllIndividuals tableEntry = new TableEntrySemanticEnrichmentAllIndividuals(individualModelBranchA.getIndividualStructures().get(currentKeyBranchA), new IndividualStructure(null), new Object[]{currentKeyBranchA, ""});
			individualTableList.add(tableEntry);

		}
		
		// Iterate over all individual URIs of branch B (will only contain the individuals which are not in A)
		Iterator<String> iteKeySetIndividualModelBranchBOnly = keySetIndividualModelBranchB.iterator();
		while (iteKeySetIndividualModelBranchBOnly.hasNext()) {
			String currentKeyBranchB = iteKeySetIndividualModelBranchBOnly.next();
			TableEntrySemanticEnrichmentAllIndividuals tableEntry = new TableEntrySemanticEnrichmentAllIndividuals(new IndividualStructure(null), individualModelBranchB.getIndividualStructures().get(currentKeyBranchB), new Object[]{"", currentKeyBranchB});
			individualTableList.add(tableEntry);


		}
		
		return individualTableList;
	}
	
	public void openRebaseModel() {
		isRebase = true;
	}
	
	public void closeRebaseModel(){
		isRebase = false;
	}
	
	public boolean getIsRebase(){
		return this.isRebase;
	}
	
}











