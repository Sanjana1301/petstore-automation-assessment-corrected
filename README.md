# Swagger Petstore API Automation QA Assessment

Automated API test framework for the Swagger Petstore service using **Java 21, RestAssured, TestNG, Jackson POJOs, JSON Schema validation and Allure**.

Target API:

`https://petstore.swagger.io/v2`

The assessment covers the Store Inventory & Order Management area and the Pet CRUD / API-key simulation area.

## Tech Stack

- Java 21
- Maven
- RestAssured 5.4.0
- TestNG 7.10.2
- Jackson 2.17.1
- RestAssured JSON Schema Validator
- Allure TestNG

## Project Structure

```text
src/
├── main/java/
│   ├── clients/
│   │   ├── OrderClient.java
│   │   └── PetClient.java
│   ├── models/
│   │   ├── Category.java
│   │   ├── Order.java
│   │   ├── Pet.java
│   │   └── Tag.java
│   └── utils/
│       └── TestDataGenerator.java
└── test/
    ├── java/tests/
    │   ├── BaseTest.java
    │   ├── PetTests.java
    │   └── StoreTests.java
    └── resources/schemas/
        └── pet-schema.json
```

## Test Design Justification

The tests are intentionally independent. There is no `priority`, `dependsOnMethods`, or class-level mutable test data used to pass IDs between tests. Every test creates the data it needs and cleans it up in a `finally` block. This allows tests to be executed individually. The test design is independent, although the public shared environment itself is not suitable for unrestricted parallel execution.

API request/response models are represented by typed POJOs rather than `HashMap`, which provides stronger typing and makes payloads easier to maintain. Pet IDs/names and order shipping dates are generated dynamically. The public Petstore contract documents order IDs in the 1..10 range, so order tests dynamically select an unused ID from that documented range rather than generating an invalid arbitrary long ID.

The Store tests validate inventory structure, a complete order lifecycle, non-existent order lookup, and malformed order input. The Pet tests cover creation, update/persistence, schema validation, and deletion with valid/missing/invalid API-key variants.

The full E2E lifecycle is intentionally retained because it verifies the business flow across multiple endpoints. Focused tests are also provided for individual behaviors so a failure can be localized more easily.

## Coverage

### Store

1. `testGetInventory`
   - Verifies HTTP 200.
   - Verifies JSON response.
   - Verifies inventory counts are numeric and non-negative.

2. `testCreateOrder`
   - Focused create operation with complete field validation and cleanup.

3. `testGetOrder`
   - Focused retrieval operation using a test-created order and complete field validation.

4. `testOrderLifecycleE2E`
   - Creates a test pet first and uses its generated ID, avoiding an assumed pre-existing pet/order relationship.
   - Dynamically selects an unused order ID from the API's documented valid range (1..10).
   - Validates all order fields: `id`, `petId`, `quantity`, `shipDate`, `status`, `complete`.
   - Retrieves the order and validates the same fields.
   - Deletes the order.
   - Verifies the deleted order can no longer be retrieved.
   - Uses `finally` so cleanup is attempted even when an assertion fails after creation.

5. `testGetNonExistentOrder`
   - Dynamically selects an unused ID from the API's documented valid 1..10 range and verifies that it currently returns 404.
   - Validates error structure without depending on exact message wording.

6. `testPlaceOrderWithInvalidQuantityType`
   - Sends a string where an integer quantity is required.
   - Verifies the malformed request is not accepted as a successful 2xx order.
   - The REST contract would normally use 400 Bad Request. The public Petstore has historically returned server-side errors for some malformed JSON/type cases, so the test deliberately does not encode HTTP 500 as correct behavior.

7. `testPlaceOrderWithMissingQuantity`
   - Sends an order without the quantity field and verifies it is not accepted as a successful order.

8. `testOrderQuantityBoundaryBehavior`
   - Covers representative boundary values: zero, negative and maximum integer quantity.
   - If the public service accepts a boundary value, the test verifies that the submitted value is preserved and the created order is cleaned up.
   - If the service rejects it, the test verifies that it returns an error response.

### Pet

1. `testCreatePet`
   - Creates a unique pet.
   - Validates ID, name, status and photo URL structure.
   - Cleans up the created pet.

2. `testUpdatePetStatus`
   - Creates its own pet.
   - Changes status from `available` to `sold`.
   - Retrieves the pet and verifies the persisted status and core data.

3. `testPetSchemaValidation`
   - Validates the response against `pet-schema.json`.
   - Verifies status is one of `available`, `pending`, or `sold`.

4. `testDeletePetWithValidApiKey`
   - Deletes a test-created pet using `api_key: special-key`.
   - Verifies the pet is no longer retrievable.

5. `testDeletePetWithMissingOrInvalidApiKey`
   - Runs with no API key and with a dummy invalid key.
   - The public Petstore is a sample/shared environment, so authentication enforcement is treated as observed behavior rather than assumed behavior.
   - If the public service accepts the deletion, the test verifies that deletion actually occurred.

## Public API Limitation

The assessment explicitly targets the public Swagger Petstore service. It is a shared external environment and is therefore not fully isolated from other users, availability issues, or service-side changes. A production automation setup would preferably run against a dedicated test environment or a controlled mock/service virtualization layer.

This project mitigates the risk by generating its own test data, avoiding reliance on pre-existing pets/orders, verifying resource deletion, and avoiding hard-coded order IDs.

## Prerequisites

- JDK 21
- Maven 3.9+
- Internet access to `https://petstore.swagger.io`

Check:

```bash
java -version
mvn -version
```

## Run Tests

Run the complete suite:

```bash
mvn clean test
```

Run a specific test class:

```bash
mvn -Dtest=StoreTests test
```

```bash
mvn -Dtest=PetTests test
```

The TestNG suite is also configured in `testing.xml`.

## Allure Report

After running tests:

```bash
allure serve allure-results
```

Or generate a report:

```bash
allure generate allure-results --clean -o allure-report
```

Then open the generated report.

## Surefire Report

Maven generates TestNG/Surefire reports under:

```text
target/surefire-reports/
```

## Notes

The public Petstore is a demonstration service, not a dedicated QA environment. Therefore, a transient external failure should be investigated separately from a deterministic test failure caused by the application behavior.

The tests avoid fixed pet IDs, fixed historical dates, execution-order dependencies, and exact error-message assertions where the exact wording is not part of the important contract. Order IDs are constrained by the public API's documented 1..10 range, so uniqueness is handled by checking that range before creating an order.
