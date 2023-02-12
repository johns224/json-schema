package org.rossjohnson.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Set;
import java.util.stream.Collectors;

class CustomerAlertValidationTests {

	private static final String INBOUND_ALERT_SCHEMA_JSON = "inbound-alert.schema.json";

	@Test
	void testValidEmailOnlyAlert() throws JsonProcessingException {
		String inboundJson = "{\n" +
				"  \"alert\": {\n" +
				"    \"customer-id\": \"12345\",\n" +
				"    \"title\": \"Transaction alert\",\n" +
				"    \"message\": \"A transaction of $5.37 has posted to your account from ACME corp\",\n" +
				"    \"email\": \"jo@schmo.com\",\n" +
				"    \"alert-type\": \"email\"\n" +
				"  }\n" +
				"}";

		CustomerAlert alert = getValidatedCustomerAlert(inboundJson);

		Assertions.assertEquals("email", alert.getAlertType());
		Assertions.assertEquals("jo@schmo.com", alert.getEmail());
	}

	@Test
	void testPushAlertRequiresNoEmailNorPhone() throws JsonProcessingException {
		String inboundJson = "{\n" +
				"  \"alert\": {\n" +
				"    \"customer-id\": \"foo124\",\n" +
				"    \"title\": \"Transaction alert\",\n" +
				"    \"message\": \"A transaction of $5.37 has posted to your account from ACME corp\",\n" +
				"    \"alert-type\": \"push\"\n" +
				"  }\n" +
				"}";

		CustomerAlert alert = getValidatedCustomerAlert(inboundJson);

		Assertions.assertEquals("push", alert.getAlertType());
		Assertions.assertNull(alert.getEmail());
		Assertions.assertNull(alert.getDestinationPhoneNumber());
	}

	@Test
	void testBothEmailAndPhoneAllowed() throws JsonProcessingException {

		// as long as the alert-type is valid, extra but unused properties (like phone in this case) are ok
		String inboundJson = "{\n" +
				"  \"alert\": {\n" +
				"    \"customer-id\": \"foo124\",\n" +
				"    \"title\": \"Transaction alert\",\n" +
				"    \"message\": \"A transaction of $5.37 has posted to your account from ACME corp\",\n" +
				"    \"destination-phone-number\": \"2443434\",\n" +
				"    \"alert-type\": \"email\",\n" +
				"    \"destination-phone-number\": \"312-445-8842\",\n" +
				"    \"email\": \"jo@schmo.com\"\n" +
				"  }\n" +
				"}";

		CustomerAlert alert = getValidatedCustomerAlert(inboundJson);

		Assertions.assertEquals("email", alert.getAlertType());
	}

	@Test
	void testInvalidAlertType() throws JsonProcessingException {
		String inboundJson = "{\n" +
				"  \"alert\": {\n" +
				"    \"customer-id\": \"12345\",\n" +
				"    \"title\": \"Transaction alert\",\n" +
				"    \"message\": \"A transaction of $5.37 has posted to your account from ACME corp\",\n" +
				"    \"email\": \"jo@schmo.com\",\n" +
				"    \"alert-type\": \"foo\"\n" +
				"  }\n" +
				"}";

		Set<ValidationMessage> errors = getValidationErrors(inboundJson);

		Assertions.assertTrue(errors.stream().anyMatch(
				e -> e.getMessage()
				.contains("does not have a value in the enumeration")),
				"Validation should fail due to invalid alert-type"
		);
	}

	@Test
	void testMissingAlertType() throws JsonProcessingException {
		String inboundJson = "{\n" +
				"  \"alert\": {\n" +
				"    \"customer-id\": \"12345\",\n" +
				"    \"title\": \"Transaction alert\",\n" +
				"    \"message\": \"A transaction of $5.37 has posted to your account from ACME corp\",\n" +
				"    \"email\": \"jo@schmo.com\"\n" +
				"  }\n" +
				"}";

		Set<ValidationMessage> errors = getValidationErrors(inboundJson);

		Assertions.assertTrue(errors.stream().anyMatch(e -> e.getMessage()
						.contains("alert-type: is missing but it is required")),
				"Validation should fail due to missing alert-type"
		);
	}

