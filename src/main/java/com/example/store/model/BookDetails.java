package com.example.store.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Book-specific details for Product.
 * 
 * This is an embedded document (no @Document annotation) that gets stored
 * as a nested object within a Product document.
 * 
 * Example in MongoDB:
 * {
 *   "_id": "prod789",
 *   "type": "Book",
 *   "name": "MongoDB Guide",
 *   "bookDetails": {
 *     "author": "Jane Smith",
 *     "isbn": "978-1234567890",
 *     "pages": 350,
 *     "publisher": "TechBooks Publishing",
 *     "language": "English"
 *   }
 * }
 * 
 * Benefits:
 * - Clear separation of type-specific fields
 * - Easy to validate independently
 * - Can be null for non-book products
 * - Self-documenting - all book fields in one place
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookDetails {
    
    /**
     * Author name(s) (e.g., "Jane Smith", "John Doe & Mary Johnson").
     * Required for book products.
     */
    private String author;
    
    /**
     * International Standard Book Number (e.g., "978-1234567890").
     * Required for book products.
     */
    private String isbn;
    
    /**
     * Number of pages in the book.
     * Required for book products.
     */
    private Integer pages;
    
    /**
     * Publishing company (e.g., "TechBooks Publishing", "O'Reilly Media").
     * Optional - some books may not have publisher info.
     */
    private String publisher;
    
    /**
     * Language of the book content (e.g., "English", "Spanish", "French").
     * Optional - defaults to English if not specified.
     */
    private String language;
}

