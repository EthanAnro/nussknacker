package pl.touk.nussknacker.engine.process.compiler

import com.typesafe.config.Config
import org.apache.flink.api.common.restartstrategy.RestartStrategies
import org.apache.flink.api.common.typeinfo.TypeInformation
import pl.touk.nussknacker.engine.ModelData
import pl.touk.nussknacker.engine.ModelData.ExtractDefinitionFun
import pl.touk.nussknacker.engine.api.dict.EngineDictRegistry
import pl.touk.nussknacker.engine.api.namespaces.ObjectNaming
import pl.touk.nussknacker.engine.api.process._
import pl.touk.nussknacker.engine.api.test.ScenarioTestData
import pl.touk.nussknacker.engine.api.typed.typing
import pl.touk.nussknacker.engine.api.typed.typing.Unknown
import pl.touk.nussknacker.engine.api.{MetaData, NodeId, ProcessListener}
import pl.touk.nussknacker.engine.canonicalgraph.CanonicalProcess
import pl.touk.nussknacker.engine.definition.clazz.ClassDefinitionSet
import pl.touk.nussknacker.engine.definition.component.ComponentDefinitionWithImplementation
import pl.touk.nussknacker.engine.definition.globalvariables.ExpressionConfigDefinition
import pl.touk.nussknacker.engine.flink.api.exception.FlinkEspExceptionConsumer
import pl.touk.nussknacker.engine.flink.api.process.{FlinkIntermediateRawSource, FlinkSource, FlinkSourceTestSupport}
import pl.touk.nussknacker.engine.flink.util.source.{CollectionSource, EmptySource}
import pl.touk.nussknacker.engine.process.exception.FlinkExceptionHandler
import pl.touk.nussknacker.engine.testmode.{ResultsCollectingListener, TestDataPreparer}

object TestFlinkProcessCompilerDataFactory {

  def apply(
      process: CanonicalProcess,
      scenarioTestData: ScenarioTestData,
      modelData: ModelData,
      collectingListener: ResultsCollectingListener
  ): FlinkProcessCompilerDataFactory = {
    new StubbedFlinkProcessCompilerDataFactory(
      process,
      modelData.configCreator,
      modelData.extractModelDefinitionFun,
      modelData.modelConfig,
      modelData.objectNaming,
      ComponentUseCase.TestRuntime
    ) {

      override protected def adjustListeners(
          defaults: List[ProcessListener],
          processObjectDependencies: ProcessObjectDependencies
      ): List[ProcessListener] = {
        collectingListener :: defaults
      }

      override protected def prepareSourceFactory(
          sourceFactory: ComponentDefinitionWithImplementation,
          context: ComponentDefinitionContext
      ): ComponentDefinitionWithImplementation = {
        sourceFactory.withImplementationInvoker(new StubbedComponentImplementationInvoker(sourceFactory) {
          override def handleInvoke(
              originalSource: Any,
              returnTypeOpt: Option[typing.TypingResult],
              nodeId: NodeId
          ): Any = {
            originalSource match {
              case sourceWithTestSupport: Source with FlinkSourceTestSupport[Object @unchecked] =>
                val sourcePreparer = new StubbedSourcePreparer(
                  context.userCodeClassLoader,
                  context.originalDefinitionWithTypes.modelDefinition.expressionConfig,
                  context.originalDictRegistry,
                  context.originalDefinitionWithTypes.classDefinitions,
                  process.metaData,
                  scenarioTestData
                )
                sourcePreparer.prepareStubbedSource(sourceWithTestSupport, returnTypeOpt, nodeId)
              case _ =>
                EmptySource[Object](returnTypeOpt.getOrElse(Unknown))(TypeInformation.of(classOf[Object]))
            }
          }
        })
      }

      override protected def prepareService(
          service: ComponentDefinitionWithImplementation,
          context: ComponentDefinitionContext
      ): ComponentDefinitionWithImplementation = service

      override protected def exceptionHandler(
          metaData: MetaData,
          processObjectDependencies: ProcessObjectDependencies,
          listeners: Seq[ProcessListener],
          classLoader: ClassLoader
      ): FlinkExceptionHandler = {
        new TestFlinkExceptionHandler(metaData, processObjectDependencies, listeners, classLoader)
      }

    }
  }

}

class StubbedSourcePreparer(
    classloader: ClassLoader,
    expressionConfig: ExpressionConfigDefinition[ComponentDefinitionWithImplementation],
    dictRegistry: EngineDictRegistry,
    classDefinitionSet: ClassDefinitionSet,
    metaData: MetaData,
    scenarioTestData: ScenarioTestData
) {

  def prepareStubbedSource(
      originalSource: Source with FlinkSourceTestSupport[Object],
      returnTypeOpt: Option[typing.TypingResult],
      nodeId: NodeId
  ): FlinkSource = {
    val samples: List[Object] = collectSamples(originalSource, nodeId)
    val returnType = returnTypeOpt.getOrElse(
      throw new IllegalStateException(
        s"${originalSource.getClass} extends FlinkSourceTestSupport and has no return type"
      )
    )
    originalSource match {
      case providerWithTransformation: FlinkIntermediateRawSource[Object @unchecked] =>
        new CollectionSource[Object](samples, originalSource.timestampAssignerForTest, returnType)(
          providerWithTransformation.typeInformation
        ) {
          override val contextInitializer: ContextInitializer[Object] = providerWithTransformation.contextInitializer
        }
      case _ =>
        new CollectionSource[Object](samples, originalSource.timestampAssignerForTest, returnType)(
          originalSource.typeInformation
        )
    }
  }

  private def collectSamples(originalSource: Source, nodeId: NodeId): List[Object] = {
    val testDataPreparer =
      new TestDataPreparer(classloader, expressionConfig, dictRegistry, classDefinitionSet, metaData)
    scenarioTestData.testRecords.filter(_.sourceId == nodeId).map { scenarioTestRecord =>
      testDataPreparer.prepareRecordForTest[Object](originalSource, scenarioTestRecord)
    }
  }

}

class TestFlinkExceptionHandler(
    metaData: MetaData,
    processObjectDependencies: ProcessObjectDependencies,
    listeners: Seq[ProcessListener],
    classLoader: ClassLoader
) extends FlinkExceptionHandler(metaData, processObjectDependencies, listeners, classLoader) {
  override def restartStrategy: RestartStrategies.RestartStrategyConfiguration = RestartStrategies.noRestart()

  override val consumer: FlinkEspExceptionConsumer = _ => {}

}