package pl.touk.nussknacker.engine.compile.nodecompilation

import cats.data.Validated.{Invalid, Valid}
import cats.data.ValidatedNel
import cats.implicits.{catsSyntaxTuple2Semigroupal, toFoldableOps, toTraverseOps}
import cats.instances.list._
import pl.touk.nussknacker.engine.api._
import pl.touk.nussknacker.engine.api.context._
import pl.touk.nussknacker.engine.api.expression.{ExpressionTypingInfo, TypedExpression}
import pl.touk.nussknacker.engine.api.typed.typing.{Typed, TypedObjectTypingResult, TypingResult, Unknown}
import pl.touk.nussknacker.engine.compile._
import pl.touk.nussknacker.engine.compile.nodecompilation.BaseComponentValidationHelper._
import pl.touk.nussknacker.engine.compile.nodecompilation.BaseNodeCompiler._
import pl.touk.nussknacker.engine.compile.nodecompilation.NodeCompiler.NodeCompilationResult
import pl.touk.nussknacker.engine.compiledgraph
import pl.touk.nussknacker.engine.graph.expression.NodeExpressionId.DefaultExpressionId
import pl.touk.nussknacker.engine.graph.expression._
import pl.touk.nussknacker.engine.graph.node
import pl.touk.nussknacker.engine.graph.node._

class BaseNodeCompiler(objectParametersExpressionCompiler: ExpressionCompiler) {

  def compileVariable(variable: Variable, ctx: ValidationContext)(
      implicit nodeId: NodeId
  ): NodeCompilationResult[expression.Expression] = {
    val (expressionCompilation, nodeCompilation) =
      compileExpression(
        variable.value,
        ctx,
        expectedType = Unknown,
        outputVar = Some(OutputVar.variable(variable.varName))
      )

    val additionalValidationResult =
      validateVariableValue(expressionCompilation.typedExpression, DefaultExpressionId)

    combineErrors(nodeCompilation, additionalValidationResult)
  }

  def compileFilter(filter: Filter, ctx: ValidationContext)(
      implicit nodeId: NodeId
  ): NodeCompilationResult[expression.Expression] = {
    val (expressionCompilation, nodeCompilation) =
      compileExpression(
        filter.expression,
        ctx,
        expectedType = Typed[Boolean],
        outputVar = None
      )

    val additionalValidationResult =
      validateBoolean(expressionCompilation.typedExpression, DefaultExpressionId)

    combineErrors(nodeCompilation, additionalValidationResult)
  }

  def compileSwitch(
      expressionRaw: Option[(String, Expression)],
      choices: List[(String, Expression)],
      ctx: ValidationContext
  )(
      implicit nodeId: NodeId
  ): NodeCompilationResult[(Option[expression.Expression], List[expression.Expression])] = {

    // the frontend uses empty string to delete deprecated expression.
    val expression = expressionRaw.filterNot(_._1.isEmpty)

    val expressionCompilation = expression.map { case (output, expression) =>
      compileExpression(
        expression,
        ctx,
        Unknown,
        NodeExpressionId.DefaultExpressionId,
        Some(OutputVar.switch(output))
      )._2
    }
    val objExpression = expressionCompilation.map(_.compiledObject).sequence

    val caseCtx = expressionCompilation.flatMap(_.validationContext.toOption).getOrElse(ctx)

    val (additionalValidations, caseExpressions) = choices.map { case (outEdge, caseExpr) =>
      val (expressionCompilation, nodeCompilation) =
        compileExpression(caseExpr, caseCtx, Typed[Boolean], outEdge, None)
      val typedExpression = expressionCompilation.typedExpression
      val validation      = validateBoolean(typedExpression, outEdge)
      val caseExpression  = nodeCompilation
      (validation, caseExpression)
    }.unzip

    val expressionTypingInfos = caseExpressions
      .map(_.expressionTypingInfo)
      .foldLeft(expressionCompilation.map(_.expressionTypingInfo).getOrElse(Map.empty)) {
        _ ++ _
      }

    val objCases = caseExpressions.map(_.compiledObject).sequence

    val compilationResult = NodeCompilationResult(
      expressionTypingInfos,
      None,
      expressionCompilation.map(_.validationContext).getOrElse(Valid(ctx)),
      objExpression.product(objCases),
      expressionCompilation.flatMap(_.expressionType)
    )

    combineErrors(compilationResult, additionalValidations.combineAll)
  }

