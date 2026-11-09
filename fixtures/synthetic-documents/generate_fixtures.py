import os
from PIL import Image, ImageDraw, ImageFont

out_dir = r"C:\Users\Musid\.gemini\antigravity\scratch\document-scanner\fixtures\synthetic-documents"
os.makedirs(out_dir, exist_ok=True)

# 1. Printed Text Document (A4 ratio: 800 x 1131)
img = Image.new("RGB", (800, 1131), color=(255, 255, 255))
draw = ImageDraw.Draw(img)
draw.rectangle([(20, 20), (780, 1111)], outline=(200, 200, 200), width=1)
draw.text((60, 60), "DOCLENS SCANNER SPECIFICATION", fill=(0, 0, 0))
draw.text((60, 100), "Synthetic Benchmark Fixture - Document #101", fill=(80, 80, 80))
for y in range(160, 1000, 36):
    draw.text((60, y), f"Paragraph line {y//36}: The quick brown fox jumps over the lazy dog repeatedly.", fill=(30, 30, 30))
draw.text((380, 1060), "Page 1 of 10", fill=(100, 100, 100))
img.save(os.path.join(out_dir, "printed_text.jpg"), quality=95)

# 2. Faint Receipt (Thermal Paper Style: 500 x 900)
img2 = Image.new("RGB", (500, 900), color=(248, 248, 246))
draw2 = ImageDraw.Draw(img2)
draw2.text((150, 40), "CITY STORE RECEIPT", fill=(130, 130, 130))
draw2.text((160, 80), "Date: 09-Nov-2026", fill=(140, 140, 140))
draw2.line([(40, 120), (460, 120)], fill=(180, 180, 180), width=1)
items = [
    ("A4 Copy Paper Bundle", "₹ 450.00"),
    ("Document Filing Folder (Pack 5)", "₹ 250.00"),
    ("Gel Ink Pens 0.5mm (Blue)", "₹ 120.00"),
    ("Highlighter Set Pastel", "₹ 180.00"),
    ("Subtotal", "₹ 1,000.00"),
    ("Tax (GST 18%)", "₹ 180.00"),
    ("Total Amount Paid", "₹ 1,180.00")
]
y = 160
for name, price in items:
    draw2.text((50, y), name, fill=(110, 110, 110))
    draw2.text((360, y), price, fill=(110, 110, 110))
    y += 50
draw2.line([(40, y + 20), (460, y + 20)], fill=(180, 180, 180), width=1)
draw2.text((170, y + 60), "Thank you for visiting!", fill=(150, 150, 150))
img2.save(os.path.join(out_dir, "faint_receipt.jpg"), quality=85)

# 3. Table Document
img3 = Image.new("RGB", (800, 1000), color=(255, 255, 255))
draw3 = ImageDraw.Draw(img3)
draw3.text((60, 40), "Quarterly Performance Table", fill=(0, 0, 0))
# Draw table grid
draw3.rectangle([(60, 100), (740, 600)], outline=(0, 0, 0), width=2)
for row_y in range(150, 600, 50):
    draw3.line([(60, row_y), (740, row_y)], fill=(180, 180, 180), width=1)
for col_x in [200, 360, 520]:
    draw3.line([(col_x, 100), (col_x, 600)], fill=(180, 180, 180), width=1)
draw3.text((80, 115), "Metric", fill=(0, 0, 0))
draw3.text((220, 115), "Q1 Baseline", fill=(0, 0, 0))
draw3.text((380, 115), "Q2 Target", fill=(0, 0, 0))
draw3.text((540, 115), "Status", fill=(0, 0, 0))
img3.save(os.path.join(out_dir, "table_document.jpg"), quality=95)

# 4. ID Card Front & Back (Aspect ratio 85.6 x 53.98mm -> 856 x 540)
img_id_f = Image.new("RGB", (856, 540), color=(240, 244, 248))
draw_idf = ImageDraw.Draw(img_id_f)
draw_idf.rectangle([(10, 10), (846, 530)], outline=(30, 60, 120), width=3)
draw_idf.rectangle([(40, 80), (220, 300)], fill=(200, 210, 220), outline=(100, 100, 100))
draw_idf.text((100, 180), "PHOTO", fill=(80, 80, 80))
draw_idf.text((260, 100), "NATIONAL IDENTITY CARD", fill=(20, 40, 100))
draw_idf.text((260, 160), "Name: CITIZEN JANE DOE", fill=(20, 20, 20))
draw_idf.text((260, 210), "ID No: 9876-5432-1000", fill=(20, 20, 20))
draw_idf.text((260, 260), "DOB: 15-MAY-1995", fill=(20, 20, 20))
img_id_f.save(os.path.join(out_dir, "id_card_front.jpg"), quality=95)

img_id_b = Image.new("RGB", (856, 540), color=(240, 244, 248))
draw_idb = ImageDraw.Draw(img_id_b)
draw_idb.rectangle([(10, 10), (846, 530)], outline=(30, 60, 120), width=3)
draw_idb.text((50, 60), "OFFICIAL USE ONLY", fill=(100, 100, 100))
draw_idb.text((50, 120), "Address: 42 Silicon Vista, Tech Corridor, Bangalore 560001", fill=(30, 30, 30))
draw_idb.rectangle([(50, 380), (800, 480)], fill=(220, 220, 220))
draw_idb.text((80, 420), "IDIND987654321000<<<<<<<<<<<<<<<950515F<<<<<<<", fill=(0, 0, 0))
img_id_b.save(os.path.join(out_dir, "id_card_back.jpg"), quality=95)

# 5. Rotated Document (90 degrees)
img_rot = img.rotate(90, expand=True)
img_rot.save(os.path.join(out_dir, "rotated_document.jpg"), quality=95)

print("Generated 6 synthetic fixtures successfully.")
