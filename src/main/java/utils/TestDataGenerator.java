package utils;

import models.Category;
import models.Order;
import models.Pet;
import models.Tag;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class TestDataGenerator {
    private TestDataGenerator() {
    }

    public static long uniquePositiveId() {
        return ThreadLocalRandom.current().nextLong(1_000_000, 2_000_000_000L);
    }

    public static long validPetId() {
        return uniquePositiveId();
    }

    public static String uniqueName(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    public static String futureShipDate() {
        return Instant.now()
                .plus(1, ChronoUnit.DAYS)
                .truncatedTo(ChronoUnit.MILLIS)
                .toString();
    }

    public static Order newOrder(long orderId, long petId) {
        return new Order(
                orderId,
                petId,
                2,
                futureShipDate(),
                "placed",
                true
        );
    }

    public static Pet newPet() {
        return newPet(uniquePositiveId());
    }

    public static Pet newPet(long petId) {
        return new Pet(
                petId,
                new Category(1L, "Dogs"),
                uniqueName("Doggo"),
                List.of("https://example.com/pet.png"),
                List.of(new Tag(1L, "test")),
                "available"
        );
    }
}