  def compileFields(
      fields: List[pl.touk.nussknacker.engine.graph.variable.Field],
      ctx: ValidationContext,
      outputVar: Option[OutputVar]
  )(implicit nodeId: NodeId): NodeCompilationResult[List[compiledgraph.variable.Field]] = {

    val (compiledRecord, indexedFields) = {
      val compiledFields = fields.zipWithIndex.map { case (field, index) =>
        val compiledField = objectParametersExpressionCompiler
          .compile(field.expression, Some(node.recordValueFieldName(index)), ctx, Unknown)
          .map(result =>
            CompiledIndexedRecordField(compiledgraph.variable.Field(field.name, result.expression), index, result)
          )
        val indexedKeys = IndexedRecordKey(field.name, index)
        (compiledField, indexedKeys)
      }
      (compiledFields.map(_._1).sequence, compiledFields.map(_._2))
    }

    val typedObject = compiledRecord match {
      case Valid(fields) =>
        TypedObjectTypingResult(fields.map(f => (f.field.name, typedExprToTypingResult(Some(f.typedExpression)))).toMap)
      case Invalid(_) => Unknown
    }

    val fieldsTypingInfo: Map[String, ExpressionTypingInfo] = compiledRecord match {
      case Valid(fields) => fields.flatMap(f => typedExprToTypingInfo(Some(f.typedExpression), f.field.name)).toMap
      case Invalid(_)    => Map.empty
    }

    val compiledFields = compiledRecord.map(_.map(_.field))

    val compilationResult = NodeCompilationResult(
      expressionTypingInfo = fieldsTypingInfo,
      parameters = None,
      validationContext = outputVar.map(ctx.withVariable(_, typedObject)).getOrElse(Valid(ctx)),
      compiledObject = compiledFields,
      expressionType = Some(typedObject)
    )

    val additionalValidationResult = RecordValidator.validate(compiledRecord, indexedFields)

    combineErrors(compilationResult, additionalValidationResult)
  }

  private def compileExpression(
      expr: Expression,
      ctx: ValidationContext,
      expectedType: TypingResult,
      fieldName: String = DefaultExpressionId,
      outputVar: Option[OutputVar]
  )(
      implicit nodeId: NodeId
  ): (CompiledExpression, NodeCompilationResult[expression.Expression]) = {
    val expressionCompilation = objectParametersExpressionCompiler
      .compile(expr, Some(fieldName), ctx, expectedType)
      .map(expr => CompiledExpression(fieldName, Valid(expr)))
      .valueOr(err => CompiledExpression(fieldName, Invalid(err)))

    val typingResult = typedExprToTypingResult(expressionCompilation.typedExpression.toOption)

    val nodeCompilation: NodeCompilationResult[expression.Expression] = NodeCompilationResult(
      expressionTypingInfo = typedExprToTypingInfo(expressionCompilation.typedExpression.toOption, fieldName),
      parameters = None,
      validationContext = outputVar.map(ctx.withVariable(_, typingResult)).getOrElse(Valid(ctx)),
      compiledObject = expressionCompilation.typedExpression.map(_.expression),
      expressionType = Some(typingResult)
    )

    (expressionCompilation, nodeCompilation)

  }

}

object BaseNodeCompiler {

  private case class CompiledExpression(
      fieldName: String,
      typedExpression: ValidatedNel[ProcessCompilationError, TypedExpression],
  )

  private def typedExprToTypingResult(expr: Option[TypedExpression]) = {
    expr.map(_.returnType).getOrElse(Unknown)
  }

  private def typedExprToTypingInfo(expr: Option[TypedExpression], fieldName: String) = {
    expr.map(te => (fieldName, te.typingInfo)).toMap
  }

  private def combineErrors[T](
      compilationResult: NodeCompilationResult[T],
      additionalValidationResult: ValidatedNel[ProcessCompilationError, Unit]
  ): NodeCompilationResult[T] = {
    val newCompiledObject = (compilationResult.compiledObject, additionalValidationResult).mapN { case (result, _) =>
      result
    }
    compilationResult.copy(compiledObject = newCompiledObject)
  }

}