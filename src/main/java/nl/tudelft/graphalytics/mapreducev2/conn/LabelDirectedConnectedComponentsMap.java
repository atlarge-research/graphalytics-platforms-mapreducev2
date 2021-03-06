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
package nl.tudelft.graphalytics.mapreducev2.conn;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reporter;

import java.io.IOException;
import java.util.StringTokenizer;

/**
 * @author Marcin Biczak
 */
public class LabelDirectedConnectedComponentsMap extends MapReduceBase implements Mapper<LongWritable, Text, Text, Text> {
    private String id;
    private String label;
    private String[] in;
    private String[] out;

    public void map(LongWritable key, Text value, OutputCollector<Text, Text> output, Reporter reporter) throws IOException {
        StringBuilder neighbours = new StringBuilder("# ");
        this.readNode(value.toString(), reporter);

        output.collect(new Text(this.id), new Text(this.label));
        for(int i=0; i<this.in.length; i++) {
            output.collect(new Text(this.in[i]), new Text(this.label));
            if(i == 0)
                neighbours.append(this.in[i]);
            else
                neighbours.append(",").append(this.in[i]);

            //report progress
            if(i % 1000 == 0) reporter.progress();
        }

        reporter.progress();

        neighbours.append("\t@ ");
        for(int i=0; i<this.out.length; i++) {
            output.collect(new Text(this.out[i]), new Text(this.label));
            if(i == 0)
                neighbours.append(this.out[i]);
            else
                neighbours.append(",").append(this.out[i]);

            //report progress
            if(i % 1000 == 0) reporter.progress();
        }

        output.collect(new Text(this.id), new Text("$" + neighbours));
    }

    public void readNode(String line, Reporter reporter) throws IOException {
        StringTokenizer basicTokenizer = new StringTokenizer(line, "\t$");
        if (basicTokenizer.countTokens() < 3 || basicTokenizer.countTokens() > 4) {
        	throw new IOException("ConnCompRecord requires 4 basicTokens as pattern got "+basicTokenizer.countTokens());
        }
        
        int tokenCount = basicTokenizer.countTokens();
        this.id = basicTokenizer.nextToken();
        if (tokenCount == 3) {
        	this.label = this.id;
        } else {
        	this.label = basicTokenizer.nextToken();
        }
        
        
        String inNeigh = basicTokenizer.nextToken();
        String outNeigh = basicTokenizer.nextToken();

        // IN
        StringTokenizer neighTokenizer = new StringTokenizer(inNeigh,", #");
        this.in = new String[neighTokenizer.countTokens()];
        int i=0;
        while (neighTokenizer.hasMoreTokens()) {
            String token = neighTokenizer.nextToken();
            if(token.equals("#")) continue;
            this.in[i] = token;

            if(i % 1000 == 0) reporter.progress();

            i++;
        }

        reporter.progress();

        // OUT
        neighTokenizer = new StringTokenizer(outNeigh,", @");
        this.out = new String[neighTokenizer.countTokens()];
        i=0;
        while (neighTokenizer.hasMoreTokens()) {
            String token = neighTokenizer.nextToken();
            if(token.equals("@")) continue;
            this.out[i] = token;

            if(i % 1000 == 0) reporter.progress();

            i++;
        }
    }
}
