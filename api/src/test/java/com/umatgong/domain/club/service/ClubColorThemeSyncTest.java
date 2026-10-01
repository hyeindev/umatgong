package com.umatgong.domain.club.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import com.umatgong.domain.club.entity.ClubColor;

/**
 * 서버가 내려주는 클럽 색 이름은 프론트 테마(app/src/theme/colors.ts의 colors.club)의 키와 같아야 한다.
 * 한쪽만 바꾸면 프론트가 색을 못 찾는다. 같은 저장소에 있으므로 파일을 직접 읽어 비교한다.
 */
class ClubColorThemeSyncTest {

	// 테스트는 api/ 에서 돈다
	private static final Path THEME = Path.of("..", "app", "src", "theme", "colors.ts");

	@Test
	void 클럽_색_이름이_프론트_테마의_키와_같다() throws IOException {
		assertThat(THEME).as("프론트 테마 파일 %s", THEME.toAbsolutePath()).exists();
		String source = Files.readString(THEME);

		Matcher block = Pattern.compile("club:\\s*\\{([^}]*)}").matcher(source);
		assertThat(block.find()).as("colors.ts에 club 블록이 있어야 한다").isTrue();
		Set<String> themeKeys = new LinkedHashSet<>();
		Matcher key = Pattern.compile("^\\s*([A-Z_]+):", Pattern.MULTILINE).matcher(block.group(1));
		while (key.find()) {
			themeKeys.add(key.group(1));
		}

		assertThat(themeKeys).containsExactlyElementsOf(
			Arrays.stream(ClubColor.values()).map(Enum::name).toList());
	}
}
