package openlineage.spark.agent.lifecycle.plan;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import openlineage.spark.agent.client.LineageEvent;
import org.apache.spark.sql.SQLContext;
import org.apache.spark.sql.catalyst.plans.logical.LogicalPlan;
import scala.PartialFunction;

/**
 * Constructs a list of valid {@link LogicalPlan} visitors that can extract an output {@link
 * LineageEvent.Dataset}. Checks the classpath for classes that are not bundled with Spark to avoid
 * {@link ClassNotFoundException}s during plan traversal.
 */
public class OutputDatasetVisitors
    implements Supplier<List<PartialFunction<LogicalPlan, List<LineageEvent.Dataset>>>> {
  private final SQLContext sqlContext;
  private final Supplier<List<PartialFunction<LogicalPlan, List<LineageEvent.Dataset>>>>
      datasetProviders;

  public OutputDatasetVisitors(
      SQLContext sqlContext,
      Supplier<List<PartialFunction<LogicalPlan, List<LineageEvent.Dataset>>>> datasetProviders) {
    this.sqlContext = sqlContext;
    this.datasetProviders = datasetProviders;
  }

  @Override
  public List<PartialFunction<LogicalPlan, List<LineageEvent.Dataset>>> get() {
    List<PartialFunction<LogicalPlan, List<LineageEvent.Dataset>>> list = new ArrayList();
    List<PartialFunction<LogicalPlan, List<LineageEvent.Dataset>>> providers =
        datasetProviders.get();

    list.add(new InsertIntoDataSourceDirVisitor());
    list.add(new InsertIntoDataSourceVisitor(providers));
    list.add(new InsertIntoHadoopFsRelationVisitor());
    list.add(new SaveIntoDataSourceCommandVisitor(sqlContext, providers));
    list.add(new DatasetSourceVisitor());
    list.add(new AppendDataVisitor(providers));
    list.add(new InsertIntoDirVisitor(sqlContext));
    return list;
  }
}
