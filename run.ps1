# Compile
if (-not (Test-Path "out")) {
    New-Item -ItemType Directory -Path "out" | Out-Null
}
javac -cp "flatlaf-3.2.jar" -d out src/PairProgrammer.java

# Run
if ($?) {
    java -cp "out;flatlaf-3.2.jar" PairProgrammer
} else {
    Write-Host "Compilation failed." -ForegroundColor Red
}