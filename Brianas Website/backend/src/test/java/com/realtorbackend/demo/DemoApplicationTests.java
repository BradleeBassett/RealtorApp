package com.realtorbackend.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.email-verification.enabled=false")
@AutoConfigureMockMvc
class DemoApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserService userService;

	private String adminAuthorization() throws Exception {
		String email = "admin." + System.nanoTime() + "@example.com";
		User admin = new User();
		admin.setFirstName("Admin");
		admin.setLastName("Tester");
		admin.setPhoneNumber("555-010-1234");
		admin.setEmail(email);
		admin.setPassword("StrongPass123!");
		admin.setRole(User.Role.ADMIN);
		userService.create(admin);

		String response = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"" + email + "\",\"password\":\"StrongPass123!\"}"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		String token = com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
				.readTree(response).get("token").asText();
		return "Bearer " + token;
	}

	@Test
	void healthEndpointReturnsOk() throws Exception {
		mockMvc.perform(get("/api/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ok"));
	}

	@Test
	void statusEndpointReportsBackendAndDatabase() throws Exception {
		mockMvc.perform(get("/api/health/status"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.backend").value("connected"))
				.andExpect(jsonPath("$.database").value("connected"));
	}

	@Test
	void userCanBeCreatedWithRole() throws Exception {
		String uniqueEmail = "role.tester." + System.nanoTime() + "@example.com";
		mockMvc.perform(post("/api/users")
				.header("Authorization", adminAuthorization())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"firstName\":\"Role\",\"lastName\":\"Tester\",\"phoneNumber\":\"555-010-1234\",\"email\":\"" + uniqueEmail + "\",\"description\":\"Met at an open house.\",\"role\":\"ADMIN\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.description").value("Met at an open house."))
				.andExpect(jsonPath("$.role").value("ADMIN"));
	}

	@Test
	void userCanRegisterAndLoginWithPasswordHash() throws Exception {
		String uniqueEmail = "auth.user." + System.nanoTime() + "@example.com";

		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"firstName\":\"Auth\",\"lastName\":\"User\",\"phoneNumber\":\"555-010-1234\",\"email\":\"" + uniqueEmail + "\",\"password\":\"StrongPass123!\",\"role\":\"ADMIN\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.email").value(uniqueEmail))
				.andExpect(jsonPath("$.phoneNumber").value("555-010-1234"))
				.andExpect(jsonPath("$.role").value("USER"));

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"" + uniqueEmail + "\",\"password\":\"StrongPass123!\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").isNotEmpty())
				.andExpect(jsonPath("$.user.email").value(uniqueEmail));
	}

	@Test
	void duplicateRegistrationReturnsClearMessage() throws Exception {
		String uniqueEmail = "duplicate.user." + System.nanoTime() + "@example.com";
		String registration = "{\"firstName\":\"Duplicate\",\"lastName\":\"User\",\"phoneNumber\":\"555-010-1234\",\"email\":\""
				+ uniqueEmail + "\",\"password\":\"StrongPass123!\"}";

		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(registration))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(registration))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("A user with that email already exists"));
	}

	@Test
	void userCanBeUpdated() throws Exception {
		String uniqueEmail = "edit.user." + System.nanoTime() + "@example.com";

		String response = mockMvc.perform(post("/api/users")
				.header("Authorization", adminAuthorization())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"firstName\":\"Before\",\"lastName\":\"Edit\",\"phoneNumber\":\"555-010-1234\",\"email\":\"" + uniqueEmail
						+ "\",\"password\":\"StrongPass123!\",\"role\":\"USER\"}"))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();

		long id = com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
				.readTree(response).get("id").asLong();

		mockMvc.perform(put("/api/users/{id}", id)
				.header("Authorization", adminAuthorization())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"firstName\":\"After\",\"lastName\":\"Updated\",\"phoneNumber\":\"555-010-5678\",\"email\":\"" + uniqueEmail
						+ "\",\"role\":\"ADMIN\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.firstName").value("After"))
				.andExpect(jsonPath("$.phoneNumber").value("555-010-5678"))
				.andExpect(jsonPath("$.role").value("ADMIN"));
	}

	@Test
	void listingDescriptionIsRequiredAndPersists() throws Exception {
		mockMvc.perform(post("/api/entries")
				.header("Authorization", adminAuthorization())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"address\":\"1 Main Street\",\"city\":\"LaBelle\",\"state\":\"FL\",\"zipcode\":\"33935\",\"price\":250000}"))
				.andExpect(status().isBadRequest());

		mockMvc.perform(post("/api/entries")
				.header("Authorization", adminAuthorization())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"address\":\"1 Main Street\",\"city\":\"LaBelle\",\"state\":\"FL\",\"zipcode\":\"33935\",\"price\":250000,\"description\":\"A peaceful home near town.\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.description").value("A peaceful home near town."));
	}

	@Test
	void protectedEndpointsRejectUnauthenticatedRequests() throws Exception {
		mockMvc.perform(post("/api/users")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isForbidden());

		mockMvc.perform(delete("/api/entries/{id}", 1L))
				.andExpect(status().isForbidden());
	}
}
