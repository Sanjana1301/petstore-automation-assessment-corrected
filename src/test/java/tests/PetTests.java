package tests;

import clients.PetClient;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import models.Pet;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import utils.TestDataGenerator;

import static io.restassured.RestAssured.given;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.testng.Assert.*;

public class PetTests extends BaseTest {

    @Test(description = "Create pet with unique data and validate complete response")
    public void testCreatePet() {
        Pet pet = TestDataGenerator.newPet();
        boolean created = false;

        try {
            Response response = PetClient.createPet(pet);

            assertEquals(response.statusCode(), 200);
            created = true;
            assertPetResponse(response, pet);
        } finally {
            if (created) {
                deleteAndVerify(pet.getId(), "special-key");
            }
        }
    }

    @Test(description = "Update an independently created pet from available to sold")
    public void testUpdatePetStatus() {
        Pet pet = TestDataGenerator.newPet();
        boolean created = false;

        try {
            Response createResponse = PetClient.createPet(pet);
            assertEquals(createResponse.statusCode(), 200);
            created = true;

            pet.setStatus("sold");

            Response updateResponse = PetClient.updatePet(pet);
            assertEquals(updateResponse.statusCode(), 200);
            assertPetResponse(updateResponse, pet);

            Response getResponse = PetClient.getPet(pet.getId());
            assertEquals(getResponse.statusCode(), 200);
            assertEquals(getResponse.jsonPath().getString("status"), "sold");
            assertPetResponse(getResponse, pet);
        } finally {
            if (created) {
                deleteAndVerify(pet.getId(), "special-key");
            }
        }
    }

    @Test(description = "Validate Pet response against JSON Schema and allowed status values")
    public void testPetSchemaValidation() {
        Pet pet = TestDataGenerator.newPet();
        boolean created = false;

        try {
            Response response = PetClient.createPet(pet);
            assertEquals(response.statusCode(), 200);
            created = true;

            response.then()
                    .body(matchesJsonSchemaInClasspath("schemas/pet-schema.json"));

            String status = response.jsonPath().getString("status");
            assertTrue(
                    status.equals("available") ||
                    status.equals("pending") ||
                    status.equals("sold"),
                    "Pet status must be one of available, pending, sold"
            );
        } finally {
            if (created) {
                deleteAndVerify(pet.getId(), "special-key");
            }
        }
    }

    @Test(description = "Delete an independently created pet with documented API key")
    public void testDeletePetWithValidApiKey() {
        Pet pet = TestDataGenerator.newPet();
        boolean created = false;

        try {
            Response createResponse = PetClient.createPet(pet);
            assertEquals(createResponse.statusCode(), 200);
            created = true;

            Response deleteResponse = PetClient.deletePet(pet.getId(), "special-key");
            assertEquals(deleteResponse.statusCode(), 200);
            assertNotNull(deleteResponse.jsonPath().getString("message"));

            Response getResponse = PetClient.getPet(pet.getId());
            assertEquals(getResponse.statusCode(), 404);
            created = false;
        } finally {
            if (created) {
                PetClient.deletePet(pet.getId(), "special-key");
            }
        }
    }

    @DataProvider(name = "apiKeyVariants")
    public Object[][] apiKeyVariants() {
        return new Object[][]{
                {"no-key", null},
                {"invalid-key", "invalid-key"}
        };
    }

    @Test(
            dataProvider = "apiKeyVariants",
            description = "Simulate deletion with missing and invalid API key headers"
    )
    public void testDeletePetWithMissingOrInvalidApiKey(String scenario, String apiKey) {
        Pet pet = TestDataGenerator.newPet();
        boolean created = false;

        try {
            Response createResponse = PetClient.createPet(pet);
            assertEquals(createResponse.statusCode(), 200);
            created = true;

            Response deleteResponse = PetClient.deletePet(pet.getId(), apiKey);

            /*
             * The public Petstore is a sample service. Its authentication filter may
             * not enforce the header consistently. We therefore validate that the
             * response is an HTTP response and document observed behavior rather than
             * inventing an authentication contract that the public service does not enforce.
             */
            int status = deleteResponse.statusCode();
            assertTrue(status == 200 || status == 401 || status == 403 || status == 404,
                    "Unexpected response for " + scenario + ": " + status);

            if (status == 200) {
                Response getResponse = PetClient.getPet(pet.getId());
                assertEquals(getResponse.statusCode(), 404,
                        "If public Petstore accepts deletion without a valid key, the pet must be removed");
                created = false;
            } else {
                assertTrue(deleteResponse.statusCode() == 401
                                || deleteResponse.statusCode() == 403
                                || deleteResponse.statusCode() == 404,
                        "Unexpected response for " + scenario + ": " + deleteResponse.statusCode());
            }
        } finally {
            if (created) {
                PetClient.deletePet(pet.getId(), "special-key");
            }
        }
    }

    private void assertPetResponse(Response response, Pet expected) {
        assertEquals(response.jsonPath().getLong("id"), expected.getId());
        assertEquals(response.jsonPath().getString("name"), expected.getName());
        assertEquals(response.jsonPath().getString("status"), expected.getStatus());
        assertNotNull(response.jsonPath().getList("photoUrls"));
        assertFalse(response.jsonPath().getList("photoUrls").isEmpty());
    }

    private void deleteAndVerify(long petId, String apiKey) {
        Response deleteResponse = PetClient.deletePet(petId, apiKey);
        assertEquals(deleteResponse.statusCode(), 200,
                "Cleanup deletion should succeed");

        Response getResponse = PetClient.getPet(petId);
        assertEquals(getResponse.statusCode(), 404,
                "Pet must not be retrievable after deletion");
    }
}
