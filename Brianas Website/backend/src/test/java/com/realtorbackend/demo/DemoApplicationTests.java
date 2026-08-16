package com.realtorbackend.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DemoApplicationTests {

	@Autowired
	private MockMvc mockMvc;

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
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"firstName\":\"Role\",\"lastName\":\"Tester\",\"email\":\"" + uniqueEmail + "\",\"role\":\"ADMIN\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.role").value("ADMIN"));
	}

	@Test
	void userCanRegisterAndLoginWithPasswordHash() throws Exception {
		String uniqueEmail = "auth.user." + System.nanoTime() + "@example.com";

		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"firstName\":\"Auth\",\"lastName\":\"User\",\"email\":\"" + uniqueEmail + "\",\"password\":\"StrongPass123!\",\"role\":\"ADMIN\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.email").value(uniqueEmail))
				.andExpect(jsonPath("$.role").value("USER"));

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"" + uniqueEmail + "\",\"password\":\"StrongPass123!\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(uniqueEmail));
	}

	@Test
	void userCanBeUpdated() throws Exception {
		String uniqueEmail = "edit.user." + System.nanoTime() + "@example.com";

		String response = mockMvc.perform(post("/api/users")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"firstName\":\"Before\",\"lastName\":\"Edit\",\"email\":\"" + uniqueEmail
						+ "\",\"password\":\"StrongPass123!\",\"role\":\"USER\"}"))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();

		long id = com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
				.readTree(response).get("id").asLong();

		mockMvc.perform(put("/api/users/{id}", id)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"firstName\":\"After\",\"lastName\":\"Updated\",\"email\":\"" + uniqueEmail
						+ "\",\"role\":\"ADMIN\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.firstName").value("After"))
				.andExpect(jsonPath("$.role").value("ADMIN"));
	}
}
