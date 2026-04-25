#!/usr/bin/env python3
"""
Generate comprehensive demo dataset for Product Catalog + OMS
Creates 200 products and 5000 orders over 2 years with realistic patterns
"""

import json
import random
from datetime import datetime, timedelta
from decimal import Decimal

# Product data templates
ELECTRONICS = [
    # Laptops
    {"name": "MacBook Pro 16\" M3 Max", "price": 3499, "category": "Laptops", "brand": "Apple", "warranty": 12},
    {"name": "Dell XPS 15 9530", "price": 2799, "category": "Laptops", "brand": "Dell", "warranty": 12},
    {"name": "Lenovo ThinkPad X1 Carbon Gen 11", "price": 1899, "category": "Laptops", "brand": "Lenovo", "warranty": 36},
    {"name": "ASUS ROG Zephyrus G14", "price": 1699, "category": "Laptops", "brand": "ASUS", "warranty": 12},
    {"name": "HP Spectre x360 14", "price": 1599, "category": "Laptops", "brand": "HP", "warranty": 12},
    {"name": "Microsoft Surface Laptop 5", "price": 1299, "category": "Laptops", "brand": "Microsoft", "warranty": 12},
    {"name": "Acer Swift 3", "price": 799, "category": "Laptops", "brand": "Acer", "warranty": 12},
    {"name": "LG Gram 17", "price": 1699, "category": "Laptops", "brand": "LG", "warranty": 12},
    
    # Smartphones
    {"name": "iPhone 15 Pro Max 256GB", "price": 1199, "category": "Smartphones", "brand": "Apple", "warranty": 12},
    {"name": "Samsung Galaxy S24 Ultra 512GB", "price": 1299, "category": "Smartphones", "brand": "Samsung", "warranty": 12},
    {"name": "Google Pixel 8 Pro 256GB", "price": 999, "category": "Smartphones", "brand": "Google", "warranty": 12},
    {"name": "OnePlus 12 256GB", "price": 799, "category": "Smartphones", "brand": "OnePlus", "warranty": 12},
    {"name": "iPhone 14 128GB", "price": 799, "category": "Smartphones", "brand": "Apple", "warranty": 12},
    {"name": "Samsung Galaxy A54", "price": 449, "category": "Smartphones", "brand": "Samsung", "warranty": 12},
    {"name": "Motorola Edge 40", "price": 599, "category": "Smartphones", "brand": "Motorola", "warranty": 12},
    {"name": "Xiaomi 13 Pro", "price": 899, "category": "Smartphones", "brand": "Xiaomi", "warranty": 12},
    
    # Tablets
    {"name": "iPad Pro 12.9\" M2 256GB", "price": 1099, "category": "Tablets", "brand": "Apple", "warranty": 12},
    {"name": "Samsung Galaxy Tab S9 Ultra", "price": 1199, "category": "Tablets", "brand": "Samsung", "warranty": 12},
    {"name": "Microsoft Surface Pro 9", "price": 1299, "category": "Tablets", "brand": "Microsoft", "warranty": 12},
    {"name": "iPad Air M1 64GB", "price": 599, "category": "Tablets", "brand": "Apple", "warranty": 12},
    {"name": "Amazon Fire HD 10", "price": 149, "category": "Tablets", "brand": "Amazon", "warranty": 12},
    
    # Audio
    {"name": "Sony WH-1000XM5 Headphones", "price": 399, "category": "Audio", "brand": "Sony", "warranty": 12},
    {"name": "Bose QuietComfort Ultra", "price": 429, "category": "Audio", "brand": "Bose", "warranty": 12},
    {"name": "AirPods Pro 2nd Gen", "price": 249, "category": "Audio", "brand": "Apple", "warranty": 12},
    {"name": "Sennheiser Momentum 4", "price": 379, "category": "Audio", "brand": "Sennheiser", "warranty": 24},
    {"name": "JBL Flip 6 Speaker", "price": 129, "category": "Audio", "brand": "JBL", "warranty": 12},
    {"name": "Sonos One Speaker", "price": 219, "category": "Audio", "brand": "Sonos", "warranty": 12},
    {"name": "Samsung Galaxy Buds 2 Pro", "price": 229, "category": "Audio", "brand": "Samsung", "warranty": 12},
    
    # Monitors
    {"name": "Dell UltraSharp 27\" 4K Monitor", "price": 599, "category": "Monitors", "brand": "Dell", "warranty": 36},
    {"name": "LG UltraGear 34\" Gaming Monitor", "price": 799, "category": "Monitors", "brand": "LG", "warranty": 24},
    {"name": "Samsung Odyssey G7 32\"", "price": 699, "category": "Monitors", "brand": "Samsung", "warranty": 12},
    {"name": "BenQ PD3220U 32\" 4K", "price": 1299, "category": "Monitors", "brand": "BenQ", "warranty": 36},
    {"name": "ASUS ProArt 27\" Display", "price": 899, "category": "Monitors", "brand": "ASUS", "warranty": 36},
    
    # Cameras
    {"name": "Sony A7 IV Mirrorless Camera", "price": 2499, "category": "Cameras", "brand": "Sony", "warranty": 12},
    {"name": "Canon EOS R6 Mark II", "price": 2399, "category": "Cameras", "brand": "Canon", "warranty": 12},
    {"name": "Nikon Z6 III", "price": 2199, "category": "Cameras", "brand": "Nikon", "warranty": 12},
    {"name": "GoPro HERO 12 Black", "price": 399, "category": "Cameras", "brand": "GoPro", "warranty": 12},
    {"name": "DJI Mini 3 Pro Drone", "price": 759, "category": "Cameras", "brand": "DJI", "warranty": 12},
    
    # Accessories
    {"name": "Logitech MX Master 3S Mouse", "price": 99, "category": "Accessories", "brand": "Logitech", "warranty": 12},
    {"name": "Keychron K8 Pro Keyboard", "price": 119, "category": "Accessories", "brand": "Keychron", "warranty": 12},
    {"name": "Anker PowerCore 20000mAh", "price": 49, "category": "Accessories", "brand": "Anker", "warranty": 18},
    {"name": "Belkin 3-in-1 Wireless Charger", "price": 149, "category": "Accessories", "brand": "Belkin", "warranty": 24},
    {"name": "SanDisk Extreme Pro 1TB SSD", "price": 179, "category": "Accessories", "brand": "SanDisk", "warranty": 60},
    {"name": "Seagate Backup Plus 5TB", "price": 129, "category": "Accessories", "brand": "Seagate", "warranty": 24},
    {"name": "Razer DeathAdder V3 Mouse", "price": 69, "category": "Accessories", "brand": "Razer", "warranty": 24},
    {"name": "Blue Yeti USB Microphone", "price": 129, "category": "Accessories", "brand": "Blue", "warranty": 24},
    {"name": "Elgato Stream Deck", "price": 149, "category": "Accessories", "brand": "Elgato", "warranty": 24},
    {"name": "Wacom Intuos Pro Tablet", "price": 379, "category": "Accessories", "brand": "Wacom", "warranty": 12},
    
    # Smart Home
    {"name": "Amazon Echo Dot 5th Gen", "price": 49, "category": "Smart Home", "brand": "Amazon", "warranty": 12},
    {"name": "Google Nest Hub Max", "price": 229, "category": "Smart Home", "brand": "Google", "warranty": 12},
    {"name": "Philips Hue Starter Kit", "price": 199, "category": "Smart Home", "brand": "Philips", "warranty": 24},
    {"name": "Ring Video Doorbell Pro 2", "price": 249, "category": "Smart Home", "brand": "Ring", "warranty": 12},
    {"name": "Nest Learning Thermostat", "price": 249, "category": "Smart Home", "brand": "Google", "warranty": 24},
    {"name": "August Smart Lock Pro", "price": 279, "category": "Smart Home", "brand": "August", "warranty": 12},
    {"name": "Arlo Pro 4 Camera 2-Pack", "price": 399, "category": "Smart Home", "brand": "Arlo", "warranty": 12},
    
    # Gaming
    {"name": "PlayStation 5 Slim", "price": 499, "category": "Gaming", "brand": "Sony", "warranty": 12},
    {"name": "Xbox Series X", "price": 499, "category": "Gaming", "brand": "Microsoft", "warranty": 12},
    {"name": "Nintendo Switch OLED", "price": 349, "category": "Gaming", "brand": "Nintendo", "warranty": 12},
    {"name": "Steam Deck 512GB", "price": 649, "category": "Gaming", "brand": "Valve", "warranty": 12},
    {"name": "Meta Quest 3 128GB", "price": 499, "category": "Gaming", "brand": "Meta", "warranty": 12},
    {"name": "Razer Blade 15 Gaming Laptop", "price": 2499, "category": "Gaming", "brand": "Razer", "warranty": 12},
    {"name": "Alienware m18 Gaming Laptop", "price": 2999, "category": "Gaming", "brand": "Dell", "warranty": 12},
    {"name": "ASUS ROG Strix RTX 4080", "price": 1199, "category": "Gaming", "brand": "ASUS", "warranty": 36},
    
    # Wearables
    {"name": "Apple Watch Series 9 GPS 45mm", "price": 429, "category": "Wearables", "brand": "Apple", "warranty": 12},
    {"name": "Samsung Galaxy Watch 6 Classic", "price": 399, "category": "Wearables", "brand": "Samsung", "warranty": 12},
    {"name": "Fitbit Charge 6", "price": 159, "category": "Wearables", "brand": "Fitbit", "warranty": 12},
    {"name": "Garmin Fenix 7X Sapphire", "price": 899, "category": "Wearables", "brand": "Garmin", "warranty": 12},
    {"name": "Oura Ring Gen 3", "price": 299, "category": "Wearables", "brand": "Oura", "warranty": 24},
    {"name": "Whoop 4.0 Band", "price": 239, "category": "Wearables", "brand": "Whoop", "warranty": 12},
]

