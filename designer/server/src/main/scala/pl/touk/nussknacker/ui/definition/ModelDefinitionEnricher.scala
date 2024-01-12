package pl.touk.nussknacker.ui.definition

import pl.touk.nussknacker.engine.ModelData
import pl.touk.nussknacker.engine.api.process.ProcessingType
import pl.touk.nussknacker.engine.canonicalgraph.CanonicalProcess
import pl.touk.nussknacker.engine.definition.component.ComponentStaticDefinition
import pl.touk.nussknacker.engine.definition.component.bultin.BuiltInComponentsStaticDefinitionsPreparer
import pl.touk.nussknacker.engine.definition.fragment.FragmentWithoutValidatorsDefinitionExtractor
import pl.touk.nussknacker.engine.definition.model.ModelDefinition
import pl.touk.nussknacker.engine.modelconfig.ComponentsUiConfigParser

class ModelDefinitionEnricher(
    builtInComponentsDefinitionsPreparer: BuiltInComponentsStaticDefinitionsPreparer,
    fragmentDefinitionExtractor: FragmentWithoutValidatorsDefinitionExtractor,
    additionalUIConfigFinalizer: AdditionalUIConfigFinalizer,
    modelDefinition: ModelDefinition[ComponentStaticDefinition]
) {

  def modelDefinitionWithBuiltInComponentsAndFragments(
      forFragment: Boolean,
      fragmentScenarios: List[CanonicalProcess],
      processingType: ProcessingType
  ): ModelDefinition[ComponentStaticDefinition] = {
    val builtInComponents =
      builtInComponentsDefinitionsPreparer.prepareStaticDefinitions(forFragment)
    val fragmentComponents =
      // TODO: Support for fragments using other fragments
      if (forFragment) List.empty
      else extractFragmentComponents(fragmentScenarios)
    additionalUIConfigFinalizer.finalizeModelDefinition(
      modelDefinition
        .withComponents(builtInComponents)
        .withComponents(fragmentComponents.toList),
      processingType
    )
  }

  private def extractFragmentComponents(
      fragmentsScenarios: List[CanonicalProcess],
  ): Map[String, ComponentStaticDefinition] = {
    (for {
      scenario   <- fragmentsScenarios
      definition <- fragmentDefinitionExtractor.extractFragmentComponentDefinition(scenario).toOption
    } yield {
      scenario.name.value -> definition
    }).toMap
  }

}

object ModelDefinitionEnricher {

  def apply(
      modelData: ModelData,
      additionalUIConfigFinalizer: AdditionalUIConfigFinalizer,
      modelDefinition: ModelDefinition[ComponentStaticDefinition]
  ): ModelDefinitionEnricher = {
    val builtInComponentConfig = ComponentsUiConfigParser.parse(modelData.modelConfig)
    new ModelDefinitionEnricher(
      new BuiltInComponentsStaticDefinitionsPreparer(builtInComponentConfig),
      new FragmentWithoutValidatorsDefinitionExtractor(modelData.modelClassLoader.classLoader),
      additionalUIConfigFinalizer,
      modelDefinition
    )
  }

}