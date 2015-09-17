package de.tud.plt.r43ples.management;

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.core.MediaType;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import de.tud.plt.r43ples.exception.InternalErrorException;
import de.tud.plt.r43ples.webservice.Endpoint;

/**
 * Provides methods for generation of example graphs.
 * 
 * @author Stephan Hensel
 *
 */
public class DatasetGenerationManagement {

	/** The logger. */
	private static Logger logger = Logger.getLogger(DatasetGenerationManagement.class);
	/** The endpoint. **/
	private static Endpoint ep = new Endpoint();
	
	
	/**
	 * Create new graph.
	 * 
	 * @param graphName the graph name
	 * @throws InternalErrorException 
	 */
	public static void createNewGraph(String graphName) throws InternalErrorException {
		// Purge silent example graph
		String query = String.format("DROP SILENT GRAPH <%s>", graphName);
		ep.sparql(MediaType.TEXT_HTML, query, false);
		
		// Create new example graph
		query = String.format("CREATE SILENT GRAPH <%s>", graphName);
		ep.sparql(MediaType.TEXT_HTML, query);
		
		logger.info("New graph <" + graphName +"> created");
	}
	
	
	/**
	 * Create new branch.
	 * 
	 * @param user the user
	 * @param message the message
	 * @param graphName the graph name
	 * @param revision the revision
	 * @param branchName the branch name
	 * @throws InternalErrorException 
	 */
	public static void createNewBranch(String user, String message, String graphName, String revision, String branchName) throws InternalErrorException {
		String query = String.format(""
				+ "USER \"%s\" %n"
				+ "MESSAGE \"%s\" %n"
				+ "BRANCH GRAPH <%s> REVISION \"%s\" TO \"%s\"", user, message, graphName, revision, branchName);
		ep.sparql(MediaType.TEXT_HTML, query);
		logger.info("New branch \"" + graphName +"\" for <" + graphName +"> created.");
	}
	
	
	/**
	 * Execute INSERT query.
	 * 
	 * @param user the user
	 * @param message the message
	 * @param graphName the graph name
	 * @param revision the revision
	 * @param triples the triples to insert
	 * @throws InternalErrorException 
	 * @throws IOException 
	 * @throws TemplateException 
	 */
	 public static void executeInsertQuery(String user, String message, String graphName, String revision, String triples) throws InternalErrorException {
		String query = String.format(
				  "USER \"%s\" %n"
				+ "MESSAGE \"%s\" %n"
				+ "INSERT DATA { GRAPH <%s> REVISION \"%s\" %n"
				+ "	{ %n"
				+ "	  %s %n"
				+ "	} %n"
				+ "}", user, message, graphName, revision, triples);
		ep.sparql(MediaType.TEXT_HTML, query);
	}
	
	
	/**
	 * Execute DELETE query.
	 * 
	 * @param user the user
	 * @param message the message
	 * @param graphName the graph name
	 * @param revision the revision
	 * @param triples the triples to delete
	 * @throws InternalErrorException 
	 */
	public static void executeDeleteQuery(String user, String message, String graphName, String revision, String triples) throws InternalErrorException {
		String query = String.format(
				  "USER \"%s\" %n"
				+ "MESSAGE \"%s\" %n"
				+ "DELETE DATA { GRAPH <%s> REVISION \"%s\" %n"
				+ "	{ %n"
				+ "	  %s %n"
				+ "	} %n"
				+ "}", user, message, graphName, revision, triples);
		ep.sparql(MediaType.TEXT_HTML, query);
	}
	
	
	/**
	 * Execute DELETE WHERE query.
	 * 
	 * @param user the user
	 * @param message the message
	 * @param graphName the graph name
	 * @param revision the revision
	 * @param triples the triples to delete
	 * @throws InternalErrorException 
	 */
	public static void executeDeleteWhereQuery(String user, String message, String graphName, String revision, String triples) throws InternalErrorException {
		String query = String.format(
				  "USER \"%s\" %n"
				+ "MESSAGE \"%s\" %n"
				+ "DELETE { GRAPH <%s> REVISION \"%s\" %n"
				+ "	{ %n"
				+ "		%s %n"
				+ "	} %n"
				+ "}"
				+ "WHERE { GRAPH <%s> REVISION \"%s\" %n"
				+ "	{ %n"
				+ "		%s %n"
				+ "	} %n"
				+ "}", user, message, graphName, revision, triples, graphName, revision, triples);
		ep.sparql(MediaType.TEXT_HTML, query);
	}
	
	
	/**
	 * Execute INSERT - DELETE query.
	 * 
	 * @param user the user
	 * @param message the message
	 * @param graphName the graphName
	 * @param revision the revision
	 * @param triplesInsert the triples to insert
	 * @param triplesDelete the triples to delete
	 * @throws InternalErrorException 
	 */
	public static void executeInsertDeleteQuery(String user, String message, String graphName, String revision, String triplesInsert, String triplesDelete) throws InternalErrorException {
		String query = String.format(
				  "USER \"%s\" %n"
				+ "MESSAGE \"%s\" %n"
				+ "INSERT DATA { GRAPH <%s> REVISION \"%s\" %n"
				+ "	{ %n"
				+ "	  %s %n"
				+ "	} %n"
				+ "} ; %n"
				+ "DELETE DATA { GRAPH <%s> REVISION \"%s\" %n"
				+ "	{ %n"
				+ "	  %s %n"
				+ "	} %n"
				+ "}", user, message, graphName, revision, triplesInsert, graphName, revision, triplesDelete);
		ep.sparql(MediaType.TEXT_HTML, query);
	}
	
	
	/**
	 * Read file to string.
	 * 
	 * @param path the path to read
	 * @return the file content
	 * @throws IOException
	 */
	public static String readFileToString(String path) throws IOException {
		InputStream is = ClassLoader.getSystemResourceAsStream(path);
		String result = IOUtils.toString(is);
		is.close();
		return result;
	}
	
}
