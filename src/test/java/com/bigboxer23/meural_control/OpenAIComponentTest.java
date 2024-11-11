package com.bigboxer23.meural_control;

import static org.junit.jupiter.api.Assertions.*;

import com.bigboxer23.meural_control.data.SourceItem;
import java.util.Optional;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class OpenAIComponentTest {
	@Autowired
	private OpenAIComponent component;

	@ParameterizedTest
	@ValueSource(ints = {1, 2, 3})
	public void testGenerateItem(int mode) {
		component.setMode(mode);
		assertTrue(component.getPrompt().isPresent());
		String prompt = component.getPrompt().get();
		Optional<SourceItem> item = component.generateItem();
		assertTrue(item.isPresent());
		assertNotNull(item.get().getUrl());
		assertNotEquals(prompt, component.getPrompt().get());
	}
}
