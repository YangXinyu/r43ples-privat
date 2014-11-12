package de.tud.plt.r43ples.webservice;

import java.io.IOException;
import java.net.URI;

import javax.ws.rs.core.UriBuilder;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.http.HttpException;
import org.apache.log4j.Logger;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.StaticHttpHandler;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;

import com.sun.jersey.api.container.ContainerFactory;
import com.sun.jersey.api.container.grizzly2.GrizzlyServerFactory;
import com.sun.jersey.api.core.ClassNamesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;

import de.tud.plt.r43ples.management.Config;
import de.tud.plt.r43ples.management.TripleStoreInterface;


/**
 * R43ples Web Service.
 * Main Class starting the web server on grizzly.
 * 
 * @author Stephan Hensel
 * @author Markus Graube
 *
 */
public class Service {

	/** The logger */
	private static Logger logger = Logger.getLogger(Service.class);
	private static HttpServer server;
	
	public static void main(String[] args) throws ConfigurationException, IOException, HttpException {
		start();
		while(true);
	}
	
	public static void start()  throws ConfigurationException, IOException, HttpException {
//		logger.info("Starting R43ples on grizzly...");
//		Config.readConfig("r43ples.conf");
//		URI BASE_URI = UriBuilder.fromUri(Config.service_uri).port(Config.service_port).build();
//		ResourceConfig rc = new ClassNamesResourceConfig("de.tud.plt.r43ples.webservice.Endpoint");
//		server = GrizzlyServerFactory.createHttpServer(BASE_URI, rc);
//		server.getServerConfiguration().addHttpHandler(
//		        new StaticHttpHandler("./resources/webapp/"), "/static/");
//		server.start();
//		logger.info(String.format("Server started - R43ples endpoint available under: %sr43ples/sparql", BASE_URI));
//		
//		logger.info("Version: "+ Service.class.getPackage().getImplementationVersion());
//		
//		TripleStoreInterface.init(Config.sparql_endpoint, Config.sparql_user, Config.sparql_password);
		
		logger.info("Starting R43ples on grizzly...");
		Config.readConfig("r43ples.conf");
		URI BASE_URI = UriBuilder.fromUri(Config.service_uri).port(Config.service_port).build();
		ResourceConfig rc = new ClassNamesResourceConfig("de.tud.plt.r43ples.webservice.Endpoint");
		
		SSLContextConfigurator sslCon = new SSLContextConfigurator();
//		sslCon.setKeyStoreFile(ConfigLoader.getKeystoreLocation()); // contains server keypair
//	    sslCon.setKeyStorePass(ConfigLoader.getKeystorePassword());
		
		HttpHandler hand = ContainerFactory.createContainer(HttpHandler.class, rc);
		
		server = GrizzlyServerFactory.createHttpServer(BASE_URI, hand, true, 
				new SSLEngineConfigurator(sslCon, false, false, false));

//		server = GrizzlyServerFactory.createHttpServer(BASE_URI, rc);

		server.getServerConfiguration().addHttpHandler(
		        new StaticHttpHandler("./resources/webapp/"), "/static/");
		server.start();
		logger.info(String.format("Server started - R43ples endpoint available under: %sr43ples/sparql", BASE_URI));
		
		logger.info("Version: "+ Service.class.getPackage().getImplementationVersion());
		
		TripleStoreInterface.init(Config.sparql_endpoint, Config.sparql_user, Config.sparql_password);
	}
	
	
	public static void stop() {
		server.stop();
	}
	
}
