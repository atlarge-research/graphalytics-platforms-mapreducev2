package nl.tudelft.graphalytics.mapreducev2.stats;

import java.io.IOException;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;
import nl.tudelft.graphalytics.mapreducev2.common.DirectedNode;
import nl.tudelft.graphalytics.mapreducev2.common.DirectedNodeNeighbourhood;
import nl.tudelft.graphalytics.mapreducev2.common.OutNode;

/**
 * @author Marcin Biczak
 */
public class GatherDirectedNodeNeighboursInfoReducer extends MapReduceBase
        implements Reducer<Text, Text, NullWritable, DirectedNodeNeighbourhood> {
    private DirectedNodeNeighbourhood nodeNeighbourhood = new DirectedNodeNeighbourhood();

    public void reduce(Text key, Iterator<Text> values,
                       OutputCollector<NullWritable, DirectedNodeNeighbourhood> output, Reporter reporter) throws IOException {

        // build central node and neighbours
        DirectedNode centralNode = new DirectedNode();
        Vector<OutNode> neighbours = new Vector<OutNode>();
        centralNode.setId(key.toString());

        while (values.hasNext()) {
            String value = values.next().toString();
            StringTokenizer tokenizer = new StringTokenizer(value, "#@");
            // central node
            if(tokenizer.countTokens() == 3) {
                centralNode.readFields(value);
            } else if(tokenizer.countTokens() == 2) { //neighbour
                OutNode node = new OutNode();
                node.readFields(value.toString());
                neighbours.add(node);
            } else
                throw new IOException("Error while reading Reducer input. Format not supported.");
        }

        //build node neighbourhood OBJ
        nodeNeighbourhood.setCentralNode(centralNode);
        nodeNeighbourhood.setDirectedNodeNeighbourhood(neighbours);

        output.collect(null, nodeNeighbourhood);
    }
}

