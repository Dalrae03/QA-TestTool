package com.tms.global.util;

/**
 * 프로젝트 간 데이터 격리를 위한 식별자 호환성 판정.
 *
 * <p>인증된 사용자 신원이 없는 현재 구조에서 이는 보안 경계가 아니라 데이터 무결성 보호다.
 * 서로 다른 프로젝트의 엔티티가 한 컨테이너(스위트/실행/케이스)에 섞이는 것을 막는다.
 */
public final class ProjectScope {

    private ProjectScope() {
    }

    /**
     * 두 프로젝트 식별자가 호환되는지 판정한다.
     * 둘 중 하나가 null(레거시·미지정)이면 허용하고, 둘 다 값이 있으면 같아야 한다.
     */
    public static boolean compatible(Long a, Long b) {
        return a == null || b == null || a.equals(b);
    }
}
