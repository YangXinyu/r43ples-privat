package de.tud.plt.r43ples.test.merge;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpException;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import static org.custommonkey.xmlunit.XMLAssert.*;
import de.tud.plt.r43ples.management.DatasetGenerationManagement;
import de.tud.plt.r43ples.management.HttpResponse;
import de.tud.plt.r43ples.webservice.Service;

/**
 * Create an example graph of the following structure:
 * 
 *                                              ADD: -      ADD: -                                                     
 *                                           +-----X-----------X-----------(Branch B1X)--+                            
 *                                           |  DEL: B      DEL: C                        \                              
 *                                           |                                             \                             
 *                  ADD: D,E       ADD: G    |        ADD: F                                \                          
 *               +-----X--------------X------+-----------X-----------------(Branch B1)-------+--+                       
 *               |  DEL: A         DEL: D             DEL: -                                     \                       
 *               |                                                                                \                      
 *               |                              ADD: J      ADD: C                                 \                     
 *               |                           +-----X-----------X-----------(Branch B2X)--+          \                                   
 *               |                           |  DEL: -      DEL: I                        \          \                  
 *               |                           |                                             \          \                  
 *               |  ADD: D,H       ADD: I    |  ADD: K,L    ADD: M                          \          \              
 *               +-----X--------------X------+-----X-----------X-----------(Branch B2)-------+----------+--+             
 *               |  DEL: C         DEL: -       DEL: I      DEL: -                                          \            
 *               |                                                                                           \           
 *               |                                                                                            \          
 * ADD: A,B,C    |          ADD: M,N            ADD: P,R,S                                                     \        
 * ---X----------+-------------X-------------------X-----------------------(MASTER)-----------------------------+--      
 * DEL: -                   DEL: C              DEL: M                                                                     
 * 
 * @author Stephan Hensel
 *
 */
public class TestR43plesMerge {

