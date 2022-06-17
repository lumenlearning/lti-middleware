package net.unicon.lti.service.waymaker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.junit.jupiter.MockServerSettings;
import org.mockserver.model.MediaType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import net.unicon.lti.model.waymaker.WaymakerCourse;

@ExtendWith(MockServerExtension.class)
@MockServerSettings(ports = {1080, 1081})
@SpringBootTest
public class WaymakerServiceTest {

    private final String MOCK_SERVER_URL = "http://localhost:1080";

    @Autowired
    private WaymakerService waymakerService;

    private final ClientAndServer client;

    public WaymakerServiceTest(ClientAndServer client) {
        this.client = client;
    }

    @Test
    public void testNullCredentials() {
        ReflectionTestUtils.setField(waymakerService, "waymakerCoursesApiUrl", null);
        ReflectionTestUtils.setField(waymakerService, "waymakerJWT", null);
        assertEquals(waymakerService.fetchWaymakerCourses(), new ArrayList<>());
    }

    @Test
    public void testEmptyCredentials() {
        ReflectionTestUtils.setField(waymakerService, "waymakerCoursesApiUrl", "");
        ReflectionTestUtils.setField(waymakerService, "waymakerJWT", "");
        assertEquals(waymakerService.fetchWaymakerCourses(), new ArrayList<>());
    }

    @Test
    public void testNullUrl() {
        ReflectionTestUtils.setField(waymakerService, "waymakerCoursesApiUrl", null);
        ReflectionTestUtils.setField(waymakerService, "waymakerJWT", "nonnull");
        assertEquals(waymakerService.fetchWaymakerCourses(), new ArrayList<>());
    }

    @Test
    public void testNullToken() {
        ReflectionTestUtils.setField(waymakerService, "waymakerCoursesApiUrl", "nonnull");
        ReflectionTestUtils.setField(waymakerService, "waymakerJWT", null);
        assertEquals(waymakerService.fetchWaymakerCourses(), new ArrayList<>());
    }

    @Test
    public void testEmptyUrl() {
        ReflectionTestUtils.setField(waymakerService, "waymakerCoursesApiUrl", "");
        ReflectionTestUtils.setField(waymakerService, "waymakerJWT", "nonnull");
        assertEquals(waymakerService.fetchWaymakerCourses(), new ArrayList<>());
    }

    @Test
    public void testEmptyToken() {
        ReflectionTestUtils.setField(waymakerService, "waymakerCoursesApiUrl", "nonnull");
        ReflectionTestUtils.setField(waymakerService, "waymakerJWT", "");
        assertEquals(waymakerService.fetchWaymakerCourses(), new ArrayList<>());
    }

    @Test
    public void testNotValidURL() {
        ReflectionTestUtils.setField(waymakerService, "waymakerCoursesApiUrl", "notvalid");
        ReflectionTestUtils.setField(waymakerService, "waymakerJWT", "nonnull");
        assertEquals(waymakerService.fetchWaymakerCourses(), new ArrayList<>());
    }

    @Test
    public void testForbiddenRequest() {
        client.reset();
        client
           .when(request().withMethod("GET"))
           .respond(response().withStatusCode(401));
        ReflectionTestUtils.setField(waymakerService, "waymakerCoursesApiUrl", MOCK_SERVER_URL);
        ReflectionTestUtils.setField(waymakerService, "waymakerJWT", "nonnull");
        assertEquals(waymakerService.fetchWaymakerCourses(), new ArrayList<>());
    }

    @Test
    public void testBadRequest() {
        client.reset();
        client
            .when(request().withMethod("GET"))
            .respond(response().withStatusCode(400));
        ReflectionTestUtils.setField(waymakerService, "waymakerCoursesApiUrl", MOCK_SERVER_URL);
        ReflectionTestUtils.setField(waymakerService, "waymakerJWT", "nonnull");
        assertEquals(waymakerService.fetchWaymakerCourses(), new ArrayList<>());
    }

    @Test
    public void testWrongResponse() {
        String jsonResponse = "[{\"not\": \"root\",\"valid\": \"Root\",\"schema\": null}]";
        client.reset();
        client
            .when(request().withMethod("GET"))
            .respond(response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(jsonResponse));
        ReflectionTestUtils.setField(waymakerService, "waymakerCoursesApiUrl", MOCK_SERVER_URL);
        ReflectionTestUtils.setField(waymakerService, "waymakerJWT", "nonnull");
        List<WaymakerCourse> courseList = waymakerService.fetchWaymakerCourses(); 
        assertNotEquals(courseList, new ArrayList<>());
        assertEquals(courseList.size(), 1);
        assertEquals(courseList.get(0).getRoot_outcome_guid(), null);
        assertEquals(courseList.get(0).getBook_title(), null);
        assertEquals(courseList.get(0).getCover_img_url(), null);
        assertEquals(courseList.get(0).getDescription(), null);
    }

    @Test
    public void testGoodResponse() {
        String jsonResponse = "[{\"root_outcome_guid\": \"root\",\"book_title\": \"Root\",\"release_date\": null,\"cover_img_url\": null,\"category\": null,\"description\": null,\"table_of_contents\": null}]";
        client.reset();
        client
            .when(request().withMethod("GET"))
            .respond(response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(jsonResponse));
        ReflectionTestUtils.setField(waymakerService, "waymakerCoursesApiUrl", MOCK_SERVER_URL);
        ReflectionTestUtils.setField(waymakerService, "waymakerJWT", "nonnull");
        List<WaymakerCourse> courseList = waymakerService.fetchWaymakerCourses(); 
        assertNotEquals(courseList, new ArrayList<>());
        assertEquals(courseList.size(), 1);
        assertEquals(courseList.get(0).getRoot_outcome_guid(), "root");
        assertEquals(courseList.get(0).getBook_title(), "Root");
    }

}
