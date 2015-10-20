/**
 * Copyright 2015 Delft University of Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.tudelft.graphalytics.mapreducev2;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import nl.tudelft.graphalytics.PlatformExecutionException;
import nl.tudelft.graphalytics.domain.*;
import nl.tudelft.graphalytics.mapreducev2.reporting.logging.MapReduceV2Logger;
import nl.tudelft.graphalytics.reporting.granula.GranulaManager;
import nl.tudelft.pds.granula.modeller.mapreducev2.job.MapReduceV2;
import nl.tudelft.pds.granula.modeller.model.job.JobModel;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.util.ToolRunner;
import nl.tudelft.graphalytics.Platform;
import nl.tudelft.graphalytics.configuration.ConfigurationUtil;
import nl.tudelft.graphalytics.mapreducev2.bfs.BreadthFirstSearchJobLauncher;
import nl.tudelft.graphalytics.mapreducev2.cd.CommunityDetectionJobLauncher;
import nl.tudelft.graphalytics.mapreducev2.conn.ConnectedComponentsJobLauncher;
import nl.tudelft.graphalytics.mapreducev2.conversion.DirectedVertexToAdjacencyListConversion;
import nl.tudelft.graphalytics.mapreducev2.conversion.EdgesToAdjacencyListConversion;
import nl.tudelft.graphalytics.mapreducev2.evo.ForestFireModelJobLauncher;
import nl.tudelft.graphalytics.mapreducev2.stats.STATSJobLauncher;

/**
 * Graphalytics Platform implementation for the MapReduce v2 platform. Manages
 * datasets on HDFS and launches MapReduce jobs to run algorithms on these
 * datasets.
 *
 * @author Tim Hegeman
 */
public class MapReduceV2Platform implements Platform {
//	private static final Logger log = LogManager.getLogger();
	
	private static final Map<Algorithm, Class<? extends MapReduceJobLauncher>> jobClassesPerAlgorithm = new HashMap<>();

	// Register the MapReduceJobLaunchers for all known algorithms
	{
		jobClassesPerAlgorithm.put(Algorithm.BFS, BreadthFirstSearchJobLauncher.class);
		jobClassesPerAlgorithm.put(Algorithm.CD, CommunityDetectionJobLauncher.class);
		jobClassesPerAlgorithm.put(Algorithm.CONN, ConnectedComponentsJobLauncher.class);
		jobClassesPerAlgorithm.put(Algorithm.EVO, ForestFireModelJobLauncher.class);
		jobClassesPerAlgorithm.put(Algorithm.STATS, STATSJobLauncher.class);
	}

	/** Property key for the directory on HDFS in which to store all input and output. */
	public static final String HDFS_DIRECTORY_KEY = "hadoop.hdfs.directory";
	/** Default value for the directory on HDFS in which to store all input and output. */
	public static final String HDFS_DIRECTORY = "graphalytics";
	
	private Map<String, String> hdfsPathForGraphName = new HashMap<>();
	
	private org.apache.commons.configuration.Configuration mrConfig;

	private String hdfsDirectory;

	/**
	 * Initialises the platform driver by reading the platform-specific properties file.
	 */
	public MapReduceV2Platform() {
		try {
			mrConfig = new PropertiesConfiguration("mapreducev2.properties");
		} catch (ConfigurationException e) {
//			log.warn("Could not find or load mapreducev2.properties.");
			mrConfig = new PropertiesConfiguration();
		}
		hdfsDirectory = mrConfig.getString(HDFS_DIRECTORY_KEY, HDFS_DIRECTORY);
	}

