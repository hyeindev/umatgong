package com.umatgong.domain.club.dto;

/**
 * @param reissue true면 새 코드를 만들고 이전 코드(이미 보낸 링크)는 무효가 된다. 클럽장만 할 수 있다.
 *                없거나 false면 지금 코드를 그대로 돌려준다
 */
public record InviteRequest(Boolean reissue) {

	public boolean wantsReissue() {
		return Boolean.TRUE.equals(reissue);
	}
}