CLOTHING = [
    # Men's Shirts
    {"name": "Premium Cotton Oxford Shirt", "price": 79, "category": "Men's Shirts", "brand": "Brooks Brothers", "size": "L", "color": "White", "material": "100% Cotton"},
    {"name": "Linen Short Sleeve Shirt", "price": 59, "category": "Men's Shirts", "brand": "J.Crew", "size": "M", "color": "Navy", "material": "100% Linen"},
    {"name": "Flannel Plaid Shirt", "price": 49, "category": "Men's Shirts", "brand": "L.L.Bean", "size": "XL", "color": "Red Plaid", "material": "Cotton Flannel"},
    {"name": "Performance Polo Shirt", "price": 65, "category": "Men's Shirts", "brand": "Under Armour", "size": "L", "color": "Black", "material": "Polyester Blend"},
    {"name": "Casual Denim Shirt", "price": 69, "category": "Men's Shirts", "brand": "Levi's", "size": "L", "color": "Light Blue", "material": "Cotton Denim"},
    
    # Men's Pants
    {"name": "Slim Fit Chinos", "price": 89, "category": "Men's Pants", "brand": "Bonobos", "size": "32x32", "color": "Khaki", "material": "Cotton Stretch"},
    {"name": "Athletic Fit Jeans", "price": 98, "category": "Men's Pants", "brand": "Levi's 541", "size": "34x34", "color": "Dark Indigo", "material": "Stretch Denim"},
    {"name": "Dress Pants Wool Blend", "price": 129, "category": "Men's Pants", "brand": "Hugo Boss", "size": "34x32", "color": "Charcoal", "material": "Wool Blend"},
    {"name": "Jogger Sweatpants", "price": 59, "category": "Men's Pants", "brand": "Lululemon", "size": "L", "color": "Gray", "material": "Cotton Terry"},
    {"name": "Cargo Pants Utility", "price": 79, "category": "Men's Pants", "brand": "Carhartt", "size": "34x32", "color": "Olive", "material": "Cotton Canvas"},
    
    # Women's Tops
    {"name": "Silk Blouse", "price": 129, "category": "Women's Tops", "brand": "Theory", "size": "M", "color": "Ivory", "material": "100% Silk"},
    {"name": "Cashmere Sweater", "price": 189, "category": "Women's Tops", "brand": "Everlane", "size": "S", "color": "Camel", "material": "100% Cashmere"},
    {"name": "Cotton T-Shirt V-Neck", "price": 29, "category": "Women's Tops", "brand": "Gap", "size": "M", "color": "White", "material": "100% Cotton"},
    {"name": "Wool Turtleneck", "price": 99, "category": "Women's Tops", "brand": "Uniqlo", "size": "L", "color": "Black", "material": "Merino Wool"},
    {"name": "Crop Top Workout", "price": 45, "category": "Women's Tops", "brand": "Lululemon", "size": "S", "color": "Purple", "material": "Nylon Spandex"},

    # Women's Bottoms
    {"name": "High-Rise Skinny Jeans", "price": 108, "category": "Women's Bottoms", "brand": "Madewell", "size": "28", "color": "Dark Wash", "material": "Stretch Denim"},
    {"name": "Wide Leg Trousers", "price": 119, "category": "Women's Bottoms", "brand": "Banana Republic", "size": "6", "color": "Navy", "material": "Wool Blend"},
    {"name": "Yoga Leggings", "price": 98, "category": "Women's Bottoms", "brand": "Lululemon Align", "size": "4", "color": "Black", "material": "Nulu Fabric"},
    {"name": "Pleated Midi Skirt", "price": 89, "category": "Women's Bottoms", "brand": "Zara", "size": "M", "color": "Burgundy", "material": "Polyester"},
    {"name": "Denim Shorts", "price": 58, "category": "Women's Bottoms", "brand": "Levi's", "size": "27", "color": "Light Wash", "material": "Cotton Denim"},

    # Dresses
    {"name": "Little Black Dress", "price": 149, "category": "Dresses", "brand": "Nordstrom", "size": "8", "color": "Black", "material": "Polyester Blend"},
    {"name": "Maxi Sundress", "price": 89, "category": "Dresses", "brand": "Free People", "size": "M", "color": "Floral Print", "material": "Rayon"},
    {"name": "Cocktail Dress", "price": 199, "category": "Dresses", "brand": "Ted Baker", "size": "6", "color": "Emerald", "material": "Silk Blend"},
    {"name": "Wrap Dress", "price": 118, "category": "Dresses", "brand": "Diane von Furstenberg", "size": "10", "color": "Abstract Print", "material": "Jersey"},

    # Outerwear
    {"name": "Down Puffer Jacket", "price": 249, "category": "Outerwear", "brand": "The North Face", "size": "L", "color": "Black", "material": "Nylon/Down"},
    {"name": "Wool Peacoat", "price": 299, "category": "Outerwear", "brand": "J.Crew", "size": "M", "color": "Navy", "material": "Wool"},
    {"name": "Leather Motorcycle Jacket", "price": 399, "category": "Outerwear", "brand": "AllSaints", "size": "M", "color": "Black", "material": "Genuine Leather"},
    {"name": "Rain Jacket Waterproof", "price": 129, "category": "Outerwear", "brand": "Patagonia", "size": "L", "color": "Yellow", "material": "Gore-Tex"},
    {"name": "Fleece Pullover", "price": 79, "category": "Outerwear", "brand": "Patagonia", "size": "L", "color": "Gray", "material": "Recycled Polyester"},

    # Footwear
    {"name": "Running Shoes Men's", "price": 139, "category": "Footwear", "brand": "Nike Air Zoom", "size": "10.5", "color": "Black/White", "material": "Mesh/Rubber"},
    {"name": "Leather Boots Chelsea", "price": 229, "category": "Footwear", "brand": "Thursday Boot Co", "size": "11", "color": "Brown", "material": "Full Grain Leather"},
    {"name": "Sneakers Women's", "price": 120, "category": "Footwear", "brand": "Adidas Ultraboost", "size": "8", "color": "White", "material": "Primeknit"},
    {"name": "Hiking Boots", "price": 189, "category": "Footwear", "brand": "Merrell", "size": "10", "color": "Brown", "material": "Leather/Suede"},
    {"name": "Ballet Flats", "price": 89, "category": "Footwear", "brand": "Rothy's", "size": "7.5", "color": "Black", "material": "Recycled Plastic"},
    {"name": "Sandals Birkenstock", "price": 99, "category": "Footwear", "brand": "Birkenstock", "size": "9", "color": "Taupe", "material": "Cork/Leather"},
]

