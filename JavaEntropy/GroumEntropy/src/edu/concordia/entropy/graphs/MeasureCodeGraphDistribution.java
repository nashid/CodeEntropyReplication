package edu.concordia.entropy.graphs;

import static config.GlobalConfig.concernedLibs;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import data.MethodInfo;
import data.NodeSequenceInfoMap;
import data.TypeInfo;
import datastructure.GlobalData;
import datastructure.Graph;
import datastructure.Node;
import edu.concordia.entropy.graphs.util.GraphUtil;
import groumvisitors.JavaGroumVisitor;
import storage.GraphDatabase;
import utils.DataUtils;
import utils.GraphDrawingUtils;
import utils.Logger;

/**
 * Measures the code graph (GROUM) distribution among the test projects.
 * 
 * @author Dharani Kumar Palani
 *
 */
public class MeasureCodeGraphDistribution {
	// static int numJavaProjects = 0;
	static int numClasses = 0;
	static int numMethods = 0;
	static long LOCs = 0l;
	public static boolean isDoCompactMethod = false;
	static final int aGraphCount = 1;
	static long addedGraphs = 0;

	public static LinkedHashSet<MethodGraphs> methodGraphs = new LinkedHashSet<MethodGraphs>();

	public static final String GRAPHDB_FILE = "graphDB_all_projects_5node_one_api";
	public static final boolean FILTER_API = true;

	public static final int TOP_N = 5;
	public static final int GRAPH_SIZE = 5;
	public static final int MAX_NEIGHBOUR_NODES = 20;

	public static final String PROJECTS_BASEDIR = "/home/d_palan/graph_entrophy/projects";

	public static final String GRAPHDB_DIR = PROJECTS_BASEDIR + File.separator + "graphDB";
	public static final String GRAPH_STATS = "graphStats";
	public static final String FREQUENCY_CSV = "dummy.csv";

	public static int NUM_GRAPHS = 0;
	public static int NUM_GRAPHS_WITH_UNKNOWNS = 0;
	public static int NUM_GRAPHS_WITH_ONLY_CONTROL_NODES = 0;
	public static int NUM_GRAPHS_WITHOUT_LIBS = 0;
	public static int NUM_GRAPHS_WITH_UNKNOWNS_AND_JDK = 0;

	public static String[] CONCERNED_LIBS_NO_CONTROL_NODES;

	public static final String TOP = "TOP_";
	public static final String DOT = ".dot";
	public static final String GIF = ".gif";
	public static final String TXT = ".txt";

