from PIL import Image

path = r'C:\Download\VPN\VPN\app\src\main\res\drawable-nodpi\corvus_logo.png'
img = Image.open(path).convert('RGBA')
data = img.getdata()

new_data = []
for item in data:
    if item[3] > 10:
        new_data.append((255, 255, 255, item[3]))
    else:
        new_data.append(item)

img.putdata(new_data)
out_path = r'C:\Download\VPN\VPN\app\src\main\res\drawable-nodpi\corvus_logo_white.png'
img.save(out_path, 'PNG')
print("Saved white logo to", out_path)
