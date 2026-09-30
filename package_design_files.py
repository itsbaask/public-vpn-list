import zipfile
import os
from pathlib import Path

scratch_dir = r"C:\Users\B A S E M\AppData\Local\Google\AndroidStudio2026.1.3\projects\vpn.345b416a\.artifacts\fb22c643-0909-4ee9-a7d5-afa2eb59154f\scratch"
os.makedirs(scratch_dir, exist_ok=True)
zip_path = os.path.join(scratch_dir, "corvus_design_files.zip")

project_main = Path(r"C:\Download\VPN\VPN\app\src\main")

design_files = []

# 1. UI Kotlin Files (Theme, Components, Screens)
ui_dir = project_main / "java" / "com" / "corvus" / "vpn" / "ui"
for path in ui_dir.rglob("*.kt"):
    design_files.append(path)

# 2. Res Resources (values, drawables, mipmaps, colors, themes, styles)
res_dir = project_main / "res"
for path in res_dir.rglob("*"):
    if path.is_file():
        rel = path.relative_to(res_dir)
        first_part = rel.parts[0]
        if first_part.startswith(("values", "drawable", "mipmap")):
            design_files.append(path)

print(f"Total design files collected: {len(design_files)}")

with zipfile.ZipFile(zip_path, "w", zipfile.ZIP_DEFLATED) as z:
    for f in design_files:
        rel_in_zip = f.relative_to(project_main)
        z.write(f, str(rel_in_zip))

print(f"Successfully generated ZIP: {zip_path}")
print(f"ZIP Size: {os.path.getsize(zip_path) / (1024 * 1024):.2f} MB")