	public static void main(String[] args) {
		CONCERNED_LIBS_NO_CONTROL_NODES = new String[concernedLibs.length - 1];
		for (int i = 0; i < concernedLibs.length - 1; i++) {
			CONCERNED_LIBS_NO_CONTROL_NODES[i] = concernedLibs[i + 1];
		}

		// Reflections reflections = new Reflections("java.lang", new
		// SubTypesScanner(true));
		// Set<String> allClassTypes = reflections.getAllTypes();

		// System.out.println("allClassTypes " + allClassTypes);

		// String projectPath = "C:/Research/GraphGeneration/Work/data/db4o";
		// String projectName = "db4o";
		// LinkedHashMap<String, MethodInfo> methodInfoMap =
		// buildProjectGroums(projectPath, projectName);
		//
		//

		//
		// String methodInfoMapPath =
		// "C:/Research/GraphGeneration/Work/out_data/methodInfoMap.dat";
		// FileUtils.writeObjectFile(methodInfoMap, methodInfoMapPath);
		//
		// LinkedHashMap<String, Graph> methodGraphMap =
		// convertAllGroumsToGraphs(methodInfoMap, database.globalData);
		// String methodGraphMapPath =
		// "C:/Research/GraphGeneration/Work/out_data/methodGraphMap.dat";
		// FileUtils.writeObjectFile(methodGraphMap, methodGraphMapPath);
		//
		// Logger.log("getAllMethodGraphs");
		// getAllMethodGraphs(methodGraphMap, GlobalConfig.maxGraphSize,
		// GlobalConfig.maxCountNode, database, projectName);
		// database.buildAllChild();
		// database.doStatistics();
		// database.storeThisDatabase(GlobalConfig.graphDatabasePath);

		long startTime = System.currentTimeMillis();

		new File(GRAPHDB_DIR).mkdir();
		// new File(GRAPHDB_DIR + File.separator + GRAPHDB_FILE).delete();

		GraphDatabase graphDB = null;

		String graphDBPath = GRAPHDB_DIR + File.separator + GRAPHDB_FILE;
		System.out.println("graphDBPath " + graphDBPath);

		if (new File(graphDBPath).exists()) {
			graphDB = GraphDatabase.readGraphDatabase(graphDBPath);
			System.out.println("loaded graphDatabase from file.");
		} else {
			graphDB = new GraphDatabase();
			System.out.println("Creating a new graph database");
		}

		List<String> projects = new ArrayList<String>();

		projects.add(PROJECTS_BASEDIR + File.separator + "neo4j/");
		projects.add(PROJECTS_BASEDIR + File.separator + "buck-master/");
		projects.add(PROJECTS_BASEDIR + File.separator + "guava/");
		projects.add(PROJECTS_BASEDIR + File.separator + "closure_compiler/");
		projects.add(PROJECTS_BASEDIR + File.separator + "j2objc/");
		projects.add(PROJECTS_BASEDIR + File.separator + "batik/");
		projects.add(PROJECTS_BASEDIR + File.separator + "cassandra/");
		projects.add(PROJECTS_BASEDIR + File.separator + "xalan/");
		projects.add(PROJECTS_BASEDIR + File.separator + "xerces/");
		projects.add(PROJECTS_BASEDIR + File.separator + "maven3/");
		projects.add(PROJECTS_BASEDIR + File.separator + "nomulus/");
		projects.add(PROJECTS_BASEDIR + File.separator + "lucene/");
		projects.add(PROJECTS_BASEDIR + File.separator + "error_prone/");
		projects.add(PROJECTS_BASEDIR + File.separator + "ant/");
		projects.add(PROJECTS_BASEDIR + File.separator + "maven2/");
		projects.add(PROJECTS_BASEDIR + File.separator + "log4j/");

		Logger.initDebugBis(PROJECTS_BASEDIR + File.separator + "graphStats.txt");

		for (String project : projects) {
			processProject(project, graphDB);
		}

		// String projectDir =
		// "/home/dharani/concordia/thesis/graph_entrophy/projects_from_musfiqur/cassandra/";
		// processProject(projectDir, graphDB);

		// Logger.initDebugBis(projectDir + File.separator + "graphStats.txt");
		graphDB.doStatistics();
		// graphDB.storeThisDatabase(projectDir + File.separator +
		// "graphDB.dat");
		graphDB.storeThisDatabase(graphDBPath);
		Logger.closeDebugBis();

		Map<Integer, Integer> frequenciesVsNumGraphs = new TreeMap<Integer, Integer>(Comparator.reverseOrder());
		Map<Integer, List<Graph>> frequenciesVsGraphList = new TreeMap<Integer, List<Graph>>(Comparator.reverseOrder());

		List<Integer> sortedFrequency = new ArrayList<Integer>();

		long totalOccurenceFrequency = 0;
		for (Integer hashVal : graphDB.h1GraphMaps.keySet()) {
			Graph[] graphs = graphDB.h1GraphMaps.get(hashVal);

			for (Graph graph : graphs) {
				sortedFrequency.add(graph.count);
				totalOccurenceFrequency += graph.count;
				Integer numGraphs = frequenciesVsNumGraphs.get(graph.count);

				List<Graph> graphList = frequenciesVsGraphList.get(graph.count);
				if (graphList != null) {
					graphList.add(graph);
				} else {
					List<Graph> graphListNew = new ArrayList<Graph>();
					graphListNew.add(graph);
					frequenciesVsGraphList.put(graph.count, graphListNew);
				}

				if (numGraphs != null) {
					frequenciesVsNumGraphs.put(graph.count, new Integer(numGraphs.intValue() + 1));
				} else {
					frequenciesVsNumGraphs.put(graph.count, 1);
				}
			}
		}

		System.out.println("totalOccurenceFrequency " + totalOccurenceFrequency);
		System.out.println(frequenciesVsNumGraphs.size());

		Set<Integer> keyset = frequenciesVsNumGraphs.keySet();
		for (Integer integer : keyset) {
			System.out.println(integer + ", " + frequenciesVsNumGraphs.get(integer));
		}

		Collections.sort(sortedFrequency, Comparator.reverseOrder());

		try (BufferedWriter bw = new BufferedWriter(new FileWriter(
				new File(PROJECTS_BASEDIR + File.separator + FREQUENCY_CSV)))) {
			// new FileWriter(new File(projectDir + File.separator +
			// FREQUENCY_CSV)))) {

			for (Integer integer : sortedFrequency) {
				bw.write("" + integer);
				bw.newLine();
			}
			bw.flush();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}

		keyset = frequenciesVsGraphList.keySet();
		int count = 0;

		for (Integer integer : keyset) {
			System.out.println("frequency " + integer);
			List<Graph> graphList = frequenciesVsGraphList.get(integer);

			for (Graph graph : graphList) {

				if (count >= 2 * TOP_N) {
					break;
				}

				// String dotFile = projectDir + File.separator + count + DOT;
				String dotFile = PROJECTS_BASEDIR + File.separator + count + DOT;
				GraphDrawingUtils.outputDotFile(graph, dotFile, graphDB.globalData);

				String outputGifPath = PROJECTS_BASEDIR + File.separator + count + GIF;
				// String outputGifPath = projectDir + File.separator + count +
				// GIF;
				GraphDrawingUtils.callConvert(dotFile, outputGifPath);

				Set<String> methodsWhereMatched = new LinkedHashSet<String>();

				for (MethodGraphs mgs : methodGraphs) {

					List<Graph> subgraphs = mgs.allSubgraphs;

					for (Graph sg : subgraphs) {
						if (graph.roleEquals(sg)) {
							methodsWhereMatched.add(mgs.ti.fileInfo.fileName + "::" + mgs.mi.methodName);
						}
					}
				}
				count++;

				if (!methodsWhereMatched.isEmpty()) {
					for (String str : methodsWhereMatched) {
						System.out.println(str);
					}
				}
			}

			if (count >= 2 * TOP_N) {
				break;
			}
		}

		System.out.println("NUM_GRAPHS " + NUM_GRAPHS);
		System.out.println("addedGraphs " + addedGraphs);
		System.out.println("NUM_GRAPHS_WITH_UNKNOWNS " + NUM_GRAPHS_WITH_UNKNOWNS);
		System.out.println("NUM_GRAPHS_WITH_UNKNOWNS_AND_JDK " + NUM_GRAPHS_WITH_UNKNOWNS_AND_JDK);
		System.out.println("NUM_GRAPHS_WITH_ONLY_CONTROL_NODES " + NUM_GRAPHS_WITH_ONLY_CONTROL_NODES);
		System.out.println("NUM_GRAPHS_WITHOUT_LIBS " + NUM_GRAPHS_WITHOUT_LIBS);

		Graph.ULLMAN_ISOMORPH_THREADPOOL.shutdown();

		System.out.println("Time taken " + ((System.currentTimeMillis() - startTime) / 1000) + " seconds");
	}

