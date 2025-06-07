/**
 * Firebase 설정 및 초기화
 */

// Firebase 설정
const firebaseConfig = {
    apiKey: "AIzaSyBEQbl5f04shqlcjSW24YoyMObO_ijs7qs",
    authDomain: "uniboook.firebaseapp.com",
    projectId: "uniboook",
    storageBucket: "uniboook.firebasestorage.app",
    messagingSenderId: "937323819946",
    appId: "1:937323819946:web:8c84233cb6fbc89db971d3",
    measurementId: "G-TDBCTWVD76"
};

// Firebase 인스턴스
let app;
let db;
let storage;

/**
 * Firebase 초기화
 */
function initializeFirebase() {
    try {
        // 이미 초기화되었는지 확인
        if (app && db) {
            return true;
        }
        
        // Firebase SDK 로드 확인
        if (typeof firebase === 'undefined') {
            console.error('Firebase SDK가 로드되지 않았습니다.');
            return false;
        }
        
        
        // Firebase 앱 초기화
        app = firebase.initializeApp(firebaseConfig);
        
        // Firestore 초기화
        db = firebase.firestore();
        
        // Storage 초기화 (이미지 업로드용)
        storage = firebase.storage();
        
        
        return true;
    } catch (error) {
        console.error('Firebase 초기화 실패:', error);
        alert('Firebase 연결에 실패했습니다. 페이지를 새로고침해주세요.');
        return false;
    }
}

/**
 * Firestore 인스턴스 반환
 */
function getFirestore() {
    if (!db) {
        throw new Error('Firebase가 초기화되지 않았습니다.');
    }
    return db;
}

/**
 * Storage 인스턴스 반환
 */
function getStorage() {
    if (!storage) {
        throw new Error('Firebase가 초기화되지 않았습니다.');
    }
    return storage;
}

/**
 * 현재 사용자 ID 반환 (Spring Security에서 가져옴)
 */
function getCurrentUserId() {
    // 메타 태그에서 사용자 ID 가져옴
    const metaUserId = document.querySelector('meta[name="user-id"]');
    if (metaUserId) {
        return parseInt(metaUserId.getAttribute('content'));
    }
    
    throw new Error('사용자 ID를 찾을 수 없습니다. 로그인이 필요합니다.');
}

/**
 * 현재 사용자 이름 반환
 */
function getCurrentUserName() {
    const metaUserName = document.querySelector('meta[name="user-name"]');
    if (metaUserName) {
        return metaUserName.getAttribute('content');
    }
    
    return '알 수 없는 사용자';
}

// Firebase 초기화는 별도로 호출