	@Test
	void testMissingMessage() throws JsonProcessingException {
		String inboundJson = "{\n" +
				"  \"alert\": {\n" +
				"    \"customer-id\": \"12345\",\n" +
				"    \"title\": \"Transaction alert\",\n" +
				"    \"email\": \"jo@schmo.com\",\n" +
				"    \"alert-type\": \"email\"\n" +
				"  }\n" +
				"}";

		Set<ValidationMessage> errors = getValidationErrors(inboundJson);

		Assertions.assertTrue(errors.stream().anyMatch(e -> e.getMessage()
						.contains("message: is missing but it is required")),
				"Validation should fail due to missing message"
		);
	}

	@Test
	void testMissingTitleAllowed() throws JsonProcessingException {
		String inboundJson = "{\n" +
				"  \"alert\": {\n" +
				"    \"customer-id\": \"12345\",\n" +
				"    \"message\": \"A transaction of $5.37 has posted to your account from ACME corp\",\n" +
				"    \"alert-type\": \"push\"\n" +
				"  }\n" +
				"}";

		CustomerAlert alert = getValidatedCustomerAlert(inboundJson);

		Assertions.assertEquals("push", alert.getAlertType());
	}

	@Test
	void testMissingCustomerId() throws JsonProcessingException {
		String inboundJson = "{\n" +
				"  \"alert\": {\n" +
				"    \"title\": \"Transaction alert\",\n" +
				"    \"message\": \"Large transaction posted to your account\",\n" +
				"    \"email\": \"jo@schmo.com\",\n" +
				"    \"alert-type\": \"email\"\n" +
				"  }\n" +
				"}";

		Set<ValidationMessage> errors = getValidationErrors(inboundJson);

		Assertions.assertTrue(errors.stream().anyMatch(e -> e.getMessage()
						.contains("customer-id: is missing but it is required")),
				"Validation should fail due to missing customer-id"
		);
	}

	////////////////////////////////////////////////////////////////////////////////////////////////
	// End of tests
	////////////////////////////////////////////////////////////////////////////////////////////////

	// for debugging only
	void printErrors(Set<ValidationMessage> errors) {
		errors.forEach(e -> System.out.println(e.getMessage()));
	}

	private Set<ValidationMessage> getValidationErrors(String inboundJson) throws JsonProcessingException {
		InputStream schemaAsStream = CustomerAlertValidationTests.class.getClassLoader().getResourceAsStream(
				INBOUND_ALERT_SCHEMA_JSON);
		JsonSchema schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(schemaAsStream);

		ObjectMapper mapper = new ObjectMapper();
		mapper.setPropertyNamingStrategy(PropertyNamingStrategy.KEBAB_CASE);
		JsonNode jsonNode = mapper.readTree(inboundJson);
		return schema.validate(jsonNode);
	}

	/*
	 Only to be used for tests that expect a successfully validated object to be returned.
	 For failure cases, use getValidationErrors()
	*/
	private CustomerAlert getValidatedCustomerAlert(String requestStr) throws JsonProcessingException {
		InputStream schemaAsStream = CustomerAlertValidationTests.class.getClassLoader().getResourceAsStream(
				INBOUND_ALERT_SCHEMA_JSON);
		JsonSchema schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(schemaAsStream);

		ObjectMapper mapper = new ObjectMapper();
		mapper.setPropertyNamingStrategy(PropertyNamingStrategy.KEBAB_CASE);
		JsonNode jsonNode = mapper.readTree(requestStr);
		Set<ValidationMessage> errors = schema.validate(jsonNode);

		if (!errors.isEmpty()) {
			String joinedErrors = "\n" + errors.stream()
					.map(ValidationMessage::getMessage)
					.collect(Collectors.joining("\n"));
			Assertions.fail(joinedErrors);
		}

		return mapper.readValue(requestStr, CustomerAlertRequest.class).getAlert();
	}
}
