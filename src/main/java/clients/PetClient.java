package clients;

import io.restassured.response.Response;
import models.Pet;

import static io.restassured.RestAssured.given;

public final class PetClient {
    private static final String BASE_URI = "https://petstore.swagger.io/v2";

    private PetClient() {
    }

    public static Response createPet(Pet pet) {
        return given()
                .baseUri(BASE_URI)
                .contentType("application/json")
                .body(pet)
                .when()
                .post("/pet");
    }

    public static Response updatePet(Pet pet) {
        return given()
                .baseUri(BASE_URI)
                .contentType("application/json")
                .body(pet)
                .when()
                .put("/pet");
    }

    public static Response getPet(long petId) {
        return given()
                .baseUri(BASE_URI)
                .pathParam("petId", petId)
                .when()
                .get("/pet/{petId}");
    }

    public static Response deletePet(long petId) {
        return deletePet(petId, null);
    }

    public static Response deletePet(long petId, String apiKey) {
        var request = given()
                .baseUri(BASE_URI)
                .pathParam("petId", petId);

        if (apiKey != null) {
            request.header("api_key", apiKey);
        }

        return request.when().delete("/pet/{petId}");
    }
}
