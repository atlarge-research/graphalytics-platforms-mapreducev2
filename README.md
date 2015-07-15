# Graphalytics MapReduce V2 platform extension

[![Build Status](http://jenkins.tribler.org/buildStatus/icon?job=Graphalytics_MapReduceV2_master_tester)](http://jenkins.tribler.org/job/Graphalytics_MapReduceV2_master_tester/)


## Getting started

Please refer to the documentation of the Graphalytics core (`graphalytics` repository) for an introduction to using Graphalytics.


## MapReduce V2-specific benchmark configuration

The `mapreducev2` benchmark runs on Hadoop version 2.4.1 or later (may work for earlier versions, this has not been verified). Before launching the benchmark, configure your Hadoop cluster to operate in pseudo-distributed or distributed mode. You must also increase the maximum number of job counters by setting the following property in `$HADOOP_CONF_DIR/mapred-site.xml`:

 - `mapreduce.job.counters.limit`: Set this to a large value, e.g. `100000`.

Next, edit the `mapreducev2`-specific configuration file for Graphalytics, `config/mapreducev2.properties`, and change the following settings:

 - `mapreducev2.reducer-count`: Set to an appropriate number of reducers for your Hadoop deployment (note: variable number of reducers per graph/algorithm is not yet supported).
 - `hadoop.home`: Set to the root of your Hadoop installation (`$HADOOP_HOME`).

Ensure that Hadoop is running before starting the benchmark.

