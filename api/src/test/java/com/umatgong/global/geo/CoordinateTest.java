package com.umatgong.global.geo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CoordinateTest {

	@Test
	void 위도_경도를_그대로_돌려준다() {
		Coordinate c = Coordinate.of(37.556, 126.914);

		assertThat(c.lat()).isEqualTo(37.556);
		assertThat(c.lng()).isEqualTo(126.914);
	}

	@Test
	void Point에는_경도가_x_위도가_y로_들어간다() {
		Coordinate c = Coordinate.of(37.556, 126.914);

		assertThat(c.toPoint().getX()).isEqualTo(126.914);
		assertThat(c.toPoint().getY()).isEqualTo(37.556);
		assertThat(c.toPoint().getSRID()).isEqualTo(4326);
	}

	@Test
	void 범위를_벗어난_좌표는_거부한다() {
		// 위도·경도를 뒤집어 넘기는 실수를 여기서 잡는다 (한국 경도 126~130은 위도 범위 밖).
		assertThatThrownBy(() -> Coordinate.of(126.914, 37.556)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Coordinate.of(0, 181)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void 같은_위경도면_같은_좌표다() {
		assertThat(Coordinate.of(37.5, 127.0)).isEqualTo(Coordinate.of(37.5, 127.0));
	}
}
