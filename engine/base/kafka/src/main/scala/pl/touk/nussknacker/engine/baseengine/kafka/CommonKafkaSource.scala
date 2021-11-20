package pl.touk.nussknacker.engine.baseengine.kafka

import org.apache.kafka.clients.consumer.ConsumerRecord
import pl.touk.nussknacker.engine.api.Context
import pl.touk.nussknacker.engine.api.process.Source
import pl.touk.nussknacker.engine.api.runtimecontext.EngineRuntimeContext

trait CommonKafkaSource extends Source[AnyRef] {

  def topics: List[String]

  def deserialize(context: EngineRuntimeContext, record: ConsumerRecord[Array[Byte], Array[Byte]]): Context

}