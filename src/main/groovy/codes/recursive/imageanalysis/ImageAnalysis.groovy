package codes.recursive.imageanalysis

import clarifai2.api.ClarifaiBuilder
import clarifai2.dto.input.ClarifaiInput
import clarifai2.dto.model.ConceptModel
import clarifai2.dto.model.output.ClarifaiOutput
import clarifai2.dto.prediction.Concept
import com.fnproject.fn.api.OutputEvent
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider
import com.oracle.bmc.ons.NotificationDataPlaneClient
import com.oracle.bmc.ons.model.MessageDetails
import com.oracle.bmc.ons.requests.PublishMessageRequest
import groovy.json.JsonOutput
import groovy.json.JsonSlurper

class ImageAnalysis {

    def analyze(String event) {
        println("Analyzing Image....")
        def parsedEvent = new JsonSlurper().parseText(event)
        def imageUrl = "https://objectstorage.us-phoenix-1.oraclecloud.com/n/${parsedEvent.data.namespace}/b/${parsedEvent.data.bucketName}/o/${parsedEvent.data.displayName}"

        def clarifaiClient = new ClarifaiBuilder(System.getenv().get("clarifaiKey")).buildSync()
        ConceptModel model = clarifaiClient.getDefaultModels().generalModel()

        List<ClarifaiOutput<Concept>> response = clarifaiClient.predict(model.id())
            .withInputs(
                ClarifaiInput.forImage(imageUrl)
            )
            .executeSync().get()

        def concepts = []
        response.each { ClarifaiOutput<Concept> it ->
            it.data().each { Concept concept ->
                concepts << "${concept.name()}"
            }
        }
        def msg = "Identified Concepts: ${concepts.join(', ')} View at: ${imageUrl} "

        ConfigFileAuthenticationDetailsProvider provider =  new ConfigFileAuthenticationDetailsProvider(
                '/.oci/config',
                'DEFAULT'
        )
        NotificationDataPlaneClient client = NotificationDataPlaneClient.builder().region(
                'us-phoenix-1')
                .build(provider)

        MessageDetails messageDetails = MessageDetails.builder().title('Image Captured').body(msg).build()
        PublishMessageRequest publishMessageRequest = PublishMessageRequest.builder()
            .messageDetails( messageDetails )
            .topicId(System.getenv().get("topicId"))
            .build()
        client.publishMessage( publishMessageRequest )

        println("Message published!")
        return OutputEvent.fromBytes( JsonOutput.toJson([event: parsedEvent]).bytes, OutputEvent.Status.Success, 'application/json')
    }

}
