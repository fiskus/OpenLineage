package openlineage.spark.agent.lifecycle.plan;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import openlineage.spark.agent.client.LineageEvent;
import openlineage.spark.agent.facets.OutputStatisticsFacet;
import org.apache.spark.sql.catalyst.plans.logical.LogicalPlan;
import org.apache.spark.sql.execution.datasources.InsertIntoHadoopFsRelationCommand;
import scala.runtime.AbstractPartialFunction;

/**
 * {@link LogicalPlan} visitor that matches an {@link InsertIntoHadoopFsRelationCommand} and
 * extracts the output {@link LineageEvent.Dataset} being written.
 */
public class InsertIntoHadoopFsRelationVisitor
    extends AbstractPartialFunction<LogicalPlan, List<LineageEvent.Dataset>> {

  @Override
  public boolean isDefinedAt(LogicalPlan x) {
    return x instanceof InsertIntoHadoopFsRelationCommand;
  }

  @Override
  public List<LineageEvent.Dataset> apply(LogicalPlan x) {
    InsertIntoHadoopFsRelationCommand command = (InsertIntoHadoopFsRelationCommand) x;
    OutputStatisticsFacet outputStats = PlanUtils.getOutputStats(command.metrics());
    URI outputPath = command.outputPath().toUri();
    String namespace = PlanUtils.namespaceUri(outputPath);
    return Collections.singletonList(
        PlanUtils.getDataset(
            outputPath,
            namespace,
            PlanUtils.datasetFacet(command.query().schema(), namespace, outputStats)));
  }
}
