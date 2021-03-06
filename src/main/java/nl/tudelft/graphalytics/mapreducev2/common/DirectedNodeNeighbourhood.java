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
package nl.tudelft.graphalytics.mapreducev2.common;

import org.apache.hadoop.io.WritableComparable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * @author Marcin Biczak
 */
public class DirectedNodeNeighbourhood implements WritableComparable<DirectedNodeNeighbourhood> {
    private DirectedNode centralNode;
    private Vector<OutNode> nodeNeighbourhood;
    private final char ignoreChar = '#';

    public DirectedNodeNeighbourhood() {}
    public DirectedNodeNeighbourhood(DirectedNode node, Vector<OutNode> neighbourhood) {
        this.centralNode = node;
        this.nodeNeighbourhood = neighbourhood;
    }
    public DirectedNodeNeighbourhood(DirectedNodeNeighbourhood nodeNeighbourhood) {
        this.setCentralNode(nodeNeighbourhood.getCentralNode());
        this.setDirectedNodeNeighbourhood(nodeNeighbourhood.getDirectedNodeNeighbourhood());
    }

    public DirectedNode getCentralNode() { return centralNode; }
    public void setCentralNode(DirectedNode centralNode) { this.centralNode = centralNode; }

    public Vector<OutNode> getDirectedNodeNeighbourhood() { return nodeNeighbourhood; }
    public void setDirectedNodeNeighbourhood(Vector<OutNode> nodeNeighbourhood) { this.nodeNeighbourhood = nodeNeighbourhood; }

    public int compareTo(DirectedNodeNeighbourhood o) {
        return ((this.getCentralNode().compareTo(o.getCentralNode())) != 0)
            ? (this.getCentralNode().compareTo(o.getCentralNode()))
            : this.compareNeighbourhood(o.getDirectedNodeNeighbourhood());
    }

    public void write(DataOutput dataOutput) throws IOException {
        String nodeNeighAsString = this.toString();
        dataOutput.writeBytes(nodeNeighAsString);
    }

    public void readFields(DataInput input) throws IOException {
        String nodeNeighbourhoodLine = input.readLine();
        this.readFields(nodeNeighbourhoodLine);
    }

    /*
        centralNodeId # in @ out | neighbourID @ [dst] | neighbourID @ [dst]
        2	#1	@3,4|1@2,3|3@|4@
     */
    public void readFields(String nodeNeighbourhoodLine) throws IOException {
        StringTokenizer nodesTokenizer = new StringTokenizer(nodeNeighbourhoodLine,"|");
        String[] nodesData = new String[nodesTokenizer.countTokens()];
        int x=0;
        while (nodesTokenizer.hasMoreTokens()) {
            nodesData[x] = nodesTokenizer.nextToken();
            x++;
        }

        // central node
        DirectedNode tmpCentralNode = new DirectedNode();
        tmpCentralNode.readFields(nodesData[0]);
        this.setCentralNode(tmpCentralNode);

        // neighbours
        Vector<OutNode> tmpNodeNeighbourhood = new Vector<OutNode>();
        for(int i=1; i<nodesData.length; i++) {
            OutNode node = new OutNode();
            node.readFields(nodesData[i]);
            tmpNodeNeighbourhood.add(node);
        }

        this.setDirectedNodeNeighbourhood(tmpNodeNeighbourhood);
    }

    private int compareNeighbourhood(Vector<OutNode> o) {
        if(this.getDirectedNodeNeighbourhood().size() < o.size())
            return -1;
        else if (this.getDirectedNodeNeighbourhood().size() > o.size())
            return 1;
        else {
            Iterator<OutNode> thisIter = this.getDirectedNodeNeighbourhood().iterator();
            Iterator<OutNode> oIter = o.iterator();
            while (thisIter.hasNext()) {
                OutNode thisNode = thisIter.next();
                OutNode oNode = oIter.next();
                if(thisNode.compareTo(oNode) != 0)
                    return thisNode.compareTo(oNode);
            }

        }

        return 0;
    }

    public String toFormattedString() {
        StringBuilder result = new StringBuilder(this.getCentralNode().toString());

        Iterator<OutNode> neighbourhoodNodes = this.getDirectedNodeNeighbourhood().iterator();
        while(neighbourhoodNodes.hasNext()){
            result.append("\n \t");
            OutNode tmpNode = neighbourhoodNodes.next();
            result.append(tmpNode.toString());
        }

        return result.append("\n").toString();
    }

    // nodeId \t #[IN,IN] \t @[OUT,OUT]&
    public String toString() {
        boolean isFirst = true;
        StringBuilder result = new StringBuilder(this.getCentralNode().toText().toString()).append("|");
        Iterator<OutNode> neighbours = this.getDirectedNodeNeighbourhood().iterator();
        while(neighbours.hasNext()) {
            if(isFirst) {
                result.append(neighbours.next().toText().toString());
                isFirst = false;
            } else
                result.append("|").append(neighbours.next().toText().toString());
        }

        return result.toString();
    }
}

