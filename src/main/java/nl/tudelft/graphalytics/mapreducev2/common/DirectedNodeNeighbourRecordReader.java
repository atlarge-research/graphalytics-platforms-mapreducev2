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

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileSplit;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.LineRecordReader;
import org.apache.hadoop.mapred.RecordReader;

import java.io.IOException;

/**
 * @author Marcin Biczak
 */
public class DirectedNodeNeighbourRecordReader implements RecordReader<LongWritable, DirectedNodeNeighbourhood> {
    private LineRecordReader lineReader;
    private LongWritable lineKey;
    private Text lineValue;

    public DirectedNodeNeighbourRecordReader(JobConf job, FileSplit split) throws IOException {
        lineReader = new LineRecordReader(job, split);

        lineKey = lineReader.createKey();
        lineValue = lineReader.createValue();
    }

    public boolean next(LongWritable key, DirectedNodeNeighbourhood value) throws IOException {
        if (!lineReader.next(lineKey, lineValue)) {
           return false;
        }

        key.set(lineKey.get());
        DirectedNodeNeighbourhood tmp = new DirectedNodeNeighbourhood(this.textValueToObj(lineValue));
        value.setCentralNode(tmp.getCentralNode());
        value.setDirectedNodeNeighbourhood(tmp.getDirectedNodeNeighbourhood());

        return true;
    }

    public LongWritable createKey() {
        return new LongWritable();
    }

    public DirectedNodeNeighbourhood createValue() {
        return new DirectedNodeNeighbourhood();
    }

    public long getPos() throws IOException {
        return lineReader.getPos();
    }

    public float getProgress() throws IOException {
        return lineReader.getProgress();
    }

    public void close() throws IOException {
        lineReader.close();
    }

    private DirectedNodeNeighbourhood textValueToObj(Text line) throws IOException{
        String dataLine = line.toString();
        DirectedNodeNeighbourhood nodeNeighbourhood = new DirectedNodeNeighbourhood();
        nodeNeighbourhood.readFields(dataLine);

        return nodeNeighbourhood;
    }
}


