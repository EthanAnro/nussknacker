package pl.touk.nussknacker.engine.process.compiler

import org.apache.flink.api.common.typeinfo.TypeInformation
import pl.touk.nussknacker.engine.ModelData
import pl.touk.nussknacker.engine.api.process.{ComponentUseCase, ProcessObjectDependencies}
import pl.touk.nussknacker.engine.api.typed.typing.{TypingResult, Unknown}
import pl.touk.nussknacker.engine.api.{NodeId, ProcessListener}
import pl.touk.nussknacker.engine.canonicalgraph.CanonicalProcess
import pl.touk.nussknacker.engine.definition.component.ComponentDefinitionWithImplementation
import pl.touk.nussknacker.engine.flink.util.source.EmptySource

object VerificationFlinkProcessCompilerDataFactory {

  def apply(process: CanonicalProcess, modelData: ModelData): FlinkProcessCompilerDataFactory = {
    new StubbedFlinkProcessCompilerDataFactory(
      process,
      modelData.configCreator,
      modelData.extractModelDefinitionFun,
      modelData.modelConfig,
      modelData.objectNaming,
      componentUseCase = ComponentUseCase.Validation
    ) {

      override protected def adjustListeners(
          defaults: List[ProcessListener],
          processObjectDependencies: ProcessObjectDependencies
      ): List[ProcessListener] = Nil

      override protected def prepareService(
          service: ComponentDefinitionWithImplementation,
          context: ComponentDefinitionContext
      ): ComponentDefinitionWithImplementation =
        service.withImplementationInvoker(new StubbedComponentImplementationInvoker(service) {
          override def handleInvoke(impl: Any, typingResult: Option[TypingResult], nodeId: NodeId): Any = null
        })

      override protected def prepareSourceFactory(
          sourceFactory: ComponentDefinitionWithImplementation,
          context: ComponentDefinitionContext
      ): ComponentDefinitionWithImplementation =
        sourceFactory.withImplementationInvoker(new StubbedComponentImplementationInvoker(sourceFactory) {
          override def handleInvoke(impl: Any, returnType: Option[TypingResult], nodeId: NodeId): Any =
            new EmptySource[Object](returnType.getOrElse(Unknown))(TypeInformation.of(classOf[Object]))
        })

    }
  }

}