	// public static void processAllProjects(GraphDatabase graphDB) {
	// List<String> projects =
	// getProjectsFromDirDat(GlobalConfig.projectDataDir);
	// Logger.log("Number of projects: " + projects.size());
	//
	// int countProject = 1;
	// int countSlot = 1;
	// for (String project : projects) {
	//
	// GlobalConfig.dummyDir = GlobalConfig.slotDummyDir + countProject + "/";
	// Logger.log("countProject: " + countProject);
	//
	// if (countProject == 188 || countProject == 207 || countProject > 2000) {
	// countProject++;
	// continue;
	// } else {
	// countProject++;
	//
	// }
	//
	// if (countProject % 100 == 0) {
	// countSlot++;
	// GlobalConfig.slotDummyDir = GlobalConfig.mainDummyDir + "slot" +
	// countSlot + "/";
	// Logger.log("store tmp database");
	// graphDB.storeThisDatabase(GlobalConfig.graphDatabasePath + countProject);
	//
	// }
	//
	// // if (countProject<=750)
	// // {
	// // countProject++;
	// // continue;
	// // }
	// // else{
	// // countProject++;
	// //
	// // }
	//
	// processProject(project, graphDB);
	// }
	//
	// // Logger.log("numJavaProjects: " + numJavaProjects);
	// Logger.log("numClasses: " + numClasses);
	// Logger.log("numMethods: " + numMethods);
	// Logger.log("LOCs: " + LOCs);
	//
	// Logger.log("buildAllChildren");
	// graphDB.buildAllChild();
	// Logger.log("doStatistics");
	// graphDB.doStatistics();
	// Logger.log("storeThisDatabase");
	// graphDB.storeThisDatabase(GlobalConfig.graphDatabasePath);
	// }

