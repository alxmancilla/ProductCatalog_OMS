package com.example.store.validation;

import com.example.store.model.BookDetails;
import com.example.store.model.Product;
import org.springframework.stereotype.Component;

/**
 * Validator for Book products.
 * 
 * Validates that book products have all required fields:
 * - bookDetails must not be null
 * - author is required and not empty
 * - isbn is required and matches ISBN format
 * - pages is required and positive
 * 
 * Example valid book product:
 * {
 *   "type": "Book",
 *   "name": "MongoDB Guide",
 *   "bookDetails": {
 *     "author": "Jane Smith",
 *     "isbn": "978-1234567890",
 *     "pages": 350,
 *     "publisher": "TechBooks Publishing",  // optional
 *     "language": "English"                 // optional
 *   }
 * }
 */
@Component
public class BookValidator implements ProductValidator {
    
    private static final String TYPE = "Book";
    private static final String ISBN_REGEX = "^(97(8|9))?\\d{9}(\\d|X)$"; // Simple ISBN-10/13 validation
    
    @Override
    public void validate(Product product) {
        // Check if bookDetails object exists
        if (product.getBookDetails() == null) {
            throw new ProductValidationException(
                TYPE, 
                "bookDetails", 
                "Book products must have bookDetails object"
            );
        }
        
        BookDetails details = product.getBookDetails();
        
        // Validate author
        if (details.getAuthor() == null || details.getAuthor().trim().isEmpty()) {
            throw new ProductValidationException(
                TYPE, 
                "author", 
                "Author is required for book products (e.g., 'Jane Smith')"
            );
        }
        
        // Validate ISBN
        if (details.getIsbn() == null || details.getIsbn().trim().isEmpty()) {
            throw new ProductValidationException(
                TYPE, 
                "isbn", 
                "ISBN is required for book products (e.g., '978-1234567890')"
            );
        }
        
        // Validate ISBN format (remove hyphens first)
        String isbnClean = details.getIsbn().replaceAll("-", "");
        if (!isbnClean.matches(ISBN_REGEX)) {
            throw new ProductValidationException(
                TYPE, 
                "isbn", 
                "Invalid ISBN format. Must be ISBN-10 (10 digits) or ISBN-13 (13 digits starting with 978/979)"
            );
        }
        
        // Validate pages
        if (details.getPages() == null || details.getPages() <= 0) {
            throw new ProductValidationException(
                TYPE, 
                "pages", 
                "Page count is required and must be greater than zero"
            );
        }
        
        // Optional fields (publisher, language) don't need validation
        // They can be null or empty
    }
    
    @Override
    public String getSupportedType() {
        return TYPE;
    }
}

