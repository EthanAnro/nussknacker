package pl.touk.nussknacker.ui.api.helpers

import pl.touk.nussknacker.engine.api.process.ProcessName
import pl.touk.nussknacker.engine.build.{GraphBuilder, ScenarioBuilder}
import pl.touk.nussknacker.engine.canonicalgraph.CanonicalProcess
import pl.touk.nussknacker.engine.graph.node.SubsequentNode
import pl.touk.nussknacker.engine.kafka.KafkaFactory.{SinkValueParamName, TopicParamName}

object SampleScenario {

  import pl.touk.nussknacker.engine.spel.Implicits._

  val scenarioName: ProcessName = ProcessName(this.getClass.getName)

  val scenario: CanonicalProcess = {
    ScenarioBuilder
      .streaming(scenarioName.value)
      .parallelism(1)
      .source("startProcess", "csv-source")
      .filter("input", "#input != null")
      .to(endWithMessage("suffix", "message"))
  }

  private def endWithMessage(idSuffix: String, message: String): SubsequentNode = {
    GraphBuilder
      .buildVariable("message" + idSuffix, "output", "message" -> s"'$message'")
      .emptySink("end" + idSuffix, "kafka-string", TopicParamName -> "'end.topic'", SinkValueParamName -> "#output")
  }

}