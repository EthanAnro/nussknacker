package pl.touk.nussknacker.restmodel.component

import pl.touk.nussknacker.engine.api.component.ComponentId
import pl.touk.nussknacker.restmodel.BaseEndpointDefinitions
import pl.touk.nussknacker.restmodel.BaseEndpointDefinitions.SecuredEndpoint
import pl.touk.nussknacker.security.AuthCredentials
import sttp.model.StatusCode.{NotFound, Ok}
import sttp.tapir._
import sttp.tapir.generic.auto.schemaForCaseClass
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.Codec.PlainCodec

class ComponentApiEndpoints(auth: EndpointInput[AuthCredentials]) extends BaseEndpointDefinitions {

  import ComponentApiEndpoints.ComponentCodec._

  val componentsListEndpoint: SecuredEndpoint[Unit, Unit, List[ComponentListElement], Any] =
    baseNuApiEndpoint
      .summary("Listing components")
      .tag("Components")
      .withSecurity(auth)
      .get
      .in("components")
      .out(
        statusCode(Ok).and(
          jsonBody[List[ComponentListElement]]
        )
      )

  val componentUsageEndpoint: SecuredEndpoint[ComponentId, String, List[ComponentUsagesInScenario], Any] =
    baseNuApiEndpoint
      .summary("Show component usage")
      .tag("Components")
      .get
      .in("components" / path[ComponentId]("id") / "usages")
      .out(
        statusCode(Ok).and(
          jsonBody[List[ComponentUsagesInScenario]]
        )
      )
      .errorOut(
        statusCode(NotFound).and(
          stringBody
            .example(s"Component {id} not exist.")
        )
      )
      .withSecurity(auth)

  object ComponentApiEndpoints {

    object ComponentCodec {
      def encode(componentId: ComponentId): String = componentId.value

      def decode(s: String): DecodeResult[ComponentId] = {
        val componentId = ComponentId.apply(s)
        DecodeResult.Value(componentId)
      }

      implicit val componentIdCodec: PlainCodec[ComponentId] = Codec.string.mapDecode(decode)(encode)
    }

  }

}