	public static void processProject(String project, GraphDatabase graphDB) {
		LinkedHashMap<String, MethodInfo> methodInfoMap = new LinkedHashMap<>();

		// new File(GlobalConfig.dummyDir).mkdirs();
		synchronized (MeasureCodeGraphDistribution.class) {

			// new File(GlobalConfig.dummyDir).mkdirs();
			Logger.log("\r\nproject: " + project);
			// Logger.logDebugBis("\r\nproject: " + project);
			// Logger.log("\tcreating dummy dir");
			// TreeMap<String, String> fileContentMap =
			// SnapshotCreation.readData(GlobalConfig.projectDataDir, project);
			// SnapshotCreation.buildDummyDir(GlobalConfig.dummyDir,
			// fileContentMap);
			/**
			 * Browse all methods and add their groums and subgroums to database
			 */
			Logger.log("\tbuilding groums");
			JavaGroumVisitor javaGroumVisitor = new JavaGroumVisitor();
			javaGroumVisitor.doMain(project);

			Logger.log("\tadding groums to database");

			List<TypeInfo> allTypeList = javaGroumVisitor.allTypeList;
			// int count = 0;

			for (TypeInfo typeInfo : allTypeList) {
				String packageName = typeInfo.packageDec;
				String className = typeInfo.typeName;
				numClasses++;

				List<MethodInfo> methodList = typeInfo.methodDecList;

				for (MethodInfo method : methodList) {
					// Logger.log(count);
					String methodName = method.methodName;
					String combinedName = normalizeStr(project) + "::" + normalizeStr(packageName) + "."
							+ normalizeStr(className) + "." + normalizeStr(methodName) + "::" + numMethods;
					// Logger.log("combinedName: " + combinedName);
					numMethods++;
					LOCs += method.LOCs;

					// Logger.log(method.controlNodeList);
					// incGraphDB.addGroumToDatabase(method, project);

					// count++;
					if (isDoCompactMethod) {
						DataUtils.compactGroum(method);
					}
					methodInfoMap.put(combinedName, method);
				}
			}

			LinkedHashMap<String, Graph> methodGraphMap = convertAllGroumsToGraphs(methodInfoMap, graphDB.globalData);
			Logger.log("\t\tmethodGraphMap size: " + methodGraphMap.size());
			getAllMethodGraphs(methodGraphMap, GRAPH_SIZE, MAX_NEIGHBOUR_NODES, graphDB, project, methodInfoMap);

		}
		try {
			// Logger.log("\tDelete project dir recursively");
			// FileUtils.deleteDirectoryContent(new
			// File(GlobalConfig.dummyDir));
			// org.apache.commons.io.FileUtils.forceDelete(new
			// File(GlobalConfig.dummyDir));
			// Thread.sleep(300);
			NodeSequenceInfoMap.clearAll();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Logger.log("addedGraphs: " + addedGraphs);
	}

	public static List<String> getProjectsFromDirDat(String dirDatPath) {
		TreeSet<String> projectList = new TreeSet<String>();
		File dirDat = new File(dirDatPath);
		if (dirDat.exists()) {
			File[] subs = dirDat.listFiles();
			for (File sub : subs) {
				String name = sub.getName();
				if (name.endsWith(".dat")) {
					projectList.add(name.substring(0, name.length() - 4));
				}
			}
		}
		ArrayList<String> projects = new ArrayList<>();
		projects.addAll(projectList);
		return projects;
	}

	public static void getAllMethodGraphs(LinkedHashMap<String, Graph> methodGraphMap, int maxGraphSize,
			int maxCountNode, GraphDatabase database, String projectName,
			LinkedHashMap<String, MethodInfo> methodInfoMap) {
		// LinkedHashMap<String, ArrayList<Graph>> allMethodGraphs = new
		// LinkedHashMap<>();
		int count = 1;
		for (String methodName : methodGraphMap.keySet()) {
			System.out.print(count + " ");
			if (count % 100 == 0) {
				System.out.println();
			}
			count++;
			MethodInfo mi = methodInfoMap.get(methodName);
			MethodGraphs mgs = new MeasureCodeGraphDistribution().new MethodGraphs();
			mgs.ti = mi.typeInfo;
			mgs.mi = mi;

			Graph methodGraph = methodGraphMap.get(methodName);

			// GraphDrawingUtils.outputDotFile(methodGraph,
			// "/home/dharani/concordia/thesis/graph_entrophy/projects_from_musfiqur/log4j_270/methodgraph.dot",
			// database.globalData);

			// Logger.log("methodName: " + methodName + " NumNodes: "
			// +methodGraph.numNodes());

			ArrayList<Graph> allSubGraphs = methodGraph.getAllSubGraphs(maxGraphSize, maxCountNode,
					database.globalData);

			// allMethodGraphs.put(methodName, allSubGraphs);
			for (Graph graph : allSubGraphs) {

				if (graph.numNodes() == GRAPH_SIZE) {
					NUM_GRAPHS++;
					if (!GraphUtil.hasUnknownClassTypeNodes(graph, database.globalData)) {

						if (FILTER_API) {
							if (graph.isConcernedGraph(concernedLibs, database.globalData)) {

								database.addGraphWithOtherData(graph, aGraphCount, projectName, methodName);
								addedGraphs++;
								mgs.allSubgraphs.add(graph);

							} else {

								boolean isAllNodesAreControlNodes = true;
								for (Node node : graph.nodes) {
									String nodeText = node.getNodeLabel(database.globalData).trim();
									if (!nodeText.startsWith("ControlInfo")) {
										isAllNodesAreControlNodes = false;
										break;
									}
								}

								if (!isAllNodesAreControlNodes) {
									System.err.println("All nodes are control nodes");
									NUM_GRAPHS_WITH_ONLY_CONTROL_NODES++;
								} else {
									NUM_GRAPHS_WITHOUT_LIBS++;
								}
							}
						} else {
							database.addGraphWithOtherData(graph, aGraphCount, projectName, methodName);
							addedGraphs++;

							mgs.allSubgraphs.add(graph);
						}
					} else {
						NUM_GRAPHS_WITH_UNKNOWNS++;

						if (graph.isConcernedGraph(concernedLibs, database.globalData)) {
							NUM_GRAPHS_WITH_UNKNOWNS_AND_JDK++;
						}
					}
				}
			}
			methodGraphs.add(mgs);
		}

		// return allMethodGraphs;
	}

	public static LinkedHashMap<String, Graph> convertAllGroumsToGraphs(LinkedHashMap<String, MethodInfo> methodInfoMap,
			GlobalData globalData) {
		LinkedHashMap<String, Graph> methodGraphMap = new LinkedHashMap<>();
		for (String methodName : methodInfoMap.keySet()) {
			MethodInfo methodInfo = methodInfoMap.get(methodName);
			Graph methodGraph = DataUtils.convertGroumToGraph(methodInfo, globalData);
			methodGraphMap.put(methodName, methodGraph);
			// System.out.println("methodGraph " + methodGraph);
			// System.out.println(GraphDrawingUtils.creatDotStr(methodGraph,
			// globalData));
		}
		return methodGraphMap;
	}

	// public static LinkedHashMap<String, MethodInfo> buildProjectGroums(String
	// projectPath, String projectName) {
	// LinkedHashMap<String, MethodInfo> methodInfoMap = new LinkedHashMap<>();
	//
	// synchronized (AllMain.class) {
	// Logger.log("projectPath: " + projectPath);
	// /**
	// * Browse all methods and add their groums and subgroums to database
	// */
	// Logger.log("\tbuilding groums");
	// JavaGroumVisitor javaGroumVisitor = new JavaGroumVisitor();
	// javaGroumVisitor.doMain(projectPath);
	//
	// Logger.log("\tadding groums to database");
	// List<TypeInfo> allTypeList = javaGroumVisitor.allTypeList;
	// // int count = 0;
	// if (allTypeList.size() > 0)
	// // numJavaProjects++;
	// for (TypeInfo typeInfo : allTypeList) {
	// String packageName = typeInfo.packageDec;
	// String className = typeInfo.typeName;
	// numClasses++;
	//
	// List<MethodInfo> methodList = typeInfo.methodDecList;
	//
	// for (MethodInfo method : methodList) {
	// // Logger.log(count);
	// String methodName = method.methodName;
	//
	// String combinedName = normalizeStr(projectName) + "::" +
	// normalizeStr(packageName) + "." + normalizeStr(className) + "." +
	// normalizeStr(methodName) + "::" + numMethods;
	// // Logger.log("combinedName: " + combinedName);
	// numMethods++;
	// LOCs += method.LOCs;
	//
	// // Logger.log(method.controlNodeList);
	// // incGraphDB.addGroumToDatabase(method, project);
	//
	// // count++;
	// if (isDoCompactMethod) {
	// DataUtils.compactGroum(method);
	// }
	// methodInfoMap.put(combinedName, method);
	// }
	// }
	// }
	// return methodInfoMap;
	// }

	public static String normalizeStr(String str) {
		if (str == null)
			return "";
		return str.trim();
	}

	public static void cleanup(String projectPath) {
		try {
			Logger.log("\tDelete project dir recursively");
			// FileUtils.deleteDirectoryContent(new
			// File(GlobalConfig.dummyDir));
			// org.apache.commons.io.FileUtils.forceDelete(new
			// File(projectPath));
			Thread.sleep(300);
			NodeSequenceInfoMap.clearAll();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	class MethodGraphs {
		public TypeInfo ti;
		public MethodInfo mi;
		public List<Graph> allSubgraphs = new ArrayList<Graph>();
	}
}
