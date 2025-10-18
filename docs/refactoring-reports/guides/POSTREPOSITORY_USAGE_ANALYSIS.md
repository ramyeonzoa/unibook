# PostRepository Usage Analysis in PostService

## Executive Summary

PostService uses 23 different PostRepository methods across 1164 lines of code. The analysis reveals that certain methods are critical for core functionality while others are used less frequently or for specific features.

## Detailed Method Usage Analysis

### 1. **findById** (6 occurrences)
- **Line numbers**: 197, 348, 402, 437, 586, 599, 637, 874, 892
- **Usage context**: 
  - Basic post retrieval for viewing (line 197)
  - Pre-update validation (line 348)
  - Pre-delete validation (line 402)
  - Status update operations (line 437)
  - View count increment (line 586, 637)
  - Admin blocking/unblocking (line 874, 892)
- **Criticality**: **HIGH** - Core CRUD operation
- **Notes**: Most frequently used method, essential for almost all post operations

### 2. **save** (5 occurrences)
- **Line numbers**: 315, 380, 458, 589, 639, 882, 901
- **Usage context**:
  - Creating new posts (line 315)
  - Updating existing posts (line 380)
  - Status changes (line 458)
  - View count updates (line 589, 639)
  - Admin blocking/unblocking (line 882, 901)
- **Criticality**: **HIGH** - Core persistence operation

### 3. **findByIdWithDetails** (1 occurrence)
- **Line number**: 204
- **Usage context**: Detailed post view with all related entities
- **Criticality**: **HIGH** - Critical for post detail pages
- **Notes**: Uses Fetch Join to prevent N+1 issues

### 4. **findRecentPostsWithDetails** (1 occurrence)
- **Line number**: 211
- **Usage context**: Homepage recent posts display
- **Criticality**: **MEDIUM** - Important for homepage but limited scope

### 5. **findByBook_BookIdAndStatusNot** (1 occurrence)
- **Line number**: 218
- **Usage context**: Finding related posts by book (excluding blocked)
- **Criticality**: **LOW** - Feature-specific (related posts)

### 6. **findBySubject_SubjectIdWithDetails** (1 occurrence)
- **Line number**: 232
- **Usage context**: Finding posts by subject for related posts
- **Criticality**: **LOW** - Feature-specific (related posts)

### 7. **findByUserIdWithDetails** (2 occurrences)
- **Line numbers**: 243, 251
- **Usage context**: User's posted items in profile
- **Criticality**: **MEDIUM** - Important for user profiles

### 8. **findByUserIdWithDetailsAndPriceFilter** (1 occurrence)
- **Line number**: 253
- **Usage context**: User's posts with price filtering
- **Criticality**: **LOW** - Advanced filtering feature

### 9. **findProfessorNameById** (1 occurrence)
- **Line number**: 277
- **Usage context**: Getting professor name for page titles
- **Criticality**: **LOW** - UI enhancement

### 10. **findBySchoolIdWithDetails** (1 occurrence)
- **Line number**: 286
- **Usage context**: School-specific post listings
- **Criticality**: **LOW** - Specific use case

### 11. **findByStatusWithDetails** (1 occurrence)
- **Line number**: 293
- **Usage context**: Status-based filtering
- **Criticality**: **LOW** - Admin/specific features

### 12. **delete** (1 occurrence)
- **Line number**: 427
- **Usage context**: Post deletion
- **Criticality**: **HIGH** - Core CRUD operation

### 13. **searchPostsWithFulltext** (1 occurrence)
- **Line number**: 108-116
- **Usage context**: Full-text search with filters
- **Criticality**: **HIGH** - Primary search functionality
- **Notes**: Complex native query with multiple joins

### 14. **findAllByIdInWithDetails** (1 occurrence)
- **Line number**: 131
- **Usage context**: Batch fetch posts after search
- **Criticality**: **HIGH** - Part of search result processing

### 15. **findByFilters** (1 occurrence)
- **Line number**: 190
- **Usage context**: Filtered browsing without search
- **Criticality**: **HIGH** - Core browsing functionality

### 16. **findBySubjectIdWithFilters** (1 occurrence)
- **Line number**: 76
- **Usage context**: Subject-specific filtering
- **Criticality**: **MEDIUM** - Important for academic filtering

### 17. **findByProfessorIdWithFilters** (1 occurrence)
- **Line number**: 83
- **Usage context**: Professor-specific filtering
- **Criticality**: **MEDIUM** - Important for academic filtering

### 18. **findByBookTitleWithFilters** (1 occurrence)
- **Line number**: 90
- **Usage context**: Book title search with filters
- **Criticality**: **MEDIUM** - Alternative search method

### 19. **count** (1 occurrence)
- **Line number**: 830
- **Usage context**: Total post count for admin dashboard
- **Criticality**: **LOW** - Admin statistics

