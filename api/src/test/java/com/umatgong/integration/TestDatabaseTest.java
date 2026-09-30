package com.umatgong.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class TestDatabaseTest {

	@Test
	void 플래그가_없으면_DB_URL이_있어도_건너뛴다() {
		assertThat(TestDatabase.enabled(null, "jdbc:postgresql://localhost:5432/umatgong")).isFalse();
		assertThat(TestDatabase.enabled("false", "jdbc:postgresql://localhost:5432/umatgong")).isFalse();
	}

	@Test
	void 플래그가_있고_로컬_DB면_돈다() {
		assertThat(TestDatabase.enabled("true", "jdbc:postgresql://localhost:5432/umatgong")).isTrue();
		assertThat(TestDatabase.enabled("true", "jdbc:postgresql://127.0.0.1:5432/umatgong")).isTrue();
	}

	@Test
	void 플래그가_있어도_원격_DB면_실패시킨다() {
		assertThatThrownBy(() -> TestDatabase.enabled("true", "jdbc:postgresql://db.railway.internal:5432/railway"))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("db.railway.internal");
		assertThatThrownBy(() -> TestDatabase.enabled("true", null)).isInstanceOf(IllegalStateException.class);
	}
}
