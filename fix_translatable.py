import os
import xml.etree.ElementTree as ET

translations = {
    "ar": {"language": "لغة التطبيق", "select_language": "اختر لغة التطبيق", "section_interface": "الواجهة والثيم"},
    "de": {"language": "Sprache", "select_language": "Sprache auswählen", "section_interface": "BENUTZEROBERFLÄCHE & THEMA"},
    "es": {"language": "Idioma", "select_language": "Seleccionar idioma", "section_interface": "INTERFAZ Y TEMA"},
    "fr": {"language": "Langue", "select_language": "Sélectionner la langue", "section_interface": "INTERFACE & THÈME"},
    "ja": {"language": "言語", "select_language": "言語を選択", "section_interface": "UI とテーマ"},
    "pt": {"language": "Idioma", "select_language": "Selecionar idioma", "section_interface": "INTERFACE & TEMA"},
    "ru": {"language": "Язык", "select_language": "Выберите язык", "section_interface": "ИНТЕРФЕЙС И ТЕМА"},
    "tr": {"language": "Dil", "select_language": "Dil Seçin", "section_interface": "ARAYÜZ & TEMA"},
    "zh": {"language": "语言", "select_language": "选择语言", "section_interface": "界面与主题"}
}

res_dir = r"C:\Download\VPN\VPN\app\src\main\res"

for lang, mapping in translations.items():
    path = os.path.join(res_dir, f"values-{lang}", "strings.xml")
    if os.path.exists(path):
        tree = ET.parse(path)
        root = tree.getroot()
        for elem in root.findall('string'):
            name = elem.attrib.get('name')
            if 'translatable' in elem.attrib:
                del elem.attrib['translatable']
            if name in mapping:
                elem.text = mapping[name]
        tree.write(path, encoding="utf-8", xml_declaration=True)
        print(f"Fixed {lang}")

print("Fix translatable completed!")