### 20. **findByTitleContainingOrDescriptionContaining** (1 occurrence)
- **Line number**: 844-845
- **Usage context**: Admin search without status filter
- **Criticality**: **LOW** - Admin feature

### 21. **findByTitleContainingOrDescriptionContainingAndStatus** (1 occurrence)
- **Line number**: 841-842
- **Usage context**: Admin search with status filter
- **Criticality**: **LOW** - Admin feature

### 22. **findByStatus** (1 occurrence)
- **Line number**: 849
- **Usage context**: Admin status filtering
- **Criticality**: **LOW** - Admin feature

### 23. **countByStatus** (1 occurrence)
- **Line number**: 864
- **Usage context**: Status statistics for admin
- **Criticality**: **LOW** - Admin statistics

### 24. **findByBookIdForPriceTrend** (1 occurrence)
- **Line number**: 940
- **Usage context**: Price trend chart data
- **Criticality**: **LOW** - Analytics feature

### 25. **findAll** (1 occurrence)
- **Line number**: 851
- **Usage context**: Admin post listing (paginated)
- **Criticality**: **LOW** - Admin feature

## Method Groupings

### Core CRUD Operations (CRITICAL)
- `findById` - Basic retrieval
- `findByIdWithDetails` - Detailed retrieval
- `save` - Create/Update
- `delete` - Deletion

### Search and Browse (CRITICAL)
- `searchPostsWithFulltext` - Full-text search
- `findAllByIdInWithDetails` - Search result processing
- `findByFilters` - Basic filtering

### Academic Filtering (MEDIUM)
- `findBySubjectIdWithFilters` - Subject-based
- `findByProfessorIdWithFilters` - Professor-based
- `findByBookTitleWithFilters` - Book title-based

### User-Specific (MEDIUM)
- `findByUserIdWithDetails` - User's posts
- `findByUserIdWithDetailsAndPriceFilter` - With price filter

### Feature-Specific (LOW)
- `findRecentPostsWithDetails` - Homepage
- `findByBook_BookIdAndStatusNot` - Related posts
- `findBySubject_SubjectIdWithDetails` - Related posts
- `findProfessorNameById` - UI labels
- `findBySchoolIdWithDetails` - School filtering
- `findByStatusWithDetails` - Status filtering
- `findByBookIdForPriceTrend` - Analytics

### Admin Operations (LOW)
- `count`, `countByStatus` - Statistics
- `findByTitleContainingOrDescriptionContaining` - Admin search
- `findByTitleContainingOrDescriptionContainingAndStatus` - Admin filtered search
- `findByStatus` - Status listing
- `findAll` - Full listing

## Refactoring Priority

### Priority 1: Core Methods (Must maintain 100% compatibility)
1. `findById` - Most used, fundamental operation
2. `save` - Core persistence
3. `findByIdWithDetails` - Critical for detail views
4. `searchPostsWithFulltext` - Primary search
5. `findAllByIdInWithDetails` - Search support
6. `findByFilters` - Core browsing
7. `delete` - Core operation

### Priority 2: Important Features
1. `findByUserIdWithDetails` - User profiles
2. `findBySubjectIdWithFilters` - Academic search
3. `findByProfessorIdWithFilters` - Academic search
4. `findByBookTitleWithFilters` - Book search
5. `findRecentPostsWithDetails` - Homepage

### Priority 3: Nice-to-have
- All admin-specific methods
- Analytics methods
- Advanced filtering methods

## Patterns Observed

1. **Fetch Join Usage**: Many methods use custom queries with Fetch Joins to prevent N+1 issues
2. **Status Filtering**: Most listing methods exclude BLOCKED posts except for admin/owner views
3. **Flexible Filtering**: Many methods accept nullable parameters for optional filtering
4. **Pagination Support**: List methods generally support Spring Data pagination
5. **Search Complexity**: Full-text search uses native queries with MySQL-specific features

## Other Repository Dependencies

PostService also uses the following repositories that may need consideration:

### WishlistRepository (3 usages)
- `findByPostIdWithUser` (lines 475, 514, 549) - For notification purposes

### SubjectRepository (3 usages)
- `findById` (lines 261, 1015, 1105) - Subject validation and connection

### BookRepository (2 usages)
- `findById` (lines 1001, 1128) - Book validation and connection

### ReportRepository (2 usages)
- `countUniqueReportersForTarget` (lines 447, 610) - Report count checking

### ChatRoomRepository
- No direct usage found in PostService

## Recommendations for Refactoring

1. **Start with read-only methods** to minimize risk
2. **Maintain exact method signatures** for backward compatibility
3. **Test with the same parameter combinations** used in PostService
4. **Pay special attention to**:
   - Fetch Join behaviors
   - Null parameter handling
   - BLOCKED status filtering rules
   - Pagination and sorting
5. **Consider creating a compatibility test suite** that exercises all these methods exactly as PostService uses them
6. **Be aware of transactional boundaries** - Some methods are called within `@Transactional` contexts
7. **Consider the async notification calls** that depend on post data consistency