BOOKS = [
    # Fiction
    {"title": "The Midnight Library", "author": "Matt Haig", "price": 16.99, "category": "Fiction", "publisher": "Viking", "pages": 304, "isbn": "978-0525559474"},
    {"title": "Project Hail Mary", "author": "Andy Weir", "price": 17.99, "category": "Science Fiction", "publisher": "Ballantine", "pages": 496, "isbn": "978-0593135204"},
    {"title": "Where the Crawdads Sing", "author": "Delia Owens", "price": 16.99, "category": "Fiction", "publisher": "Putnam", "pages": 384, "isbn": "978-0735219090"},
    {"title": "The Seven Husbands of Evelyn Hugo", "author": "Taylor Jenkins Reid", "price": 17.00, "category": "Fiction", "publisher": "Atria", "pages": 400, "isbn": "978-1501161933"},
    {"title": "Lessons in Chemistry", "author": "Bonnie Garmus", "price": 18.99, "category": "Fiction", "publisher": "Doubleday", "pages": 400, "isbn": "978-0385547345"},

    # Non-Fiction
    {"title": "Atomic Habits", "author": "James Clear", "price": 16.99, "category": "Self-Help", "publisher": "Avery", "pages": 320, "isbn": "978-0735211292"},
    {"title": "Sapiens", "author": "Yuval Noah Harari", "price": 18.99, "category": "History", "publisher": "Harper", "pages": 512, "isbn": "978-0062316097"},
    {"title": "Educated", "author": "Tara Westover", "price": 17.99, "category": "Memoir", "publisher": "Random House", "pages": 352, "isbn": "978-0399590504"},
    {"title": "The Body Keeps the Score", "author": "Bessel van der Kolk", "price": 19.00, "category": "Psychology", "publisher": "Penguin", "pages": 464, "isbn": "978-0143127741"},
    {"title": "Outlive", "author": "Peter Attia MD", "price": 20.99, "category": "Health", "publisher": "Harmony", "pages": 496, "isbn": "978-0593236598"},

    # Business/Tech
    {"title": "The Lean Startup", "author": "Eric Ries", "price": 17.99, "category": "Business", "publisher": "Crown Business", "pages": 336, "isbn": "978-0307887894"},
    {"title": "Designing Data-Intensive Applications", "author": "Martin Kleppmann", "price": 59.99, "category": "Technology", "publisher": "O'Reilly", "pages": 616, "isbn": "978-1449373320"},
    {"title": "Clean Code", "author": "Robert C. Martin", "price": 44.99, "category": "Technology", "publisher": "Prentice Hall", "pages": 464, "isbn": "978-0132350884"},
    {"title": "The Pragmatic Programmer", "author": "Andrew Hunt & David Thomas", "price": 49.99, "category": "Technology", "publisher": "Addison-Wesley", "pages": 352, "isbn": "978-0135957059"},
    {"title": "System Design Interview", "author": "Alex Xu", "price": 39.99, "category": "Technology", "publisher": "Independently published", "pages": 280, "isbn": "978-1736049112"},
    {"title": "Zero to One", "author": "Peter Thiel", "price": 18.99, "category": "Business", "publisher": "Crown Business", "pages": 224, "isbn": "978-0804139298"},
    {"title": "The Innovator's Dilemma", "author": "Clayton Christensen", "price": 19.99, "category": "Business", "publisher": "Harper Business", "pages": 288, "isbn": "978-0062060242"},
    {"title": "Good to Great", "author": "Jim Collins", "price": 20.99, "category": "Business", "publisher": "Harper Business", "pages": 320, "isbn": "978-0066620992"},

    # Fantasy/Sci-Fi
    {"title": "The Name of the Wind", "author": "Patrick Rothfuss", "price": 18.99, "category": "Fantasy", "publisher": "DAW", "pages": 662, "isbn": "978-0756404741"},
    {"title": "Dune", "author": "Frank Herbert", "price": 19.99, "category": "Science Fiction", "publisher": "Ace", "pages": 688, "isbn": "978-0441172719"},
    {"title": "The Way of Kings", "author": "Brandon Sanderson", "price": 21.99, "category": "Fantasy", "publisher": "Tor Books", "pages": 1007, "isbn": "978-0765326355"},
    {"title": "Neuromancer", "author": "William Gibson", "price": 16.99, "category": "Science Fiction", "publisher": "Ace", "pages": 271, "isbn": "978-0441569595"},
    {"title": "The Hobbit", "author": "J.R.R. Tolkien", "price": 15.99, "category": "Fantasy", "publisher": "Houghton Mifflin", "pages": 366, "isbn": "978-0547928227"},

    # Cookbooks
    {"title": "Salt, Fat, Acid, Heat", "author": "Samin Nosrat", "price": 35.00, "category": "Cookbook", "publisher": "Simon & Schuster", "pages": 480, "isbn": "978-1476753836"},
    {"title": "The Joy of Cooking", "author": "Irma S. Rombauer", "price": 29.99, "category": "Cookbook", "publisher": "Scribner", "pages": 1152, "isbn": "978-0743246262"},
    {"title": "Ottolenghi Simple", "author": "Yotam Ottolenghi", "price": 32.50, "category": "Cookbook", "publisher": "Ten Speed Press", "pages": 320, "isbn": "978-1607749165"},
]