	/** The logger. */
	private static Logger logger = Logger.getLogger(TestR43plesMerge.class);
	/** The r43ples endpoint. **/
	private static String r43ples_endpoint = "http://localhost:9998/r43ples/sparql";
	/** The graph name. **/
	private static String graphName = "http://exampleGraph.com/r43ples/merge";
	/** The user. **/
	private static String user = "shensel";
	
	
	/**
	 * Set up.
	 * 
	 * @throws ConfigurationException
	 * @throws IOException
	 * @throws HttpException
	 */
	@Before
	public void setUp() throws ConfigurationException, IOException, HttpException{
		Service.start();
		createInitialDataSet();
	}

	
	/**
	 * Create the initial data set
	 * 
	 * @throws IOException
	 * @throws HttpException 
	 */
	private void createInitialDataSet() throws IOException, HttpException {
		// Create new example graph
		DatasetGenerationManagement.createNewGraph(graphName);
		
		// Initial commit
		String triples =  "<http://example.com/testS> <http://example.com/testP> \"A\". \n"
						+ "<http://example.com/testS> <http://example.com/testP> \"B\". \n"
						+ "<http://example.com/testS> <http://example.com/testP> \"C\". \n";
		
		DatasetGenerationManagement.executeInsertQuery(user, "Initial commit", graphName, "0", triples);
		
		// Create a new branch B1
		DatasetGenerationManagement.createNewBranch(user, "Create a new branch B1", graphName, "1", "B1");
		
		// Create a new branch B2
		DatasetGenerationManagement.createNewBranch(user, "Create a new branch B2", graphName, "1", "B2");
		
		// First commit to B1
		String triplesInsert =	  "<http://example.com/testS> <http://example.com/testP> \"D\". \n"
								+ "<http://example.com/testS> <http://example.com/testP> \"E\". \n";
		
		String triplesDelete =	  "<http://example.com/testS> <http://example.com/testP> \"A\". \n";
		
		DatasetGenerationManagement.executeInsertDeleteQuery(user, "First commit to B1", graphName, "B1", triplesInsert, triplesDelete);
		
		// Second commit to B1
		triplesInsert =	  "<http://example.com/testS> <http://example.com/testP> \"G\". \n";
		
		triplesDelete =	  "<http://example.com/testS> <http://example.com/testP> \"D\". \n";
		
		DatasetGenerationManagement.executeInsertDeleteQuery(user, "Second commit to B1", graphName, "B1", triplesInsert, triplesDelete);
		
		// Create a new branch B1X
		DatasetGenerationManagement.createNewBranch(user, "Create a new branch B1X", graphName, "B1", "B1X");
		
		// First commit to B1X
		triplesDelete =	  "<http://example.com/testS> <http://example.com/testP> \"B\". \n";
		
		DatasetGenerationManagement.executeDeleteQuery(user, "First commit to B1X", graphName, "B1X", triplesDelete);
		
		// Second commit to B1X
		triplesDelete =	  "<http://example.com/testS> <http://example.com/testP> \"C\". \n";
		
		DatasetGenerationManagement.executeDeleteQuery(user, "Second commit to B1X", graphName, "B1X", triplesDelete);
		
		// Third commit to B1
		triplesInsert =	  "<http://example.com/testS> <http://example.com/testP> \"F\". \n";
		
		DatasetGenerationManagement.executeInsertQuery(user, "Third commit to B1", graphName, "B1", triplesInsert);
		
		// First commit to B2
		triplesInsert =	  "<http://example.com/testS> <http://example.com/testP> \"D\". \n"
						+ "<http://example.com/testS> <http://example.com/testP> \"H\". \n";
		
		triplesDelete =	  "<http://example.com/testS> <http://example.com/testP> \"C\". \n";
		
		DatasetGenerationManagement.executeInsertDeleteQuery(user, "First commit to B2", graphName, "B2", triplesInsert, triplesDelete);
		
		// Second commit to B2
		triplesInsert =	  "<http://example.com/testS> <http://example.com/testP> \"I\". \n";
		
		DatasetGenerationManagement.executeInsertQuery(user, "Second commit to B2", graphName, "B2", triplesInsert);
		
		// Create a new branch B2X
		DatasetGenerationManagement.createNewBranch(user, "Create a new branch B2X", graphName, "B2", "B2X");
		
		// First commit to B2X
		triplesInsert =	  "<http://example.com/testS> <http://example.com/testP> \"J\". \n";
		
		DatasetGenerationManagement.executeInsertQuery(user, "First commit to B2X", graphName, "B2X", triplesInsert);
		
		// Second commit to B2X
		triplesInsert =	  "<http://example.com/testS> <http://example.com/testP> \"C\". \n";
		
		triplesDelete =	  "<http://example.com/testS> <http://example.com/testP> \"I\". \n";
		
		DatasetGenerationManagement.executeInsertDeleteQuery(user, "Second commit to B2X", graphName, "B2X", triplesInsert, triplesDelete);
		
		// Third commit to B2
		triplesInsert =	  "<http://example.com/testS> <http://example.com/testP> \"K\". \n"
						+ "<http://example.com/testS> <http://example.com/testP> \"L\". \n";
		
		triplesDelete =	  "<http://example.com/testS> <http://example.com/testP> \"I\". \n";
		
		DatasetGenerationManagement.executeInsertDeleteQuery(user, "Third commit to B2", graphName, "B2", triplesInsert, triplesDelete);
		
		// Fourth commit to B2
		triplesInsert =	  "<http://example.com/testS> <http://example.com/testP> \"M\". \n";
		
		DatasetGenerationManagement.executeInsertQuery(user, "Fourth commit to B2", graphName, "B2", triplesInsert);
		
		// Second commit to master
		triplesInsert =	  "<http://example.com/testS> <http://example.com/testP> \"M\". \n"
						+ "<http://example.com/testS> <http://example.com/testP> \"N\". \n";
		
		triplesDelete =	  "<http://example.com/testS> <http://example.com/testP> \"C\". \n";
		
		DatasetGenerationManagement.executeInsertDeleteQuery(user, "Second commit to MASTER", graphName, "master", triplesInsert, triplesDelete);
		
		// Third commit to master
		triplesInsert =	  "<http://example.com/testS> <http://example.com/testP> \"P\". \n"
						+ "<http://example.com/testS> <http://example.com/testP> \"R\". \n"
						+ "<http://example.com/testS> <http://example.com/testP> \"S\". \n";
		
		triplesDelete =	  "<http://example.com/testS> <http://example.com/testP> \"M\". \n";
		
		DatasetGenerationManagement.executeInsertDeleteQuery(user, "Third commit to MASTER", graphName, "master", triplesInsert, triplesDelete);
		
		logger.info("Example graph created.");
	}
	
	
	/**
	 * Test the created graph.
	 * 
	 * @throws IOException 
	 */
	@Test
	public void testCreatedGraph() throws IOException {
		// Test branch B1
		String result1 = executeR43plesQueryWithFormat(createSelectQuery(graphName, "B1"), "application/xml").replace("\n", "").replace("\r", "");
		String expected1 = DatasetGenerationManagement.readFileToString("test/resources/merge/response-B1.xml", StandardCharsets.UTF_8).replace("\n", "").replace("\r", "");
		Assert.assertEquals(expected1, result1);
		
		// Test branch B1X
		String result2 = executeR43plesQueryWithFormat(createSelectQuery(graphName, "B1X"), "application/xml").replace("\n", "").replace("\r", "");
		String expected2 = DatasetGenerationManagement.readFileToString("test/resources/merge/response-B1X.xml", StandardCharsets.UTF_8).replace("\n", "").replace("\r", "");
		Assert.assertEquals(expected2, result2);
		
		// Test branch B2
		String result3 = executeR43plesQueryWithFormat(createSelectQuery(graphName, "B2"), "application/xml").replace("\n", "").replace("\r", "");
		String expected3 = DatasetGenerationManagement.readFileToString("test/resources/merge/response-B2.xml", StandardCharsets.UTF_8).replace("\n", "").replace("\r", "");
		Assert.assertEquals(expected3, result3);
		
		// Test branch B2X
		String result4 = executeR43plesQueryWithFormat(createSelectQuery(graphName, "B2X"), "application/xml").replace("\n", "").replace("\r", "");
		String expected4 = DatasetGenerationManagement.readFileToString("test/resources/merge/response-B2X.xml", StandardCharsets.UTF_8).replace("\n", "").replace("\r", "");
		Assert.assertEquals(expected4, result4);
		
		// Test branch MASTER
		String result5 = executeR43plesQueryWithFormat(createSelectQuery(graphName, "master"), "application/xml").replace("\n", "").replace("\r", "");
		String expected5 = DatasetGenerationManagement.readFileToString("test/resources/merge/response-MASTER.xml", StandardCharsets.UTF_8).replace("\n", "").replace("\r", "");
		Assert.assertEquals(expected5, result5);
	}
	
	
	/**
	 * Test AUTO-MERGE.
	 * 
	 * @throws IOException 
	 * @throws SAXException 
	 */
	@Test
	public void testAutoMerge() throws IOException, SAXException {
		// The SDD to use
		String sdd = "http://eatld.et.tu-dresden.de/sdd#defaultSDD";
		
		// Merge B1X into B1
		executeR43plesQueryWithFormat(createAutoMergeQuery(graphName, sdd, user, "Merge B1X into B1", "B1X", "B1"), "application/xml");
		// Test branch B1
		String result1 = executeR43plesQueryWithFormat(createSelectQuery(graphName, "B1"), "application/xml");
		String expected1 = DatasetGenerationManagement.readFileToString("test/resources/merge/auto/response-B1-B1X-into-B1.xml", StandardCharsets.UTF_8);
		assertXMLEqual(expected1, result1);
		
		
		// Merge B2X into B2
		executeR43plesQueryWithFormat(createAutoMergeQuery(graphName, sdd, user, "Merge B2X into B2", "B2X", "B2"), "application/xml");
		
		// Test branch B2
		String result2 = executeR43plesQueryWithFormat(createSelectQuery(graphName, "B2"), "application/xml").replace("\n", "").replace("\r", "");
		String expected2 = DatasetGenerationManagement.readFileToString("test/resources/merge/auto/response-B2-B2X-into-B2.xml", StandardCharsets.UTF_8).replace("\n", "").replace("\r", "");
		Assert.assertEquals(expected2, result2);
		
		
		// Merge B1 into B2
		executeR43plesQueryWithFormat(createAutoMergeQuery(graphName, sdd, user, "Merge B1 into B2", "B1", "B2"), "application/xml");
		
		// Test branch B2
		String result3 = executeR43plesQueryWithFormat(createSelectQuery(graphName, "B2"), "application/xml").replace("\n", "").replace("\r", "");
		String expected3 = DatasetGenerationManagement.readFileToString("test/resources/merge/auto/response-B2-B1-into-B2.xml", StandardCharsets.UTF_8).replace("\n", "").replace("\r", "");
		Assert.assertEquals(expected3, result3);
		
		
		// Merge B2 into MASTER
		executeR43plesQueryWithFormat(createAutoMergeQuery(graphName, sdd, user, "Merge B2 into MASTER", "B2", "master"), "application/xml");
		
		// Test branch MASTER
		String result4 = executeR43plesQueryWithFormat(createSelectQuery(graphName, "master"), "application/xml").replace("\n", "").replace("\r", "");
		String expected4 = DatasetGenerationManagement.readFileToString("test/resources/merge/auto/response-MASTER-B2-into-MASTER.xml", StandardCharsets.UTF_8).replace("\n", "").replace("\r", "");
		Assert.assertEquals(expected4, result4);
	}
	
	
	/**
	 * Test common MERGE.
	 * 
	 * @throws IOException 
	 * @throws SAXException 
	 */
	@Test
	public void testCommonMerge() throws IOException, SAXException {
		// The SDD to use
		String sdd = "http://eatld.et.tu-dresden.de/sdd#defaultSDD";
		
		// Merge B1X into B1
		HttpResponse queryResult1 = executeQueryWithoutAuthorizationPostResponse(createCommonMergeQuery(graphName, sdd, user, "Merge B1X into B1", "B1X", "B1"), "application/xml");
		Assert.assertEquals("", queryResult1.getBody());
		
		// Test branch B1
		String result1 = executeR43plesQueryWithFormat(createSelectQuery(graphName, "B1"), "application/xml");
		String expected1 = DatasetGenerationManagement.readFileToString("test/resources/merge/common/response-B1-B1X-into-B1.xml", StandardCharsets.UTF_8);
		assertXMLEqual(expected1, result1);
		
		
		// Merge B2X into B2
		HttpResponse queryResult2 = executeQueryWithoutAuthorizationPostResponse(createCommonMergeQuery(graphName, sdd, user, "Merge B2X into B2", "B2X", "B2"), "application/xml");
		String queryExpected2 = DatasetGenerationManagement.readFileToString("test/resources/merge/common/response-query-B2X-into-B2.xml", StandardCharsets.UTF_8).replace("\n", "").replace("\r", "");
		Assert.assertEquals(queryExpected2, queryResult2.getBody().replace("\n", "").replace("\r", ""));
		
		// Test branch B2
		String result2 = executeR43plesQueryWithFormat(createSelectQuery(graphName, "B2"), "application/xml").replace("\n", "").replace("\r", "");
		String expected2 = DatasetGenerationManagement.readFileToString("test/resources/merge/common/response-B2-B2X-into-B2.xml", StandardCharsets.UTF_8).replace("\n", "").replace("\r", "");
		Assert.assertEquals(expected2, result2);
		
		
		// Merge B1 into B2
		HttpResponse queryResult3 = executeQueryWithoutAuthorizationPostResponse(createCommonMergeQuery(graphName, sdd, user, "Merge B1 into B2", "B1", "B2"), "application/xml");
		String queryExpected3 = DatasetGenerationManagement.readFileToString("test/resources/merge/common/response-query-B1-into-B2.xml", StandardCharsets.UTF_8).replace("\n", "").replace("\r", "");
		Assert.assertNotEquals(queryExpected3, queryResult3.getBody().replace("\n", "").replace("\r", ""));
		
		// Merge B1 into B2 (WITH)
		String triples = "<http://example.com/testS> <http://example.com/testP> \"D\". \n";
		
		HttpResponse queryResult3_1 = executeQueryWithoutAuthorizationPostResponse(createMergeWithQuery(graphName, sdd, user, "Merge B1 into B2", "B1", "B2", triples), "application/xml");
		String queryExpected3_1 = DatasetGenerationManagement.readFileToString("test/resources/merge/common/response-query-with-B1-into-B2.xml", StandardCharsets.UTF_8).replace("\n", "").replace("\r", "");
		Assert.assertEquals(queryExpected3_1, queryResult3_1.getBody().replace("\n", "").replace("\r", ""));

		// Test branch B2
		String result3 = executeR43plesQueryWithFormat(createSelectQuery(graphName, "B2"), "application/xml").replace("\n", "").replace("\r", "");
		String expected3 = DatasetGenerationManagement.readFileToString("test/resources/merge/common/response-B2-B1-into-B2.xml", StandardCharsets.UTF_8).replace("\n", "").replace("\r", "");
		Assert.assertEquals(expected3, result3);

		
		// Merge B2 into MASTER
		HttpResponse queryResult4 = executeQueryWithoutAuthorizationPostResponse(createCommonMergeQuery(graphName, sdd, user, "Merge B2 into MASTER", "B2", "master"), "application/xml");
		String queryExpected4 = DatasetGenerationManagement.readFileToString("test/resources/merge/common/response-query-B2-into-MASTER.xml", StandardCharsets.UTF_8).replace("\n", "").replace("\r", "");
		Assert.assertNotEquals(queryExpected4, queryResult4.getBody().replace("\n", "").replace("\r", ""));
		
		// Merge B1 into B2 (WITH)
		triples = "<http://example.com/testS> <http://example.com/testP> \"M\". \n";
		
		HttpResponse queryResult4_1 = executeQueryWithoutAuthorizationPostResponse(createMergeWithQuery(graphName, sdd, user, "Merge B2 into MASTER", "B2", "master", triples), "application/xml");
		String queryExpected4_1 = DatasetGenerationManagement.readFileToString("test/resources/merge/common/response-query-with-B2-into-MASTER.xml", StandardCharsets.UTF_8).replace("\n", "").replace("\r", "");
		Assert.assertEquals(queryExpected4_1, queryResult4_1.getBody().replace("\n", "").replace("\r", ""));
		
		// Test branch MASTER
		String result4 = executeR43plesQueryWithFormat(createSelectQuery(graphName, "master"), "application/xml").replace("\n", "").replace("\r", "");
		String expected4 =DatasetGenerationManagement.readFileToString("test/resources/merge/common/response-MASTER-B2-into-MASTER.xml", StandardCharsets.UTF_8).replace("\n", "").replace("\r", "");
		Assert.assertEquals(expected4, result4);
	}
	
	
	/**
	 * Test MANUAL-MERGE.
	 * 
	 * @throws IOException 
	 */
	@Test
	public void testManualMerge() throws IOException {
		// The SDD to use
		String sdd = "http://eatld.et.tu-dresden.de/sdd#defaultSDD";
		
		// Merge B1X into B1
		String triples = "<http://example.com/testS> <http://example.com/testP> \"X\". \n";
		
		executeR43plesQueryWithFormat(createManualMergeQuery(graphName, sdd, user, "Merge B1X into B1", "B1X", "B1", triples), "application/xml");
		// Test branch B1
		String result1 = executeR43plesQueryWithFormat(createSelectQuery(graphName, "B1"), "application/xml").replace("\n", "").replace("\r", "");
		String expected1 = DatasetGenerationManagement.readFileToString("test/resources/merge/manual/response-B1-B1X-into-B1.xml", StandardCharsets.UTF_8).replace("\n", "").replace("\r", "");
		Assert.assertEquals(expected1, result1);
		
		
		// Merge B2X into B2
		triples = "<http://example.com/testS> <http://example.com/testP> \"Y\". \n";
		
		executeR43plesQueryWithFormat(createManualMergeQuery(graphName, sdd, user, "Merge B2X into B2", "B2X", "B2", triples), "application/xml");
		
		// Test branch B2
		String result2 = executeR43plesQueryWithFormat(createSelectQuery(graphName, "B2"), "application/xml").replace("\n", "").replace("\r", "");
		String expected2 = DatasetGenerationManagement.readFileToString("test/resources/merge/manual/response-B2-B2X-into-B2.xml", StandardCharsets.UTF_8).replace("\n", "").replace("\r", "");
		Assert.assertEquals(expected2, result2);
		
		
		// Merge B1 into B2
		triples = "<http://example.com/testS> <http://example.com/testP> \"X\". \n"
				+ "<http://example.com/testS> <http://example.com/testP> \"Z\". \n";
		
		executeR43plesQueryWithFormat(createManualMergeQuery(graphName, sdd, user, "Merge B1 into B2", "B1", "B2", triples), "application/xml");
		
		// Test branch B2
		String result3 = executeR43plesQueryWithFormat(createSelectQuery(graphName, "B2"), "application/xml").replace("\n", "").replace("\r", "");
		String expected3 = DatasetGenerationManagement.readFileToString("test/resources/merge/manual/response-B2-B1-into-B2.xml", StandardCharsets.UTF_8).replace("\n", "").replace("\r", "");
		Assert.assertEquals(expected3, result3);
		
		
		// Merge B2 into MASTER
		triples = "<http://example.com/testS> <http://example.com/testP> \"A\". \n"
				+ "<http://example.com/testS> <http://example.com/testP> \"B\". \n"
				+ "<http://example.com/testS> <http://example.com/testP> \"C\". \n";
		
		executeR43plesQueryWithFormat(createManualMergeQuery(graphName, sdd, user, "Merge B2 into MASTER", "B2", "master", triples), "application/xml");
		
		// Test branch MASTER
		String result4 = executeR43plesQueryWithFormat(createSelectQuery(graphName, "master"), "application/xml").replace("\n", "").replace("\r", "");
		String expected4 = DatasetGenerationManagement.readFileToString("test/resources/merge/manual/response-MASTER-B2-into-MASTER.xml", StandardCharsets.UTF_8).replace("\n", "").replace("\r", "");
		Assert.assertEquals(expected4, result4);
	}
	
	
	/**
	 * Tear down.
	 */
	@After
	public void tearDown() {
		Service.stop();
	}
	
	
	/**
	 * Create the SELECT query.
	 * 
	 * @param graphName the graph name
	 * @param revision the revision
	 * @return the query
	 */
	private String createSelectQuery(String graphName, String revision) {
		return String.format( "SELECT * FROM <%s> REVISION \"%s\" %n"
							+ "WHERE { %n"
							+ "	?s ?p ?o . %n"
							+ "} %n"
							+ "ORDER BY ?s ?p ?o", graphName, revision);
	}
	

