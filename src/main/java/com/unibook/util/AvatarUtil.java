package com.unibook.util;

/**
 * 사용자 아바타 생성 유틸리티
 * 일관된 색상과 이니셜을 제공합니다.
 */
public class AvatarUtil {
    
    private static final String[] AVATAR_COLORS = {
        "avatar-blue", "avatar-purple", "avatar-pink", "avatar-orange",
        "avatar-green", "avatar-teal", "avatar-indigo", "avatar-red"
    };
    
    /**
     * 사용자 이름과 이메일을 기반으로 아바타 색상 클래스를 생성합니다.
     * 같은 사용자는 항상 같은 색상을 가집니다.
     * 
     * @param name 사용자 이름
     * @param email 사용자 이메일 (선택사항)
     * @return 아바타 색상 CSS 클래스
     */
    public static String getAvatarColorClass(String name, String email) {
        if (name == null || name.trim().isEmpty()) {
            return AVATAR_COLORS[0]; // 기본 색상
        }
        
        // 일관된 해시를 위해 이메일을 우선 사용, 없으면 이름 사용
        String hashSource = (email != null && !email.trim().isEmpty()) ? email : name;
        
        int hash = hashSource.hashCode();
        int colorIndex = Math.abs(hash) % AVATAR_COLORS.length;
        
        return AVATAR_COLORS[colorIndex];
    }
    
    /**
     * 사용자 이름에서 이니셜을 생성합니다.
     * 
     * @param name 사용자 이름
     * @return 아바타에 표시할 이니셜 (최대 2글자)
     */
    public static String getAvatarInitials(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "U"; // 기본값
        }
        
        String trimmedName = name.trim();
        String[] nameParts = trimmedName.split("\\s+");
        
        if (nameParts.length == 1) {
            // 한 단어인 경우 첫 글자 사용
            return nameParts[0].substring(0, 1).toUpperCase();
        } else {
            // 여러 단어인 경우 첫 번째와 마지막 단어의 첫 글자 사용
            String firstInitial = nameParts[0].substring(0, 1);
            String lastInitial = nameParts[nameParts.length - 1].substring(0, 1);
            return (firstInitial + lastInitial).toUpperCase();
        }
    }
    
    /**
     * 아바타 데이터를 담는 내부 클래스
     */
    public static class AvatarData {
        private final String initials;
        private final String colorClass;
        
        public AvatarData(String initials, String colorClass) {
            this.initials = initials;
            this.colorClass = colorClass;
        }
        
        public String getInitials() {
            return initials;
        }
        
        public String getColorClass() {
            return colorClass;
        }
    }
    
    /**
     * 완전한 아바타 데이터를 생성합니다.
     * 
     * @param name 사용자 이름
     * @param email 사용자 이메일
     * @return 아바타 데이터 객체
     */
    public static AvatarData generateAvatarData(String name, String email) {
        String initials = getAvatarInitials(name);
        String colorClass = getAvatarColorClass(name, email);
        return new AvatarData(initials, colorClass);
    }
}