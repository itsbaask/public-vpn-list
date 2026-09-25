Add-Type -AssemblyName System.Drawing
$path = "C:\Download\VPN\VPN\app\src\main\res\drawable-nodpi\corvus_logo.png"
$bmp = [System.Drawing.Bitmap]::new($path)
for ($y=0; $y -lt $bmp.Height; $y++) {
    for ($x=0; $x -lt $bmp.Width; $x++) {
        $color = $bmp.GetPixel($x, $y)
        if ($color.A -gt 10) {
            $newColor = [System.Drawing.Color]::FromArgb($color.A, 255, 255, 255)
            $bmp.SetPixel($x, $y, $newColor)
        }
    }
}
$outPath = "C:\Download\VPN\VPN\app\src\main\res\drawable-nodpi\corvus_logo_white.png"
$bmp.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
Write-Host "Success! White logo saved to $outPath"
