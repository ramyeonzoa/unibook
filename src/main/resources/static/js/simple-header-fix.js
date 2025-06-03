/**
 * Simple Header Fix
 */
document.addEventListener('DOMContentLoaded', function() {
    // Handle dark mode for guest desktop
    const guestDesktopToggle = document.getElementById('themeToggleGuestDesktop');
    if (guestDesktopToggle) {
        const icon = document.getElementById('themeIconGuestDesktop');
        
        // Set initial state
        const currentTheme = document.documentElement.getAttribute('data-bs-theme') || 'light';
        icon.className = currentTheme === 'dark' ? 'bi bi-moon-fill' : 'bi bi-sun-fill';
        
        guestDesktopToggle.addEventListener('click', function() {
            const html = document.documentElement;
            const currentTheme = html.getAttribute('data-bs-theme') || 'light';
            const newTheme = currentTheme === 'dark' ? 'light' : 'dark';
            
            html.setAttribute('data-bs-theme', newTheme);
            localStorage.setItem('theme', newTheme);
            
            // Update all theme icons
            document.querySelectorAll('[id^="themeIcon"]').forEach(i => {
                i.className = newTheme === 'dark' ? 'bi bi-moon-fill' : 'bi bi-sun-fill';
            });
        });
    }
});