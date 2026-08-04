import os
from PIL import Image

APP_DIR = os.path.join(os.environ.get("APPDATA", ""), "SandaDataSaver")
LOGO_FILE = os.path.join(APP_DIR, "sanda_logo.png")
OLD_LOGO = os.path.join(APP_DIR, "logo.png")

print(f"Checking APP_DIR: {APP_DIR}")
print(f"LOGO_FILE sanda_logo.png exists: {os.path.exists(LOGO_FILE)} size: {os.path.getsize(LOGO_FILE) if os.path.exists(LOGO_FILE) else 'N/A'}")
print(f"OLD LOGO logo.png exists: {os.path.exists(OLD_LOGO)} size: {os.path.getsize(OLD_LOGO) if os.path.exists(OLD_LOGO) else 'N/A'}")

for path in [LOGO_FILE, OLD_LOGO]:
    if os.path.exists(path):
        print(f"\nTrying to open {path}...")
        try:
            img = Image.open(path)
            print(f"  Opened OK: format={img.format}, size={img.size}, mode={img.mode}")
            img_conv = img.convert("RGBA")
            print(f"  Convert to RGBA OK: {img_conv.size}")
            # Try resize
            img_resized = img_conv.resize((64, 64), Image.LANCZOS)
            print(f"  Resize to 64x64 OK")
        except Exception as e:
            print(f"  FAILED: {e}")
    else:
        print(f"\n{path} not found")

# Also check assets folder in current dir
for cand in ["sanda_logo.png", "assets/sanda_logo.png", "logo.png", "assets/logo.png"]:
    if os.path.exists(cand):
        print(f"\nFound in current dir: {cand} size {os.path.getsize(cand)}")
        try:
            img = Image.open(cand)
            print(f"  Open OK: {img.format} {img.size}")
        except Exception as e:
            print(f"  Open FAILED: {e}")

input("\nPress Enter to exit...")
