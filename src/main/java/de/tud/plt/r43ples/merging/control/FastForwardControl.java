package de.tud.plt.r43ples.merging.control;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import de.tud.plt.r43ples.exception.InternalErrorException;
import de.tud.plt.r43ples.management.GitRepositoryState;
import de.tud.plt.r43ples.management.RevisionManagement;
import de.tud.plt.r43ples.merging.management.StrategyManagement;
import de.tud.plt.r43ples.merging.model.structure.CommitModel;
import de.tud.plt.r43ples.webservice.Endpoint;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;

public class FastForwardControl {
	private static Logger logger = Logger.getLogger(FastForwardControl.class);
	private CommitModel commitModel;
	
	/**get the report page of fast forward query
	 * @throws IOException 
	 * @throws TemplateException */

	public String getFastForwardReportView(String graphName) throws TemplateException, IOException{
		Map<String, Object> scope = new HashMap<String, Object>();
		StringWriter sw = new StringWriter();
		freemarker.template.Template temp = null; 
		String name = "mergingResultView.ftl";
		try {  
            // 通过Freemarker的Configuration读取相应的Ftl  
            Configuration cfg = new Configuration();  
            // 设定去哪里读取相应的ftl模板  
            cfg.setClassForTemplateLoading(MergingControl.class, "/templates");
            // 在模板文件目录中寻找名称为name的模板文件  
            temp = cfg.getTemplate(name);  
        } catch (IOException e) {  
            e.printStackTrace();  
        }  
		
		scope.put("graphName", graphName);
		scope.put("commit", commitModel);
		scope.put("version", Endpoint.class.getPackage().getImplementationVersion() );
		scope.put("git", GitRepositoryState.getGitRepositoryState());
		
		temp.process(scope,sw);		
		return sw.toString();	
	}
	
	/**check the condition for fast forward strategy
	 * @param graphName : name of named graph
	 * @param branch1 : name of branch1
	 * @param branch2 : name of branch2
	 * @throws InternalErrorException */
	
	public static boolean fastForwardCheck(String graphName, String branch1 , String branch2) throws InternalErrorException {
		if(branch1.equals(branch2)) {
			return false;
		}
		
		//get last revision of each branch
		String revisionUriA = RevisionManagement.getRevisionUri(graphName, branch1);
		String revisionUriB = RevisionManagement.getRevisionUri(graphName, branch2);
		
		return StrategyManagement.isFastForward(revisionUriA, revisionUriB);
	}
	
	
	public void createCommitModel(String graphName, String sddName, String user, String message, String branch1, String branch2, String strategy, String type){
		commitModel = new CommitModel(graphName, sddName, user, message, branch1, branch2, strategy, type);
	}
	
}
