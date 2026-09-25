import os
import xml.etree.ElementTree as ET

res_dir = r"C:\Download\VPN\VPN\app\src\main\res"
base_path = os.path.join(res_dir, "values", "strings.xml")
base_tree = ET.parse(base_path)
base_strings = {elem.attrib['name']: elem.text for elem in base_tree.getroot().findall('string') if 'name' in elem.attrib}

for folder in os.listdir(res_dir):
    if folder.startswith("values-"):
        path = os.path.join(res_dir, folder, "strings.xml")
        if os.path.exists(path):
            tree = ET.parse(path)
            root = tree.getroot()
            untranslated = []
            for elem in root.findall('string'):
                if 'name' in elem.attrib:
                    name = elem.attrib['name']
                    if elem.attrib.get('translatable') != 'false':
                        if name in base_strings and elem.text == base_strings[name]:
                            untranslated.append(name)
            print(f"{folder}: {len(untranslated)} untranslated keys matching English -> {untranslated}")
