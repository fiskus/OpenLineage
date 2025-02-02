package openlineage.spark.agent.lifecycle.plan;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import openlineage.spark.agent.client.LineageEvent;
import openlineage.spark.agent.facets.OutputStatisticsFacet;
import org.apache.spark.sql.catalyst.plans.logical.LogicalPlan;
import org.apache.spark.sql.execution.command.InsertIntoDataSourceDirCommand;
import scala.runtime.AbstractPartialFunction;

/**
 * {@link LogicalPlan} visitor that matches an {@link InsertIntoDataSourceDirCommand} and extracts
 * the output {@link LineageEvent.Dataset} being written.
 */
public class InsertIntoDataSourceDirVisitor
    extends AbstractPartialFunction<LogicalPlan, List<LineageEvent.Dataset>> {

  @Override
  public boolean isDefinedAt(LogicalPlan x) {
    return x instanceof InsertIntoDataSourceDirCommand;
  }

  @Override
  public List<LineageEvent.Dataset> apply(LogicalPlan x) {
    InsertIntoDataSourceDirCommand command = (InsertIntoDataSourceDirCommand) x;
    OutputStatisticsFacet outputStats = PlanUtils.getOutputStats(command.metrics());
    // URI is required by the InsertIntoDataSourceDirCommand
    URI outputPath = command.storage().locationUri().get();
    if (outputPath.getScheme() == null) {
      outputPath = URI.create("file://" + outputPath);
    }
    String namespace = PlanUtils.namespaceUri(outputPath);
    return Collections.singletonList(
        PlanUtils.getDataset(
            outputPath,
            namespace,
            PlanUtils.datasetFacet(command.schema(), namespace, outputStats)));
  }
}