	/**
	 * Create AUTO-MERGE query.
	 * 
	 * @param graphName the graph name
	 * @param sdd the SDD
	 * @param user the user
	 * @param commitMessage the commit message
	 * @param branchNameA the branch name A
	 * @param branchNameB the branch name B
	 * @return the query
	 */
	private String createAutoMergeQuery(String graphName, String sdd, String user, String commitMessage, String branchNameA, String branchNameB) {
		return String.format( "USER \"%s\" %n"
							+ "MESSAGE \"%s\" %n"
							+ "MERGE AUTO GRAPH <%s> SDD <%s> BRANCH \"%s\" INTO \"%s\"", user, commitMessage, graphName, sdd, branchNameA, branchNameB);
	}
	
	
	/**
	 * Create common MERGE query.
	 * 
	 * @param graphName the graph name
	 * @param sdd the SDD
	 * @param user the user
	 * @param commitMessage the commit message
	 * @param branchNameA the branch name A
	 * @param branchNameB the branch name B
	 * @return the query
	 */
	private String createCommonMergeQuery(String graphName, String sdd, String user, String commitMessage, String branchNameA, String branchNameB) {
		return String.format( "USER \"%s\" %n"
							+ "MESSAGE \"%s\" %n"
							+ "MERGE GRAPH <%s> SDD <%s> BRANCH \"%s\" INTO \"%s\"", user, commitMessage, graphName, sdd, branchNameA, branchNameB);
	}

	
	/**
	 * Create MERGE-WITH query.
	 * 
	 * @param graphName the graph name
	 * @param sdd the SDD
	 * @param user the user
	 * @param commitMessage the commit message
	 * @param branchNameA the branch name A
	 * @param branchNameB the branch name B
	 * @param triples the triples which should be in the WITH part as N-Triples
	 * @return the query
	 */
	private String createMergeWithQuery(String graphName, String sdd, String user, String commitMessage, String branchNameA, String branchNameB, String triples) {
		return String.format( "USER \"%s\" %n"
							+ "MESSAGE \"%s\" %n"
							+ "MERGE GRAPH <%s> SDD <%s> BRANCH \"%s\" INTO \"%s\" WITH { %n"
							+ "	%s %n"
							+ "}", user, commitMessage, graphName, sdd, branchNameA, branchNameB, triples);
	}
	
	
	/**
	 * Create MANUAL-MERGE query.
	 * 
	 * @param graphName the graph name
	 * @param sdd the SDD
	 * @param user the user
	 * @param commitMessage the commit message
	 * @param branchNameA the branch name A
	 * @param branchNameB the branch name B
	 * @param triples the triples which should be in the WITH part as N-Triples
	 * @return the query
	 */
	private String createManualMergeQuery(String graphName, String sdd, String user, String commitMessage, String branchNameA, String branchNameB, String triples) {
		return String.format( "USER \"%s\" %n"
							+ "MESSAGE \"%s\" %n"
							+ "MERGE MANUAL GRAPH <%s> SDD <%s> BRANCH \"%s\" INTO \"%s\" WITH { %n"
							+ "	%s %n"
							+ "}", user, commitMessage, graphName, sdd, branchNameA, branchNameB, triples);
	}

	
	/**
	 * Executes a SPARQL-query against the R43ples r43ples_endpoint
	 * 
	 * @param query the SPARQL query
	 * @return the result of the query
	 * @throws IOException 
	 */
	public static String executeR43plesQuery(String query) throws IOException {
		return executeR43plesQueryWithFormat(query, "application/xml");
	}
	