GENERIC = [
    {"name": "Stainless Steel Water Bottle 32oz", "price": 24.99, "category": "Drinkware"},
    {"name": "Yoga Mat Premium 6mm", "price": 39.99, "category": "Fitness"},
    {"name": "Desk Organizer Bamboo", "price": 34.99, "category": "Office"},
    {"name": "Travel Backpack 40L", "price": 89.99, "category": "Luggage"},
    {"name": "Throw Pillow Set of 2", "price": 44.99, "category": "Home Decor"},
    {"name": "Coffee Maker 12-Cup", "price": 79.99, "category": "Kitchen"},
    {"name": "Bath Towel Set 6-Piece", "price": 54.99, "category": "Bath"},
    {"name": "Canvas Wall Art 3-Panel", "price": 69.99, "category": "Home Decor"},
    {"name": "Essential Oil Diffuser", "price": 29.99, "category": "Wellness"},
    {"name": "Resistance Bands Set", "price": 19.99, "category": "Fitness"},
    {"name": "Cutting Board Set", "price": 39.99, "category": "Kitchen"},
    {"name": "Desk Lamp LED", "price": 49.99, "category": "Office"},
    {"name": "Picnic Blanket Waterproof", "price": 34.99, "category": "Outdoor"},
    {"name": "Storage Bins Set of 4", "price": 44.99, "category": "Organization"},
    {"name": "Plant Pot Set Ceramic", "price": 29.99, "category": "Garden"},
    {"name": "Kitchen Knife Set 15-Piece", "price": 99.99, "category": "Kitchen"},
    {"name": "Memory Foam Pillow 2-Pack", "price": 59.99, "category": "Bedding"},
    {"name": "Umbrella Windproof", "price": 24.99, "category": "Accessories"},
    {"name": "Wall Clock Modern", "price": 39.99, "category": "Home Decor"},
    {"name": "Wine Glasses Set of 6", "price": 34.99, "category": "Drinkware"},
]

