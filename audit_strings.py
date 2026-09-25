import os
import xml.etree.ElementTree as ET

res_dir = r"C:\Download\VPN\VPN\app\src\main\res"
base_path = os.path.join(res_dir, "values", "strings.xml")
base_tree = ET.parse(base_path)
base_keys = {elem.attrib['name'] for elem in base_tree.getroot().findall('string') if 'name' in elem.attrib}

print(f"Base strings keys count: {len(base_keys)}")

for folder in os.listdir(res_dir):
    if folder.startswith("values-"):
        path = os.path.join(res_dir, folder, "strings.xml")
        if os.path.exists(path):
            try:
                tree = ET.parse(path)
                keys = {elem.attrib['name'] for elem in tree.getroot().findall('string') if 'name' in elem.attrib}
                missing = base_keys - keys
                print(f"{folder}: {len(keys)} keys. Missing: {len(missing)}")
                if missing:
                    print(f"  Missing keys in {folder}: {missing}")
            except Exception as e:
                print(f"Error parsing {path}: {e}")