	// TODO: Should the preprocessing be part of executeAlgorithmOnGraph?
	public void uploadGraph(Graph graph, String graphFilePath) throws IOException {
//		log.entry(graph, graphFilePath);


		MapReduceV2Logger.stopCoreLogging();
		
		String hdfsPathRaw = hdfsDirectory + "/mapreducev2/input/raw-" + graph.getName();
		String hdfsPath = hdfsDirectory + "/mapreducev2/input/" + graph.getName();
		
		// Establish a connection with HDFS and upload the graph
		Configuration conf = new Configuration();
		FileSystem dfs = FileSystem.get(conf);
		dfs.copyFromLocalFile(new Path(graphFilePath), new Path(hdfsPathRaw));
		
		// If the graph needs to be preprocessed, do so, otherwise rename it
		if (graph.getGraphFormat().isEdgeBased()) {
			try {
				EdgesToAdjacencyListConversion job = new EdgesToAdjacencyListConversion(hdfsPathRaw, hdfsPath, graph.getGraphFormat().isDirected());
				if (mrConfig.containsKey("mapreducev2.reducer-count"))
					job.withNumberOfReducers(ConfigurationUtil.getInteger(mrConfig, "mapreducev2.reducer-count"));
				job.run();
			} catch (Exception e) {
				throw new IOException("Failed to preprocess graph: ", e);
			}
		} else if (graph.getGraphFormat().isDirected()) {
			try {
				DirectedVertexToAdjacencyListConversion job =
						new DirectedVertexToAdjacencyListConversion(hdfsPathRaw, hdfsPath);
				if (mrConfig.containsKey("mapreducev2.reducer-count"))
					job.withNumberOfReducers(ConfigurationUtil.getInteger(mrConfig, "mapreducev2.reducer-count"));
				job.run();
			} catch (Exception e) {
				throw new IOException("Failed to preprocess graph: ", e);
			}
		} else {
			// Rename the graph
			dfs.rename(new Path(hdfsPathRaw), new Path(hdfsPath));
		}
		
		hdfsPathForGraphName.put(graph.getName(), hdfsPath);

		MapReduceV2Logger.startCoreLogging();
//		log.exit();
	}


	@Override
	public void preBenchmark(Benchmark benchmark) {

		MapReduceV2Logger.stopCoreLogging();
		if(GranulaManager.isLoggingEnabled) {
			String logDataPath = benchmark.getLogPath();
			MapReduceV2Logger.startPlatformLogging(logDataPath + "/OperationLog/driver.logs");
		}

	}

	@Override
	public void postBenchmark(Benchmark benchmark) {

		if(GranulaManager.isLoggingEnabled) {
			String logDataPath = benchmark.getLogPath();
			MapReduceV2Logger.collectYarnLogs(logDataPath);
			MapReduceV2Logger.stopPlatformLogging();
		}
		MapReduceV2Logger.startCoreLogging();
	}

	public PlatformBenchmarkResult executeAlgorithmOnGraph(Benchmark benchmark)
			throws PlatformExecutionException {

		Algorithm algorithm = benchmark.getAlgorithm();
		Graph graph = benchmark.getGraph();
		Object parameters = benchmark.getAlgorithmParameters();

//		log.entry(algorithm, graph);

		int result;
		try {
			MapReduceJobLauncher job = jobClassesPerAlgorithm.get(algorithm).newInstance();

			job.parseGraphData(graph, parameters);
			job.setInputPath(hdfsPathForGraphName.get(graph.getName()));
			job.setIntermediatePath(hdfsDirectory + "/mapreducev2/intermediate/" + algorithm + "-" + graph.getName());
			job.setOutputPath(hdfsDirectory + "/mapreducev2/output/" + algorithm + "-" + graph.getName());

			// Set the number of reducers, if specified
			if (mrConfig.containsKey("mapreducev2.reducer-count"))
				job.setNumReducers(ConfigurationUtil.getInteger(mrConfig, "mapreducev2.reducer-count"));

			Configuration configuration = new Configuration();
			configuration.set("mapreduce.job.user.classpath.first", "true");
			result = ToolRunner.run(configuration, job, new String[0]);
		} catch (Exception e) {
			throw new PlatformExecutionException("MapReduce job failed with exception: ", e);
		}

		if (result != 0)
			throw new PlatformExecutionException("MapReduce job completed with exit code = " + result);

		return new PlatformBenchmarkResult(NestedConfiguration.empty());
	}

	public void deleteGraph(String graphName) {
		// TODO Auto-generated method stub
//		log.entry(graphName);

//		log.exit();
	}
	
	@Override
	public String getName() {
		return "mapreducev2";
	}

	@Override
	public NestedConfiguration getPlatformConfiguration() {
		try {
			org.apache.commons.configuration.Configuration configuration =
					new PropertiesConfiguration("mapreducev2.properties");
			return NestedConfiguration.fromExternalConfiguration(configuration, "mapreducev2.properties");
		} catch (ConfigurationException ex) {
			return NestedConfiguration.empty();
		}
	}

	@Override
	public JobModel getGranulaModel() {
		return new MapReduceV2();
	}
}