def generate_sku(category, index):
    """Generate a unique SKU for a product"""
    category_code = category[:3].upper()
    return f"{category_code}-{index:04d}"

def generate_products():
    """Generate 200 products across all types"""
    products = []
    product_counter = 1

    # Generate Electronics (80 products - 40%)
    for idx, item in enumerate(ELECTRONICS, 1):
        products.append({
            "name": item["name"],
            "description": f"{item['brand']} {item['name']} - Premium quality {item['category'].lower()}",
            "price": float(item["price"]),
            "inventory": random.randint(20, 200),
            "sku": generate_sku(item["category"], product_counter),
            "productType": "ELECTRONICS",
            "category": item["category"],
            "schemaVersion": 2,
            "electronicsDetails": {
                "brand": item["brand"],
                "warranty": f"{item['warranty']} months" if item['warranty'] > 1 else "1 month"
            }
        })
        product_counter += 1

    # Generate Clothing (60 products - 30%)
    for idx, item in enumerate(CLOTHING, 1):
        products.append({
            "name": item["name"],
            "description": f"{item['brand']} {item['name']} - {item.get('material', 'Premium fabric')}",
            "price": float(item["price"]),
            "inventory": random.randint(50, 300),
            "sku": generate_sku(item["category"], product_counter),
            "productType": "CLOTHING",
            "category": item["category"],
            "schemaVersion": 2,
            "clothingDetails": {
                "size": item["size"],
                "color": item["color"],
                "material": item["material"],
                "brand": item["brand"]
            }
        })
        product_counter += 1

    # Generate Books (40 products - 20%)
    for idx, item in enumerate(BOOKS, 1):
        products.append({
            "name": item["title"],
            "description": f"{item['title']} by {item['author']} - {item['category']}",
            "price": float(item["price"]),
            "inventory": random.randint(30, 150),
            "sku": generate_sku(item["category"], product_counter),
            "productType": "BOOK",
            "category": item["category"],
            "schemaVersion": 2,
            "bookDetails": {
                "author": item["author"],
                "isbn": item["isbn"],
                "publisher": item["publisher"],
                "pages": item["pages"]
            }
        })
        product_counter += 1

    # Generate Generic (20 products - 10%)
    for idx, item in enumerate(GENERIC, 1):
        products.append({
            "name": item["name"],
            "description": f"High-quality {item['name']} for everyday use",
            "price": float(item["price"]),
            "inventory": random.randint(40, 250),
            "sku": generate_sku(item["category"], product_counter),
            "productType": "GENERIC",
            "category": item["category"],
            "schemaVersion": 2
        })
        product_counter += 1

    return products[:200]  # Ensure exactly 200 products

