import os
import xml.etree.ElementTree as ET

res_dir = r"C:\Download\VPN\VPN\app\src\main\res"
base_path = os.path.join(res_dir, "values", "strings.xml")

base_tree = ET.parse(base_path)
base_root = base_tree.getroot()
base_strings = {elem.attrib['name']: elem.text for elem in base_root.findall('string') if 'name' in elem.attrib}

print(f"Base keys: {len(base_strings)}")

for folder in os.listdir(res_dir):
    if folder.startswith("values-"):
        path = os.path.join(res_dir, folder, "strings.xml")
        if os.path.exists(path):
            try:
                tree = ET.parse(path)
                root = tree.getroot()
                existing_keys = {elem.attrib['name'] for elem in root.findall('string') if 'name' in elem.attrib}

                added = 0
                for name, text in base_strings.items():
                    if name not in existing_keys:
                        new_elem = ET.Element('string', {'name': name})
                        new_elem.text = text
                        root.append(new_elem)
                        added += 1

                if added > 0:
                    tree.write(path, encoding="utf-8", xml_declaration=True)
                    print(f"Updated {folder}: added {added} missing keys.")
            except Exception as e:
                print(f"Error updating {path}: {e}")

print("String synchronization completed!")