	/**
	 * Executes a SPARQL-query against the R43ples r43ples_endpoint
	 * 
	 * @param query the SPARQL query
	 * @return the result of the query
	 * @throws IOException 
	 */
	public static String executeR43plesQueryWithFormat(String query, String format) throws IOException {
		logger.debug(query);
		
		URL url = null;
		
		url = new URL(r43ples_endpoint+ "?query=" + URLEncoder.encode(query, "UTF-8")+ "&format=" + URLEncoder.encode(format, "UTF-8") );
		logger.debug(url.toString());

		URLConnection con = null;
		InputStream in = null;
		con = url.openConnection();
		in = con.getInputStream();
	
		String encoding = con.getContentEncoding();
		encoding = (encoding == null) ? "UTF-8" : encoding;
		String body = IOUtils.toString(in, encoding);
		return body;
	}
	
	
	/**
	 * Executes a SPARQL-query against the triple store without authorization using HTTP-POST.
	 * 
	 * @param query the SPARQL query
	 * @param format the format of the result (e.g. HTML, xml/rdf, JSON, ...)
	 * @return the response
	 * @throws IOException 
	 */
	public static HttpResponse executeQueryWithoutAuthorizationPostResponse(String query, String format) throws IOException {
		URL url = new URL(r43ples_endpoint);
		Map<String,Object> params = new LinkedHashMap<>();
		params.put("query", query);
		params.put("format", format);
		params.put("timeout", 0);
				
		StringBuilder postData = new StringBuilder();
		for (Map.Entry<String,Object> param : params.entrySet()) {
			if (postData.length() != 0) postData.append('&');
			postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
			postData.append('=');
			postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
			}
		byte[] postDataBytes = postData.toString().getBytes("UTF-8");
		logger.debug(postData.toString());
		HttpURLConnection conn = (HttpURLConnection)url.openConnection();
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
		conn.setDoOutput(true);
		conn.getOutputStream().write(postDataBytes);
		
		InputStream in;
		if (conn.getResponseCode() >= 400) {
			in = conn.getErrorStream();
		} else {
			in = conn.getInputStream();
		}

		String encoding = conn.getContentEncoding();
		encoding = (encoding == null) ? "UTF-8" : encoding;
		String body = IOUtils.toString(in, encoding);

		// Return the response
		return new HttpResponse(conn.getResponseCode(), conn.getHeaderFields(), body);
	}

}
