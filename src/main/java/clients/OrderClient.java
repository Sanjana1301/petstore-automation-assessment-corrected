package clients;

import io.restassured.response.Response;
import models.Order;

import static io.restassured.RestAssured.given;

public final class OrderClient {
    private static final String BASE_URI = "https://petstore.swagger.io/v2";

    private OrderClient() {
    }

    public static Response getInventory() {
        return given()
                .baseUri(BASE_URI)
                .when()
                .get("/store/inventory");
    }

    public static Response createOrder(Order order) {
        return given()
                .baseUri(BASE_URI)
                .contentType("application/json")
                .body(order)
                .when()
                .post("/store/order");
    }

    public static Response getOrder(long orderId) {
        return given()
                .baseUri(BASE_URI)
                .pathParam("orderId", orderId)
                .when()
                .get("/store/order/{orderId}");
    }

    public static Response deleteOrder(long orderId) {
        return given()
                .baseUri(BASE_URI)
                .pathParam("orderId", orderId)
                .when()
                .delete("/store/order/{orderId}");
    }
}