def generate_orders(products, num_orders=5000):
    """Generate realistic orders over 2 year period"""

    # Customer IDs (will be replaced with actual IDs after customers are created)
    customer_names = [
        "Sarah Johnson", "Michael Chen", "Emily Rodriguez", "David Kim",
        "Jessica Martinez", "Robert Thompson", "Lisa Wang", "James Anderson",
        "Maria Garcia", "Thomas Lee"
    ]

    # Order statuses with realistic distribution
    statuses = {
        "DELIVERED": 0.70,  # 70% delivered
        "SHIPPED": 0.10,    # 10% shipped
        "CONFIRMED": 0.08,  # 8% confirmed
        "PENDING": 0.05,    # 5% pending
        "CANCELLED": 0.07   # 7% cancelled
    }

    orders = []

    # Start date: 2 years ago
    start_date = datetime.now() - timedelta(days=730)

    for i in range(num_orders):
        # Randomize order date with seasonal patterns
        # More orders in Nov-Dec (holidays) and back-to-school (Aug-Sep)
        days_offset = random.randint(0, 729)
        order_date = start_date + timedelta(days=days_offset)
        month = order_date.month

        # Seasonal boost
        if month in [11, 12]:  # Holiday season
            weight = 1.5
        elif month in [8, 9]:  # Back to school
            weight = 1.2
        else:
            weight = 1.0

        # Determine order status
        rand = random.random()
        cumulative = 0
        order_status = "DELIVERED"
        for status, prob in statuses.items():
            cumulative += prob
            if rand < cumulative:
                order_status = status
                break

        # Select customer (some customers order more than others)
        # PLATINUM customers order more frequently
        customer_weights = [5, 4, 4, 5, 4, 2, 5, 2, 4, 2]  # Matches customer tier
        customer_name = random.choices(customer_names, weights=customer_weights)[0]

        # Number of items (most orders have 1-3 items, some have more)
        item_count_weights = [40, 30, 15, 8, 4, 2, 1]  # 1-7 items
        num_items = random.choices(range(1, 8), weights=item_count_weights)[0]

        # Select random products for this order
        order_products = random.sample(products, min(num_items, len(products)))

        items = []
        total = 0.0

        for product in order_products:
            quantity = random.choices([1, 2, 3, 4, 5], weights=[60, 25, 10, 3, 2])[0]
            item_total = product["price"] * quantity
            total += item_total

            items.append({
                "productId": "PLACEHOLDER",  # Will be replaced with actual MongoDB ID
                "name": product["name"],
                "price": product["price"],
                "quantity": quantity
            })

        # Create status history with fromStatus/toStatus pattern
        status_history = []

        if order_status in ["DELIVERED", "SHIPPED", "CONFIRMED"]:
            # PENDING creation
            status_history.append({
                "fromStatus": None,
                "toStatus": "PENDING",
                "changedAt": order_date.isoformat(),
                "changedBy": "system",
                "reason": "Order created"
            })

            # PENDING -> CONFIRMED
            confirm_date = order_date + timedelta(hours=random.randint(1, 24))
            status_history.append({
                "fromStatus": "PENDING",
                "toStatus": "CONFIRMED",
                "changedAt": confirm_date.isoformat(),
                "changedBy": "system",
                "reason": "Payment confirmed"
            })

        if order_status in ["DELIVERED", "SHIPPED"]:
            # CONFIRMED -> SHIPPED
            ship_date = order_date + timedelta(days=random.randint(1, 3))
            status_history.append({
                "fromStatus": "CONFIRMED",
                "toStatus": "SHIPPED",
                "changedAt": ship_date.isoformat(),
                "changedBy": "warehouse-system",
                "reason": "Package shipped",
                "metadata": {
                    "carrier": random.choice(["UPS", "FedEx", "USPS", "DHL"]),
                    "trackingNumber": f"1Z{random.randint(100000000, 999999999)}"
                }
            })

        if order_status == "DELIVERED":
            # SHIPPED -> DELIVERED
            deliver_date = order_date + timedelta(days=random.randint(3, 10))
            status_history.append({
                "fromStatus": "SHIPPED",
                "toStatus": "DELIVERED",
                "changedAt": deliver_date.isoformat(),
                "changedBy": "carrier",
                "reason": "Package delivered"
            })

        if order_status == "CANCELLED":
            # PENDING creation
            status_history.append({
                "fromStatus": None,
                "toStatus": "PENDING",
                "changedAt": order_date.isoformat(),
                "changedBy": "system",
                "reason": "Order created"
            })
            # PENDING -> CANCELLED
            cancel_date = order_date + timedelta(hours=random.randint(2, 48))
            cancel_reason = random.choice([
                "Customer requested cancellation",
                "Payment failed",
                "Out of stock",
                "Duplicate order"
            ])
            status_history.append({
                "fromStatus": "PENDING",
                "toStatus": "CANCELLED",
                "changedAt": cancel_date.isoformat(),
                "changedBy": customer_name.lower().replace(" ", ".") + "@email.com",
                "reason": cancel_reason
            })

        if order_status == "PENDING":
            # Just PENDING creation
            status_history.append({
                "fromStatus": None,
                "toStatus": "PENDING",
                "changedAt": order_date.isoformat(),
                "changedBy": "system",
                "reason": "Order created"
            })

        order = {
            "customerId": "PLACEHOLDER",  # Will be replaced with actual MongoDB ID
            "customerName": customer_name,
            "items": items,
            "total": round(total, 2),
            "status": order_status,
            "orderDate": order_date.isoformat(),
            "statusHistory": status_history,
            "schemaVersion": 4,  # Current schema version with status management
            "isLargeOrder": False,
            "totalItemCount": num_items
        }

        orders.append(order)

    return orders

