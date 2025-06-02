/**
 * Enhanced Header JavaScript
 * Handles user avatars, scroll effects, search functionality, and micro-interactions
 */

(function() {
    'use strict';

    // ===============================================
    // USER AVATAR SYSTEM
    // ===============================================
    
    const avatarColors = [
        'avatar-blue', 'avatar-purple', 'avatar-pink', 'avatar-orange',
        'avatar-green', 'avatar-teal', 'avatar-indigo', 'avatar-red'
    ];

    function generateUserAvatar(name, email) {
        if (!name || name.trim() === '') return null;
        
        // Generate initials
        const nameParts = name.trim().split(' ');
        let initials = '';
        
        if (nameParts.length === 1) {
            // Single name - take first character
            initials = nameParts[0].charAt(0);
        } else {
            // Multiple names - take first character of first and last name
            initials = nameParts[0].charAt(0) + nameParts[nameParts.length - 1].charAt(0);
        }
        
        // Generate consistent color based on email hash
        const colorIndex = hashCode(email || name) % avatarColors.length;
        const colorClass = avatarColors[Math.abs(colorIndex)];
        
        return {
            initials: initials.toUpperCase(),
            colorClass: colorClass
        };
    }

    function hashCode(str) {
        let hash = 0;
        for (let i = 0; i < str.length; i++) {
            const char = str.charCodeAt(i);
            hash = ((hash << 5) - hash) + char;
            hash = hash & hash; // Convert to 32-bit integer
        }
        return hash;
    }

    function createAvatarElement(avatarData) {
        if (!avatarData) return null;
        
        const avatar = document.createElement('div');
        avatar.className = `user-avatar ${avatarData.colorClass}`;
        avatar.textContent = avatarData.initials;
        avatar.setAttribute('aria-label', `사용자 아바타: ${avatarData.initials}`);
        
        return avatar;
    }

    function initializeUserAvatars() {
        // Get user info from meta tags
        const userNameMeta = document.querySelector('meta[name="user-name"]');
        const userEmailMeta = document.querySelector('meta[name="user-email"]');
        
        if (!userNameMeta) return;
        
        const userName = userNameMeta.content;
        const userEmail = userEmailMeta ? userEmailMeta.content : '';
        
        // Generate avatar data
        const avatarData = generateUserAvatar(userName, userEmail);
        if (!avatarData) return;
        
        // Replace all person-circle icons with avatars
        const profileIcons = document.querySelectorAll('.bi-person-circle');
        profileIcons.forEach(icon => {
            const avatar = createAvatarElement(avatarData);
            if (avatar) {
                icon.parentNode.replaceChild(avatar, icon);
            }
        });
    }

    // ===============================================
    // SCROLL EFFECTS
    // ===============================================
    
    function initializeScrollEffects() {
        const navbar = document.querySelector('.navbar');
        let lastScrollTop = 0;
        
        function handleScroll() {
            const scrollTop = window.pageYOffset || document.documentElement.scrollTop;
            
            if (scrollTop > 50) {
                navbar.classList.add('scrolled');
            } else {
                navbar.classList.remove('scrolled');
            }
            
            lastScrollTop = scrollTop;
        }
        
        // Throttle scroll events for better performance
        let ticking = false;
        window.addEventListener('scroll', () => {
            if (!ticking) {
                requestAnimationFrame(() => {
                    handleScroll();
                    ticking = false;
                });
                ticking = true;
            }
        });
    }

    // ===============================================
    // SEARCH FUNCTIONALITY
    // ===============================================
    
    function initializeSearchBar() {
        const searchInput = document.getElementById('headerSearch');
        if (!searchInput) return;
        
        // Handle search submission
        function handleSearch(event) {
            event.preventDefault();
            const query = searchInput.value.trim();
            
            if (query) {
                // Redirect to posts page with search query
                window.location.href = `/posts?search=${encodeURIComponent(query)}`;
            }
        }
        
        // Add search form handler
        const searchForm = searchInput.closest('form');
        if (searchForm) {
            searchForm.addEventListener('submit', handleSearch);
        }
        
        // Add keyboard shortcut (Ctrl+K)
        document.addEventListener('keydown', (event) => {
            if ((event.ctrlKey || event.metaKey) && event.key === 'k') {
                event.preventDefault();
                searchInput.focus();
                searchInput.select();
            }
        });
        
        // Add search input animations
        searchInput.addEventListener('focus', () => {
            searchInput.parentNode.style.transform = 'scale(1.02)';
        });
        
        searchInput.addEventListener('blur', () => {
            searchInput.parentNode.style.transform = 'scale(1)';
        });
    }

    // ===============================================
    // THEME TOGGLE ENHANCEMENTS
    // ===============================================
    
    function enhanceThemeToggle() {
        const themeToggle = document.getElementById('themeToggle');
        const themeIcon = document.getElementById('themeIcon');
        
        if (!themeToggle || !themeIcon) return;
        
        // Add enhanced click animation
        themeToggle.addEventListener('click', () => {
            // Add a ripple effect
            const ripple = document.createElement('span');
            ripple.style.cssText = `
                position: absolute;
                top: 50%;
                left: 50%;
                width: 0;
                height: 0;
                border-radius: 50%;
                background: rgba(102, 126, 234, 0.3);
                transform: translate(-50%, -50%);
                animation: ripple 0.6s ease-out;
                pointer-events: none;
            `;
            
            themeToggle.appendChild(ripple);
            
            setTimeout(() => {
                if (ripple.parentNode) {
                    ripple.parentNode.removeChild(ripple);
                }
            }, 600);
        });
        
        // Add ripple animation CSS
        if (!document.getElementById('ripple-animation')) {
            const style = document.createElement('style');
            style.id = 'ripple-animation';
            style.textContent = `
                @keyframes ripple {
                    to {
                        width: 40px;
                        height: 40px;
                        opacity: 0;
                    }
                }
            `;
            document.head.appendChild(style);
        }
    }

    // ===============================================
    // LOADING PROGRESS BAR
    // ===============================================
    
    function initializeLoadingBar() {
        // Create loading bar element
        const loadingBar = document.createElement('div');
        loadingBar.className = 'loading-bar';
        loadingBar.id = 'loadingBar';
        
        const navbar = document.querySelector('.navbar');
        if (navbar) {
            navbar.appendChild(loadingBar);
        }
        
        // Show loading bar on navigation
        const links = document.querySelectorAll('a[href]:not([href^="#"]):not([href^="javascript:"]):not([target="_blank"])');
        links.forEach(link => {
            link.addEventListener('click', (event) => {
                // Don't show loading for same page links or if prevented
                if (event.defaultPrevented) return;
                
                const href = link.getAttribute('href');
                if (href && href !== window.location.pathname) {
                    loadingBar.classList.add('active');
                }
            });
        });
        
        // Hide loading bar when page loads
        window.addEventListener('load', () => {
            loadingBar.classList.remove('active');
        });
        
        // Hide loading bar on back/forward navigation
        window.addEventListener('pageshow', () => {
            loadingBar.classList.remove('active');
        });
    }

    // ===============================================
    // MICRO-INTERACTIONS
    // ===============================================
    
    function initializeMicroInteractions() {
        // Add hover effects to navigation items
        const navLinks = document.querySelectorAll('.nav-link');
        navLinks.forEach(link => {
            link.addEventListener('mouseenter', () => {
                link.style.transform = 'translateY(-1px)';
            });
            
            link.addEventListener('mouseleave', () => {
                link.style.transform = 'translateY(0)';
            });
        });
        
        // Add click feedback to buttons
        const buttons = document.querySelectorAll('.btn');
        buttons.forEach(button => {
            button.addEventListener('mousedown', () => {
                button.style.transform = 'scale(0.95)';
            });
            
            button.addEventListener('mouseup', () => {
                button.style.transform = 'scale(1)';
            });
            
            button.addEventListener('mouseleave', () => {
                button.style.transform = 'scale(1)';
            });
        });
        
        // Add notification badge animation
        const badges = document.querySelectorAll('.badge');
        badges.forEach(badge => {
            if (badge.textContent && parseInt(badge.textContent) > 0) {
                badge.style.animation = 'pulse 2s infinite';
            }
        });
    }

    // ===============================================
    // RESPONSIVE BEHAVIOR
    // ===============================================
    
    function initializeResponsiveBehavior() {
        const navbar = document.querySelector('.navbar');
        const navbarCollapse = document.querySelector('.navbar-collapse');
        const navbarToggler = document.querySelector('.navbar-toggler');
        
        if (!navbar || !navbarCollapse || !navbarToggler) return;
        
        // Smooth collapse animation
        navbarToggler.addEventListener('click', () => {
            navbarCollapse.style.transition = 'all 0.3s ease';
        });
        
        // Close mobile menu when clicking outside
        document.addEventListener('click', (event) => {
            const isClickInsideNav = navbar.contains(event.target);
            const isNavOpen = navbarCollapse.classList.contains('show');
            
            if (!isClickInsideNav && isNavOpen) {
                navbarToggler.click();
            }
        });
        
        // Close mobile menu when pressing Escape
        document.addEventListener('keydown', (event) => {
            if (event.key === 'Escape' && navbarCollapse.classList.contains('show')) {
                navbarToggler.click();
            }
        });
    }

    // ===============================================
    // ACCESSIBILITY IMPROVEMENTS
    // ===============================================
    
    function initializeAccessibility() {
        // Add skip-to-content link
        const skipLink = document.createElement('a');
        skipLink.href = '#main-content';
        skipLink.textContent = '메인 콘텐츠로 건너뛰기';
        skipLink.className = 'visually-hidden-focusable btn btn-primary';
        skipLink.style.cssText = `
            position: fixed;
            top: 10px;
            left: 10px;
            z-index: 1050;
            transform: translateY(-100px);
            transition: transform 0.3s ease;
        `;
        
        skipLink.addEventListener('focus', () => {
            skipLink.style.transform = 'translateY(0)';
        });
        
        skipLink.addEventListener('blur', () => {
            skipLink.style.transform = 'translateY(-100px)';
        });
        
        document.body.insertBefore(skipLink, document.body.firstChild);
        
        // Improve dropdown accessibility
        const dropdownToggles = document.querySelectorAll('[data-bs-toggle="dropdown"]');
        dropdownToggles.forEach(toggle => {
            toggle.addEventListener('keydown', (event) => {
                if (event.key === 'Enter' || event.key === ' ') {
                    event.preventDefault();
                    toggle.click();
                }
            });
        });
    }

    // ===============================================
    // INITIALIZATION
    // ===============================================
    
    function initializeEnhancedHeader() {
        // Wait for DOM to be ready
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', init);
        } else {
            init();
        }
        
        function init() {
            try {
                initializeUserAvatars();
                initializeScrollEffects();
                initializeSearchBar();
                enhanceThemeToggle();
                initializeLoadingBar();
                initializeMicroInteractions();
                initializeResponsiveBehavior();
                initializeAccessibility();
                
                console.log('Enhanced header initialized successfully');
            } catch (error) {
                console.error('Error initializing enhanced header:', error);
            }
        }
    }

    // Start initialization
    initializeEnhancedHeader();

})();