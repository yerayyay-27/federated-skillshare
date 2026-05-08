package it.unibo;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class GreetingTest {

    private Greeting service;

    @BeforeEach
    public void setUp() {
        service = new Greeting();
    }

    @Test
    public void greetServer_shouldReturnHelloWithName() {
        String result = service.greet("World");
        assertEquals("Hello, from server World!", result);
    }

    @Test
    public void greetServer_shouldNotReturnNull() {
        assertNotNull(service.greet("Test"));
    }
}