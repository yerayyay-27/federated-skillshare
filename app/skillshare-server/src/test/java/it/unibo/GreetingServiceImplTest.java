package it.unibo;

import com.google.gwt.user.server.rpc.jakarta.AbstractRemoteServiceServlet;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GreetingServiceImplTest {

    @Mock private ServletConfig servletConfig;
    @Mock private ServletContext servletContext;
    @Mock private HttpServletRequest request;

    private GreetingServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        when(servletConfig.getServletContext()).thenReturn(servletContext);
        when(servletContext.getServerInfo()).thenReturn("MockServer/1.0");
        when(request.getHeader("User-Agent")).thenReturn("MockBrowser/1.0");

        service = new GreetingServiceImpl();
        service.init(servletConfig);

        // Crea un ThreadLocal con la request mock e iniettalo via reflection
        ThreadLocal<HttpServletRequest> threadLocal = new ThreadLocal<>();
        threadLocal.set(request);

        Field field = AbstractRemoteServiceServlet.class
            .getDeclaredField("perThreadRequest");
        field.setAccessible(true);
        field.set(service, threadLocal); // sostituiamo il campo con il nostro ThreadLocal
    }


    @Test
    void greetServer_greetingShouldContainName() throws IllegalArgumentException {
        GreetingResponse response = service.greetServer("Mario");

        assertTrue(response.getGreeting().contains("Mario"));
    }

    @Test
    void greetServer_shouldThrowWhenNameTooShort() {
        assertThrows(IllegalArgumentException.class,
            () -> service.greetServer("Al"));
    }

    @Test
    void greetServer_shouldThrowWhenNameIsNull() {
        assertThrows(IllegalArgumentException.class,
            () -> service.greetServer(null));
    }
}