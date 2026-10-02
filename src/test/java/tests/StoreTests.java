package tests;

import clients.OrderClient;
import clients.PetClient;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import models.Order;
import models.Pet;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import utils.TestDataGenerator;

import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class StoreTests extends BaseTest {

    @Test(description = "Verify inventory response structure and non-negative numeric counts")
    public void testGetInventory() {

        Response response = OrderClient.getInventory();

        assertEquals(
                response.statusCode(),
                200,
                "Inventory endpoint should return 200"
        );

        assertTrue(
                response.contentType().contains(ContentType.JSON.toString()),
                "Inventory response should be JSON"
        );

        Map<String, Object> inventory = response.jsonPath().getMap("$");

        assertNotNull(
                inventory,
                "Inventory response should contain a JSON object"
        );

        assertFalse(
                inventory.isEmpty(),
                "Inventory should not be empty"
        );

        inventory.forEach((status, value) -> {

            assertNotNull(
                    status,
                    "Inventory status should not be null"
            );

            assertNotNull(
                    value,
                    "Inventory count should not be null"
            );

            assertTrue(
                    value instanceof Number,
                    "Inventory count for '" + status + "' must be numeric"
            );

            assertTrue(
                    ((Number) value).longValue() >= 0,
                    "Inventory count for '" + status + "' must be non-negative"
            );
        });
    }


    @Test(description = "Create an order and validate all returned fields")
    public void testCreateOrder() {

        long petId = TestDataGenerator.uniquePositiveId();
        long orderId = TestDataGenerator.uniquePositiveId();

        Pet pet = TestDataGenerator.newPet(petId);

        boolean petCreated = false;
        boolean orderCreated = false;

        try {

            // Create our own pet so the order uses a valid petId
            Response petResponse = PetClient.createPet(pet);

            assertEquals(
                    petResponse.statusCode(),
                    200,
                    "Pet should be created successfully"
            );

            petCreated = true;

            assertEquals(
                    petResponse.jsonPath().getLong("id"),
                    petId,
                    "Created pet ID should match"
            );

            Order expectedOrder =
                    TestDataGenerator.newOrder(orderId, petId);

            Response response =
                    OrderClient.createOrder(expectedOrder);

            assertEquals(
                    response.statusCode(),
                    200,
                    "Order should be created successfully"
            );

            orderCreated = true;

            assertOrderMatches(
                    expectedOrder,
                    response
            );

        } finally {

            cleanupOrder(orderId, orderCreated);
            cleanupPet(petId, petCreated);
        }
    }


    @Test(description = "Retrieve a created order and validate data integrity")
    public void testGetOrder() {

        long petId = TestDataGenerator.uniquePositiveId();
        long orderId = TestDataGenerator.uniquePositiveId();

        Pet pet = TestDataGenerator.newPet(petId);

        boolean petCreated = false;
        boolean orderCreated = false;

        try {

            Response petResponse =
                    PetClient.createPet(pet);

            assertEquals(
                    petResponse.statusCode(),
                    200,
                    "Pet should be created successfully"
            );

            petCreated = true;

            Order expectedOrder =
                    TestDataGenerator.newOrder(orderId, petId);

            Response createResponse =
                    OrderClient.createOrder(expectedOrder);

            assertEquals(
                    createResponse.statusCode(),
                    200,
                    "Order should be created successfully"
            );

            orderCreated = true;

            Response getResponse =
                    OrderClient.getOrder(orderId);

            assertEquals(
                    getResponse.statusCode(),
                    200,
                    "Created order should be retrievable"
            );

            assertOrderMatches(
                    expectedOrder,
                    getResponse
            );

        } finally {

            cleanupOrder(orderId, orderCreated);
            cleanupPet(petId, petCreated);
        }
    }


    @Test(description = "E2E: create pet, create order, retrieve order and clean up")
    public void testOrderLifecycleE2E() {

        long petId = TestDataGenerator.uniquePositiveId();
        long orderId = TestDataGenerator.uniquePositiveId();

        Pet pet = TestDataGenerator.newPet(petId);

        boolean petCreated = false;
        boolean orderCreated = false;

        try {

            // STEP 1: Create pet
            Response petResponse =
                    PetClient.createPet(pet);

            assertEquals(
                    petResponse.statusCode(),
                    200,
                    "Pet creation should return 200"
            );

            petCreated = true;

            assertEquals(
                    petResponse.jsonPath().getLong("id"),
                    petId,
                    "Created pet ID should match requested ID"
            );


            // STEP 2: Create order
            Order expectedOrder =
                    TestDataGenerator.newOrder(orderId, petId);

            Response createResponse =
                    OrderClient.createOrder(expectedOrder);

            assertEquals(
                    createResponse.statusCode(),
                    200,
                    "Order creation should return 200"
            );

            orderCreated = true;

            assertOrderMatches(
                    expectedOrder,
                    createResponse
            );


            // STEP 3: Retrieve order
            Response getResponse =
                    OrderClient.getOrder(orderId);

            assertEquals(
                    getResponse.statusCode(),
                    200,
                    "Created order should be retrievable"
            );

            assertOrderMatches(
                    expectedOrder,
                    getResponse
            );


            // STEP 4: Verify order belongs to our pet
            assertEquals(
                    getResponse.jsonPath().getLong("petId"),
                    petId,
                    "Order should reference the created pet"
            );

        } finally {

            // STEP 5: Cleanup order
            cleanupOrder(orderId, orderCreated);

            // STEP 6: Cleanup pet
            cleanupPet(petId, petCreated);
        }
    }


    @Test(description = "Negative: retrieve a non-existent order")
    public void testGetNonExistentOrder() {

        long unknownOrderId =
                TestDataGenerator.uniquePositiveId();

        Response response =
                OrderClient.getOrder(unknownOrderId);

        assertEquals(
                response.statusCode(),
                404,
                "Non-existent order should return 404"
        );

        String message =
                response.jsonPath().getString("message");

        assertNotNull(
                message,
                "Error response should contain a message"
        );

        assertFalse(
                message.isBlank(),
                "Error message should not be blank"
        );

        String type =
                response.jsonPath().getString("type");

        assertNotNull(
                type,
                "Error response should contain error type"
        );
    }


    @Test(description = "Negative: invalid quantity type must not be accepted")
    public void testPlaceOrderWithInvalidQuantityType() {

        long orderId =
                TestDataGenerator.uniquePositiveId();

        long petId =
                TestDataGenerator.uniquePositiveId();

        String invalidPayload = """
                {
                  "id": %d,
                  "petId": %d,
                  "quantity": "not_a_number",
                  "shipDate": "%s",
                  "status": "placed",
                  "complete": true
                }
                """.formatted(
                orderId,
                petId,
                TestDataGenerator.futureShipDate()
        );

        boolean created = false;

        try {

            Response response =
                    RestAssured
                            .given()
                            .baseUri("https://petstore.swagger.io/v2")
                            .contentType(ContentType.JSON)
                            .body(invalidPayload)
                            .when()
                            .post("/store/order");

            /*
             * A malformed quantity should not be accepted
             * as a successful order.
             *
             * The public Petstore API may currently return
             * different 4xx/5xx responses depending on its
             * server-side behaviour, therefore we only verify
             * that the request is not treated as successful.
             */

            if (response.statusCode() >= 200
                    && response.statusCode() < 300) {

                created = true;
            }

            assertTrue(
                    response.statusCode() >= 400,
                    "Invalid quantity type must not result in a successful 2xx response"
            );

        } finally {

            if (created) {

                Response deleteResponse =
                        OrderClient.deleteOrder(orderId);

                assertEquals(
                        deleteResponse.statusCode(),
                        200,
                        "Unexpectedly created invalid order should be cleaned up"
                );
            }
        }
    }


    @DataProvider(name = "quantityBoundaries")
    public Object[][] quantityBoundaries() {

        return new Object[][]{
                {0},
                {-1},
                {Integer.MAX_VALUE}
        };
    }


    @Test(
            dataProvider = "quantityBoundaries",
            description = "Verify order quantity boundary behaviour"
    )
    public void testOrderQuantityBoundaryBehavior(int quantity) {

        long petId =
                TestDataGenerator.uniquePositiveId();

        long orderId =
                TestDataGenerator.uniquePositiveId();

        Pet pet =
                TestDataGenerator.newPet(petId);

        boolean petCreated = false;
        boolean orderCreated = false;

        try {

            // Create valid pet
            Response petResponse =
                    PetClient.createPet(pet);

            assertEquals(
                    petResponse.statusCode(),
                    200,
                    "Pet should be created successfully"
            );

            petCreated = true;


            // Create order with boundary quantity
            Order order =
                    TestDataGenerator.newOrder(
                            orderId,
                            petId
                    );

            order.setQuantity(quantity);

            Response response =
                    OrderClient.createOrder(order);


            /*
             * Depending on the public API implementation,
             * boundary values may either be accepted or rejected.
             *
             * What we must not accept is a server error.
             */

            assertTrue(
                    response.statusCode() >= 200
                            && response.statusCode() < 500,
                    "Boundary quantity should not cause a server-side 5xx error"
            );


            if (response.statusCode() == 200) {

                orderCreated = true;

                assertEquals(
                        response.jsonPath().getInt("quantity"),
                        quantity,
                        "Returned quantity should match requested quantity"
                );
            }

        } finally {

            cleanupOrder(
                    orderId,
                    orderCreated
            );

            cleanupPet(
                    petId,
                    petCreated
            );
        }
    }


    private void assertOrderMatches(
            Order expected,
            Response actual
    ) {

        /*
         * Explicit primitive conversions are used here
         * to avoid TestNG assertEquals overload ambiguity.
         */

        assertEquals(
                actual.jsonPath().getLong("id"),
                expected.getId().longValue(),
                "Order ID mismatch"
        );

        assertEquals(
                actual.jsonPath().getLong("petId"),
                expected.getPetId().longValue(),
                "Pet ID mismatch"
        );

        assertEquals(
                actual.jsonPath().getInt("quantity"),
                expected.getQuantity().intValue(),
                "Quantity mismatch"
        );


        String actualShipDate =
                actual.jsonPath().getString("shipDate");

        String expectedShipDate =
                expected.getShipDate();

        assertEquals(
                normalizeShipDate(actualShipDate),
                normalizeShipDate(expectedShipDate),
                "Ship date mismatch"
        );


        assertEquals(
                actual.jsonPath().getString("status"),
                expected.getStatus(),
                "Order status mismatch"
        );


        assertEquals(
                actual.jsonPath().getBoolean("complete"),
                expected.getComplete().booleanValue(),
                "Complete flag mismatch"
        );
    }


    private String normalizeShipDate(String date) {

        if (date == null) {
            return null;
        }

        /*
         * The API may return:
         *
         * 2026-10-03T04:55:18.223Z
         *
         * or:
         *
         * 2026-10-03T04:55:18.223+0000
         *
         * Both represent UTC.
         */

        return date.replace("+0000", "Z");
    }


    private void cleanupOrder(
            long orderId,
            boolean orderCreated
    ) {

        if (!orderCreated) {
            return;
        }

        Response deleteResponse =
                OrderClient.deleteOrder(orderId);

        assertEquals(
                deleteResponse.statusCode(),
                200,
                "Created order must be deleted during cleanup"
        );

        Response getAfterDelete =
                OrderClient.getOrder(orderId);

        assertEquals(
                getAfterDelete.statusCode(),
                404,
                "Deleted order must not be retrievable"
        );
    }


    private void cleanupPet(
            long petId,
            boolean petCreated
    ) {

        if (!petCreated) {
            return;
        }

        Response deleteResponse =
                PetClient.deletePet(
                        petId,
                        "special-key"
                );

        assertEquals(
                deleteResponse.statusCode(),
                200,
                "Created test pet must be deleted during cleanup"
        );

        Response getAfterDelete =
                PetClient.getPet(petId);

        assertEquals(
                getAfterDelete.statusCode(),
                404,
                "Deleted pet must not be retrievable"
        );
    }
}

