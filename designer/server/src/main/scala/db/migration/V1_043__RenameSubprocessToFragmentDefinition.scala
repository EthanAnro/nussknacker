package db.migration

import io.circe.Json._
import io.circe._
import pl.touk.nussknacker.ui.db.migration.ProcessJsonMigration

trait V1_043__RenameSubprocessToFragmentDefinition extends ProcessJsonMigration {

  override def updateProcessJson(jsonProcess: Json): Option[Json] =
    V1_043__RenameSubprocessToFragmentDefinition.updateProcessJson(jsonProcess)

}

object V1_043__RenameSubprocessToFragmentDefinition {

  private val legacyProperty = "subprocessParams"
  private val newProperty = "fragmentParams"

  private[migration] def updateProcessJson(jsonProcess: Json): Option[Json] = {
    Option(updateField(jsonProcess, "nodes", updateCanonicalNodes))
  }

  private def updateCanonicalNodes(array: Json): Json = {
    updateNodes(array, json => updatePropertyKey(updateCanonicalNode(json)))
  }

  private def updateField(obj: Json, field: String, update: Json => Json): Json = {
    (obj.hcursor downField field).success.flatMap(_.withFocus(update).top).getOrElse(obj)
  }

  private def updateNodes(array: Json, fun: Json => Json) = fromValues(array.asArray.getOrElse(List()).map(fun))

  private def updateCanonicalNode(node: Json): Json = {
    node.hcursor.downField("type").focus.flatMap(_.asString).getOrElse("") match {
      case "SubprocessInput" => updateField(node, "type", _ => Json.fromString("FragmentInput"))
      case "SubprocessOutput" => updateField(node, "type", _ => Json.fromString("FragmentOutput"))
      case "SubprocessOutputDefinition" => updateField(node, "type", _ => Json.fromString("FragmentOutputDefinition"))
      case "SubprocessInputDefinition" => updateField(node, "type", _ => Json.fromString("FragmentInputDefinition"))
      case "Switch" =>
        val updatedDefault = updateField(node, "defaultNext", updateCanonicalNodes)
        updateField(updatedDefault, "nexts", updateNodes(_, updateField(_, "nodes", updateCanonicalNodes)))
      case "Filter" => updateField(node, "nextFalse", updateCanonicalNodes)
      case "Split" => updateField(node, "nexts", updateNodes(_, updateCanonicalNodes))
      case _ => node
    }
  }

  private def updatePropertyKey(node: Json): Json = {
    node.hcursor.withFocus(json => {
      json.asObject.map {
        case obj if obj.contains(legacyProperty) =>
          Json.fromJsonObject(obj
            .add(newProperty, obj(legacyProperty).get)
            .filterKeys(_ != legacyProperty))
        case _ => json
      }.getOrElse(json)
    }).top.getOrElse(node)
  }

}