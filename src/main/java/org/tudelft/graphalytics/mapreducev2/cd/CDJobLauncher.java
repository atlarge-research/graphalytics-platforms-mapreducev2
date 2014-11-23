package org.tudelft.graphalytics.mapreducev2.cd;

import org.apache.hadoop.util.Tool;
import org.tudelft.graphalytics.algorithms.CDParameters;
import org.tudelft.graphalytics.mapreducev2.MapReduceJobLauncher;

public class CDJobLauncher extends MapReduceJobLauncher {
	// Stopping condition
    public enum Label {
        CHANGED
    }
	
    public static final String NODE_PREFERENCE = "CD.NodePreference";
    public static final String HOP_ATTENUATION = "CD.HopAttenuation";

    private CDParameters getParameters() {
    	assert (parameters instanceof CDParameters);
    	return (CDParameters)parameters;
    }

	@Override
	protected Tool createDirectedJob(String input, String intermediate, String output) {
		return new DirectedCambridgeLPAJob(input, intermediate, output, getParameters());
	}

	@Override
	protected Tool createUndirectedJob(String input, String intermediate, String output) {
		return new UndirectedCambridgeLPAJob(input, intermediate, output, getParameters());
	}
    
}