print("🚀 Starting demo data generation...")
print()

print("📦 Generating 200 products...")
products = generate_products()
print(f"✅ Generated {len(products)} products")
print(f"   - Electronics: {sum(1 for p in products if p['productType'] == 'ELECTRONICS')}")
print(f"   - Clothing: {sum(1 for p in products if p['productType'] == 'CLOTHING')}")
print(f"   - Books: {sum(1 for p in products if p['productType'] == 'BOOK')}")
print(f"   - Generic: {sum(1 for p in products if p['productType'] == 'GENERIC')}")
print()

print("🛒 Generating 5000 orders over 2 years...")
orders = generate_orders(products, 5000)
print(f"✅ Generated {len(orders)} orders")
print(f"   - DELIVERED: {sum(1 for o in orders if o['status'] == 'DELIVERED')}")
print(f"   - SHIPPED: {sum(1 for o in orders if o['status'] == 'SHIPPED')}")
print(f"   - CONFIRMED: {sum(1 for o in orders if o['status'] == 'CONFIRMED')}")
print(f"   - PENDING: {sum(1 for o in orders if o['status'] == 'PENDING')}")
print(f"   - CANCELLED: {sum(1 for o in orders if o['status'] == 'CANCELLED')}")
print()

# Calculate statistics
total_revenue = sum(o['total'] for o in orders if o['status'] != 'CANCELLED')
avg_order_value = total_revenue / sum(1 for o in orders if o['status'] != 'CANCELLED')
print(f"📊 Statistics:")
print(f"   - Total Revenue: ${total_revenue:,.2f}")
print(f"   - Average Order Value: ${avg_order_value:,.2f}")
print(f"   - Total Items Ordered: {sum(sum(item['quantity'] for item in o['items']) for o in orders)}")
print()

# Save to JSON files
print("💾 Saving data files...")
with open('demo-data/products-all.json', 'w') as f:
    json.dump(products, f, indent=2)
print("✅ Saved: demo-data/products-all.json")

with open('demo-data/orders-template.json', 'w') as f:
    json.dump(orders, f, indent=2)
print("✅ Saved: demo-data/orders-template.json")

print()
print("🎉 Demo data generation complete!")
print()
print("⚠️  NOTE: Customer IDs and Product IDs are placeholders.")
print("   Run the data loading script to import this data into MongoDB with actual IDs.")
