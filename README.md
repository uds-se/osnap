# O!Snap: Cost-Efficient Testing in the Cloud

This repository contains the latest public version of the O!Snap framework for planning cost-efficient test execution in the cloud.

Currently, this version of the code corresponds to the one used for the evaluation of the paper under submission at the International Conference on Software Testing (ICST'17), 2017.

## What O!Snap does?

O!Ssnap is a technique to automatically generate plans to cost-efficiently execute tests in the cloud.
It takes as input the list of tests to execute with the dependencies they require, 
the list of available virtual machine images with the depependencies they provide,
and additional configuration parameters, such as the cost model adopted by the cloud provider and the objective function to minimize. 

As output, it produces a test execution plan that suggests which virtual machine images to use for running the tests, 
how to schedule their execution inside cloud instances,
and how to create suitable virtual machine images.

O!Snap works as a two-staged pipeline: it starts with *opportunistic snapshotting*, which aims to maximize the reuse of virtual machine images across test executions and define new images, or snapshots, to limit the effort of setting up test environments. And it ends with *test schedule planning*, which computes the test execution plan by interleaving the creation of new images and the execution of tests to minimize the overall test execution time and cost.

###### How to use O!Snap?

To compute a test execution plan using our prototype you need to run the ```evaluation-driver```
command as shown below. We introduce each paramater in the following.

```{r, engine='bash', count_lines}
./osnap/bin/evaluation-driver \
    --planners [COMMA-SEPARATED-LIST-OF-PLANNERS] \
    --test-jobs [TEST JOB YML FILE] \
    --available-images [AVAILABLE IMAGES YML FILE] \
      --base-image-id [ID OF THE BASE IMAGE TO USE] \
    --cloud-model [CLOUD MODEL YML FILE] \
    --goal [GOAL YML FILE]  \
    --output-file [OPTIONAL OUTPUT FILE]  \
    --result-processor [OPTIONAL RESULT PROCESSOR]
```

Our prototype comes with a full implementation of the O!Snap approach (Opportunistic Snapshotting - On-line plus ILP Scheduler) and, additionally, it provides the implementation of competitive approaches for planning test executions in the cloud. All the planners are identified by their name (actually their qualified class name).

*Basic planners* do not use opportunistic snapshotting. They are:

  - de.unisaarland.cs.st.planners.SequentialPlanner
  - de.unisaarland.cs.st.planners.RandomPlanner
  - de.unisaarland.cs.st.planners.RoundRobinPlanner 
  - de.unisaarland.cs.st.planners.MinLoadPlanner
  - de.unisaarland.cs.st.planners.MaxParallelismPlanner
  - de.unisaarland.cs.st.planners.MaxParallelismPlannerOnlyOnDemand
  - de.unisaarland.cs.st.planners.ILPPlanner

*Advanced planners* use opportunistic snapshotting, either off-line or on-line.
Their name can be obtained by appending to the name of basic planners the following suffix:

  - WithOpportunisticSnapshot
  - WithOpportunisticSnapshotOffLine

If multiple planner names are provided to the evaluation-driver command, it will execute all of them.

So for example, to run the full O!Snap approach and the sequential planner on the same input, we use `de.unisaarland.cs.st.planners.ILPPlannerWithOpportunisticSnapshot,de.unisaarland.cs.st.planners.SequentialPlanner` as value for the `--planners` parameter.

The test-jobs file contains the details (name, version and list of dependencies with timing information) of the input test jobs to execute in the cloud. In particular, the file is a yml serialization of a `Set` object that contains `TestJob` instances. You can generate such files using the `test-job-generator` command.

The available-images file contains details of available images (provided dependencies) and like the test-jobs file is a yml serialization of a collection of java objects of type `Image`. Images can be uniquely identified using the `id` fields. We use the id to identify which base-image (if any) the execution planner shall consider. You can generate such files using the `base-image-generator` command.

The cloud-model and goal files are pojo object that merely contains data. Cloud-model contains data about the configuration of the cloud (pricing, time to create snapshots, availability of resources, etc.). Goal contains data about the objective function that the planners must optimize and optional constraints on maximum execution time, resource usage, and cost.

Optionally you can store the output of the execution (a serialized collection of `Result` objects) in a specific output file (`--output-file`), and/or you can post process results from previous run (. This is useful for example to perform analysis on the execution, plotting test execution plans, or extracting relevant information from them (`--result-processor`). Additionally, post-processing of results can be done using the `result-reader` command.

In the code you can find several implementations of result processor by searching for classes that implement the `IResultProcessor` interface.

Assume that you run the following command:
```{r, engine='bash', count_lines}
./osnap/bin/evaluation-driver \
  --planners \
     de.unisaarland.cs.st.planners.SequentialPlanner,\
     de.unisaarland.cs.st.planners.ILPPlannerWithOpportunisticSnapshot,\
     de.unisaarland.cs.st.planners.MaxParallelismPlannerOnlyOnDemand,\
  --test-jobs $(pwd)/test-jobs.yml \
  --available-images $(pwd)/available-images.yml \
  --base-image-id $(head -1 $(pwd)/base-image) \
  --cloud-model $(pwd)/cloud-model.yml \
  --goal $(pwd)/goal.yml \
  --output-file $(pwd)/results \
  --result-processor de.unisaarland.cs.st.evaluation.resultprocessors.StdOutResultProcessor
```

You get as output a summary table which reports the value of the objective function (Objective), the predicted test execution time (Time), the additional cost for the execution (Cost), and the time for computing the solution (Planning-Time):
```
Name                                | Objective | Time | Cost  | Planning-Time
----------------------------------------------------------------------------------
SequentialPlanner                   |  5383.620 | 5438 |   0.0 |         0.004
ILPPlannerWithOpportunisticSnapshot |  2633.970 | 2647 | 13.44 |         0.854
MaxParallelismPlannerOnlyOnDemand 	|  1999.410 | 1967 | 52.08 |         0.001
```

Now, if you want to have a deeper view on the output of the planning, you can run a command similar to:

```{r, engine='bash', count_lines}
./osnap/bin/result-reader \
  --input-file $(pwd)/results \
  --result-processor de.unisaarland.cs.st.evaluation.resultprocessors.StdOutScheduleProcessor
```

This commadn reads the serialized result and plot the actual test schedule plans as computed by each of the planners.
Below we report only the one computed using O!Snap (ILPPlannerWithOpportunisticSnapshot)

```
Schedule for: ILPPlannerWithOpportunisticSnapshot
------------------------------------------------------------
Objective:           2633.9700000000003
------------------------------------------------------------
Time:                    2647 sec
Cost:                    13.44 $
------------------------------------------------------------
Computed in:         8.539999999999999E-4 sec
------------------------------------------------------------
------------------- RESERVED INSTANCE 0 --------------------
6  S    1-> 5                       0    ->   804    1
9  T    php-horde-view_2.0.5-2      804  ->   805    5
8  T    python-persistent_4.1.1-1   804  ->   972    5
7  T    roboptim-core_2.0-7.1       972  ->  1433    5
11 T    postgresql-9.4_9.4.5-1      1433 ->  2647    5
------------------------------------------------------------
------------------- ON-DEMAND INSTANCE 0 --------------------
10 T    kwallet-kf5_5.14.0-1        1258 ->  2647    5
------------------------------------------------------------
```

This output shows the main information about the Schedule computed by our approach in the header.
And the actual job distribution (both test jobs - T, and snapshot creation - S) in the body.
For each job the tool shows deployment information (RESERVED INSTANCE or ON-DEMAND INSTANCE), 
the type of the job (T or S), the content of the job, timing information and image associated
with the job.

For example, at time 0 on RESERVED INSTANCE 0, a (S)napshot job takes place. The snapshot produces the new Image #5 as output, and the process takes 804 sec. At this point, on RESERVED INSTANCE 0 the test of package `php-horde-view` version `2.0.5-2` starts using the snapshot #5. The test takes only 1 second to complete. If we would have used the base image (Image 1) to run the same test, that would have taken 785 seconds instead.
    
## Where do I find more on O!Snap?

Visit the [O!Snap Home Page](https://www.st.cs.uni-saarland.de/testing/osnap/) or [write us an email](mailto:gambi@st.cs.uni-saarland.de).

## What O!Snap requires?

O!Snap relies on the IBM ILOG CPLEX Studio to efficently solve the optimization problems at the core of (Opportunistic Snapshotting and ILP Scheduler); therefore, you need to have CPLEX installed to run O!Snap. You can ask for a (free) academic license to IBM and then setup the tool as the vendor explains. Additionally, you need to install the cplex.jar in your maven local repository (or in the "my-repo" maven repository that comes with the code). 

###### How to build O!Snap from source?

Assuming that you have installed cplex and the cplex.jar, simply running the following commands should be enough:

```
mvn clean compile package -DskipTests appassembler:assemble
```

This will create a ready-to-go distribution under `target/appassembler`
