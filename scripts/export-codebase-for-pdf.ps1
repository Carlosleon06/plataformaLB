<#
.SYNOPSIS
  Genera un único archivo .txt con (casi) todo el código versionado en git, para imprimir / convertir a PDF.

.DESCRIPTION
  - Usa `git ls-files`: solo archivos trackeados (el código del repo, sin node_modules ni target/).
  - Omite .md (documentacion) salvo que uses -IncludeMarkdown. Omite binarios típicos (png, jpg, fuentes, jar…).
  Salida: un .txt con el contenido de cada archivo; una linea con la ruta relativa antes de cada uno (sin cabeceras ni lista de omitidos).

.PARAMETER OutFile
  Ruta completa del .txt de salida.

.PARAMETER IncludeLockFiles
  Incluye package-lock.json si existe (muy largo, una sola línea).

.PARAMETER IncludeMarkdown
  Incluye archivos .md (por defecto se omiten: documentacion, no codigo).

.EXAMPLE
  cd "D:\BDA\Plataforma Leon\scripts"
  .\export-codebase-for-pdf.ps1
#>
param(
  [string]$OutFile = "",
  [switch]$IncludeLockFiles,
  [switch]$IncludeMarkdown
)

$ErrorActionPreference = "Stop"
$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Set-Location $RepoRoot

if (-not (Test-Path (Join-Path $RepoRoot ".git"))) {
  Write-Error "No hay carpeta .git en $RepoRoot. Usa el repo clonado o inicializa git."
}

$binaryExt = @(
  ".png", ".jpg", ".jpeg", ".gif", ".webp", ".ico", ".bmp",
  ".woff", ".woff2", ".ttf", ".eot", ".otf",
  ".pdf", ".zip", ".jar", ".class", ".exe", ".dll", ".so", ".dylib"
)

$maxBytesPerFile = 4MB

function Test-IsProbablyText([string]$Path) {
  $bytes = [System.IO.File]::ReadAllBytes($Path)
  if ($bytes.Length -eq 0) { return $true }
  $sample = [Math]::Min($bytes.Length, 8000)
  for ($i = 0; $i -lt $sample; $i++) {
    if ($bytes[$i] -eq 0) { return $false }
  }
  return $true
}

$relPaths = git -C $RepoRoot ls-files
if ($LASTEXITCODE -ne 0) { Write-Error "git ls-files fallo." }

$included = [System.Collections.Generic.List[string]]::new()

foreach ($rel in $relPaths) {
  if (-not $IncludeLockFiles -and ($rel -eq "package-lock.json" -or $rel -match "[/\\]package-lock\.json$")) {
    continue
  }

  $ext = [System.IO.Path]::GetExtension($rel).ToLowerInvariant()
  if (-not $IncludeMarkdown -and $ext -eq ".md") {
    continue
  }
  if ($binaryExt -contains $ext) {
    continue
  }

  $full = Join-Path $RepoRoot ($rel -replace "/", [IO.Path]::DirectorySeparatorChar)
  if (-not (Test-Path -LiteralPath $full -PathType Leaf)) {
    continue
  }

  $len = (Get-Item -LiteralPath $full).Length
  if ($len -gt $maxBytesPerFile) {
    continue
  }

  if ($ext -eq "" -or $ext -eq ".svg") {
    if (-not (Test-IsProbablyText $full)) {
      continue
    }
  }

  [void]$included.Add($rel)
}

$exportDir = Join-Path $RepoRoot "export"
if (-not (Test-Path $exportDir)) {
  New-Item -ItemType Directory -Path $exportDir | Out-Null
}

if (-not $OutFile) {
  $stamp = Get-Date -Format "yyyyMMdd-HHmmss"
  $OutFile = Join-Path $exportDir "leonbon-codigo-$stamp.txt"
}

$utf8NoBom = New-Object System.Text.UTF8Encoding $false
$sw = New-Object System.IO.StreamWriter($OutFile, $false, $utf8NoBom)

try {
  $first = $true
  foreach ($rel in $included) {
    $full = Join-Path $RepoRoot ($rel -replace "/", [IO.Path]::DirectorySeparatorChar)
    if (-not $first) {
      $sw.WriteLine("")
    }
    $first = $false
    $sw.WriteLine($rel)
    $content = [System.IO.File]::ReadAllText($full, $utf8NoBom)
    $sw.Write($content)
    if (-not $content.EndsWith("`n")) {
      $sw.WriteLine("")
    }
  }
}
finally {
  $sw.Close()
}

$item = Get-Item $OutFile
Write-Host "Listo: $($item.FullName)"
Write-Host "Tamano MB: $([math]::Round($item.Length / 1MB, 2)) | Archivos: $($included.Count)"
